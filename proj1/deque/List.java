package deque;

import java.util.Iterator;

public interface List<T> {
    public void addFirst(T item);

    public void addLast(T item);

    public boolean isEmpty(T item);

    public int size();

    public void printDeque();

    public T removeFirst();

    public T removeLast();

    public T get(int index);

    public boolean equals(Object o);
}
