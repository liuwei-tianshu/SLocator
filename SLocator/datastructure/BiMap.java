package SLocator.datastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * a bi-directional hashmap
 * 
 * a template for the node structure in a tree
 * K:child node type		V:parent node type
 */
public class BiMap<K extends Object, V extends Object> {
	
	// Map<child node, parent node>
	private Map<K, V> childToParent = new HashMap<K, V>();
	
	// Map<parent node, ArrayList<child node>>
	private Map<V, ArrayList<K>> parentToChildren = new HashMap<V, ArrayList<K>>();

	/**
	 * Key is child, Value is parent
	 * 
	 * @param key
	 * @param value
	 */
	public synchronized void put(K key, V value) {
		childToParent.put(key, value);
		if (!parentToChildren.containsKey(value))
			parentToChildren.put(value, new ArrayList<K>());
		parentToChildren.get(value).add(key);
	}

	/**
	 * Given child, get parent
	 * 
	 * @param key
	 * @return
	 */
	public synchronized V getParent(K key) {
		return childToParent.get(key);
	}

	/**
	 * Given parent, get children
	 * 
	 * @param key
	 * @return
	 */
	public synchronized ArrayList<K> getChildren(V value) {
		return parentToChildren.get(value);
	}

	public synchronized boolean containsChild(K key) {
		return childToParent.containsKey(key);
	}

	public synchronized boolean containsParent(K key) {
		return parentToChildren.containsKey(key);
	}

}
