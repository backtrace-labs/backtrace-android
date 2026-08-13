package backtraceio.coroner.serialization;

import backtraceio.coroner.response.CoronerResponseGroup;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;

public class CoronerResponseGroupDeserializer implements JsonDeserializer<CoronerResponseGroup> {
    @Override
    public CoronerResponseGroup deserialize(
            final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException {
        try {
            final JsonArray jsonArray = json.getAsJsonArray();
            final List<Object> obj = new Gson().fromJson(jsonArray, (Type) Object.class);
            return new CoronerResponseGroup(obj);
        } catch (RuntimeException failure) {
            throw new JsonParseException("Unable to deserialize Coroner response group", failure);
        }
    }
}
