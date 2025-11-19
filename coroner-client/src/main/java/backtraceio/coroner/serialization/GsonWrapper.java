package backtraceio.coroner.serialization;

import backtraceio.coroner.response.CoronerResponseGroup;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonWrapper {

    public static <T> T fromJson(final String json, final Class<T> type) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .registerTypeAdapter(CoronerResponseGroup.class, new CoronerResponseGroupDeserializer())
                .create();

        return gson.fromJson(json, type);
    }
}
