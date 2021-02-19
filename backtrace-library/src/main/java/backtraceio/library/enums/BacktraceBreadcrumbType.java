package backtraceio.library.enums;

import java.util.EnumSet;

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

    public static final EnumSet<BacktraceBreadcrumbType> ALL = EnumSet.allOf(BacktraceBreadcrumbType.class);

    public static final EnumSet<BacktraceBreadcrumbType> NONE = EnumSet.noneOf(BacktraceBreadcrumbType.class);
}
