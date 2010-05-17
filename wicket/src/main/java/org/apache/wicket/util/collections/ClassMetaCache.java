package org.apache.wicket.util.collections;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class wraps a WeakHashMap that holds one ConcurrentHashMap per ClassLoader. In the rare
 * event of a previously unmapped ClassLoader, the WeakHashMap is replaced by a new one. This avoids
 * any synchronization overhead, much like a {@link java.util.concurrent.CopyOnWriteArrayList}
 * 
 * @param <T>
 *            type of objects stored in cache
 */
public class ClassMetaCache<T>
{
	private volatile Map<ClassLoader, ConcurrentHashMap<String, T>> cache = Collections.emptyMap();

	/**
	 * Puts value into cache
	 * 
	 * @param key
	 * @param value
	 * @return value previously stored in cache for this key, or {@code null} if none
	 */
	public T put(Class<?> key, T value)
	{
		ConcurrentHashMap<String, T> container = getClassLoaderCache(key.getClassLoader(), true);
		return container.put(key(key), value);
	}

	/**
	 * Gets value from cache or returns {@code null} if not in cache
	 * 
	 * @param key
	 * @return value stored in cache or {@code null} if none
	 */
	public T get(Class<?> key)
	{
		ConcurrentHashMap<String, T> container = getClassLoaderCache(key.getClassLoader(), false);
		if (container == null)
		{
			return null;
		}
		else
		{
			return container.get(key(key));
		}
	}

	/**
	 * @param classLoader
	 * @param create
	 * @return a {@link ConcurrentHashMap} mapping class names to injectable fields, never
	 *         <code>null</code>
	 */
	private ConcurrentHashMap<String, T> getClassLoaderCache(ClassLoader classLoader, boolean create)
	{
		ConcurrentHashMap<String, T> container = cache.get(classLoader);
		if (container == null)
		{
			if (!create)
			{
				return container;
			}

			// only lock in rare event of unknown ClassLoader
			synchronized (this)
			{
				// check again inside lock
				container = cache.get(classLoader);
				if (container == null)
				{
					container = new ConcurrentHashMap<String, T>();

					/*
					 * don't write to current cache, copy instead
					 */
					Map<ClassLoader, ConcurrentHashMap<String, T>> newCache = new WeakHashMap<ClassLoader, ConcurrentHashMap<String, T>>(
						cache);
					newCache.put(classLoader, container);
					cache = Collections.unmodifiableMap(newCache);
				}
			}
		}
		return container;
	}

	/**
	 * converts class into a key used by the cache
	 * 
	 * @param clazz
	 * 
	 * @return string representation of the clazz
	 */
	private static String key(Class<?> clazz)
	{
		return clazz.getName();
	}
}