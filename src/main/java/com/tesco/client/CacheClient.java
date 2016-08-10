package com.tesco.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class CacheClient implements Runnable {

	private static final String host = "localhost";
	private static final int port = 8080;
	SocketChannel channel;

	private static final long KEY_NUMBER = 110; 

	private static final String GET_MESSAGE = "GET\nKEY\n";

	private static final String PUT_MESSAGE = "PUT\nKEY\nTesco-KEY\n";  // Key= Random number between 1 and million. Value = Tesco-<Key>

	public CacheClient() throws Exception{
		channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(host, port));
	}

	@Override
	public void run() {
		try {
			while (!channel.finishConnect()) {
				System.out.println("Connecting");
			}
			int randomSelection = 0;
			ByteBuffer bufferA = ByteBuffer.allocate(1024);
			while (true) {

				int count = 0;
				long randomKeySuffix = (long) (Math.random() *KEY_NUMBER);

				String message ;
				if(randomSelection % 2 == 0) {
					message = PUT_MESSAGE.replaceAll("KEY", String.valueOf(randomKeySuffix));
				}else {
					message = GET_MESSAGE.replace("KEY",String.valueOf(randomKeySuffix)) ;
				}
				randomSelection++;
				
				CharBuffer buffer = CharBuffer.wrap(message);
				while (buffer.hasRemaining()) {
					channel.write(Charset.defaultCharset().encode(buffer));
				}
				boolean readMarker = false;
				StringBuilder sb = new StringBuilder();
				while(!readMarker) {
					int numbytes = channel.read(bufferA);
					if( numbytes == 0) 
						continue;
					bufferA.flip();
					if( numbytes == -1) {
						channel.close();
						System.exit(-1);
					}
					byte tmp[] = new byte[numbytes];
					bufferA.get(tmp,0,numbytes);
					sb.append(new String(tmp));
					if (sb.indexOf("\n") != -1 ) 
						readMarker = true;
					System.out.println("Status for :" + randomKeySuffix+ " " + sb.toString());
				}
				bufferA.clear();
				try {
					Thread.sleep(1); // Artifical delay to prevent laptop from heating up :)
				} catch (InterruptedException e) {
					//Ignore and recyle. 
				} 
			} 
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {

		new Thread(new CacheClient()).start();
		new Thread(new CacheClient()).start();
	}




}