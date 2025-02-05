package backtraceio.library.common.json.deserialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import backtraceio.library.TestUtils;
import backtraceio.library.common.json.serialization.BacktraceOrgJsonDeserializer;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;


public class BacktraceResultDeserializerTest {

    @Test
    public void deserializeCoronerJsonResponse() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceResult.json");

        // WHEN
        BacktraceResult result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceResult.class);

        // THEN
        assertNotNull(result);
        assertNull(result.getBacktraceReport());
        assertNull(result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertEquals("95000000-eb43-390b-0000-000000000000", result.rxId);
    }

    @Test
    public void deserializeCoronerJsonErrorResponse() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceApiResult.error.json");

        // WHEN
        BacktraceApiResult apiResult = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceApiResult.class);
        BacktraceResult result = new BacktraceResult(apiResult);
        // THEN
        assertNotNull(result);
        assertNull(result.getBacktraceReport());
        assertNull(result.message);
        assertEquals(BacktraceResultStatus.ServerError, result.status);
        assertNull(result.rxId);
    }

    @Test
    public void deserializeCoronerAPIJsonResponse() throws JSONException {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceApiResult.json");

        BacktraceApiResultDeserializer deserializer = new BacktraceApiResultDeserializer();

        // WHEN
        BacktraceApiResult result = deserializer.deserialize(new JSONObject(json));

        // THEN
        assertNotNull(result);
        assertEquals("ok", result.getResponse());
        assertEquals("95000000-eb43-390b-0000-000000000000", result.getRxId());
    }

    @Test
    public void deserializeCoronerAPIErrorJsonResponse() throws JSONException {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceApiResult.error.json");

        BacktraceApiResultDeserializer deserializer = new BacktraceApiResultDeserializer();

        // WHEN
        BacktraceApiResult result = deserializer.deserialize(new JSONObject(json));

        // THEN
        assertNotNull(result);
        assertEquals("ServerError", result.getResponse());
        assertNull(result.getRxId());
    }
}
