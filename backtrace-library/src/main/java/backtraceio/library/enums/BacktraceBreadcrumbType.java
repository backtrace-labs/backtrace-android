package backtraceio.library.enums;

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
