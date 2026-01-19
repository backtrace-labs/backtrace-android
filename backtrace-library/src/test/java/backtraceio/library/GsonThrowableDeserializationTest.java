package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;

import backtraceio.gson.JsonParseException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import backtraceio.library.common.BacktraceSerializeHelper;


public class GsonThrowableDeserializationTest {
    @Test
    public void testDeserializeException() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "serializedException.json");

        // WHEN
        IllegalArgumentException deserializedException = BacktraceSerializeHelper.fromJson(json, IllegalArgumentException.class);

        // THEN
        assertNotNull(deserializedException);
        assertEquals("test-msg", deserializedException.getMessage());
        assertNull(deserializedException.getCause()); // No cause in this specific JSON
        assertEquals(2, deserializedException.getStackTrace().length);
        assertEquals("sample-class-1", deserializedException.getStackTrace()[0].getClassName());
        assertEquals("method-1", deserializedException.getStackTrace()[0].getMethodName());
        assertEquals("file-name1", deserializedException.getStackTrace()[0].getFileName());
        assertEquals(100, deserializedException.getStackTrace()[0].getLineNumber());
        assertEquals("sample-class-2", deserializedException.getStackTrace()[1].getClassName());
        assertEquals("method-2", deserializedException.getStackTrace()[1].getMethodName());
        assertEquals("file-name2", deserializedException.getStackTrace()[1].getFileName());
        assertEquals(200, deserializedException.getStackTrace()[1].getLineNumber());
    }

    @Test
    public void testDeserializeThrowableWithoutCause() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "serializedThrowable.json");

        // WHEN
        Throwable deserializedThrowable = BacktraceSerializeHelper.fromJson(json, Throwable.class);

        // THEN
        assertNotNull(deserializedThrowable);
        assertEquals("Something went wrong", deserializedThrowable.getMessage());
        assertNull(deserializedThrowable.getCause());
        assertEquals(2, deserializedThrowable.getStackTrace().length);
        assertEquals("sample-class-1", deserializedThrowable.getStackTrace()[0].getClassName());
        assertEquals("method-1", deserializedThrowable.getStackTrace()[0].getMethodName());
    }

    @Test
    public void testDeserializeThrowableWithCause() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "serializedThrowableWithCause.json");

        // WHEN
        Throwable deserializedThrowable = BacktraceSerializeHelper.fromJson(json, Throwable.class);

        // THEN
        assertNotNull(deserializedThrowable);
        assertEquals("Something went wrong", deserializedThrowable.getMessage());
        assertNotNull(deserializedThrowable.getCause());
        assertEquals("test-msg", deserializedThrowable.getCause().getMessage());
        assertEquals(2, deserializedThrowable.getStackTrace().length);
        assertEquals("sample-class-1", deserializedThrowable.getStackTrace()[0].getClassName());
        assertEquals("method-1", deserializedThrowable.getStackTrace()[0].getMethodName());
    }

    @NonNull
    private static StackTraceElement[] generateStackTraceElements() {
        List<StackTraceElement> elements = new ArrayList<StackTraceElement>() {{
            add(new StackTraceElement("sample-class-1", "method-1", "file-name1", 100));
            add(new StackTraceElement("sample-class-2", "method-2", "file-name2", 200));
        }};
        return elements.toArray(new StackTraceElement[0]);
    }

    @Test
    public void testDeserializeThrowableNoCause() {
        // GIVEN
        // We reuse serializedThrowable.json but for this test, we are interested in a Throwable without a cause.
        // The BacktraceSerializeHelper.fromJson for Throwable will create a generic Throwable if specific exception type is not known
        // It might not reconstruct the exact original type (e.g. if it was an Exception originally)
        // For this test, let's create a JSON that represents a simple Throwable without a cause.
        Throwable originalThrowable = new Throwable("Simple throwable message");
        originalThrowable.setStackTrace(generateStackTraceElements());
        String json = BacktraceSerializeHelper.toJson(originalThrowable);


        // WHEN
        Throwable deserializedThrowable = BacktraceSerializeHelper.fromJson(json, Throwable.class);

        // THEN
        assertNotNull(deserializedThrowable);
        assertEquals("Simple throwable message", deserializedThrowable.getMessage());
        assertNull(deserializedThrowable.getCause());
        assertEquals(2, deserializedThrowable.getStackTrace().length);
        assertEquals("sample-class-2", deserializedThrowable.getStackTrace()[1].getClassName());
        assertEquals("method-2", deserializedThrowable.getStackTrace()[1].getMethodName());
    }


    @Test
    public void testDeserializeError() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "serializedError.json");

        // WHEN
        Error deserializedError = BacktraceSerializeHelper.fromJson(json, Error.class);

        // THEN
        assertNotNull(deserializedError);
        assertEquals("Critical system error", deserializedError.getMessage());
        assertNull(deserializedError.getCause());
        assertEquals(2, deserializedError.getStackTrace().length);
        assertEquals("sample-class-1", deserializedError.getStackTrace()[0].getClassName());
    }

    @Test
    public void testDeserializeExceptionWithCause() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "serializedExceptionWithCause.json");

        // WHEN
        Exception deserializedException = BacktraceSerializeHelper.fromJson(json, Exception.class);

        // THEN
        assertNotNull(deserializedException);
        assertEquals("test-msg", deserializedException.getMessage()); // Gson might serialize the cause's message into the main message
        assertNotNull(deserializedException.getCause());
        assertEquals("test-msg", deserializedException.getCause().getMessage());
        assertEquals(2, Objects.requireNonNull(deserializedException.getCause()).getStackTrace().length);
        assertEquals("sample-class-1", deserializedException.getCause().getStackTrace()[0].getClassName());
    }

    @Test(expected = JsonParseException.class)
    public void testDeserializeInvalidJson() {
        // GIVEN
        String invalidJson = "{ \"message\": \"test\", \"stackTrace\": [invalid";

        // WHEN
        BacktraceSerializeHelper.fromJson(invalidJson, Exception.class);

        // THEN
        // Expected JsonParseException
    }

    @Test
    public void testDeserializeEmptyJsonToThrowable() {
        // GIVEN
        String emptyJson = "{}";

        // WHEN
        Throwable deserializedThrowable = BacktraceSerializeHelper.fromJson(emptyJson, Throwable.class);

        // THEN
        assertNotNull(deserializedThrowable);
        assertNull(deserializedThrowable.getMessage());
        assertNull(deserializedThrowable.getCause());
        assertNotNull(deserializedThrowable.getStackTrace());
    }

    @Test
    public void testDeserializeJsonWithOnlyMessage() {
        // GIVEN
        String jsonWithOnlyMessage = "{ \"message\": \"Error occurred\" }";

        // WHEN
        Exception deserializedException = BacktraceSerializeHelper.fromJson(jsonWithOnlyMessage, Exception.class);

        // THEN
        assertNotNull(deserializedException);
        assertEquals("Error occurred", deserializedException.getMessage());
        assertNull(deserializedException.getCause());
        assertNotNull(deserializedException.getStackTrace());
    }
}
