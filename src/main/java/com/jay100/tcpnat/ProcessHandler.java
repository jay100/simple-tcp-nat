package com.jay100.tcpnat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

class ProcessHandler extends Thread{
	 private Socket socket;
	 private String client="";
	private String thisHost;
	private int thisPort;
	private  BufferedReader br;
	private  OutputStream out;

	public ProcessHandler(Socket socket){
		this.socket = socket;
	}
	public void run(){
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = socket.getOutputStream();
			while (true){
				String s = br.readLine();
				if(s==null) {
					socket.close();
					break;
				}

				InetAddress add = socket.getInetAddress();
				thisHost = add.getHostAddress();
				thisPort = socket.getPort();
				if(client.equals("")) {
					client = s;
					System.out.println("我是 :"+ client+"["+thisHost+":"+thisPort+"]");
				}

				TestServer.sendmsg("我是 :"+ client+"["+thisHost+":"+thisPort+"]",out);

				if(TestServer.connections.size()>=2){
					List<ProcessHandler> connections = TestServer.connections;
					ProcessHandler h1 = connections.get(0);
					ProcessHandler h2 = connections.get(1);
					String host1 = h1.thisHost;
					String host2 = h2.thisHost;
					int port1 = h1.thisPort;
					int port2 = h2.thisPort;
					String msg1 = "GET_REMOTE_ADDRESS-"+host2+"-"+port2;
					String msg2 = "GET_REMOTE_ADDRESS-"+host1+"-"+port1;
					TestServer.sendmsg(msg1,h1.out); //交换地址
					TestServer.sendmsg(msg2,h2.out);
				}

			}
		} catch (IOException e) {
		} finally {
			System.out.println("删除socket ");
			TestServer.connections.remove(this);
		}
	}
}