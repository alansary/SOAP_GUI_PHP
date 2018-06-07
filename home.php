<meta name="viewport" content="width=device-width version=1.0">
<html>
<head>
<title>SOAP GUI</title>
<style>
body{
    background-color:black;
    color:white;
}
input[type='text'],input[type='number']{
    background-color:rgb(50,50,50);
    color:yellow;
    border:none;
    height:25px;
    width:239px;
    padding-left:10px;
}
input[type='submit']{
    width:175px;
    border:none;
    background-color:gold;
    color:white;
    height:25px;
}
.entry{
    border:1px solid khaki;
    padding:8px;
    border-bottom-left-radius:8px;
    border-top-right-radius:8px;
}
textarea{
    background-color:black;
    border:1px solid khaki;
    padding:8px;
    border-bottom-left-radius:8px;
    border-top-right-radius:8px;
    width:330px;
    height:150px;
}
.wsdl{
    color:blue;
}
.SOAPreq{
    color:yellow;
}
.SOAPres{
    color:orange;
}
</style>
</head>
<body>
<?php
$vNo  = 0;
$host = "";
$port = "";
$path = "";
$operNames = array();
$soapOpers = array();

function computeOperations($wsdl) {
    global $operNames,$soapOpers;
    $operNames=getTextAround("operation name=\"","\"",$wsdl);
    for($i=0;$i<sizeof($operNames);$i+=1) {
        $argsRaw=getTextAround("message name=\"".$operNames[$i]."\">","</*>",$wsdl);
        if($argsRaw[0]!="") {
            $args=getTextAround("part name=\"", "\"", $argsRaw[0]);
            if($args[0]!="") {
  $soapOpers[$operNames[$i]]=$args;
            } else {
  $soapOpers[$operNames[$i]]=null;
            }
        } else {
  $argsOuter=getTextAround("element name=\"".$operNames[$i]."\">","</*>",$wsdl);
            if($argsOuter[0]==="") {
  $soapOpers[$operNames[$i]]=null;
            } else {
  $args=getTextAround("name=\"", "\"", $argsOuter[0]);
                if($args[0]!="") {
       for($j=0;$j<sizeof($args);$j+=1){
           $args[$j]="com:".$args[$j];
       }
  $soapOpers[$operNames[$i]]=$args;
                } else {
  $soapOpers[$operNames[$i]]=null;
                }
            }
        }
    }
}

function getTextAround($srchStrt, $srchEnd, $STRR) {

    $lst=array();

    $indx = strpos($STRR,$srchStrt);

    $endIndx = 0;

    $str = "";

    if($indx>-1) {
        if($srchEnd=="</*>") {

            //find proper closing tag

            $i=$indx-1;

            $ns="";

            while($STRR[$i]!='<') {

                $ns=$STRR[$i].$ns;

                $i-=1;

            }

            $i=$indx;

            $tagName="";

            while($STR[$i]!='<' && $STRR[$i]!=' ') {

                $tagName=$tagName.$STRR[$i];

                $i+=1;

            }

            $srchEnd="</".$ns.$tagName.">";

        }

        while($indx>-1) {

            $STRR=substr($STRR,$indx+strlen($srchStrt));

            $endIndx=strpos($STRR,$srchEnd);

            if($endIndx<0) {

                break;

            }

            $str=trim(substr($STRR,0, $endIndx));

            if($str!="" && !in_array($str,$lst)) {

                array_push($lst,$str);

            }

            $STRR=substr($STRR,$endIndx);

            $indx=strpos($STRR,$srchStrt);

        }

    }

    if(sizeof($lst)===0) {array_push($lst,"");}

    return $lst;

}

function getSocketResp($host,$port,$req,$ensureInResponse){
    $ensureInResponse=strtoupper($ensureInResponse);
    $socket=socket_create(AF_INET,SOCK_STREAM,0) or die("Could not create socket\n");
    $result=socket_connect($socket,$host,$port) or die("Could not connect to socket\n");
    socket_write($socket,$req,strlen($req)) or die("Could not send data to server\n");
    $resp='';
    $buff='';
    $byts=0;
    $bytsOld=0;
    $cnt=0;
    do {
        $byts+=socket_recv($socket,$buff,2048, MSG_DONTWAIT);
        $resp.=$buff;
        if($byts===$bytsOld){
            $cnt+=1;
            if($cnt>300000){break;}
        }else{
            $cnt=0;
        }
        $bytsOld=$byts;
 if(strpos(strtoupper($resp),$ensureInResponse)>-1) break;
    } while(true);
    return $resp;
}

function getWSDL(){
    global $host,$port,$path;
    $wsdl="";
    if(isset($_POST['host']) && isset($_POST['path']) && isset($_POST['port'])){
        $host=$_POST['host'];
        $path=$_POST['path'];
        $port=$_POST['port'];
    }
    if($_POST['opr']==="Fetch WSDL"){
        $wsdl=getWSDLSocket();
    } else {
        if(isset($_POST['wsdl'])){
            $wsdl=$_POST['wsdl'];
        }
    }
    computeOperations($wsdl);
    return $wsdl;
}

function getWSDLSocket(){
    global $host,$port,$path;
    $req="GET ".$path." HTTP/1.1\r\n";
    $req.="Host: ".$host."\r\n\r\n";
    $wsdl=getSocketResp($host,$port,$req,"definitions>");
    return $wsdl;
}

