package backtraceio.library.models.json;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;

public class SourceCodeTest {

    private final String JSON_FILE = "sourceCode.json";
    @Test
    public void serialize() {
        // GIVEN
        SourceCode obj = new SourceCode(17, "InvokeMethod.java");

        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);

        // THEN
        String expectedJson = TestUtils.unifyJsonString(
                TestUtils.readFileAsString(this, JSON_FILE)
        );

        assertEquals(expectedJson, json);
    }

    @Test
    public void deserialize() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, JSON_FILE);
        // WHEN
        SourceCode result = BacktraceSerializeHelper.fromJson(json, SourceCode.class);
        // THEN
        assertEquals("InvokeMethod.java", result.getSourceCodeFileName());
        assertEquals(17, result.getStartLine().intValue());
    }

    @Test
    public void serializeAndDeserialize() {
        // GIVEN
        SourceCode obj = new SourceCode(17, "InvokeMethod.java");

        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);

        // WHEN
        SourceCode result = BacktraceSerializeHelper.fromJson(json, SourceCode.class);
        assertEquals(obj.getSourceCodeFileName(), result.getSourceCodeFileName());
        assertEquals(obj.getStartLine(), result.getStartLine());
    }
}
