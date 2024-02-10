package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import backtraceio.library.common.serializers.deserializers.BacktraceApiResultDeserializer;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

// TODO: move to standard unit tests not instrumented
public class BacktraceResultDeserializerTest {
    @Test
    public void deserializeCoronerApiJsonResponse() throws JSONException {
        // GIVEN
        String json = "{\"response\":\"Ok\",\"_rxid\":\"01000000-5360-240b-0000-000000000000\"}";

        // WHEN
        BacktraceApiResultDeserializer deserializer = new BacktraceApiResultDeserializer();
        BacktraceResult result = deserializer.deserialize(new JSONObject(json));

        // THEN
        assertNotNull(result);
        assertNull(result.getBacktraceReport());
        assertNull(result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertEquals("01000000-5360-240b-0000-000000000000", result.rxId);
    }

    // TODO: Add test case with another response e.g. server error
}
