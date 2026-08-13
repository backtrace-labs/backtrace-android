package backtraceio.coroner.serialization;

import static org.junit.Assert.assertThrows;

import backtraceio.coroner.response.CoronerResponseGroup;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.junit.Test;

public class CoronerResponseGroupDeserializerTest {

    @Test
    public void invalidJsonDeserialization() {
        // GIVEN
        final CoronerResponseGroupDeserializer deserializer = new CoronerResponseGroupDeserializer();
        final JsonElement jsonElement = JsonParser.parseString("[]").getAsJsonArray();

        // WHEN / THEN
        assertThrows(
                JsonParseException.class,
                () -> deserializer.deserialize(jsonElement, CoronerResponseGroup.class, null));
    }
}
