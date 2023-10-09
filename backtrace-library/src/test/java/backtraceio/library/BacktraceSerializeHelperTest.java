package backtraceio.library;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

import junit.framework.Assert;

import org.junit.Test;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceSerializeHelperTest {
    @Test
    public void testSerialization() {
        // GIVEN
        BacktraceResult backtraceResult = new BacktraceResult(null, "result-message", BacktraceResultStatus.Ok);
        // WHEN
        String json = BacktraceSerializeHelper.toJson(backtraceResult);
        // THEN
        assertEquals("{\"message\":\"result-message\",\"status\":\"Ok\"}", json);
    }

    @Test
    public void testDeserialization() {
        // GIVEN
        String json = "{\"_rxid\": \"12345\", \"message\":\"result-message\",\"status\":\"Ok\"}";

        // WHEN
        BacktraceResult result = BacktraceSerializeHelper.fromJson(json ,BacktraceResult.class);

        // THEN
        assertNotNull(result);
        assertEquals("result-message", result.message);
        assertEquals("12345", result.rxId);
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertNull(result.getBacktraceReport());
    }
}
