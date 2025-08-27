package backtraceio.library;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceSerializeHelperTest {
    static class MyCustomException extends Exception {

        public MyCustomException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Test
    public void testSerializeAndDeserializeException() {
        // GIVEN
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("test-exception-1");
        illegalArgumentException.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("test-1", "test-2", "test-3", 1)
        });

        MyCustomException exception = new MyCustomException("test-exception-2", illegalArgumentException);

        // WHEN
        String json = BacktraceSerializeHelper.toJson(exception);

        MyCustomException deserializedException = BacktraceSerializeHelper.fromJson(json, MyCustomException.class);

        // THEN
        assertNotNull(deserializedException);
        assertEquals("MyCustomException", deserializedException.getClass().getSimpleName());
        assertEquals("test-exception-2", deserializedException.getMessage());
        assertTrue(deserializedException.getStackTrace().length > 0);

        Throwable cause = deserializedException.getCause();
        assertNotNull(cause);
        assertEquals("test-1", cause.getStackTrace()[0].getClassName());
        assertEquals("test-2", cause.getStackTrace()[0].getMethodName());
        assertEquals("test-3", cause.getStackTrace()[0].getFileName());
        assertEquals(1, cause.getStackTrace()[0].getLineNumber());
        assertEquals("IllegalArgumentException", cause.getClass().getSimpleName());
        assertEquals("test-exception-1", cause.getMessage());
        assertEquals(1, cause.getStackTrace().length);
    }

    @Test
    public void testSerializeAndDeserializeError() {
        // GIVEN
        OutOfMemoryError outOfMemoryError = new OutOfMemoryError("test-error-1");
        outOfMemoryError.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("test-1", "test-2", "test-3", 1)
        });

        ExceptionInInitializerError error = new ExceptionInInitializerError(outOfMemoryError);

        // WHEN
        String json = BacktraceSerializeHelper.toJson(error);

        ExceptionInInitializerError deserializedError = BacktraceSerializeHelper.fromJson(json, ExceptionInInitializerError.class);

        // THEN
        assertNotNull(deserializedError);
        assertEquals("ExceptionInInitializerError", deserializedError.getClass().getSimpleName());
        assertNull(deserializedError.getMessage());
        assertTrue(deserializedError.getStackTrace().length > 0);

        Throwable cause = deserializedError.getCause();
        assertNotNull(cause);
        assertEquals("test-1", cause.getStackTrace()[0].getClassName());
        assertEquals("test-2", cause.getStackTrace()[0].getMethodName());
        assertEquals("test-3", cause.getStackTrace()[0].getFileName());
        assertEquals(1, cause.getStackTrace()[0].getLineNumber());
        assertEquals("test-error-1", cause.getMessage());
        assertEquals(1, cause.getStackTrace().length);
    }

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
