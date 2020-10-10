import java.util.HashMap;

// LRU Cache
public class URLCache {
	HashMap<String, Node> cache;
	int size;
	Node head;
	Node tail;

	public URLCache(int size) {
		this.size = size;
		cache = new HashMap<>();
		head = null;
		tail = null;
	}

	public void add(String key, String value) {
    Node node = new Node(key, value);
		if (cache.size() == 0) {
			head = node;
			tail = node;
		} else {
			if (cache.size() == this.size) {
				remove(this.tail);
			}
			node.next = this.head;
			this.head.prev = node;
			head = node;
		}
		cache.put(key, node);
    }
    
    public String get(String key) {
		if (cache.containsKey(key)) {
			Node node = cache.get(key);
            remove(node);
			return node.value;
		}
		return null;
	}

	private void remove(Node node) {
		if (this.head == node)
			this.head = node.next;
		if (this.tail == node)
			this.tail = node.prev;
		if (node.prev != null)
			node.prev.next = node.next;
		if (node.next != null)
			node.next.prev = node.prev;
		cache.remove(node.key);
	}
}

class Node {
	String key;
	String value;
	Node next;
	Node prev;

	public Node(String key, String value) {
		this.key = key;
		this.value = value;
		this.next= null;
		this.prev = null;
	}
}
