package backtraceio.library.common.serializers;

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
        return WRAPPER_TYPE_MAP.containsKey(source.getClass());
    }

    public static String decapitalizeString(String string) {
        return string == null || string.isEmpty() ? "" : Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    private static JSONArray serializeCollection(Collection<?> collection) throws IllegalAccessException, JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Object item : collection) {
            jsonArray.put(serialize(item));
        }
        return jsonArray;
    }

    private static List<Field> getAllFields(Class<?> klass, Object obj) throws IllegalAccessException {
        // TODO: improve naming

        List<Field> fields = new ArrayList<>();
        for (Class<?> c = klass; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }

        for (Field f: fields) {
            f.setAccessible(true);
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                System.out.println(f.getName() + " " + f.get(obj));
            }

        }

        return fields;
    }

    private static JSONObject serializeMap(Map<?, ?> map) throws IllegalAccessException, JSONException {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            jsonObject.put(key, serialize(value));
        }
        return jsonObject;
    }

    public static Object serialize(Object obj, int serializationDepth) throws IllegalAccessException, JSONException {

    }

    public static Object serialize(Object obj) throws IllegalAccessException, JSONException {
        if (obj == null) {
            return new JSONObject();
        }

        // TODO: check if all of the types
        if (SerializerHelper.isPrimitiveType(obj)) {
            return obj;
        }

        if (obj instanceof Collection<?>) {
            return serializeCollection((Collection<?>) obj);
        }

        if (obj instanceof Map<?, ?>) {
            return serializeMap((Map<?,?>) obj);
        }

        JSONObject jsonObject = new JSONObject();
        Class<?> clazz = obj.getClass();
        List<Field> fields = getAllFields(clazz, obj);
//        Map<String, Object> getters = executeAndGetMethods(obj);
//        Map<String, Object> fields = getAllFields(obj.getClass(), obj);
//        for (Field field : fields) {
//            String fieldName = field.getName();
//            Object fieldValue = field.get(obj);
//            jsonObject.put(fieldName, fieldValue);
//        }

        return jsonObject;
    }
}
