package com.tesco.cache.api;

/**
 * Minimalistic Cache interface to support LRU
 * @author akhilesh.singh
 *
 * @param <K>
 * @param <V>
 */
public interface ICache<K,V> {
	
	 V get(K key);
	 boolean put(K key, V value);

}
