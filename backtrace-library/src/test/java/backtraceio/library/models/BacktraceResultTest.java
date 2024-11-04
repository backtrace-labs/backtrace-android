package backtraceio.library.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.junit.Test;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceResultTest {

    @Test
    public void createFromBacktraceApiResult() {
        // GIVEN
        BacktraceApiResult example = new BacktraceApiResult("95000000-eb43-390b-0000-000000000000", "Ok");
        // WHEN
        BacktraceResult result = new BacktraceResult(example);
        // THEN
        assertEquals(example.getRxId(), result.getRxId());
        assertEquals(BacktraceResultStatus.Ok, result.getStatus());
    }

    @Test
    public void createFromBacktraceApiResultStatusLowercase() {
        // GIVEN
        BacktraceApiResult example = new BacktraceApiResult("95000000-eb43-390b-0000-000000000000", "ok");
        // WHEN
        BacktraceResult result = new BacktraceResult(example);
        // THEN
        assertEquals(example.getRxId(), result.getRxId());
        assertEquals(BacktraceResultStatus.Ok, result.getStatus());
    }

    @Test
    public void serialize() throws JSONException {
        // GIVEN
        BacktraceResult example = new BacktraceResult("95000000-eb43-390b-0000-000000000000", "Ok");
        // WHEN
        String result = TestUtils.minifyJsonString(BacktraceSerializeHelper.toJson(example));
        // THEN

        String expectedJson = TestUtils.minifyJsonString(TestUtils.readFileAsString(this, "backtraceResult.json"));
        assertTrue(TestUtils.compareJson(expectedJson, result));
    }

    @Test
    public void deserialize() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceResult.json");
        // WHEN
        BacktraceResult result = BacktraceSerializeHelper.fromJson(json, BacktraceResult.class);
        // THEN
        assertNotNull(result);
        assertNull(result.getBacktraceReport());
        assertEquals("95000000-eb43-390b-0000-000000000000", result.getRxId());
        assertEquals(BacktraceResultStatus.Ok, result.getStatus());
        assertNull(result.getMessage());
    }

    @Test
    public void serializeAndDeserialize() {
        // GIVEN
        BacktraceResult example = new BacktraceResult("95000000-eb43-390b-0000-000000000000", "Ok");
        // WHEN
        String json = BacktraceSerializeHelper.toJson(example);
        BacktraceResult result = BacktraceSerializeHelper.fromJson(json, BacktraceResult.class);
        // THEN
        assertNotNull(result);
        assertEquals(example.getRxId(), result.getRxId());
        assertEquals(example.getMessage(), result.getMessage());
        assertEquals(example.getStatus().ordinal(), result.getStatus().ordinal());
    }

    @Test
    public void serializeAndDeserializeServerError() {
        // GIVEN
        BacktraceResult example = new BacktraceResult("95000000-eb43-390b-0000-000000000000", "ServerError");
        // WHEN
        String json = BacktraceSerializeHelper.toJson(example);
        BacktraceResult result = BacktraceSerializeHelper.fromJson(json, BacktraceResult.class);
        // THEN
        assertNotNull(result);
        assertEquals(example.getRxId(), result.getRxId());
        assertEquals(example.getMessage(), result.getMessage());
        assertEquals(example.getStatus().ordinal(), result.getStatus().ordinal());
    }

    @Test
    public void testOnErrorCreation() {
        // GIVEN
        Exception exception = new Exception("test-1");
        BacktraceReport report = new BacktraceReport("test-report");

        // WHEN
        BacktraceResult result = BacktraceResult.OnError(new BacktraceReport("test-report"), new Exception("test-1"));

        // THEN
        assertEquals(null, result.getRxId());
        assertEquals(BacktraceResultStatus.ServerError, result.getStatus());
        assertNotNull(result.getBacktraceReport());
        assertEquals(exception.getMessage(), result.getMessage());
        assertEquals(report.message, result.getBacktraceReport().message);
    }
}
