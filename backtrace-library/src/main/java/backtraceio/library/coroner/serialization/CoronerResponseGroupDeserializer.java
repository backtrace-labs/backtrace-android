package backtraceio.library.coroner.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.List;

import backtraceio.library.coroner.response.CoronerResponseGroup;
import backtraceio.library.logger.BacktraceLogger;

public class CoronerResponseGroupDeserializer implements JsonDeserializer<CoronerResponseGroup> {
    private static final transient String LOG_TAG = CoronerResponseGroupDeserializer.class.getSimpleName();

    @Override
    public CoronerResponseGroup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray jsonArray = json.getAsJsonArray();
        List<Object> obj = new Gson().fromJson(jsonArray, (Type) Object.class);
        try {
            return new CoronerResponseGroup(obj);
        } catch (Exception e) {
            BacktraceLogger.e(LOG_TAG, "" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
