package backtraceio.library.models.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.lang.StackTraceElement;
import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.BacktraceStackFrame;

public class BacktraceReportTest {
    private final String JSON_FILE = "backtraceReport.json";
    @Test
    public void serialize() throws JSONException {
        // GIVEN
        final List<BacktraceStackFrame> diagnosticStack = new ArrayList<>();

        diagnosticStack.add(new BacktraceStackFrame("backtraceio.library.SettingAttributesTest.tmpGsonTest", null, 75, "c37b9ae3-eab1-4928-9533-f1c14b6149f5"));
        diagnosticStack.add(new BacktraceStackFrame("java.lang.reflect.Method.invoke", null, null, "6f280747-feee-4f4b-9eff-dda0d8eaa535"));
        final Exception exception = new IllegalAccessException();
        final StackTraceElement[] stackTraceElements = new StackTraceElement[1];
        stackTraceElements[0] = new StackTraceElement("backtraceio.library.SettingAttributesTest", "tmpGsonTest", "SettingAttributesTest.java", 75);

        exception.setStackTrace(stackTraceElements);

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("error.type", "Exception");
        final BacktraceReport report = new BacktraceReport(UUID.fromString("a62a533a-a7b8-415c-9a99-253c51f00827"), 1709680251, true, "java.lang.IllegalAccessException", attributes, null, exception, new ArrayList<String>() {{ add("abc.txt"); }}, diagnosticStack);

        // WHEN
        String json = BacktraceSerializeHelper.toJson(report);

        // THEN
        String expectedJson = TestUtils.minifyJsonString(
                TestUtils.readFileAsString(this, JSON_FILE)
        );

        assertTrue(TestUtils.compareJson(json, expectedJson));
    }

    @Test
    public void deserialize() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, JSON_FILE);

        // WHEN
        final BacktraceReport obj = BacktraceSerializeHelper.fromJson(json, BacktraceReport.class);

        // THEN
        assertNotNull(obj);
        assertEquals("a62a533a-a7b8-415c-9a99-253c51f00827", obj.uuid.toString());
        assertEquals(1709680251, obj.timestamp);
        assertEquals(true, obj.exceptionTypeReport);

        assertEquals("java.lang.IllegalAccessException", obj.classifier);
        assertEquals(1, obj.attributes.size());
        assertEquals("Exception", obj.attributes.get("error.type"));
        assertNull(obj.message);
        assertNotNull(obj.exception);
        assertEquals(1, obj.exception.getStackTrace().length);
        assertEquals(1, obj.attachmentPaths.size());
        assertEquals("abc.txt", obj.attachmentPaths.get(0));


        // THEN diagnostic stack
        assertNotNull(obj.diagnosticStack);
        assertEquals(2, obj.diagnosticStack.size());

        assertEquals("backtraceio.library.SettingAttributesTest.tmpGsonTest", obj.diagnosticStack.get(0).functionName);
        assertEquals(75, obj.diagnosticStack.get(0).line.intValue());
        assertEquals("c37b9ae3-eab1-4928-9533-f1c14b6149f5", obj.diagnosticStack.get(0).sourceCode);
        assertNull(obj.diagnosticStack.get(0).sourceCodeFileName);

        assertEquals("java.lang.reflect.Method.invoke", obj.diagnosticStack.get(1).functionName);
        assertNull(obj.diagnosticStack.get(1).line);
        assertEquals("6f280747-feee-4f4b-9eff-dda0d8eaa535", obj.diagnosticStack.get(1).sourceCode);
        assertNull(obj.diagnosticStack.get(1).sourceCodeFileName);
    }

    @Test
    public void serializeAndDeserialize() {
        // GIVEN
        final List<BacktraceStackFrame> diagnosticStack = new ArrayList<>();

        diagnosticStack.add(new BacktraceStackFrame("backtraceio.library.SettingAttributesTest.tmpGsonTest", null, 75, "c37b9ae3-eab1-4928-9533-f1c14b6149f5"));
        diagnosticStack.add(new BacktraceStackFrame("java.lang.reflect.Method.invoke", null, null, "6f280747-feee-4f4b-9eff-dda0d8eaa535"));
        final Exception exception = new IllegalAccessException();
        final StackTraceElement[] stackTraceElements = new StackTraceElement[1];
        stackTraceElements[0] = new StackTraceElement("backtraceio.library.SettingAttributesTest", "tmpGsonTest", "SettingAttributesTest.java", 75);

        exception.setStackTrace(stackTraceElements);

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("error.type", "Exception");
        final BacktraceReport report = new BacktraceReport(UUID.fromString("a62a533a-a7b8-415c-9a99-253c51f00827"), 1709680251, true, "java.lang.IllegalAccessException", attributes, null, exception, new ArrayList<String>() {{ add("abc.txt"); }}, diagnosticStack);

        // WHEN
        String json = BacktraceSerializeHelper.toJson(report);
        final BacktraceReport obj = BacktraceSerializeHelper.fromJson(json, BacktraceReport.class);

        // THEN
        assertNotNull(obj);
        assertEquals("a62a533a-a7b8-415c-9a99-253c51f00827", obj.uuid.toString());
        assertEquals(1709680251, obj.timestamp);
        assertEquals(true, obj.exceptionTypeReport);

        assertEquals("java.lang.IllegalAccessException", obj.classifier);
        assertEquals(1, obj.attributes.size());
        assertEquals("Exception", obj.attributes.get("error.type"));
        assertNull(obj.message);
        assertNotNull(obj.exception);
        assertEquals(1, obj.exception.getStackTrace().length);
        assertEquals(1, obj.attachmentPaths.size());
        assertEquals("abc.txt", obj.attachmentPaths.get(0));


        // THEN diagnostic stack
        assertNotNull(obj.diagnosticStack);
        assertEquals(2, obj.diagnosticStack.size());

        assertEquals("backtraceio.library.SettingAttributesTest.tmpGsonTest", obj.diagnosticStack.get(0).functionName);
        assertEquals(75, obj.diagnosticStack.get(0).line.intValue());
        assertEquals("c37b9ae3-eab1-4928-9533-f1c14b6149f5", obj.diagnosticStack.get(0).sourceCode);
        assertNull(obj.diagnosticStack.get(0).sourceCodeFileName);

        assertEquals("java.lang.reflect.Method.invoke", obj.diagnosticStack.get(1).functionName);
        assertNull(obj.diagnosticStack.get(1).line);
        assertEquals("6f280747-feee-4f4b-9eff-dda0d8eaa535", obj.diagnosticStack.get(1).sourceCode);
        assertNull(obj.diagnosticStack.get(1).sourceCodeFileName);
    }
}
