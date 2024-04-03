package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;

import backtraceio.library.common.serializers.SerializedName;


public final class ReflectionDeserializer implements Deserializable<Object> {

    @Override
    public Object deserialize(JSONObject obj) throws JSONException {
        try {
            Class<?> clazz = Object.class;

            // Get the class type from the JSON object if available
            if (obj.has("classType")) {
                String className = obj.getString("classType");
                clazz = Class.forName(className);
            }

            // Create an instance of the class using reflection
            Object instance = clazz.getDeclaredConstructor().newInstance();
            // Assuming that the class has a default (no-argument) constructor

            // Iterate through the fields of the class
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                // Make the field accessible (public, private, etc.)
                field.setAccessible(true);

                // Get the field name
                String fieldName = field.getName();

                // Check if the JSON object has a key with the field name
                if (obj.has(fieldName)) {
                    // Set the field value using reflection
                    field.set(instance, obj.get(fieldName));
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
}
