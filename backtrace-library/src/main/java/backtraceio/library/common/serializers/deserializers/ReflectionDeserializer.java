package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.common.serializers.SerializedName;
import backtraceio.library.common.serializers.SerializerHelper;
import backtraceio.library.logger.BacktraceLogger;


public final class ReflectionDeserializer implements Deserializable<Object> {

    private final static String LOG_TAG = ReflectionDeserializer.class.getSimpleName();

    @Override
    public Object deserialize(JSONObject obj) throws JSONException {
        return this.deserialize(obj, Object.class);
    }

    public Object deserialize(JSONObject obj, Class<?> clazz) throws JSONException {
        try {
            // Get the class type from the JSON object if available
            if (obj.has("classType")) {
                String className = obj.getString("classType");
                clazz = Class.forName(className);
            }

            // Create an instance of the class using reflection
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            // Assuming that the class has a default (no-argument) constructor

            // Iterate through the fields of the class
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                // Make the field accessible (public, private, etc.)
                field.setAccessible(true);

                if (java.lang.reflect.Modifier.isTransient(field.getModifiers()) ||
                        java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                if (field.isAnnotationPresent(SerializedName.class)) {
                    SerializedName annotation = field.getAnnotation(SerializedName.class);
                    if (annotation != null) {
                        String customName = annotation.value();
                        if (obj.has(customName)) {
                            field.set(instance, this.deserialize(obj.get(customName), field.getType(), field));
                            continue;
                        }
                    }
                }

                // Get the field name
                String fieldName = field.getName();
                // Check if the JSON object has a key with the field name
                if (obj.has(fieldName)) {
                    // Set the field value using reflection
                    try {
                        field.set(instance, this.deserialize(obj.get(fieldName), field.getType(), field)); // TODO: what if obj.get(fieldNAme) is JSONObject e.g. in BacktraceResult
                    } catch (IllegalArgumentException e) {
                        BacktraceLogger.e(LOG_TAG, String.format("IllegalArgumentException on reflection deserialization of object %s, " +
                                "field %s, reason %s", obj, fieldName, e.getMessage()), e);
                    }
                }
            }

            return instance;
        } catch (Exception e){
            BacktraceLogger.e(LOG_TAG, String.format("Exception on reflection deserialization of object %s, " +
                    "exception type %s, reason %s", obj, e.getClass(), e.getMessage()), e);
        }

        return null;
    }

    public Object deserialize(Object object, Class<?> clazz, Field field) throws JSONException {
        if (object == null) {
            return null;
        }

        if (field.getType().isEnum() && object instanceof String) {
            return Enum.valueOf((Class<Enum>) field.getType(), (String) object);
        }
        // TODO: check if all of the types
        if (SerializerHelper.isPrimitiveType(object)) {
            return object;
        }

        if (clazz == JSONObject.class) {
            return this.deserialize(object, JSONObject.class, field);
        }

        if (clazz == Map.class && object.getClass() == JSONObject.class) {
            return this.deserializeMap((JSONObject) object, clazz, field);
        }
        if (clazz == UUID.class && (object instanceof String || object instanceof UUID)) {
            return UUID.fromString(object.toString());
        }

        // TODO: check other types e.g. List/Array
        return object;
    }

    private Map<?, ?> deserializeMap(JSONObject map, Class<?> clazz, Field field) throws JSONException {

        Map<String, Object> result = new HashMap<>();

        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;

            // Get the actual type arguments (e.g., String, CustomClass)
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length != 2){
                return null;
            }

            Type keyType = typeArguments[0];
            Type valueType = typeArguments[1];
            JSONArray keys = map.names();

            if (keys == null) {
                return null;
            }

            for (int i = 0; i < keys.length(); i++) {
                String key = keys.getString(i);
                Object value = map.get(key);

                if (value instanceof JSONObject && valueType instanceof Class && ((Class) keyType).isInstance(key)) {
                    result.put(key, deserialize((JSONObject) value, (Class) valueType));
                }
            }

            return result;
        }
        return result;
    }
}
