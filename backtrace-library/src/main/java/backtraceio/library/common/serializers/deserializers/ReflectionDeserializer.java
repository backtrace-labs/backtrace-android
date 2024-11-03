package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.common.serializers.SerializedName;
import backtraceio.library.common.serializers.SerializerHelper;


public final class ReflectionDeserializer implements Deserializable<Object> {

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
            clazz.getDeclaredConstructor().getModifiers();

            // TODO: what if constructor is private?
            Object instance = clazz.getDeclaredConstructor().newInstance();
            // Assuming that the class has a default (no-argument) constructor

            // Iterate through the fields of the class
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                // Make the field accessible (public, private, etc.)
                field.setAccessible(true);

                if (java.lang.reflect.Modifier.isTransient(field.getModifiers()) ||
                        java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                // Get the field name
                String fieldName = field.getName();
                new JSONObject();
                // Check if the JSON object has a key with the field name
                if (obj.has(fieldName)) {
                    // Set the field value using reflection
                    try {
                        field.set(instance, this.deserialize(obj.get(fieldName), field.getType(), field)); // TODO: what if obj.get(fieldNAme) is JSONObject e.g. in BacktraceResult
                    } catch (IllegalArgumentException exception) {
                        // TODO: log error
                        continue;
                    }
                    continue;
                }


                if (field.isAnnotationPresent(SerializedName.class)) {
                    SerializedName annotation = field.getAnnotation(SerializedName.class);
                    if (annotation != null) {
                        String customName = annotation.value();
                        field.set(instance, obj.get(customName));
                        continue;
                    }

                }
            }

            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace(); // Handle the exception appropriately
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public Object deserialize(Object object, Class<?> clazz, Field field) throws JSONException {
        if (object == null) {
            return null;
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

        // TODO: check other types
        return object;
    }

    private Map<?, ?> deserializeMap(JSONObject map, Class<?> clazz, Field field) throws JSONException {

        Map<String, Object> result = new HashMap<>();

        JSONArray keys = map.names();

        if (keys == null) {
            return null;
        }

        for (int i = 0; i < keys.length (); i++) {
            String key = keys.getString (i);
            Object value = map.get(key);

            if(value instanceof JSONObject) {
                result.put(key, deserialize(value, clazz, field));
            }
        }

        return result;
    }
}
