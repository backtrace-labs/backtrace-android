package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static backtraceio.library.TestUtils.readFileAsString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import backtraceio.library.common.serializers.deserializers.BacktraceReportDeserializer;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceReportDeserializerTest {
    private final String JSON_FILE = "backtraceReport2.json";

    @Test
    public void testReportDeserializer() throws JSONException {
        // GIVEN
        String json = readFileAsString(this, JSON_FILE);
        BacktraceReportDeserializer deserializer = new BacktraceReportDeserializer();

        // WHEN
        BacktraceReport report = deserializer.deserialize(new JSONObject(json));

        // THEN
        assertNotNull(report);
        assertEquals("e8e77163-8c50-47c5-9814-3b9f64b83825", report.uuid.toString());
        assertEquals(44, report.diagnosticStack.size());
        assertEquals("example-message", report.message);
        assertEquals(false, report.exceptionTypeReport);
        assertEquals("", report.classifier);
        assertNull(report.exception);
        assertEquals(1, report.attachmentPaths.size());
        assertEquals(3, report.attributes.size());
        assertEquals(1732135979, report.timestamp);
    }
}
