package backtraceio.library.common.serializers;

import static backtraceio.library.common.serializers.BacktraceDataSerializer.executeAndGetMethods;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SerializerHelper {
    public static int MAX_SERIALIZATION_LEVEL = 5;
    private static final Map<Class<?>, Class<?>> WRAPPER_TYPE_MAP;

    static {
        WRAPPER_TYPE_MAP = new HashMap<>();
        WRAPPER_TYPE_MAP.put(Integer.class, int.class);
        WRAPPER_TYPE_MAP.put(Byte.class, byte.class);
        WRAPPER_TYPE_MAP.put(Character.class, char.class);
        WRAPPER_TYPE_MAP.put(Boolean.class, boolean.class);
        WRAPPER_TYPE_MAP.put(Double.class, double.class);
        WRAPPER_TYPE_MAP.put(Float.class, float.class);
        WRAPPER_TYPE_MAP.put(Long.class, long.class);
        WRAPPER_TYPE_MAP.put(Short.class, short.class);
        WRAPPER_TYPE_MAP.put(Void.class, void.class);
    }

    public static boolean isPrimitiveType(Object source) {
        return WRAPPER_TYPE_MAP.containsKey(source.getClass()) || source instanceof String || source instanceof Number;
    }

    public static String decapitalizeString(String string) {
        return string == null || string.isEmpty() ? "" : Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    private static JSONArray serializeCollection(Collection<?> collection) throws JSONException {
        return serializeCollection(collection, 0);
    }

    private static JSONArray serializeArray(Object[] array, int serializationDepth) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Object item : array) {
            jsonArray.put(serialize(item, serializationDepth));
        }
        return jsonArray;
    }
    private static JSONArray serializeCollection(Collection<?> collection, int serializationDepth) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Object item : collection) {
            jsonArray.put(serialize(item, serializationDepth));
        }
        return jsonArray;
    }

    private static JSONObject getAllFields(Class<?> klass, Object obj, int serializationDepth) {
        // TODO: improve naming

        List<Field> fields = new ArrayList<>();
        for (Class<?> c = klass; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }

        JSONObject result = new JSONObject();
        for (Field f : fields) {
            f.setAccessible(true);
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                try {
                    Object value = f.get(obj);
                    if (value == obj) {
                        continue;
                    }
                    result.put(f.getName(), serialize(value, serializationDepth));
                } catch (Exception ex) {

                }

            }

        }

        return result;
    }

    private static JSONObject serializeMap(Map<?, ?> map) throws JSONException {
        return serializeMap(map, 0);
    }

    private static JSONObject serializeMap(Map<?, ?> map, int serializationDepth) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            jsonObject.put(key, serialize(value, serializationDepth));
        }
        return jsonObject;
    }

    public static Object serialize(Object obj) throws JSONException {
        return serialize(obj, 0);
    }

    public static Object serialize(Object obj, int serializationDepth) throws JSONException {
        if (obj == null) {
            return null;
        }

        if (serializationDepth > MAX_SERIALIZATION_LEVEL) {
            return new JSONObject();
        }

        // TODO: check if all of the types
        if (SerializerHelper.isPrimitiveType(obj)) {
            return obj;
        }

        serializationDepth++;

        if (obj instanceof Map<?, ?>) {
            return serializeMap((Map<?, ?>) obj, serializationDepth);
        }


        if (obj.getClass().isArray()) {
            return serializeArray((Object[]) obj, serializationDepth);
        }

        if (obj instanceof Collection<?>) {
            return serializeCollection((Collection<?>) obj, serializationDepth);
        }

        Class<?> clazz = obj.getClass();
        JSONObject jsonObject = getAllFields(clazz, obj, serializationDepth);
        Map<String, Object> getters = executeAndGetMethods(obj);

        for (Map.Entry<String, Object> entry: getters.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
            jsonObject.put(entry.getKey(), entry.getValue()); // TODO: remove
        }

        return jsonObject;
    }
}
