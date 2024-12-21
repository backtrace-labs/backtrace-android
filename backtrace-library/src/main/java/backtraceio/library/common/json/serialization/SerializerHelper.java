package backtraceio.library.common.json.serialization;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.common.json.naming.NamingPolicy;
import backtraceio.library.logger.BacktraceLogger;

public class SerializerHelper {
    private static final String LOG_TAG = SerializerHelper.class.getSimpleName();
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

    private static JSONArray serializeArray(NamingPolicy namingPolicy, Object[] array, int serializationDepth) throws JSONException {
        if (array == null) {
            return null;
        }

        JSONArray jsonArray = new JSONArray();
        for (Object item : array) {
            jsonArray.put(serialize(namingPolicy, item, serializationDepth));
        }
        return jsonArray;
    }
    private static JSONArray serializeCollection(NamingPolicy namingPolicy, Collection<?> collection, int serializationDepth) throws JSONException {
        if (collection == null) {
            return null;
        }

        JSONArray jsonArray = new JSONArray();
        for (Object item : collection) {
            jsonArray.put(serialize(namingPolicy, item, serializationDepth));
        }
        return jsonArray;
    }

    private static Object serializeException(NamingPolicy namingPolicy, Exception exception) {
        try {
            return getAllFields(namingPolicy, exception.getClass(), exception, 2);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static JSONObject getAllFields(NamingPolicy namingPolicy, Class<?> serializedClass, Object obj, int serializationDepth) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> clazz = serializedClass; clazz != null; clazz = clazz.getSuperclass()) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        }

        JSONObject result = new JSONObject();
        for (Field field : fields) {
            field.setAccessible(true);

            if (java.lang.reflect.Modifier.isTransient(field.getModifiers()) ||
                    java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            try {
                Object fieldValue = field.get(obj);
                if (fieldValue == obj) {
                    continue;
                }
                result.put(getFieldName(namingPolicy, field), serialize(namingPolicy, fieldValue, serializationDepth));
            } catch (Exception e) {
                BacktraceLogger.e(LOG_TAG, String.format("Exception on getting object fields, " +
                        "field %s, object %s", field.getName(), obj), e);
            }
        }

        return result;
    }

    private static String getFieldName(NamingPolicy namingPolicy, Field field) {
        if (field.isAnnotationPresent(SerializedName.class)) {
            SerializedName annotation = field.getAnnotation(SerializedName.class);
            if (annotation != null) {
                return annotation.value();
            }
        }
        return namingPolicy.convert(field.getName());
    }

    private static JSONObject serializeMap(NamingPolicy namingPolicy, Map<?, ?> map, int serializationDepth) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            jsonObject.put(key, serialize(namingPolicy, value, serializationDepth));
        }
        return jsonObject;
    }

    public static Object serialize(NamingPolicy namingPolicy, Object obj) throws JSONException {
        return serialize(namingPolicy, obj, 0);
    }

    public static Map<String, Object> executeAndGetMethods(NamingPolicy namingPolicy, Object obj) {
        Class<?> clazz = obj.getClass();
        Map<String, Object> fields = new HashMap<>();
        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            String methodName = method.getName();

            if (methodName.equals("getClass") || methodName.equals("getClassName")) {
                continue;
            }

            if (methodName.startsWith("get") && method.getParameterCount() == 0) {
                try {
                    Object result = method.invoke(obj);
                    String propertyName = methodName.substring(3); // Remove 'get' prefix
                    fields.put(namingPolicy.convert(propertyName), serialize(namingPolicy, result));
                } catch (Exception e) {
                    BacktraceLogger.e(LOG_TAG, String.format("Exception on executing getters, " +
                            "class %s, method name %s, object %s", clazz, methodName, obj), e);
                }
            }
        }
        return fields;
    }

    public static Object serialize(NamingPolicy namingPolicy, Object obj, int serializationDepth) throws JSONException {
        if (obj == null) {
            return null;
        }

        if (serializationDepth > MAX_SERIALIZATION_LEVEL) {
            return new JSONObject();
        }

        if (SerializerHelper.isPrimitiveType(obj)) {
            return obj;
        }

        serializationDepth++;

        if (obj instanceof UUID) {
            return obj.toString();
        }

        if (obj instanceof Map<?, ?>) {
            return serializeMap(namingPolicy, (Map<?, ?>) obj, serializationDepth);
        }

        if (obj.getClass().isArray()) {
            return serializeArray(namingPolicy, (Object[]) obj, serializationDepth);
        }

        if (obj instanceof Collection<?>) {
            return serializeCollection(namingPolicy, (Collection<?>) obj, serializationDepth);
        }

        if (obj instanceof Exception) {
            return serializeException(namingPolicy, (Exception) obj);
        }

        if (obj instanceof Enum) {
            return ((Enum<?>) obj).name();
        }

        return extractFieldsAndGetters(namingPolicy, obj, serializationDepth, obj.getClass());
    }

    @NonNull
    private static JSONObject extractFieldsAndGetters(NamingPolicy namingPolicy, Object obj, int serializationDepth, Class<?> clazz) throws JSONException {
        JSONObject jsonObject = getAllFields(namingPolicy, clazz, obj, serializationDepth);
//        Map<String, Object> getters = executeAndGetMethods(namingPolicy, obj);

//        for (Map.Entry<String, Object> entry: getters.entrySet()) {
//            jsonObject.put(namingPolicy.convert(entry.getKey()), entry.getValue());
//        }

        return jsonObject;
    }
}
