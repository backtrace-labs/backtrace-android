package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.ReportExceptionTransformer;

public class ReportExceptionTransformerTest {

    final String innerExceptionMessage = "Inner exception message";
    final String outerExceptionMessage = "Outer exception message";
    final String suppressedExceptionMessage = "Outer exception message";
    final String attributeKey = "attribute-key";
    final Map<String, Object> attributes = new HashMap<String, Object>() {{
        put(attributeKey, "test");
    }};

    @Test
    public void generateReportOnlyForExceptionWithoutInnerExceptions() {
        final Exception exception = new Exception("Exception without inner or suppressed exceptions");
        ReportExceptionTransformer reportExceptionTransformer = new ReportExceptionTransformer();

        List<BacktraceReport> reports = reportExceptionTransformer.transformReportWithInnerExceptions(new BacktraceReport(exception, attributes));

        BacktraceReport exceptionReport = reports.get(0);
        assertEquals(exception.getMessage(), exceptionReport.exception.getMessage());
        assertNull(exceptionReport.attributes.get(ReportExceptionTransformer.ErrorParentIdAttribute));
        assertEquals(exceptionReport.uuid.toString(), exceptionReport.attributes.get(ReportExceptionTransformer.ErrorIdAttribute));
        assertNotNull(exceptionReport.attributes.get(ReportExceptionTransformer.ErrorTraceAttribute));
    }

    @Test
    public void generateReportForInnerAndOuterException() {
        final int expectedNumberOfReports = 2;
        Exception innerException = new Exception(innerExceptionMessage);
        Exception outerException = new Exception(outerExceptionMessage, innerException);
        ReportExceptionTransformer reportExceptionTransformer = new ReportExceptionTransformer();

        reportExceptionTransformer.sendInnerExceptions(true);
        List<BacktraceReport> reports = reportExceptionTransformer.transformReportWithInnerExceptions(new BacktraceReport(outerException, attributes));

        assertEquals(expectedNumberOfReports, reports.size());
        BacktraceReport outerExceptionReport = reports.get(0);
        BacktraceReport innerExceptionReport = reports.get(reports.size() - 1);

        assertEquals(
                outerExceptionReport.uuid.toString(),
                innerExceptionReport.attributes.get(ReportExceptionTransformer.ErrorParentIdAttribute));
        assertEquals(
                outerExceptionReport.uuid.toString(),
                outerExceptionReport.attributes.get(ReportExceptionTransformer.ErrorIdAttribute));
        assertEquals(
                innerExceptionReport.attributes.get(ReportExceptionTransformer.ErrorTraceAttribute),
                outerExceptionReport.attributes.get(ReportExceptionTransformer.ErrorTraceAttribute));
    }

    @Test
    public void doNotGenerateInnerExceptionIfDisabled() {
        Exception innerException = new Exception(innerExceptionMessage);
        Exception outerException = new Exception(outerExceptionMessage, innerException);
        ReportExceptionTransformer reportExceptionTransformer = new ReportExceptionTransformer();

        reportExceptionTransformer.sendInnerExceptions(false);
        List<BacktraceReport> reports = reportExceptionTransformer.transformReportWithInnerExceptions(new BacktraceReport(outerException, attributes));

        assertEquals(1, reports.size());
        BacktraceReport outerExceptionReport = reports.get(0);
        assertEquals(outerExceptionMessage, outerExceptionReport.exception.getMessage());
    }

