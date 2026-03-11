package backtraceio.library.models;

import static backtraceio.library.TestUtils.compareJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import org.junit.Test;

public class BacktraceApiResultTest {

    @Test
    public void serialize() {
        // GIVEN
        BacktraceApiResult example = new BacktraceApiResult("95000000-eb43-390b-0000-000000000000", "ok");
        // WHEN
        String result = BacktraceSerializeHelper.toJson(example);
        // THEN
        String expectedJson = TestUtils.readFileAsString(this, "backtraceApiResult.json");
        compareJson(expectedJson, result);
    }

    @Test
    public void deserialize() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceApiResult.json");
        // WHEN
        BacktraceApiResult result = BacktraceSerializeHelper.fromJson(json, BacktraceApiResult.class);
        // THEN
        assertNotNull(result);
        assertEquals("95000000-eb43-390b-0000-000000000000", result.getRxId());
        assertEquals("ok", result.getResponse());
    }

    @Test
    public void serializeAndDeserialize() {
        // GIVEN
        BacktraceApiResult example = new BacktraceApiResult("95000000-eb43-390b-0000-000000000000", "ok");
        // WHEN
        String json = BacktraceSerializeHelper.toJson(example);
        BacktraceApiResult result = BacktraceSerializeHelper.fromJson(json, BacktraceApiResult.class);
        // THEN
        assertNotNull(result);
        assertEquals(example.getRxId(), result.getRxId());
        assertEquals(example.getResponse(), result.getResponse());
    }
}
