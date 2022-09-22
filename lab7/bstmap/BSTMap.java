package bstmap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V>{
    /* The root node of this BST */
    private Node root;

    /* The structure which stores the essential data
     * to generate the miniature unit of the BST */
    private class Node {
        private K key;  //sorted by key
        private V val;    //associated data
        private Node left, right;   //left and right subtrees
        private int size;   //number of nodes in subtree

        public Node(K key, V val, int size) {
            this.key = key;
            this.val = val;
            this.size = size;
        }
    }
    @Override
    /* Remove all the mapping pairs in the map */
    public void clear() {
        root.key = null;
        root.val = null;
        root.left = null;
        root.right = null;
        root.size = 0;
    }

    @Override
    /* Return true if the map contains the mapping for the specific key */
    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("argument to containsKey() is null");
        }
        return find(root, key);
    }

    private boolean find(Node x, K key) {
        if (key == null) {
            throw new IllegalArgumentException("calls find with a null key");
        }
        if (x == null || size() == 0) {
            return false;
        }
        int cmp = key.compareTo(x.key);
        if (cmp > 0) {
            return find(x.right, key);
        } else if (cmp < 0) {
            return find(x.left, key);
        } else {
            return true;
        }
    }

    @Override
    /* Return the number of mappings contained in the map */
    public int size() {
        return size(root);
    }

    /* Helper function of the size() */
    private int size(Node x) {
        if (x == null) {
            return 0;
        } else {
            return x.size;
        }
    }

    @Override
    /* Get the value from the mapping for the specific key */
    public V get(K key) {
        return get(root, key);
    }

    private V get(Node x, K key) {
        if (key == null) {
            throw new IllegalArgumentException("calls get() with a null key");
        }
        if (x == null || size() == 0) {
            return null;
        }
        int cmp = key.compareTo(x.key);
        if (cmp > 0) {
            return get(x.right, key);
        } else if (cmp < 0) {
            return get(x.left, key);
        } else {
            return x.val;
        }
    }

    @Override
    /* Unsupported yet */
    public V remove(K key) {
        root = remove(root, key);
        return root.val;
    }

    private Node remove(Node x, K key) {
        if(x == null) {
            return null;
        }
        int cmp = key.compareTo(x.key);
        if(cmp < 0) {
            x.left = remove(x.left, key);
        } else if (cmp > 0) {
            x.right = remove(x.right, key);
        } else {
            if (x.right == null) {
                return x.left;
            }
            if (x.left == null) {
                return x.right;
            }
            Node t = x;
            x = min(t.right);
            x.right = removeMin(t.right);
            x.left = t.left;
        }
        x.size = size(x.left) + size(x.right) + 1;
        return x;
    }

    /* Return the Node whose key is the smallest */
    private Node min(Node x) {
        if (x.left == null) {
            return x;
        } else {
            return min(x.left);
        }
    }
    /* Return if the map is empty or not */
    private boolean isEmpty() {
        return size() == 0;
    }


    private void removeMin() {
        if (isEmpty()) {
            throw new NoSuchElementException("Symbol table underflow");
        }
        root = removeMin(root);
    }

    private Node removeMin(Node x) {
        if (x.left == null) {
            return x.right;
        }
        x.left = removeMin(x.left);
        x.size = size(x.left) + size(x.right) + 1;
        return x;
    }

    @Override
    /* Unsupported yet */
    public V remove(K key, V val) {
        throw new UnsupportedOperationException("remove() is unsupported yet");
    }

    @Override
    /* Put the mapping of given key and value into the map */
    public void put(K key, V val) {
        if (key == null) {
            throw new IllegalArgumentException("calls put() with a null key");
        }
//        if (val == null) {
//            remove(key);
//            return;
//        }
        root = put(root, key, val);
    }

    private Node put(Node x, K key, V val) {
        if (x == null) {
            return new Node(key, val, 1);
        }
        int cmp = key.compareTo(x.key);
        if (cmp > 0) {
            x.right = put(x.right, key, val);
        } else if (cmp < 0) {
            x.left = put(x.left, key, val);
        } else {
            x.val = val;
        }
        x.size = 1 + size(x.left) + size(x.right);
        return x;
    }

    @Override
    /* Unsupported yet */
    public Set<K> keySet() {
        HashSet<K> set = new HashSet<>();
        addKeys(root, set);
        return set;
    }

    private void addKeys(Node node, Set<K> set) {
        if (node == null) {
            return;
        }
        set.add(node.key);
        addKeys(node.left, set);
        addKeys(node.right, set);
    }

    @Override
    /* Unsupported yet */
    public Iterator<K> iterator() {
        return keySet().iterator();
    }
}
