package backtraceio.library.common;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import backtraceio.library.models.BacktraceResult;

public class BacktraceSerializeHelper{

    public static BacktraceResult backtraceResultFromJson(String json)
    {
        return new Gson().fromJson(json, BacktraceResult.class);
    }

    public static String toJson(Object object)
    {
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
        return gson.toJson(object);
    }
}
