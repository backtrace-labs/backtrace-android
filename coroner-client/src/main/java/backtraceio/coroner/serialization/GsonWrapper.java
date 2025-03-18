package backtraceio.coroner.serialization;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import backtraceio.coroner.response.CoronerResponseGroup;

public class GsonWrapper {

    public static <T> T fromJson(final String json, final Class<T> type) {
        backtraceio.coroner.common.Logger.d("CoronerHttpClient GsonWrapper fromJson", "started");


        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .registerTypeAdapter(CoronerResponseGroup.class, new CoronerResponseGroupDeserializer())
                .create();
        backtraceio.coroner.common.Logger.d("CoronerHttpClient GsonWrapper", gson.toString());


        backtraceio.coroner.common.Logger.d("CoronerHttpClient GsonWrapper fromJson", "json");
        backtraceio.coroner.common.Logger.d("CoronerHttpClient GsonWrapper fromJson", json);

        backtraceio.coroner.common.Logger.d("CoronerHttpClient GsonWrapper fromJson", "started");
        T result = gson.fromJson(json, type);
        backtraceio.coroner.common.Logger.d("CoronerHttpClient GsonWrapper fromJson", "result");
        backtraceio.coroner.common.Logger.d("CoronerHttpClient GsonWrapper fromJson", result.toString());

        return result;
    }

}
