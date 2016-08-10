package com.tesco;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tesco.cache.api.ICache;
import com.tesco.cache.impl.MinimalLRUCache;

/**
 * Minimalistic Cache Server to expose LRU cache over TCP.
 * @author akhilesh.singh
 *
 * @param <K>
 * @param <V>
 */
public class CacheServer {

	private static final String NEWLINE = "\n";
	private static final String OK ="OK\n";
	private static final String FAIL = "FAIL\n";
	private ICache<String,String> cache;
	
	private int port;
	Map<SelectionKey, List<String>> writeEvents = new ConcurrentHashMap<>();
	
	private static final int CACHE_KEYS = 100;   

	public  CacheServer(int port) {
		cache = new MinimalLRUCache<>(CACHE_KEYS); 
		this.port = port;
		
	}

	public static void main(String[] args) {
		new CacheServer(8080).bootstrap();


	}

	private void bootstrap(){
		Selector selector = null;
		ServerSocketChannel server = null;

		try { 
			selector = Selector.open(); 
			server = ServerSocketChannel.open(); 
			server.socket().bind(new InetSocketAddress(port)); 
			server.configureBlocking(false); 
			server.register(selector, SelectionKey.OP_ACCEPT); 
			ByteBuffer byteBuffer = ByteBuffer.allocate( 1024 );
			while (true) {
				selector.select();
				for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();) { 
					SelectionKey key = i.next(); 
					i.remove(); 
					if (key.isConnectable()) { 
						((SocketChannel)key.channel()).finishConnect(); 
					} 
					if (key.isAcceptable()) { 
						SocketChannel client = server.accept(); 
						client.configureBlocking(false); 
						client.socket().setTcpNoDelay(true); 
						client.register(selector, SelectionKey.OP_READ, new StringBuilder());
					} 
					if (key.isReadable()) { 
						SocketChannel ch = (SocketChannel) key.channel();
						StringBuilder stringBuilder = (StringBuilder)key.attachment();
						int bytesRead = ch.read( byteBuffer ); 
						byteBuffer.flip();
						if( bytesRead == 0) 
							continue;
						if( bytesRead == -1) {
							cleanup(key);
							continue;
						}
						byte bytes[] = new byte[bytesRead];
						byteBuffer.get( bytes,0, bytesRead);
						stringBuilder.append(new String(bytes));
						byteBuffer.clear();
						processMessage(stringBuilder, key);

					} 
					if(key.isWritable()) {
						SocketChannel ch = (SocketChannel) key.channel();
						List<String> writes = writeEvents.get(key);
						
						if( writes != null) {
							for(String value: writes) {
								ch.write(ByteBuffer.wrap(value.getBytes()));
								//writes.remove(value);
							}
							writeEvents.remove(key);
						}
						key.interestOps(SelectionKey.OP_READ);
					}
				}
			}   		
		} catch (Throwable e) { 
			e.printStackTrace();
		} finally {
			try {
				selector.close();
				server.socket().close();
				server.close();
			} catch (Exception e) {
			}
		}
	}

	private void cleanup(SelectionKey key) {
		try {
			key.channel().close();
			key.cancel();
		}catch(Exception ex) {
			System.err.println("Error in cleanup");
		}

	}

	private void processMessage(StringBuilder sb, SelectionKey selectionKey) {
		if(sb.length() < 3)
			return;
		String commandType = sb.substring(0,3);
		if(commandType.equals("PUT")) {
			int index = sb.indexOf(NEWLINE);
			if(index == -1) 
				return;
			int index2 = sb.indexOf(NEWLINE,index+1);
			if( index2 == -1) 
				return;
			int index3 = sb.indexOf(NEWLINE, index2+1);
			if(index3 == -1)
				return;
			String key = sb.substring(index+1, index2);
			String value = sb.substring(index2+1,index3);
			System.out.println( "Cache Request " + commandType+ ":" + key +":" + value);

			executePut(key,value, selectionKey);
			sb.delete(0, index3+1);

		} else if( commandType.equals("GET")) {
			int index = sb.indexOf(NEWLINE);
			if(index == -1) 
				return;
			int index2 = sb.indexOf(NEWLINE,index+1);
			if( index2 == -1) 
				return;
			String key = sb.substring(index+1, index2);
			System.out.println( "Cache Request " + commandType+ ":" + key );
			executeGet(key,selectionKey);
			sb.delete(0, index2+1);

		} else {
			executeError();
			sb.delete(0, sb.length());
		}
		selectionKey.interestOps(SelectionKey.OP_WRITE);

	}

	private void executeError() {

	}

	private void executeGet(String key, SelectionKey selectionKey) {
		String value = cache.get(key);
		System.out.println("Key" + key+ " value" + value);
		raiseWriteEvent(value + NEWLINE,selectionKey);
	}

	private void executePut(String key, String value, SelectionKey selectionKey) {
		if( cache.put(key,value))
			raiseWriteEvent(OK ,selectionKey);
		else 
			raiseWriteEvent(FAIL ,selectionKey);
	}

	private void raiseWriteEvent(String value, SelectionKey key) {
		List<String> values =  writeEvents.get(key);
		if(values == null) {
			values = new ArrayList<>();
			writeEvents.put(key, values);
		}
		values.add(value);	
	}

}

