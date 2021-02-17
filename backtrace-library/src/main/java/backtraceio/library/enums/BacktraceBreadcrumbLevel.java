package backtraceio.library.enums;

/**
 * @Note: These are also maintained in the public header backtrace-android.h
 */
public enum BacktraceBreadcrumbLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
