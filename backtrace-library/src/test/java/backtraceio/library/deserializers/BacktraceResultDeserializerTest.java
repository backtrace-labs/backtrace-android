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
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;


public class BacktraceResultDeserializerTest {

    private final String JSON_1 ="{\"response\":\"ok\",\"_rxid\":\"01000000-5360-240b-0000-000000000000\"}";

    @Test
    public void deserializeCoronerJsonResponse() {

        // WHEN
        BacktraceResult result = BacktraceOrgJsonDeserializer.deserialize(JSON_1, BacktraceResult.class);

        // THEN
        assertNotNull(result);
        assertNull(result.getBacktraceReport());
        assertNull(result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertEquals("01000000-5360-240b-0000-000000000000", result.rxId);
    }

    @Test
    public void deserializeCoronerAPIJsonResponse() throws JSONException {
        // GIVEN
        BacktraceApiResultDeserializer deserializer = new BacktraceApiResultDeserializer();

        // WHEN
        BacktraceApiResult result = deserializer.deserialize(new JSONObject(JSON_1));

        // THEN
        assertNotNull(result);
        assertEquals("ok", result.getResponse());
        assertEquals("01000000-5360-240b-0000-000000000000", result.getRxId());
    }

}
