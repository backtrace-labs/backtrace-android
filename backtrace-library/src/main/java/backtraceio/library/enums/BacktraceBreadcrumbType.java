package backtraceio.library.enums;

/**
 * @Note: These are also maintained in the public header backtrace-android.h
 */
public enum BacktraceBreadcrumbType {
    MANUAL,
    LOG,
    NAVIGATION,
    HTTP,
    SYSTEM,
    USER,
    CONFIGURATION;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
