package deque;

import net.sf.saxon.z.IntIterator;

import java.util.Iterator;

public class ArrayDeque<T> implements Deque<T>, Iterable<T> {
    private T[] items;
    private int size;
    private int nextFirst;
    private int nextLast;

    public ArrayDeque() {
        items = (T[]) new Object[8];
        nextFirst = items.length / 2 - 1;
        nextLast = items.length / 2;
        size = 0;
    }
    @Override
    public int size() {
        return size;
    }

    private boolean isFull() {
        return size == items.length;
    }

    private boolean isWasting() {
        return size < items.length / 4 & size > 8;
    }

    private void resize() {
        int newSize = 2 * size;
        T[] a = (T[]) new Object[newSize];
        for (int i = 0; i < size; i += 1) {
            a[i] = get(i);
        }
        nextFirst = a.length - 1;
        nextLast = size;
        items = a;
    }
    @Override
    public void addFirst(T item) {
        if (isFull()) {
            resize();
        }
        items[nextFirst] = item;
        if (nextFirst - 1 == -1) {
            nextFirst = items.length - 1;
        } else {
            nextFirst -= 1;
        }
        size += 1;
    }

    @Override
    public void addLast(T item) {
        if (isFull()) {
            resize();
        }
        items[nextLast] = item;
        if (nextLast + 1 == items.length) {
            nextLast = 0;
        } else {
            nextLast += 1;
        }
        size += 1;
    }

    @Override
    public T get(int index) {
        if (index < size) {
            if (nextFirst + 1 + index >= items.length) {
                return items[nextFirst + 1 + index - items.length];
            } else {
                return items[nextFirst + 1 + index];
            }
        }
        return null;
    }

    @Override
    public void printDeque() {
        if (nextFirst + 1 >= nextLast & size > 0) {
            for (int i = nextFirst + 1; i < items.length; i += 1) {
                System.out.print(items[i] + " ");
            }
            for (int i = 0; i < nextLast; i += 1) {
                System.out.print(items[i] + " ");
            }
        } else {
            for (int i = nextFirst + 1; i < nextLast; i += 1) {
                System.out.print(items[i] + " ");
            }
        }
    }

    @Override
    public T removeFirst() {
        if (isWasting()) {
            resize();
        }
        if (isEmpty()) {
            return null;
        } else if (nextFirst + 1 == items.length) {
            T first = items[0];
            nextFirst = 0;
            items[nextFirst] = null;
            size -= 1;
            return first;
        } else {
            T first = items[nextFirst + 1];
            items[nextFirst + 1] = null;
            nextFirst += 1;
            size -= 1;
            return first;
        }
    }

    @Override
    public T removeLast() {
        if (isWasting()) {
            resize();
        }
        if (isEmpty()) {
            return null;
        } else if (nextLast - 1 == -1) {
            T last = items[items.length - 1];
            nextLast = items.length - 1;
            items[nextLast] = null;
            size -= 1;
            return last;
        } else {
            T last = items[nextLast - 1];
            items[nextLast - 1] = null;
            nextLast -= 1;
            size -= 1;
            return last;
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof ArrayDeque)) {
            return false;
        } else {
            Deque d = (Deque) o;
            if (d.size() != this.size) {
                return false;
            } else {
                for (int i = 0; i < this.size(); i += 1) {
                    if (!(this.get(i).equals(d.get(i)))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Iterator<T> iterator() {
        return new DequeIterator();
    }

    private class DequeIterator implements Iterator<T> {
        private int pos;

        DequeIterator() {
            pos = 0;
        }
        public boolean hasNext() {
            return pos < size();
        }

        public T next() {
            T returnItem = get(pos);
            pos += 1;
            return returnItem;
        }
    }
}


