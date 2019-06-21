package com.jay100.tcpnat;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientA{
    private Socket socket;
    private int localPort;
    private  String host;
    private  int port;
    private BufferedReader br;
    private  OutputStream out;
    boolean isinput=true;

    long sendFileProcess=0;
    long sendFileTotal=0;
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        if(args.length==0){
            //args = new String[]{"外网IP","8088"};
        }
        int port = Integer.parseInt(args[1]);
        String host = args[0];
        //初始化
        ClientA client = new ClientA(host,port);
        client.start();

    }

    public ClientA(String host,int port){
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

            System.out.println("连接成功，等待CLient B 连接...");
            send("I'm CLIENTA");

            while (true){
                String s = br.readLine();
                if(s==null) break;
                if(s.startsWith("GET_REMOTE_ADDRESS")){ //获取A的地址
                    String[] cmds = s.split("-");
                    String phost =cmds[1];
                    int pport = Integer.parseInt(cmds[2]);
                    System.out.println("获取来自 CLIENT B 的地址["+phost+":"+pport+"]");
                    //开始连接
                    p2pConn(phost,pport); //连接 B
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

                    final InputStream newin= newsocket.getInputStream();
                    final PrintWriter p = new PrintWriter(newsocket.getOutputStream());
                    final OutputStream newout = newsocket.getOutputStream();

                    String in;

                    new Thread(){
                        @Override
                        public void run() {
                            try {
                            byte[] buffer = new byte[8192];
                            while (true) {
                                int count =   newin.read(buffer);
                                if(count==-1) break;
                                String  message = new String(buffer,0,count,"UTF-8");
                                System.out.print(message);


                            }
                            }catch (Exception e){
                                closeP2PConn(newsocket);
                            }
                        }
                    }.start();

                    System.out.println("请输入指令 [sendFile] ：");
                    final byte[] buffer = new byte[8192];
                    final AtomicBoolean isStopSend = new AtomicBoolean();
                    while (isinput){
                        in = scanner.next();
                        if(in.equals("exit")) break;
                        if(in.equals("stopSendFile")){
                            isStopSend.set(false);
                        }
                        else if(in.equals("sendFile")){ //发送一个文件试试
                            String[] strings = in.split("");
                            System.out.println("请输入文件路径：");
                            String fileName =  scanner.next();
                            p.write("sendFile");
                            p.println();
                            p.flush();
                            sendFileTotal =0;
                            isStopSend.set(true);
                           final File f = new File(fileName);
                            try {
                                final FileInputStream fin = new FileInputStream(f);

                                new Thread(){
                                    @Override
                                    public void run() {
                                        long fileLength = f.length();

                                        System.out.println("请输入 “stopSendFile” 停止发送");
                                        while (sendFileProcess!=-1) {
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                            }
                                            if(sendFileProcess>0)
                                                System.out.println((sendFileProcess / 1024) + "KB/S" + " | " + ( String.format("%.2f",sendFileTotal*1.0/fileLength *100) )+"%" );
                                            if(sendFileProcess!=-1)
                                                sendFileProcess=0;
                                        }
                                    }
                                }.start();

                                new Thread(){
                                    @Override
                                    public void run() {
                                        try {
                                            while (isStopSend.get()) {
                                                int bc = fin.read(buffer); //  字节
                                                if (bc == -1) {
                                                    sendFileProcess = -1;
                                                    System.out.println("发送完成!");
                                                    break;
                                                }
                                                sendFileProcess += bc;
                                                sendFileTotal+=bc;
                                                newout.write(buffer, 0, bc);
                                                newout.flush();
                                            }
                                        }catch (Exception e){
                                        }
                                    }
                                }.start();
                               /* while (isStopSend.get()){
                                    int bc = fin.read(buffer);
                                    if(bc==-1){
                                        sendFileProcess=-1;
                                        System.out.println("发送完成!");
                                        break;
                                    }
                                    sendFileProcess+=bc;
                                    newout.write(buffer,0,bc);
                                    newout.flush();
                                }*/
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else {
                            p.write(in);
                            p.println();
                            p.flush();
                        }
                    }
                    newsocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isinput = false;
                    System.out.println("连接已断开....");
                    closeP2PConn(newsocket);
                }
            }
        }.start();
    }
    public void closeP2PConn(Socket s){
        try{
            s.close();
            socket.close();
            System.out.println("输入任意键结束");
            isinput=false;
            scanner.close();
        } catch (Exception e){
        }
    }
    public void send(String msg) throws IOException {
        msg+="\r\n";
        out.write(msg.getBytes());
        out.flush();
    }
}
