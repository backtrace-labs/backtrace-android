package backtraceio.library.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.models.json.BacktraceReport;

public class ReportExceptionTransformer {

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
     * @param sourceReport BacktraceReport
     * @return list of Backtrace reports
     */
    public List<BacktraceReport> transformReportWithInnerExceptions(BacktraceReport sourceReport) {
        final String exceptionTrace = UUID.randomUUID().toString();
        final List<BacktraceReport> reports = new ArrayList<BacktraceReport>() {{
            add(sourceReport);
        }};

        if (!sourceReport.exceptionTypeReport) {
            return reports;
        }
        Throwable throwable = sourceReport.getException();
        if (throwable == null) {
            return reports;
        }
        /**
         * To keep the original report, we're not re-creating it but rather, we copy all known possible values
         * that should be also available in inner exceptions. We should keep the original report, in case of potential
         * changes that could
         */
        String parentId = sourceReport.uuid.toString();
        Map<String, Object> attributes = sourceReport.attributes;
        List<String> attachments = sourceReport.attachmentPaths;
        reports.addAll(this.getSuppressedReports(throwable, attachments, attributes, exceptionTrace, parentId));

        throwable = throwable.getCause();

        while (throwable != null) {
            BacktraceReport report = new BacktraceReport(throwable, attributes);
            report.attachmentPaths.addAll(attachments);
            this.extendReportWithNestedExceptionAttributes(report, exceptionTrace, parentId);
            reports.add(report);

            parentId = report.uuid.toString();
            reports.addAll(this.getSuppressedReports(throwable, attachments, attributes, exceptionTrace, parentId));

            if (!sendInnerExceptions) {
                break;
            }
            throwable = throwable.getCause();
        }

        return reports;
    }

    private List<BacktraceReport> getSuppressedReports(
            Throwable throwable,
            List<String> attachments,
            Map<String, Object> attributes,
            String exceptionTrace,
            String parentId) {
        List<BacktraceReport> reports = new ArrayList<>();
        if (!this.sendSuppressedExceptions) {
            return reports;
        }

        for (Throwable suppressedException :
                throwable.getSuppressed()) {
            BacktraceReport suppressedExceptionReport = new BacktraceReport(suppressedException, attributes);
            this.extendReportWithNestedExceptionAttributes(suppressedExceptionReport, exceptionTrace, parentId);
            suppressedExceptionReport.attachmentPaths.addAll(attachments);
            reports.add(suppressedExceptionReport);
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