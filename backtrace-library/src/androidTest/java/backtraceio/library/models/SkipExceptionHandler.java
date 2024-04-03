package backtraceio.library.models;

public class SkipExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.err.println("Exception caught in thread " + t.getName() + ":");
        e.printStackTrace();
        System.err.println("Exception skipped");
    }
}
