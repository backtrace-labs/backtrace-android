package backtraceio.library.common;

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
        return BacktraceOrgJsonSerializer.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return BacktraceOrgJsonDeserializer.deserialize(json, type);
    }

//    public static <T> T fromJson(Gson gson, String json, Class<T> type) {
//        return BacktraceOrgJsonDeserializer.deserialize(json, type);
//    }
}
