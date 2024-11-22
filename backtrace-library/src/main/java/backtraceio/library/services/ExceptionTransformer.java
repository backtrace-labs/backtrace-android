package backtraceio.library.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.models.json.BacktraceReport;

public class ExceptionTransformer {

    /**
     * Error trace attribute key
     */
    public static final String ErrorTraceAttribute = "error.trace";

    /**
     * Current error id attribute key
     */
    public static final String ErrorIdAttribute = "error.id";

    /**
     * Parent id attribute key
     */
    public static final String ErrorParentIdAttribute = "error.parent";

    private boolean sendSuppressedExceptions = true;
    private boolean sendInnerExceptions = true;

    /**
     * Determine if reports should be generated for inner exceptions
     *
     * @param sendInnerExceptions boolean flag that enabled/disable sending inner exceptions
     */
    public void sendInnerExceptions(boolean sendInnerExceptions) {
        this.sendInnerExceptions = sendInnerExceptions;
    }

    /**
     * Determine if reports should be generated for suppressed exceptions
     *
     * @param sendSuppressedExceptions boolean flag that enabled/disable sending suppressed exceptions
     */
    public void sendSuppressedExceptions(boolean sendSuppressedExceptions) {
        this.sendSuppressedExceptions = sendSuppressedExceptions;
    }

    /**
     * Transforms throwable into an array of Backtrace reports. During the transformation, inner exception and suppressed exceptions
     * are being converted into Backtrace reports with a proper exception reference.
     *
     * @param throwable  throwable
     * @param attributes report attributes - attributes will be included in each report
     * @return list of Backtrace reports
     */
    public List<BacktraceReport> transformExceptionIntoReports(Throwable throwable, Map<String, Object> attributes) {
        final String exceptionTrace = UUID.randomUUID().toString();
        final List<BacktraceReport> reports = new ArrayList<>();
        String parentId = null;

        while (throwable != null) {
            BacktraceReport report = new BacktraceReport(throwable, attributes);
            this.extendReportWithNestedExceptionAttributes(report, exceptionTrace, parentId);
            reports.add(report);

            parentId = report.uuid.toString();
            if (sendSuppressedExceptions) {
                for (Throwable suppressedException :
                        throwable.getSuppressed()) {
                    BacktraceReport suppressedExceptionReport = new BacktraceReport(suppressedException, attributes);
                    this.extendReportWithNestedExceptionAttributes(suppressedExceptionReport, exceptionTrace, parentId);
                    reports.add(suppressedExceptionReport);
                }
            }

            if (!sendInnerExceptions) {
                break;
            }
            throwable = throwable.getCause();
        }

        return reports;
    }

    /**
     * Add exception trace attributes to the nested exception
     *
     * @param report         Backtrace report
     * @param exceptionTrace trace id
     * @param parentId       parent id
     */
    private void extendReportWithNestedExceptionAttributes(BacktraceReport report, String exceptionTrace, String parentId) {
        report.attributes.put(ErrorTraceAttribute, exceptionTrace);
        report.attributes.put(ErrorIdAttribute, report.uuid.toString());
        report.attributes.put(ErrorParentIdAttribute, parentId);
    }
}
