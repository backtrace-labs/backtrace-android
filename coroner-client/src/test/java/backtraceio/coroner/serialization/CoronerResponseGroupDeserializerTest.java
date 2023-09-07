package backtraceio.coroner.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.junit.Test;


import backtraceio.coroner.response.CoronerResponseGroup;

public class CoronerResponseGroupDeserializerTest {

    @Test
    public void invalidJsonDeserialization() {
        // GIVEN
        final CoronerResponseGroupDeserializer deserializer = new CoronerResponseGroupDeserializer();
        final JsonElement jsonElement = JsonParser.parseString("[]").getAsJsonArray();

        // WHEN
        final CoronerResponseGroup result = deserializer.deserialize(jsonElement, CoronerResponseGroup.class, null);

        // THEN
        assertNull(result);
    }
}
