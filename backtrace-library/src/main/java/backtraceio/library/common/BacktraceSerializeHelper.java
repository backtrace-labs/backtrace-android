package backtraceio.library.common;

import backtraceio.library.common.serializers.BacktraceOrgJsonDeserializer;
import backtraceio.library.common.serializers.BacktraceOrgJsonSerializer;

/**
 * Helper class for serialize and deserialize objects
 */
public class BacktraceSerializeHelper {

    /**
     * Converts an object to its JSON string representation.
     *
     * @param object  The object to serialize to JSON.
     * @return        A JSON string representation of the provided object.
     */
    public static String toJson(Object object) {
        return BacktraceOrgJsonSerializer.toJson(object);
    }

    /**
     * Converts a JSON string to an object of the specified type.
     *
     * @param <T>   The type of the object to return.
     * @param json  The JSON string to deserialize.
     * @param type  The class of the type to which the JSON string should be converted.
     * @return      An object of type {@code T} deserialized from the provided JSON string.
     */
    public static <T> T fromJson(String json, Class<T> type) {
        return BacktraceOrgJsonDeserializer.deserialize(json, type);
    }
}
