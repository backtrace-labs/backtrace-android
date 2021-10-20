package backtraceio.library.enums;

import java.util.EnumSet;

/**
 * @Note: These are also maintained in the public header backtrace-android.h
 */
public enum BacktraceBreadcrumbLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL;

    public static final EnumSet<BacktraceBreadcrumbLevel> ALL = EnumSet.allOf(BacktraceBreadcrumbLevel.class);
    public static final EnumSet<BacktraceBreadcrumbLevel> NONE = EnumSet.noneOf(BacktraceBreadcrumbLevel.class);

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
