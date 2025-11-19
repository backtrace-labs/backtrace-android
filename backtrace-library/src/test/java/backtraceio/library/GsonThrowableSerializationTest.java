package backtraceio.library;

import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import backtraceio.library.common.BacktraceSerializeHelper;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.junit.Test;

public class GsonThrowableSerializationTest {
    @Test
    public void testSerializeException() throws JSONException {
        // GIVEN
        Exception exception = generateException();

        // WHEN
        String json = BacktraceSerializeHelper.toJson(exception);

        // THEN
        String expectedJson = TestUtils.minifyJsonString(TestUtils.readFileAsString(this, "serializedException.json"));

        assertTrue(TestUtils.compareJson(json, expectedJson));
    }

    @Test
    public void testSerializeThrowableWithCause() throws JSONException {
        // GIVEN
        Exception exception = generateException();
        Throwable throwable = new Throwable("Something went wrong", exception);
        throwable.setStackTrace(generateStackTraceElements());

        // WHEN
        String json = BacktraceSerializeHelper.toJson(throwable);

        // THEN
        String expectedJson =
                TestUtils.minifyJsonString(TestUtils.readFileAsString(this, "serializedThrowableWithCause.json"));

        assertTrue(TestUtils.compareJson(json, expectedJson));
    }

    @Test
    public void testSerializeThrowable() throws JSONException {
        // GIVEN
        Throwable throwable = new Throwable("Something went wrong");
        throwable.setStackTrace(GsonThrowableSerializationTest.generateStackTraceElements());

        // WHEN
        String json = BacktraceSerializeHelper.toJson(throwable);

        // THEN
        String expectedJson = TestUtils.minifyJsonString(TestUtils.readFileAsString(this, "serializedThrowable.json"));

        assertTrue(TestUtils.compareJson(json, expectedJson));
    }

    @Test
    public void testSerializeError() throws JSONException {
        // GIVEN
        Error error = new Error("Critical system error");
        error.setStackTrace(generateStackTraceElements());

        // WHEN
        String json = BacktraceSerializeHelper.toJson(error);

        // THEN
        String expectedJson = TestUtils.minifyJsonString(TestUtils.readFileAsString(this, "serializedError.json"));

        assertTrue(TestUtils.compareJson(json, expectedJson));
    }

    @Test
    public void serializeMultiCauseException() throws JSONException {
        // GIVEN
        IllegalArgumentException e1 = new IllegalArgumentException("1");
        JSONException e2 = new JSONException("2", e1);
        RuntimeException e3 = new RuntimeException("3", e2);
        AssertionError e4 = new AssertionError("4", e3);

        Throwable[] throwableArray = {e1, e2, e3, e4};

        for (Throwable t : throwableArray) {
            t.setStackTrace(generateStackTraceElements());
        }

        // WHEN
        String json = BacktraceSerializeHelper.toJson(e4);

        // THEN
        String expectedJson =
                TestUtils.minifyJsonString(TestUtils.readFileAsString(this, "serializedMultiCauseException.json"));

        assertTrue(TestUtils.compareJson(json, expectedJson));
    }

    @Test
    public void serializeExceptionWithException() throws JSONException {
        // GIVEN
        Exception e = generateException();
        Exception e2;
        try {
            throw e;
        } catch (Exception ex) {
            e2 = new JSONException(ex);
            e2.setStackTrace(generateStackTraceElements());
        }

        // WHEN
        String json = BacktraceSerializeHelper.toJson(e2);

        // THEN
        String expectedJson =
                TestUtils.minifyJsonString(TestUtils.readFileAsString(this, "serializedExceptionWithCause.json"));

        assertTrue(TestUtils.compareJson(json, expectedJson));
    }

    @NonNull
    private static Exception generateException() {
        Exception exception = new Exception("test-msg");

        exception.setStackTrace(generateStackTraceElements());
        return exception;
    }

    @NonNull
    private static StackTraceElement[] generateStackTraceElements() {
        List<StackTraceElement> elements = new ArrayList<StackTraceElement>() {
            {
                add(new StackTraceElement("sample-class-1", "method-1", "file-name1", 100));
                add(new StackTraceElement("sample-class-2", "method-2", "file-name2", 200));
            }
        };
        return elements.toArray(new StackTraceElement[0]);
    }
}
