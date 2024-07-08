package backtraceio.library.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;

public class BacktraceStackFrameTest {
    private final String JSON_FILE = "backtraceStackFrame.json";
    @Test
    public void serialize() {
        // GIVEN
        BacktraceStackFrame obj = new BacktraceStackFrame("java.util.TimerThread.run", "TimerThread", 512, "85c0915f-3b99-4942-91c8-221e23846ded");
        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);
        // THEN
        String expectedJson = TestUtils.unifyJsonString(TestUtils.readFileAsString(this, JSON_FILE));

        assertEquals(expectedJson, json);
    }

    @Test
    public void serializeFromStackTraceElement() {
        // GIVEN
        String sourceCodeUuid = "85c0915f-3b99-4942-91c8-221e23846ded";
        BacktraceStackFrame obj = BacktraceStackFrame.fromStackTraceElement(new StackTraceElement("java.util.TimerThread", "run", "", 512));
        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);
        // THEN
        String expectedJson = TestUtils.unifyJsonString(TestUtils.readFileAsString(this, JSON_FILE)).
                replace(sourceCodeUuid, obj.sourceCode);

        assertEquals(expectedJson, json);
    }

    @Test
    public void createFromNullStackTraceElement() {
        // GIVEN
        BacktraceStackFrame obj = BacktraceStackFrame.fromStackTraceElement(null);
        // THEN
        assertNull(obj);
    }

    @Test
    public void deserialize() {
        // GIVEN
        String json = TestUtils.unifyJsonString(TestUtils.readFileAsString(this, JSON_FILE));
        // WHEN
        BacktraceStackFrame obj = BacktraceSerializeHelper.fromJson(json, BacktraceStackFrame.class);
        // THEN
        assertEquals(512, obj.line.intValue());
        assertEquals("85c0915f-3b99-4942-91c8-221e23846ded", obj.sourceCode);
        assertEquals("java.util.TimerThread.run", obj.functionName);
        assertNull(obj.sourceCodeFileName);
    }

    @Test
    public void serializeAndDeserialize() {
        // GIVEN
        BacktraceStackFrame obj = new BacktraceStackFrame("java.util.TimerThread.run", "TimerThread", 512, "85c0915f-3b99-4942-91c8-221e23846ded");
        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);
        BacktraceStackFrame result = BacktraceSerializeHelper.fromJson(json, BacktraceStackFrame.class);

        // THEN
        assertEquals(512, result.line.intValue());
        assertEquals("85c0915f-3b99-4942-91c8-221e23846ded", result.sourceCode);
        assertEquals("java.util.TimerThread.run", result.functionName);
        assertNull(result.sourceCodeFileName);
    }
}
