package backtraceio.library.common;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import backtraceio.library.models.BacktraceResult;

/**
 * Helper class for serialize and deserialize objects
 */
public class BacktraceSerializeHelper{

    /**
     * Deserialize JSON into BacktraceResult object
     * @param json JSON string which will be deserialized
     * @return object created during deserialization of given json string
     */
    public static BacktraceResult backtraceResultFromJson(String json)
    {
        return new Gson().fromJson(json, BacktraceResult.class);
    }

    /**
     * Serialize given object to JSON string
     * @param object object which will be serialized
     * @return serialized object in JSON string format
     */
    public static String toJson(Object object)
    {
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
        return gson.toJson(object);
    }
}
