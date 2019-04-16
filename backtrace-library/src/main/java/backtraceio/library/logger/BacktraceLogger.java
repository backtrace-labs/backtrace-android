package backtraceio.library.logger;

import android.util.Log;

public class BacktraceLogger {

    private static LogLevel logLevel = LogLevel.OFF;
    private static final String BASE_TAG = "BacktraceLogger: ";

    public static void setLevel(LogLevel level) {
        BacktraceLogger.logLevel = level;
    }

    public static int d(String tag, String message){
        if(BacktraceLogger.logLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
            return Log.d(getTag(tag), message);
        }
        return 0;
    }

    public static int w(String tag, String message){
        if(BacktraceLogger.logLevel.ordinal() <= LogLevel.WARN.ordinal()) {
            return Log.w(getTag(tag), message);
        }
        return 0;
    }

    public static int e(String tag, String message){
        if(BacktraceLogger.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            return Log.e(getTag(tag), message);
        }
        return 0;
    }

    public static int e(String tag, String message, Throwable tr){
        if(BacktraceLogger.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            return Log.e(getTag(tag), message, tr);
        }
        return 0;
    }

    private static String getTag(String tag){
        return BacktraceLogger.BASE_TAG + tag;
    }
}