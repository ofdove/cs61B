package timingtest;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeAList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void main(String[] args) {
        timeAListConstruction();
    }

    public static void timeAListConstruction() {
        // TODO: YOUR CODE HERE
        AList Ns = new AList<Integer>();
        AList ops = new AList<Integer>();
        AList times = new AList<Double>();
        AList Tom = new AList<Integer>();
        for (int i = 1000; i <= 128000; i *= 2) {
            Stopwatch sw = new Stopwatch();
            Ns.addLast(i);
            ops.addLast(i);
            for (int j = 0; j < i; j += 1) {
                Tom.addLast(j);
            }
            double time = sw.elapsedTime();
            times.addLast(time);
        }
        printTimingTable(Ns, times, Ns);
    }
}