    @Test
    public void generateReportForSuppressedException() {
        final int expectedNumberOfReports = 2;
        Exception suppressedException = new Exception(suppressedExceptionMessage);
        Exception exception = new Exception(outerExceptionMessage);
        exception.addSuppressed(suppressedException);
        ReportExceptionTransformer reportExceptionTransformer = new ReportExceptionTransformer();

        reportExceptionTransformer.sendSuppressedExceptions(true);
        List<BacktraceReport> reports = reportExceptionTransformer.transformReportWithInnerExceptions(new BacktraceReport(exception, attributes));

        assertEquals(expectedNumberOfReports, reports.size());
        BacktraceReport outerExceptionReport = reports.get(0);
        BacktraceReport suppressedExceptionReport = reports.get(reports.size() - 1);

        assertEquals(suppressedExceptionMessage, suppressedExceptionReport.exception.getMessage());
        assertEquals(
                outerExceptionReport.uuid.toString(),
                suppressedExceptionReport.attributes.get(ReportExceptionTransformer.ErrorParentIdAttribute));
        assertEquals(
                outerExceptionReport.uuid.toString(),
                outerExceptionReport.attributes.get(ReportExceptionTransformer.ErrorIdAttribute));
        assertEquals(
                suppressedExceptionReport.attributes.get(ReportExceptionTransformer.ErrorTraceAttribute),
                outerExceptionReport.attributes.get(ReportExceptionTransformer.ErrorTraceAttribute));
    }

    @Test
    public void doNotGenerateSuppressedExceptionIfDisabled() {
        Exception suppressedException = new Exception(suppressedExceptionMessage);
        Exception exception = new Exception(outerExceptionMessage);
        exception.addSuppressed(suppressedException);
        ReportExceptionTransformer reportExceptionTransformer = new ReportExceptionTransformer();

        reportExceptionTransformer.sendSuppressedExceptions(false);
        List<BacktraceReport> reports = reportExceptionTransformer.transformReportWithInnerExceptions(new BacktraceReport(exception, attributes));

        assertEquals(1, reports.size());
        BacktraceReport exceptionReport = reports.get(0);
        assertEquals(outerExceptionMessage, exceptionReport.exception.getMessage());
    }

    @Test
    public void generateReportForInnerSuppressedAndOuterException() {
        final int expectedNumberOfReports = 3;
        Exception suppressedException = new Exception(suppressedExceptionMessage);
        Exception innerException = new Exception(innerExceptionMessage);
        innerException.addSuppressed(suppressedException);
        Exception outerException = new Exception(outerExceptionMessage, innerException);
        ReportExceptionTransformer reportExceptionTransformer = new ReportExceptionTransformer();

        reportExceptionTransformer.sendInnerExceptions(true);
        reportExceptionTransformer.sendSuppressedExceptions(true);
        List<BacktraceReport> reports = reportExceptionTransformer.transformReportWithInnerExceptions(new BacktraceReport(outerException, attributes));

        assertEquals(expectedNumberOfReports, reports.size());
        BacktraceReport outerExceptionReport = reports.get(0);
        BacktraceReport innerExceptionReport = reports.get(1);
        BacktraceReport suppressedExceptionReport = reports.get(reports.size() - 1);
        assertEquals(outerException.getMessage(), outerExceptionReport.exception.getMessage());
        assertEquals(innerException.getMessage(), innerExceptionReport.exception.getMessage());
        assertEquals(suppressedException.getMessage(), suppressedExceptionReport.exception.getMessage());
    }

    @Test
    public void reportsHasCorrectlySetAttributes() {
        final int expectedNumberOfReports = 3;
        Exception suppressedException = new Exception(suppressedExceptionMessage);
        Exception innerException = new Exception(innerExceptionMessage);
        innerException.addSuppressed(suppressedException);
        Exception outerException = new Exception(outerExceptionMessage, innerException);
        ReportExceptionTransformer reportExceptionTransformer = new ReportExceptionTransformer();

        reportExceptionTransformer.sendInnerExceptions(true);
        reportExceptionTransformer.sendSuppressedExceptions(true);
        List<BacktraceReport> reports = reportExceptionTransformer.transformReportWithInnerExceptions(new BacktraceReport(outerException, attributes));

        assertEquals(expectedNumberOfReports, reports.size());
        BacktraceReport outerExceptionReport = reports.get(0);
        BacktraceReport innerExceptionReport = reports.get(1);
        BacktraceReport suppressedExceptionReport = reports.get(reports.size() - 1);

        Object attributeValue = attributes.get(attributeKey);
        assertEquals(attributeValue, outerExceptionReport.attributes.get(attributeKey));
        assertEquals(attributeValue, innerExceptionReport.attributes.get(attributeKey));
        assertEquals(attributeValue, suppressedExceptionReport.attributes.get(attributeKey));
    }
}
