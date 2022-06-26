package deque;
import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.*;

public class MaxArrayDequeTest {
    @Test
    public void testMax() {
        Comparator<Integer> c = new Comparator<Integer>() {
            @Override
            public int compare(Integer integer, Integer t1) {
                return integer - t1;
            }
        };

        MaxArrayDeque<Integer> mad = new MaxArrayDeque<Integer>(c);
        for (int i = 0; i < 7; i += 1) {
            mad.addLast(i);
        }
        int a = mad.max().intValue();
        assertEquals(6, a);
    }
}
