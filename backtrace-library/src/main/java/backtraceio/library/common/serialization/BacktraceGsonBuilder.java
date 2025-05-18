package backtraceio.library.common.serialization;

import backtraceio.gson.FieldNamingPolicy;
import backtraceio.gson.Gson;
import backtraceio.gson.GsonBuilder;

public class BacktraceGsonBuilder implements CustomGsonBuilder {

    @Override
    public Gson buildGson() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
    }
}
