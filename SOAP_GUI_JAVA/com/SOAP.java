package com;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SOAP {

    private static HashMap<String,ArrayList<String>> operations = null;

    private static String getResponse(URL url, String req, String ensureExists1 , String ensureExists2) {
        try {
            Socket sock = null;
            try {
                sock = new Socket(url.getHost(), url.getPort());
            } catch (java.lang.IllegalArgumentException iae){
                sock = new Socket(url.getHost(), 80);
            }
            OutputStream os = sock.getOutputStream();
            os.write(req.getBytes());
            os.flush();
            BufferedInputStream is = new BufferedInputStream(sock.getInputStream());
            String ret = "";
            int c = 0;
            do {
                c=is.read();
                if(c!=-1) {
                    ret+=(char)c;
                }
            } while(is.available()>0);
            if(ret.indexOf(ensureExists1)==-1 && ret.indexOf(ensureExists2)==-1) {
                while((ret.indexOf(ensureExists1)==-1 && ret.indexOf(ensureExists2)==-1)) {
                    do {
                        c = is.read();
                        if (c != -1) {
                            ret += (char) c;
                        }
                    } while (is.available() > 0);
                }
            }
            is.close();
            os.close();
            ret = ret.replace("><",">\r\n<");
            return ret;
        } catch(IOException ioe) {
            System.out.println(ioe.toString());
            return "err: Unable to read from '"+url+"'"+ioe.toString();
        }
    }

    public static String getWSDL(String urlStr) {
        URL url = null;
        try {
            url = new URL(urlStr.trim());
        } catch (MalformedURLException murle) {
            return "err: invalid url! '"+urlStr+"'";
        }
        String req = "GET " + url.getPath() + (url.getQuery()==null?"":("?"+url.getQuery())) + " HTTP/1.1\r\nHost: "+ url.getHost() +"\r\n\r\n";
        return getResponse(url,req,"</wsdl:definitions>","</definitions>");
    }

    public static String getResp(String urlStr,String req) {
        URL url = null;
        try {
            url = new URL(urlStr.trim());
        } catch (MalformedURLException murle) {
            return "err: invalid url! '"+urlStr+"'";
        }
        return getResponse(url,req,"<","<");
    }

    public static void computeOperations(String WSDL) {
        operations = new HashMap<String, ArrayList<String>>();
        ArrayList<String> ops = getTextAround("operation name=\"","\"",WSDL);
        for(int i=0;i<ops.size();i++) {
            ArrayList<String> argsRaw = getTextAround("message name=\""+ops.get(i)+"\">","</*>",WSDL);
            if(!argsRaw.get(0).equals("")) {
                ArrayList<String> args = getTextAround("part name=\"", "\"", argsRaw.get(0));
                if(!args.get(0).equals("")) {
                    operations.put(ops.get(i), args);
                } else {
                    operations.put(ops.get(i),null);
                }
            } else {
                ArrayList<String> argsOuter = getTextAround("element name=\""+ops.get(i)+"\">","</*>",WSDL);
                if(argsOuter.get(0).equals("")) {
                    operations.put(ops.get(i), null);
                } else {
                    ArrayList<String> args = getTextAround("name=\"", "\"", argsOuter.get(0));
                    if(!args.get(0).equals("")) {
                        ArrayList<String> argsTmp = new ArrayList<String>();
                        for(int j=0;j<args.size();j++) {
                            argsTmp.add("com:"+args.get(j));
                        }
                        operations.put(ops.get(i), argsTmp);
                    } else {
                        operations.put(ops.get(i),null);
                    }
                }
            }
        }
    }

    public static ArrayList<String> getOperationsNames() {
        Iterator itr = operations.keySet().iterator();
        ArrayList<String> ret = new ArrayList<String>();
        while(itr.hasNext()) {
            ret.add((String)itr.next());
        }
        return ret;
    }

    private static ArrayList<String> getTextAround(String srchStrt, String srchEnd, String STR) {
        ArrayList<String> lst = new ArrayList<String>();
        int indx = STR.indexOf(srchStrt);
        int endIndx = 0;
        String str = "";
        if(indx>-1) {
            if(srchEnd.equals("</*>")) {
                //find proper closing tag
                int i=indx-1;
                String ns="";
                while(STR.charAt(i)!='<') {
                    ns=STR.charAt(i)+ns;
                    i--;
                }
                i=indx;
                String tagName="";
                while(STR.charAt(i)!='<' && STR.charAt(i)!=' ') {
                    tagName+=STR.charAt(i);
                    i++;
                }
                srchEnd="</"+ns+tagName+">";
            }
            while(indx>-1) {
                STR = STR.substring(indx+srchStrt.length());
                endIndx = STR.indexOf(srchEnd);
                if(endIndx<0) {
                    break;
                }
                str = STR.substring(0, endIndx).trim();
                if(!str.equals("") && !lst.contains(str)) {
                    lst.add(str);
                }
                STR = STR.substring(endIndx);
                indx = STR.indexOf(srchStrt);
            }
        }
        if(lst.size()==0) {lst.add("");}
        return lst;
    }

    private static ArrayList<String> getSOAPNamespaces(String WSDL){
        ArrayList<String> ns = new ArrayList<String>();
        String STR = getTextAround("<soap:binding transport=\"","\"",WSDL).get(0);
        STR = STR.replace("/http","/envelope")+"/";
        ns.add(STR);
        STR = getTextAround("targetNamespace=\"","\"",WSDL).get(0);
        ns.add(STR);
        return ns;
    }

    public static String getSOAPReq(String urlStr,String WSDL,String operationName) {
        URL url = null;
        try {
            url = new URL(urlStr.replace("?WSDL","").trim());
        } catch (MalformedURLException murle) {
            return "err: invalid url! '"+urlStr+"'";
        }
        ArrayList<String> ns = getSOAPNamespaces(WSDL);
        String req="POST "+url+" HTTP/1.1\r\n";
        String content="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n";
        content+="<soap:Envelope xmlns:soap=\"" + ns.get(0) + "\" xmlns:com=\"" + ns.get(1) + "\">\r\n";
        content+="<soap:Header/>\r\n";
        content+="<soap:Body>\r\n";
        if(!operationName.equals("")) {
            ArrayList<String> args = operations.get(operationName);
            if (args != null) {
                content += "<com:" + operationName + ">\r\n";
                for (int i = 0; i < args.size(); i++) {
                    content += "<" + args.get(i) + ">?<" + "/" + args.get(i) + ">\r\n";
                }
                content += "</com:" + operationName + ">\r\n";
            } else {
                content += "<com:" + operationName + "/>\r\n";
            }
        }
        content+="</soap:Body>\r\n";
        content+="</soap:Envelope>\r\n";
        String header="Host: " + url.getHost()+"\r\n";
        header+="Content-Type: text/xml;charset=UTF-8\r\n";
        //header+="SOAPAction: \"\"\r\n";
        header+="Content-Length: "+content.length()+"\r\n";
        header+="\r\n";
        req+=header+content;
        return req;
    }
}
