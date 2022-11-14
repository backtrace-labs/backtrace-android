package backtraceio.coroner.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import backtraceio.coroner.response.CoronerResponseGroup;


public class CoronerResponseGroupDeserializer implements JsonDeserializer<CoronerResponseGroup> {
    private static final Logger LOGGER = Logger.getLogger( CoronerResponseGroupDeserializer.class.getName() );

    @Override
    public CoronerResponseGroup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray jsonArray = json.getAsJsonArray();
        List<Object> obj = new Gson().fromJson(jsonArray, (Type) Object.class);
        try {
            return new CoronerResponseGroup(obj);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Coroner response deserialization exception ", e);
            e.printStackTrace();
            return null;
        }
    }
}
