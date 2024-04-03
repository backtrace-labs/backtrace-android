package backtraceio.library.common.deserializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import backtraceio.library.common.serializers.BacktraceOrgJsonDeserializer;
import backtraceio.library.common.serializers.deserializers.BacktraceApiResultDeserializer;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceResultDeserializerTest {

    private final String JSON_1 ="{\"response\":\"ok\",\"_rxid\":\"01000000-5360-240b-0000-000000000000\"}";

    @Test
    public void deserializeCoronerJsonResponse() throws JSONException {
        // GIVEN
        String json = JSON_1;

        // WHEN
        BacktraceResult result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceResult.class);

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
        String json = JSON_1;

        // WHEN
        BacktraceResult result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceResult.class);

        // THEN
        assertNotNull(result);
        assertNull(result.getBacktraceReport());
        assertNull(result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertEquals("01000000-5360-240b-0000-000000000000", result.rxId);
    }

}
