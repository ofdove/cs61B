package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T> {
    public T item;
    public Node next;
    public class Node {
        public T item;
        public Node next;
        public Node previous;
        public Node(T i, Node n, Node p) {
            item = i;
            next = n;
            previous = p;
        }
    }
    private Node first;

    private Node last;
    private int size;
    private Node sentinel;
    public LinkedListDeque() {
        sentinel = new Node(null, sentinel, sentinel);
        size = 0;
    }
    public LinkedListDeque(T x) {
        sentinel = new Node(null, sentinel, sentinel);
        sentinel.next = new Node(x, sentinel, sentinel);
        sentinel.previous = sentinel.next;
        size = 1;
    }
    @Override
    public void addFirst(T item) {
        if (isEmpty()) {
            sentinel.next = new Node(item, sentinel, sentinel);
            sentinel.previous = sentinel.next;
        } else {
            sentinel.next.previous = new Node(item, sentinel.next, sentinel);
            sentinel.next = sentinel.next.previous;
        }
        size += 1;
    }
    @Override
    public void addLast(T item) {
        if (isEmpty()) {
            sentinel.previous = new Node(item, sentinel, sentinel);
            sentinel.next = sentinel.previous;
        } else {
            sentinel.previous.next = new Node(item, sentinel, sentinel.previous);
            sentinel.previous = sentinel.previous.next;
        }
        size += 1;
    }
    @Override
    public int size() {
        return size;
    }
    @Override
    public void printDeque() {
        Node d = sentinel.next;
        while (d != sentinel) {
            System.out.print(d.item + " ");
            d = d.next;
        }
    }
    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        } else {
            Node first = sentinel.next;
            sentinel.next.next.previous = sentinel;
            sentinel.next = sentinel.next.next;
            size -= 1;
            return first.item;
        }
    }
    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        } else {
            Node last = sentinel.previous;
            sentinel.previous.previous.next = sentinel;
            sentinel.previous = sentinel.previous.previous;
            size -= 1;
            return last.item;
        }
    }
    @Override
    public T get(int index) {
        int count;
        Node d;
        if (index > size - 1) {
            return null;
        } else if (index > size / 2) {
            count = (size - 1) - index;
            d = sentinel.previous;
            while (count > 0) {
                d = d.previous;
                count -= 1;
            }
        } else {
            count = index;
            d = sentinel.next;
            while (count > 0) {
                d = d.next;
                count -= 1;
            }
        }
        return d.item;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LinkedListDeque)) {
            return false;
        } else {
            LinkedListDeque d = (LinkedListDeque) o;
            if (d.size() != this.size) {
                return false;
            } else {
                for (int i = 0; i < this.size(); i += 1) {
                    if(!(this.get(i).equals(d.get(i)))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    public T getRecursive(int index) {
        return getRecursive(index, sentinel.next);
    }
    private T getRecursive(int index, Node d) {
        if (index == 0) {
            return d.item;
        }
        return getRecursive(index - 1, d.next);
    }

    public Iterator<T> iterator() {
        return new DequeIterator();
    }

    private class DequeIterator implements Iterator<T> {
        private Node pos;
        public DequeIterator() {
            pos = sentinel.next;
        }

        public boolean hasNext() {
            return pos.next != sentinel;
        }

        public T next() {
            T returnItem = pos.item;
            pos = pos.next;
            return returnItem;
        }
    }
}
