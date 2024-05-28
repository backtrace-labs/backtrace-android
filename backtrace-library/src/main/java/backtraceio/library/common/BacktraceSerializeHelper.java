package backtraceio.library.common;

import com.google.gson.Gson;

import backtraceio.library.common.serialization.BacktraceGsonBuilder;

/**
 * Helper class for serialize and deserialize objects
 */
public class BacktraceSerializeHelper {

    /**
     * Serialize given object to JSON string
     *
     * @param object object which will be serialized
     * @return serialized object in JSON string format
     */
    public static String toJson(Object object) {
        return BacktraceSerializeHelper.toJson(new BacktraceGsonBuilder().buildGson(), object);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return BacktraceSerializeHelper.fromJson(new BacktraceGsonBuilder().buildGson(), json, type);
    }

    public static String toJson(Gson gson, Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromJson(Gson gson, String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

}
