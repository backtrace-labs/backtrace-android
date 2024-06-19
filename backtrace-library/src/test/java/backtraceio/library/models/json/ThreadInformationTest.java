package backtraceio.library.models.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.BacktraceStackFrame;

public class ThreadInformationTest {

    @Test
    public void serialize() {
        // GIVEN
        List<BacktraceStackFrame> frames = new ArrayList<>();
        frames.add(new BacktraceStackFrame("backtraceio.backtraceio.MainActivity.handledException", null, 150, "cde23509-3dcc-494d-af1f-4b4e2af4cc5e"));
        frames.add(new BacktraceStackFrame("java.lang.reflect.Method.invoke", null, null, "7fc374ec-e276-46da-8d1a-05b37425927e"));
        ThreadInformation obj = new ThreadInformation("main", true, frames);

        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);

        // THEN
        String expectedJson = TestUtils.readFileAsString(this, "threadInformation.json")
                .replace("\n", "")
                .replace(" ", "")
                .replace("\t", "");

        assertEquals(expectedJson,json);
    }

    @Test
    public void deserialize() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "threadInformation.json");
        // WHEN
        ThreadInformation result = BacktraceSerializeHelper.fromJson(json, ThreadInformation.class);
        // THEN
        assertNotNull(result);
        assertEquals("main", result.getName());
        assertTrue(result.getFault());
        assertNotNull(result.getStack());
        assertEquals(2, result.getStack().size());

        // THEN Frame 1
        assertEquals(Integer.valueOf(150), result.getStack().get(0).line);
        assertEquals("backtraceio.backtraceio.MainActivity.handledException", result.getStack().get(0).functionName);
        assertEquals("cde23509-3dcc-494d-af1f-4b4e2af4cc5e", result.getStack().get(0).sourceCode);
        assertNull(result.getStack().get(0).sourceCodeFileName);

        // THEN Frame 2
        assertNull(result.getStack().get(1).line);
        assertEquals("java.lang.reflect.Method.invoke", result.getStack().get(1).functionName);
        assertEquals("7fc374ec-e276-46da-8d1a-05b37425927e", result.getStack().get(1).sourceCode);
        assertNull(result.getStack().get(1).sourceCodeFileName);
    }

    @Test
    public void serializeAndDeserialize() {
        List<BacktraceStackFrame> frames = new ArrayList<>();
        frames.add(new BacktraceStackFrame("backtraceio.backtraceio.MainActivity.handledException", null, 150, "cde23509-3dcc-494d-af1f-4b4e2af4cc5e"));
        frames.add(new BacktraceStackFrame("java.lang.reflect.Method.invoke", null, null, "7fc374ec-e276-46da-8d1a-05b37425927e"));
        ThreadInformation obj = new ThreadInformation("main", true, frames);

        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);

        // WHEN
        ThreadInformation result = BacktraceSerializeHelper.fromJson(json, ThreadInformation.class);
        // THEN
        assertNotNull(result);
        assertEquals("main", result.getName());
        assertTrue(result.getFault());
        assertNotNull(result.getStack());
        assertEquals(2, result.getStack().size());

        // THEN Frame 1
        assertEquals(Integer.valueOf(150), result.getStack().get(0).line);
        assertEquals("backtraceio.backtraceio.MainActivity.handledException", result.getStack().get(0).functionName);
        assertEquals("cde23509-3dcc-494d-af1f-4b4e2af4cc5e", result.getStack().get(0).sourceCode);
        assertNull(result.getStack().get(0).sourceCodeFileName);

        // THEN Frame 2
        assertNull(result.getStack().get(1).line);
        assertEquals("java.lang.reflect.Method.invoke", result.getStack().get(1).functionName);
        assertEquals("7fc374ec-e276-46da-8d1a-05b37425927e", result.getStack().get(1).sourceCode);
        assertNull(result.getStack().get(1).sourceCodeFileName);
    }
}
