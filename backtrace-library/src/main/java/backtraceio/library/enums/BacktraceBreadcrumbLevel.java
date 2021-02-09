package backtraceio.library.enums;

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
