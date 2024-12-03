package backtraceio.library.common.serializers.deserializers;

import android.os.Build;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
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

            Object instance = createNewInstance(clazz);

//            if (clazz.getConstructors().length == 0) {
//                BacktraceLogger.e(LOG_TAG, String.format("Can`t find constructor for %s when deserializing object %s", clazz, obj));
//                return null;
//            }
            // Create an instance of the class using reflection
//            Constructor<?> constructor = clazz.getDeclaredConstructor();
//            constructor.setAccessible(true);
//            Object instance = constructor.newInstance();
            // Assuming that the class has a default (no-argument) constructor

            // Iterate through the fields of the class
            Class<?> currentClass = clazz;
            while (currentClass != null) {
                for (java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
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
                currentClass = currentClass.getSuperclass();
            }

            return instance;
        } catch (Exception e) {
            BacktraceLogger.e(LOG_TAG, String.format("Exception on reflection deserialization of object %s, " +
                    "exception type %s, reason %s", obj, e.getClass(), e.getMessage()), e);
        }

        return null;
    }

    public Object createNewInstance(Class<?> clazz) {
        try {
            Constructor<?> nonArgConstructor = getNonArgConstructor(clazz);
            if (nonArgConstructor != null) {
                nonArgConstructor.setAccessible(true);
                return nonArgConstructor.newInstance();
            }
            return createInstanceUsingAllocate(clazz);
        } catch (Exception e) {
            // TODO:
            return null;
        }
    }

    @Nullable
    private static Object createInstanceUsingAllocate(Class<?> clazz) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return allocateInstance.invoke(unsafe, clazz);
    }

    public Constructor<?> getNonArgConstructor(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if(constructor.getParameters().length == 0) {
                    return constructor;
                }
            }
        }
        return null;
    }

    public Object deserialize(Object object, Class<?> clazz, Field field) throws JSONException {
        if (object == null) {
            return null;
        }


        // TODO: check if all of the types
        if (SerializerHelper.isPrimitiveType(object) && (field == null || field.getType().isPrimitive())) {
            return this.handlePrimitiveType(object, clazz);
        }

        if (field == null) {
            return null;
        }

        if (field.getType().isEnum() && object instanceof String) {
            return Enum.valueOf((Class<Enum>) field.getType(), (String) object);
        }

        if (clazz == JSONObject.class && field != null) {
            return this.deserialize(object, JSONObject.class, field);
        }

        if (Map.class.isAssignableFrom(clazz) && object.getClass() == JSONObject.class && field != null) {
            return this.deserializeMap((JSONObject) object, clazz, field);
        }

        if (clazz == UUID.class && (object instanceof String || object instanceof UUID)) {
            return UUID.fromString(object.toString());
        }

        if (Collection.class.isAssignableFrom(clazz) && (object instanceof JSONArray)) {
            return this.deserializeCollection((JSONArray) object, clazz, field);
        }

        if (field.getType().isArray() && (object instanceof JSONArray)) {
            return this.deserializeArray((JSONArray) object, clazz, field);
        }

        if (clazz != null && field != null && object != null && object.getClass() == JSONObject.class) {
            return this.deserialize((JSONObject) object, clazz);
        }

        return this.deserialize(object, clazz, null);
    }

    private <T> T[] deserializeArray(JSONArray jsonArray, Class<T> clazz, Field field) {
        // Deserialize the JSON array into a collection
        Collection<?> result = deserializeCollection(jsonArray, clazz, field);

        // Create a strongly-typed array of the appropriate type
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(clazz.getComponentType(), result.size());
        return result.toArray(array);
    }

//    private Object[] deserializeArray(JSONArray jsonArray, Class<?> clazz, Field field) {
//
//        Collection<?> result = deserializeCollection(jsonArray, clazz, field);
//
//        return Arrays.copyOf(result.toArray(), result.size(), XYZ);
//        if (jsonArray == null) {
//            return null;
//        }
//
//        Object[] result = new Object[jsonArray.length()];
//        for (int i = 0; i < jsonArray.length(); i++) {
//            try {
//                Object obj = jsonArray.get(i);
//
//                Class<?> objType = clazz.getComponentType();
//
//                if (obj instanceof JSONObject) {
//                    result[i] = objType.cast(deserialize((JSONObject) obj, objType));
//                } else {
//                    result[i] = objType.cast(deserialize(obj, objType, null));
//                }
//            } catch (Exception e) {
//
//                BacktraceLogger.w(LOG_TAG, ""); // TODO: add error msg
//            }
//        }
//        return result;
//    }

    private Collection<?> deserializeCollection(JSONArray jsonArray, Class<?> clazz, Field field) {
        if (jsonArray == null) {
            return null;
        }

        Collection<Object> result = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Class<?> objType;
                if (clazz.getComponentType() != null){
                    objType = clazz.getComponentType();
                } else {
                    objType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                }

                Object obj = jsonArray.get(i);
                if (obj instanceof JSONObject) {
                    result.add(objType.cast(deserialize((JSONObject) obj, objType)));
                } else {
                    result.add(objType.cast(deserialize(obj, objType, null)));
                }
            } catch (Exception e) {
                BacktraceLogger.w(LOG_TAG, ""); // TODO: add error msg
            }
        }

        return result;
    }

    private Map<?, ?> deserializeMap(JSONObject map, Class<?> clazz, Field field) throws JSONException {

        Map<String, Object> result = new HashMap<String, Object>();

//        (Map<String, Object>) new HashMap<String, Object>();

//        if (result == null) {
//            return null;
//        }

        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;

            // Get the actual type arguments (e.g., String, CustomClass)
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length != 2) {
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

                if (value instanceof JSONObject && valueType instanceof Class && ((Class<?>) keyType).isInstance(key)) {
                    result.put(key, deserialize((JSONObject) value, (Class<?>) valueType));
                } else {
                    result.put(key, deserialize(value, (Class<?>) valueType, null));
                }
            }
        }
        return (Map<String, Object>) result;
    }

    private Object handlePrimitiveType(Object object, Class<?> clazz) {
        // Handle primitive types and their boxed counterparts
        if (clazz == int.class || clazz == Integer.class) {
            return ((Number) object).intValue();
        } else if (clazz == double.class || clazz == Double.class) {
            return ((Number) object).doubleValue();
        } else if (clazz == float.class || clazz == Float.class) {
            return ((Number) object).floatValue();
        } else if (clazz == long.class || clazz == Long.class) {
            return ((Number) object).longValue();
        } else if (clazz == short.class || clazz == Short.class) {
            return ((Number) object).shortValue();
        } else if (clazz == byte.class || clazz == Byte.class) {
            return ((Number) object).byteValue();
        } else if (clazz == boolean.class || clazz == Boolean.class) {
            return Boolean.valueOf(object.toString());
        } else if (clazz == char.class || clazz == Character.class) {
            return object.toString().charAt(0);
        } else if (clazz == String.class) {
            return object.toString();
        }
        return object;
//        throw new IllegalArgumentException("Unsupported primitive type: " + clazz);
    }
}
