package com.tesco.cache.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tesco.cache.api.ICache;

/**
 * Minimal LRU Cache implementation
 * @author akhilesh.singh
 *
 */
public class MinimalLRUCache<K,V> implements ICache<K,V>{

	int capacity;
	Map<K,Node<K,V>> map= new ConcurrentHashMap<>();
	Node<K,V> head;
	Node<K,V> end;

	public MinimalLRUCache(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public V get(K key) {
		Node<K,V> node = map.get(key);
		if( node != null ) {
			remove(node);
			setHead(node);
			return node.getValue();
		}
		return null;
	}

	public void setHead(Node<K,V> n){
		n.setNext(head);
		n.setPrevious(null);

		if(head!=null)
			head.setPrevious(n);

		head = n;
		if(end ==null)
			end = head;
	}

	private void remove(Node<K,V> n) {
		if(n.getPrevious() !=null){
			n.getPrevious().setNext(n.getNext());
		}else{
			head = n.getNext();
		}

		if(n.getNext()!=null){
			n.getNext().setPrevious(n.getPrevious());
		}else{
			end = n.getPrevious();
		}
	}

	@Override
	public boolean put(K key, V value) {
		Node<K,V> valueTmp = map.get(key);
		try {
		if( valueTmp != null){
			Node<K,V> old = map.get(key);
			old.setValue(value);
			remove(old);
			setHead(old);
			return true;
		}else{
			Node<K,V> created = new Node<>(key,value);
			if(map.size()>=capacity){
				map.remove(end.key);
				remove(end);
				setHead(created);
			}else{
				setHead(created);
			}    
			map.put(key, created);
			return true;
		}
		} catch(Exception ex) {
			return false;
		}
	}

	private class Node<K,V> {
		private Node<K,V> previous;
		private Node<K,V> next;
		private V value;
		private K key;

		public Node(K key, V value) {
			this.value = value;
			this.key =key;
		}

		public Node<K,V> getPrevious() {
			return previous;
		}

		public void setPrevious(Node<K,V> previous) {
			this.previous = previous;
		}

		public Node<K,V> getNext() {
			return next;
		}

		public void setNext(Node<K,V> next) {
			this.next = next;
		}

		public V getValue() {
			return value;
		}
		public void setValue(V value) {
			this.value = value;
		}
	}
}

