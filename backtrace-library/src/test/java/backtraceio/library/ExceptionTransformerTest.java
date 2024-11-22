package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.ExceptionTransformer;

public class ExceptionTransformerTest {

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
        ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

        List<BacktraceReport> reports = exceptionTransformer.transformExceptionIntoReports(exception, attributes);

        BacktraceReport exceptionReport = reports.get(0);
        assertEquals(exception.getMessage(), exceptionReport.exception.getMessage());
        assertNull(exceptionReport.attributes.get(ExceptionTransformer.ErrorParentIdAttribute));
        assertEquals(exceptionReport.uuid.toString(), exceptionReport.attributes.get(ExceptionTransformer.ErrorIdAttribute));
        assertNotNull(exceptionReport.attributes.get(ExceptionTransformer.ErrorTraceAttribute));
    }

    @Test
    public void generateReportForInnerAndOuterException() {
        final int expectedNumberOfReports = 2;
        Exception innerException = new Exception(innerExceptionMessage);
        Exception outerException = new Exception(outerExceptionMessage, innerException);
        ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

        exceptionTransformer.sendInnerExceptions(true);
        List<BacktraceReport> reports = exceptionTransformer.transformExceptionIntoReports(outerException, attributes);

        assertEquals(expectedNumberOfReports, reports.size());
        BacktraceReport outerExceptionReport = reports.get(0);
        BacktraceReport innerExceptionReport = reports.get(reports.size() - 1);

        assertEquals(
                outerExceptionReport.uuid.toString(),
                innerExceptionReport.attributes.get(ExceptionTransformer.ErrorParentIdAttribute));
        assertEquals(
                outerExceptionReport.uuid.toString(),
                outerExceptionReport.attributes.get(ExceptionTransformer.ErrorIdAttribute));
        assertEquals(
                innerExceptionReport.attributes.get(ExceptionTransformer.ErrorTraceAttribute),
                outerExceptionReport.attributes.get(ExceptionTransformer.ErrorTraceAttribute));
    }

    @Test
    public void DoNotGenerateInnerExceptionIfDisabled() {
        Exception innerException = new Exception(innerExceptionMessage);
        Exception outerException = new Exception(outerExceptionMessage, innerException);
        ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

        exceptionTransformer.sendInnerExceptions(false);
        List<BacktraceReport> reports = exceptionTransformer.transformExceptionIntoReports(outerException, attributes);

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
        ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

        exceptionTransformer.sendSuppressedExceptions(true);
        List<BacktraceReport> reports = exceptionTransformer.transformExceptionIntoReports(exception, attributes);

        assertEquals(expectedNumberOfReports, reports.size());
        BacktraceReport outerExceptionReport = reports.get(0);
        BacktraceReport suppressedExceptionReport = reports.get(reports.size() - 1);

        assertEquals(suppressedExceptionMessage, suppressedExceptionReport.exception.getMessage());
        assertEquals(
                outerExceptionReport.uuid.toString(),
                suppressedExceptionReport.attributes.get(ExceptionTransformer.ErrorParentIdAttribute));
        assertEquals(
                outerExceptionReport.uuid.toString(),
                outerExceptionReport.attributes.get(ExceptionTransformer.ErrorIdAttribute));
        assertEquals(
                suppressedExceptionReport.attributes.get(ExceptionTransformer.ErrorTraceAttribute),
                outerExceptionReport.attributes.get(ExceptionTransformer.ErrorTraceAttribute));
    }

    @Test
    public void DoNotGenerateSuppressedExceptionIfDisabled() {
        Exception suppressedException = new Exception(suppressedExceptionMessage);
        Exception exception = new Exception(outerExceptionMessage);
        exception.addSuppressed(suppressedException);
        ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

        exceptionTransformer.sendSuppressedExceptions(false);
        List<BacktraceReport> reports = exceptionTransformer.transformExceptionIntoReports(exception, attributes);

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
        ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

        exceptionTransformer.sendInnerExceptions(true);
        exceptionTransformer.sendSuppressedExceptions(true);
        List<BacktraceReport> reports = exceptionTransformer.transformExceptionIntoReports(outerException, attributes);

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
        ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

        exceptionTransformer.sendInnerExceptions(true);
        exceptionTransformer.sendSuppressedExceptions(true);
        List<BacktraceReport> reports = exceptionTransformer.transformExceptionIntoReports(outerException, attributes);

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
