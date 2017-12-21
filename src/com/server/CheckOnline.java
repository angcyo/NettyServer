package com.server;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;

public class CheckOnline extends Thread {
	private boolean isRuning = false;
	public Map<String, PlayerInfo> players;

	String ip;
	PlayerInfo info;

	@Override
	public void run() {
		super.run();

		while (isRuning) {
//			System.out.println("检测在线状态...");
			
			players = HttpSnoopServerHandler.playerMap;
			for (Entry<String, PlayerInfo> iterable_element : players
					.entrySet()) {
				ip = iterable_element.getKey();
				info = iterable_element.getValue();

				if (!isOnline(ip, 8080)) {
					System.out.println(ip + " 已离线");
					info.state = "离线";
					players.put(ip, info);
				}else {
					info.state = "连接正常";
					players.put(ip, info);
				}
			}
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean isOnline(String srvip, int port) {
		// 检查是否能连上服务器
		Socket client = new Socket();
		try {
			client.setSoLinger(true, 0);
			client.setTcpNoDelay(true);
			client.setTrafficClass(0x04 | 0x10);
			client.setKeepAlive(true);
			InetSocketAddress isa = new InetSocketAddress(srvip, port);
			client.connect(isa, 1000);
			return true;
		} catch (Exception e1) {
			return false;
		}

		finally {
			if (client != null) {
				try {
					client.close();
				} catch (Exception e) {

				}
			}
			client = null;
		}
	}

	public synchronized void startThread() {
		if (!isRuning) {
			isRuning = true;
			super.start();
		}
	}

	public synchronized void stopThread() {
		isRuning = false;
	}
}