function getSoapReq($wsdl,$oper){
    global $host,$port,$path,$soapOpers;
    if(isset($_POST['opr']) && $_POST['opr']==="Prepare Request"){
    if($wsdl==="") {
        return "";
    } else {
        if($oper==="") {
            return "";
        } else {
            $ns=getTextAround("binding transport=\"","\"",$wsdl);

    $ns=str_replace("/http","/envelope/",$ns);

    $oprns=getTextAround("targetNamespace=\"","\"",$wsdl);


    $body="<?xml version='1.0' encoding='utf-8'?>\r\n";

    $body.="<soap:Envelope";

    if(sizeof($ns)>0){

        $body.=" xmlns:soap=\"".$ns[0]."\"";

    }

    if(sizeof($oprns)>0){

        $body.=" xmlns:com=\"".$oprns[0]."\"";

    }

    $body.=">\r\n";
    $body.="<soap:Body>\r\n";
    $body.="<com:".$oper.">\r\n";
    if($soapOpers[$oper]!=null){
        $arg=$soapOpers[$oper];
        for($j=0;$j<sizeof($arg);$j+=1){
            $body.="<".$arg[$j].">?";
            $body.="</".$arg[$j].">\r\n";
        }
    }
    $body.="</com:".$oper.">\r\n";
    $body.="</soap:Body>\r\n";
    $body.="</soap:Envelope>";
    $arr=preg_split("/[?]/",$path);
    $hd="POST http://".$host.":".$port.$path." HTTP/1.1\r\n";
    if(sizeof($arr)>0){
        $hd="POST http://".$host.":".$port.$arr[0]." HTTP/1.1\r\n";
    }

    $hd.="Host: ".$host."\r\n";

    $hd.="Content-Type: text/xml;charset=UTF-8\r\n";

    $hd.="Content-Length: ".strlen($body)."\r\n";

    $hd.="\r\n";

    return $hd.$body;
        }
    }
    } else {
        if(isset($_POST['SOAPreq'])){
            return $_POST['SOAPreq'];
        } else {
            return "";
        }
    }
}

function getSoapRes($soapReq){
    global $host,$port,$path;
    if($soapReq==="") {
        return "";
    }
    if($_POST['opr']==="Get Response"){
        return getSocketResp($host,$port,$soapReq,"envelope>");
    } else {
        return "";
    }
}

function showVisitCounter(){
    global $vNo;
    if(isset($_POST['vNo'])){
        $vNo=intval($_POST['vNo']);
        $vNo+=1;
    } else {
        $vNo+=1;
    }
    echo "Visit No: ".$vNo."<br>";
    echo "<input type='text' name='vNo' value='".$vNo."' hidden>";
}

function createRow($caption,$type,$name){
    echo "<tr>";
    echo "<td><font size=2 color='green'>".$caption.":</font></td>";
    echo "<td><input type='".$type."' name='".$name."'";
    echo isset($_POST[$name])?" value='".$_POST[$name]."' readonly>":">";
    echo "</td></tr>";
}

function addFormElememts(){
    echo "<table class='entry'>";
    createRow("Hostname","text","host");
    createRow("Path","text","path");
    createRow("Port","number","port");
    echo "<tr><td colspan='2'><input type='submit' name='opr' value='Fetch WSDL' style='width:310px'></td></tr>";
    echo "</table>";
}

function getOprDrpDn(){
    global $operNames;
    $sel="";
    if(isset($_POST['drpdwn'])){
        $sel=$_POST['drpdwn'];
    }
    $drpDn="<select name='drpdwn'>";
    for($i=0;$i<sizeof($operNames);$i+=1){
        $opr=$operNames[$i];
        $drpDn.="<option".($opr===$sel?" selected":"").">".$opr."</option>";
    }
    $drpDn.="</select>";
    return $drpDn;
}

function addSoapRows($caption,$name,$value,$btn,$wsdl){
    global $operNames;
    $oprDrpDwn="";
    if($btn!="") {
        if($btn==="Prepare Request") {
            $oprDrpDwn=getOprDrpDn();
        }
        $btn="<input name='opr' type='submit' value='".$btn."' style='float:right;width:110px;background-color:orange'>";
    }
    echo "<tr><td><font size=2 color='green'>".$caption."</font></td><td>".$oprDrpDwn."</td><td>".$btn."</td></tr>";
    echo "<tr><td colspan='3'><textarea class='".$name."' name='".$name."'";
    echo ($btn==="")?" readonly>":">";
    echo $value."</textarea></td></tr>";
}

function addSOAPElements(){
    global $operNames;
    $wsdl=getWSDL();
    $o="";
    if(isset($_POST['drpdwn'])){
        $o=$_POST['drpdwn'];
    }
    if($o===""){
        if(sizeof($operNames)>0){
            $o=$operNames[0];
        }
    }
    $soapReq=getSoapReq($wsdl,$o);
    $soapRes=getSoapRes($soapReq);
    echo "<table>";
    addSoapRows("WSDL","wsdl",$wsdl,"","");
    addSoapRows("SOAP Request","SOAPreq",$soapReq,"Prepare Request",$wsdl);
    addSoapRows("SOAP Response","SOAPres",$soapRes,"Get Response","");
    echo "</table>";
}
?>

<form method='POST' action='./index.php'>

<?php
showVisitCounter();
addFormElememts();
addSOAPElements();
?>
</form>
<p id='reqLen'></p>
<script>
var reqLen=document.querySelector("#reqLen");
var req=document.querySelector(".SOAPreq");
req.addEventListener("keyup",function(){
    var str=req.value;
    var arr=str.split("\r\n\r\n");
    if(arr.length<=1){
        arr=str.split("\n\n");
    }
    if(arr.length>1){
        var tmp=arr[1].replace(/\n/g,"\r\n");
        reqLen.innerText="Req Header:"+arr[0].length+" chars\nReq Body:"+tmp.length+" chars";
    } else {
        reqLen.innerText=arr.length;
    }
});
</script>
</body>
</html>
