package backtraceio.library.deserializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import backtraceio.library.common.serializers.BacktraceOrgJsonDeserializer;
import backtraceio.library.common.serializers.deserializers.BacktraceApiResultDeserializer;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.types.BacktraceResultStatus;

// TODO: move to standard unit tests not instrumented
// TODO: use from resources
public class BacktraceApiResultDeserializerTest {

    private final String JSON_1 = "{\"response\":\"Ok\",\"_rxid\":\"01000000-5360-240b-0000-000000000000\"}";
    @Test
    public void deserializeCoronerApiJsonResponse() throws JSONException {
        // GIVEN
        String json = JSON_1;

        // WHEN
        BacktraceApiResultDeserializer deserializer = new BacktraceApiResultDeserializer();
        BacktraceApiResult result = deserializer.deserialize(new JSONObject(json));

        // THEN
        assertNotNull(result);
        assertEquals(BacktraceResultStatus.Ok.toString(), result.getResponse());
        assertEquals("01000000-5360-240b-0000-000000000000", result.getRxId());
    }

    @Test
    public void deserializeCoronerApiJsonResponseUsingOrgDeserializer() throws JSONException {
        // GIVEN
        String json = JSON_1;

        // WHEN
        BacktraceApiResult result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceApiResult.class);

        // THEN
        assertNotNull(result);
        assertEquals(BacktraceResultStatus.Ok.toString(), result.getResponse());
        assertEquals("01000000-5360-240b-0000-000000000000", result.getRxId());
    }

    @Test
    public void deserializeCoronerApiErrorResponse() throws JSONException {
        String json = "{ \"error\": { \"code\": 6, \"message\": \"invalid token\" } }"; // TODO: add proper error json

        // WHEN
        BacktraceApiResult result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceApiResult.class);

        // THEN
        assertNotNull(result);
        assertEquals(BacktraceResultStatus.ServerError.toString(), result.getResponse());
        assertNull(result.getRxId());
    }
}
