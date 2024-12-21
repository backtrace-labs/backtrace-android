package backtraceio.library.common.json.deserialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import backtraceio.library.TestUtils;
import backtraceio.library.common.json.serialization.BacktraceOrgJsonDeserializer;
import backtraceio.library.logger.BacktraceInternalLogger;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceApiResultDeserializerTest {
    @Test
    public void deserializeCoronerApiJsonResponse() throws JSONException {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceApiResult.json");

        // WHEN
        BacktraceApiResultDeserializer deserializer = new BacktraceApiResultDeserializer();
        BacktraceApiResult result = deserializer.deserialize(new JSONObject(json));

        // THEN
        assertNotNull(result);
        assertEquals("ok", result.getResponse());
        assertEquals("95000000-eb43-390b-0000-000000000000", result.getRxId());
    }

    @Test
    public void deserializeCoronerApiJsonResponseUsingOrgDeserializer() throws JSONException {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceApiResult.json");;

        // WHEN
        BacktraceApiResult result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceApiResult.class);

        // THEN
        assertNotNull(result);
        assertEquals("ok", result.getResponse());
        assertEquals("95000000-eb43-390b-0000-000000000000", result.getRxId());
    }

    @Test
    public void deserializeCoronerApiErrorResponseInvalidToken() throws JSONException {
        String json = TestUtils.readFileAsString(this, "backtraceApiResultInvalidToken.error.json");
        BacktraceInternalLogger x = new BacktraceInternalLogger();
        x.setLevel(LogLevel.DEBUG);
        BacktraceLogger.setLogger(x);
        // WHEN
        BacktraceApiResult result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceApiResult.class);

        // THEN
        assertNotNull(result);
        assertEquals(BacktraceResultStatus.ServerError.toString(), result.getResponse());
        assertNull(result.getRxId());
    }

    @Test
    public void deserializeCoronerApiErrorResponseUnauthorized() throws JSONException {
        String json = TestUtils.readFileAsString(this, "backtraceApiResultUnauthorized.error.json");

        // WHEN
        BacktraceApiResult result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceApiResult.class);

        // THEN
        assertNotNull(result);
        assertEquals(BacktraceResultStatus.ServerError.toString(), result.getResponse());
        assertNull(result.getRxId());
    }
}
