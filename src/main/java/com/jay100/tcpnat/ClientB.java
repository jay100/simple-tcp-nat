package com.jay100.tcpnat;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientB{
    private Socket socket;
    private int localPort;
    private String host;
    private  int port;
    private BufferedReader br;
    private  OutputStream out;
    boolean isinput=true;
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        if(args.length==0){
            //args = new String[]{"外网IP","8088"};
        }
        int port = Integer.parseInt(args[1]);
        String host = args[0];
        //初始化
        ClientB client = new ClientB(host,port);
        client.start();
    }

    public ClientB(String host,int port){
        this.host = host;
        this.port = port;
    }

    public void start(){

        socket = new Socket();
        try {
            socket.setReuseAddress(true);
            socket.connect(new InetSocketAddress(host, port));
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = socket.getOutputStream();

            localPort = socket.getLocalPort();
            System.out.println("连接成功，等待CLient A 连接...");
            send("I'm CLIENTB");

            while (true){
                String s = br.readLine();
                if(s==null) break;
                if(s.startsWith("GET_REMOTE_ADDRESS")){ //获取A的地址
                    String[] cmds = s.split("-");
                    String phost =cmds[1];
                    int pport = Integer.parseInt(cmds[2]);
                    System.out.println("获取来自 CLIENT A 的地址["+phost+":"+pport+"]");
                    //开始连接
                    p2pConn(phost,pport); //连接 A
                } else{
                    System.out.println("来自服务器："+s);
                }
            }


        } catch (Exception e) {

        }
    }

    public void p2pConn(final String remoteHost,final int remotePort){
        System.out.println("p2p 开始打洞["+remoteHost+":"+remotePort+"]");
        new Thread(){
            @Override
            public void run() {
                final Socket newsocket = new Socket();
                try {
                    newsocket.setReuseAddress(true);
                    newsocket.bind(new InetSocketAddress(
                            InetAddress.getLocalHost().getHostAddress(), localPort));

                    System.out.println("直接连接： " + new InetSocketAddress(remoteHost, remotePort));
                    newsocket.connect(new InetSocketAddress(remoteHost, remotePort));

                    System.out.println("打洞成功");

                    final InputStream newin = newsocket.getInputStream();
                    final PrintWriter p = new PrintWriter(newsocket.getOutputStream());

                    String in;

                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                byte[] buffer = new byte[8192];
                                while (true) {
                                      int count =   newin.read(buffer);
                                      if(count==-1) break;
                                 // String  message = new String(buffer,0,count,"UTF-8");
                                  System.out.println("字节个数："+count);
                                }
                            } catch (IOException e) {
                                closeP2PConn(newsocket);
                            }
                        }
                    }.start();

                    System.out.println("请输入指令：");

                    while (isinput){
                        in = scanner.next();
                        if(in.equals("exit")) break;
                        p.write(in);
                        p.println();
                        p.flush();
                    }
                    newsocket.close();
                } catch (Exception e) {
                } finally {
                    isinput=false;
                    scanner.close();
                    System.out.println("连接已断开....");
                    closeP2PConn(newsocket);
                }
            }
        }.start();
    }

    public void send(String msg) throws IOException {
        msg+="\r\n";
        out.write(msg.getBytes());
        out.flush();
    }

    public void closeP2PConn(Socket s){
        try{
            s.close();
            socket.close();
            isinput=false;
            System.out.println("输入任意键结束");
            scanner.close();
        } catch (Exception e){}
    }
}
