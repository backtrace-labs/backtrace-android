package backtraceio.library.coroner.serialization;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import backtraceio.library.common.serialization.CustomGsonBuilder;
import backtraceio.library.coroner.response.CoronerResponseGroup;

public class CoronerResponseGsonBuilder implements CustomGsonBuilder {

    @Override
    public Gson buildGson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .registerTypeAdapter(CoronerResponseGroup.class, new CoronerResponseGroupDeserializer())
                .create();
    }
}
