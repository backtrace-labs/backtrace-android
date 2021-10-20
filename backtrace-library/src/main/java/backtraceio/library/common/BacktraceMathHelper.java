package backtraceio.library.common;

import java.util.Random;

public class BacktraceMathHelper {
    public static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static double uniform(double minimum, double maximum) {
        return (new Random().nextDouble()) * (maximum - minimum) + minimum;
    }
}
