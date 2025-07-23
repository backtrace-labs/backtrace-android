package backtraceio.library;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

import org.junit.Test;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceSerializeHelperTest {
    @Test
    public void testSerializeException() {
        // GIVEN
        Exception exception = new Exception("test-msg");
        StackTraceElement[] elements = new StackTraceElement[1];
        elements[0] = new StackTraceElement("class", "method", "file-name", 1);
        exception.setStackTrace(elements);
        // WHEN
        String json = BacktraceSerializeHelper.toJson(exception);
        String result = "{\n" +
                "  \"detail-message\" : \"test-msg\",\n" +
                "  \"stack-trace\" : [ {\n" +
                "    \"declaring-class\" : \"class\",\n" +
                "    \"method-name\" : \"method\",\n" +
                "    \"file-name\" : \"file-name\",\n" +
                "    \"line-number\" : 1,\n" +
                "    \"format\" : 0\n" +
                "  } ],\n" +
                "  \"suppressed-exceptions\" : [ ]\n" +
                "}";
        // THEN
        assertEquals("{\"message\":\"result-message\",\"status\":\"Ok\"}", json);
    }

    @Test
    public void testSerializeAndDeserializeException() {
        // GIVEN
        Exception exception = new Exception("test-msg");
        StackTraceElement[] elements = new StackTraceElement[1];
        elements[0] = new StackTraceElement("class", "method", "file-name", 1);
        exception.setStackTrace(elements);
        // WHEN
        String json = BacktraceSerializeHelper.toJson(exception);
        Exception result = BacktraceSerializeHelper.fromJson(json, Exception.class);
        // THEN
        assertEquals(result.getMessage(), "");
        assertEquals(result.getStackTrace().length, 1);
//        assertEquals("{\"message\":\"result-message\",\"status\":\"Ok\"}", json);
    }

    @Test
    public void testSerialization() {
        Throwable x = new Throwable("test");
//        x.
//        x.setStackTrace( );
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
