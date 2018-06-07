package com;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

public class UI extends Frame {

    private Label msgLabel    = new Label("");
    private Label urlLbl      = new Label("URL");
    private TextField urlTxt  = new TextField("http://localhost:8888/com?WSDL");
    private TextArea wsdlArea = new TextArea();
    private TextArea reqArea  = new TextArea();
    private TextArea resArea  = new TextArea();
    private Button   connect  = new Button("Connect");
    private Button   send     = new Button("Send");
    private Choice   methods  = new Choice();
    private Label reqBodyLen  = new Label("Body len: 0 chars");

    public UI() {
        super("SOAP-GUI");
        setSize(800,770);
        setLayout(null);
        setResizable(false);
        init();
        positionAll();
        styleAll();
        addListeners();
        addAll();
        setVisible(true);
    }

    private void setReq(String req) {
        reqArea.setText(req);
        String[] arr = req.split("\n\n");
        if(arr.length<=1) {
            arr = req.split("\r\n\r\n");
        }
        if(arr.length>1) {
            String str = arr[1];
            reqBodyLen.setText("Body len: " + str.length() + " chars");
        } else {
            reqBodyLen.setText("Body len: 0 chars");
        }
    }

    private void init() {

    }

    private void positionAll() {
        msgLabel.setBounds(40,35,700,20);
        urlLbl.setBounds(40,70,35,20);
        urlTxt.setBounds(75,70,370,20);
        connect.setBounds(450,70,70,20);
        reqBodyLen.setBounds(400,110,120,20);
        methods.setBounds(525,110,155,20);
        send.setBounds(680,110,70,20);
        wsdlArea.setBounds(40,140,350,600);
        reqArea.setBounds(400,140,350,290);
        resArea.setBounds(400,440,350,300);
    }

    private void styleAll() {
        setBackground(Color.BLACK);
        urlTxt.setBackground(Color.BLACK);
        urlTxt.setForeground(Color.ORANGE);
        wsdlArea.setBackground(Color.BLACK);
        reqArea.setBackground(Color.BLACK);
        resArea.setBackground(Color.BLACK);
        wsdlArea.setForeground(Color.WHITE);
        reqArea.setForeground(new Color(80,80,250));
        resArea.setForeground(new Color(80,250,80));
        urlLbl.setForeground(Color.YELLOW);
        connect.setBackground(Color.BLACK);
        connect.setForeground(Color.YELLOW);
        send.setBackground(Color.BLACK);
        send.setForeground(Color.GREEN);
        msgLabel.setForeground(Color.RED);
        reqBodyLen.setBackground(Color.BLACK);
        reqBodyLen.setForeground(Color.ORANGE);
    }

    private void addListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String WSDL = SOAP.getWSDL(urlTxt.getText());
                SOAP.computeOperations(WSDL);
                ArrayList<String> ops = SOAP.getOperationsNames();
                methods.removeAll();
                resArea.setText("");
                for(int i=0;i<ops.size();i++) {
                    methods.add(ops.get(i));
                }
                if(WSDL.indexOf("err:")==0){
                    err(WSDL.replace("err:",""),2300);
                } else {
                    wsdlArea.setText(WSDL);
                    String req = SOAP.getSOAPReq(urlTxt.getText(), WSDL, ops.get(0));
                    setReq(req);
                }
            }
        });
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ret = SOAP.getResp(urlTxt.getText(), reqArea.getText());
                if(ret.indexOf("err:")==0){
                    err(ret.replace("err:",""),2300);
                } else {
                    resArea.setText(ret);
                }
            }
        });
        methods.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                String req = SOAP.getSOAPReq(urlTxt.getText(), wsdlArea.getText(), methods.getSelectedItem());
                setReq(req);
                resArea.setText("");
            }
        });
        reqBodyLen.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {
                setReq(reqArea.getText());
            }
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });
    }

    private void addAll() {
        add(reqBodyLen);
        add(wsdlArea);
        add(reqArea);
        add(resArea);
        add(msgLabel);
        add(urlLbl);
        add(urlTxt);
        add(connect);
        add(methods);
        add(send);
    }

    public void err(String err,int ms) {
        msgLabel.setText(err);
        long t1 = System.currentTimeMillis();
        long t2 = System.currentTimeMillis();
        while(t2-t1<ms) {
            t2 = System.currentTimeMillis();
        }
        msgLabel.setText("");
    }
}