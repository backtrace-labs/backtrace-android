package backtraceio.library.common;

public class BacktraceTimeHelper {
    /**
     * Get timestamp in seconds
     */
    public static long getTimestampSeconds() {
        return System.currentTimeMillis() / 1000;
    }
}
