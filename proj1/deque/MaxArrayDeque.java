package deque;

import java.util.Comparator;

public class MaxArrayDeque<T> extends ArrayDeque<T>{
    private Comparator<T> myComparator;
    public MaxArrayDeque(Comparator<T> c) {
        super();
        this.myComparator = c;
    }

    public T max() {
        return max(myComparator);
    }

    public T max(Comparator<T> c) {
        if (isEmpty()) {
            return null;
        }
        T maxVal = get(0);
        for (int i = 1; i < size(); i += 1) {
            T currentVal = get(i);
            if (c.compare(maxVal, currentVal) < 0) {
                maxVal = currentVal;
            }
        }
        return maxVal;
    }
}
