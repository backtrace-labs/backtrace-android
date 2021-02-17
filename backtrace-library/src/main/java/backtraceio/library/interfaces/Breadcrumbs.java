package backtraceio.library.interfaces;

import java.util.Map;

import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;

public interface Breadcrumbs {
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level);
}
