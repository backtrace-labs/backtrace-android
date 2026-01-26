package backtraceio.library.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.BacktraceMockLogger;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

public class BacktraceStackFrameTest {
    @Before
    public void setUp() {
        BacktraceLogger.setLogger(new BacktraceMockLogger());
    }

    private final String JSON_FILE = "backtraceStackFrame.json";

    @Test
    public void serialize() throws JSONException {
        // GIVEN
        BacktraceStackFrame obj = new BacktraceStackFrame(
                "java.util.TimerThread.run", "TimerThread", 512, "85c0915f-3b99-4942-91c8-221e23846ded");
        // WHEN
        String json = TestUtils.minifyJsonString(BacktraceSerializeHelper.toJson(obj));
        // THEN
        String expectedJson = TestUtils.minifyJsonString(TestUtils.readFileAsString(this, JSON_FILE));

        assertEquals(expectedJson, json);
    }

    @Test
    public void serializeFromStackTraceElement() throws JSONException {
        // GIVEN
        String sourceCodeUuid = "85c0915f-3b99-4942-91c8-221e23846ded";
        BacktraceStackFrame obj = BacktraceStackFrame.fromStackTraceElement(
                new StackTraceElement("java.util.TimerThread", "run", "", 512));
        // WHEN
        String json = TestUtils.minifyJsonString(BacktraceSerializeHelper.toJson(obj));
        // THEN
        String expectedJson = TestUtils.minifyJsonString(TestUtils.readFileAsString(this, JSON_FILE))
                .replace(sourceCodeUuid, obj.sourceCode);

        assertEquals(expectedJson, json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFromNullStackTraceElement() {
        // GIVEN
        BacktraceStackFrame.fromStackTraceElement(null);
    }

    @Test
    public void deserialize() throws JSONException {
        // GIVEN
        String json = TestUtils.minifyJsonString(TestUtils.readFileAsString(this, JSON_FILE));
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
        BacktraceStackFrame obj = new BacktraceStackFrame(
                "java.util.TimerThread.run", "TimerThread", 512, "85c0915f-3b99-4942-91c8-221e23846ded");
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
