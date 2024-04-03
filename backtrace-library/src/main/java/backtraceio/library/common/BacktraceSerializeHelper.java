package backtraceio.library.common;

import com.google.gson.Gson;

import backtraceio.library.common.serialization.BacktraceGsonBuilder;
import backtraceio.library.common.serializers.BacktraceOrgJsonDeserializer;
import backtraceio.library.common.serializers.BacktraceOrgJsonSerializer;

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
//        return new BacktraceGsonBuilder().buildGson().toJson(object);
        return BacktraceOrgJsonSerializer.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
        return BacktraceOrgJsonDeserializer.deserialize(json, type);
    } catch (Exception e) {
        //TODO: remove this try-catch
            return null;
    }
//        return BacktraceSerializeHelper.fromJson(new BacktraceGsonBuilder().buildGson(), json, type);
    }

    public static <T> T fromJson(Gson gson, String json, Class<T> type) {
        try {
            return BacktraceOrgJsonDeserializer.deserialize(json, type);
        } catch (Exception e) {
            //TODO: remove this try-catch
            return null;
        }
//        return gson.fromJson(json, type);
    }

}
