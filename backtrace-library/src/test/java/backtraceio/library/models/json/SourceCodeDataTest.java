package backtraceio.library.models.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.BacktraceStackFrame;

public class SourceCodeDataTest {
    private final String JSON_FILE = "sourceCodeData.json";
    @Test
    public void serialize() throws JSONException {
        // GIVEN
        List<BacktraceStackFrame> frames = new ArrayList<BacktraceStackFrame>() {{
            add(new BacktraceStackFrame(null, "VMStack.java", null, "8751bea6-d6f6-48f4-9f96-1355c3408a9a"));
            add(new BacktraceStackFrame(null, "InvokeMethod.java", 17, "27948842-7c2b-4898-a74a-ba3ca4afe814"));
        }};

        SourceCodeData obj = new SourceCodeData(frames);
        // WHEN
        String json = TestUtils.minifyJsonString(BacktraceSerializeHelper.toJson(obj));

        // THEN
        String expectedJson = TestUtils.minifyJsonString(
                TestUtils.readFileAsString(this, JSON_FILE)
        );

        assertEquals(expectedJson, json);
    }

    @Test
    public void deserialize() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, JSON_FILE);
        // WHEN
        final SourceCodeData obj = BacktraceSerializeHelper.fromJson(json, SourceCodeData.class);
        // THEN
        assertEquals(2, obj.data.size());
        assertEquals("VMStack.java", obj.data.get("8751bea6-d6f6-48f4-9f96-1355c3408a9a").getSourceCodeFileName());
        assertNull(obj.data.get("8751bea6-d6f6-48f4-9f96-1355c3408a9a").getStartLine());
        assertEquals("InvokeMethod.java", obj.data.get("27948842-7c2b-4898-a74a-ba3ca4afe814").getSourceCodeFileName());
        assertEquals(17, obj.data.get("27948842-7c2b-4898-a74a-ba3ca4afe814").getStartLine().intValue());
    }

    @Test
    public void serializeAndDeserialize() {
        // GIVEN
        List<BacktraceStackFrame> frames = new ArrayList<BacktraceStackFrame>() {{
            add(new BacktraceStackFrame(null, "VMStack.java", null, "8751bea6-d6f6-48f4-9f96-1355c3408a9a"));
            add(new BacktraceStackFrame(null, "InvokeMethod.java", 17, "27948842-7c2b-4898-a74a-ba3ca4afe814"));
        }};

        SourceCodeData obj = new SourceCodeData(frames);
        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);

        SourceCodeData result = BacktraceSerializeHelper.fromJson(json, SourceCodeData.class);

        // THEN
        assertNotNull(result);
        assertEquals(2, result.data.size());

        assertEquals("VMStack.java", result.data.get("8751bea6-d6f6-48f4-9f96-1355c3408a9a").getSourceCodeFileName());
        assertNull(result.data.get("8751bea6-d6f6-48f4-9f96-1355c3408a9a").getStartLine());
        assertEquals("InvokeMethod.java", result.data.get("27948842-7c2b-4898-a74a-ba3ca4afe814").getSourceCodeFileName());
        assertEquals(17, result.data.get("27948842-7c2b-4898-a74a-ba3ca4afe814").getStartLine().intValue());
    }
}
