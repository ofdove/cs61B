package deque;

import java.util.Iterator;

public interface Deque<T> {
    void addFirst(T item);

    void addLast(T item);

    default boolean isEmpty() {
        int size = this.size();
        if (size == 0) {
            return true;
        }
        return false;
    }

    int size();

    void printDeque();

    T removeFirst();

    T removeLast();

    T get(int index);

    boolean equals(Object o);

    Iterator<T> iterator();
}
