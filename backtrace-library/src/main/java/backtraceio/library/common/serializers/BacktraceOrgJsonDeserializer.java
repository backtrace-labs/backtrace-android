package backtraceio.library.common.serializers;

import org.json.JSONException;
import org.json.JSONObject;

public class BacktraceOrgJsonDeserializer {

        public static <T> T deserialize(String jsonString, Class<T> clazz) throws JSONException {
            return BacktraceDeserializer.deserialize(new JSONObject(jsonString), clazz);
        }

//        public static <T> T deserialize(JSONObject jsonObject, Class<T> clazz) {
//            try {
//                T instance = clazz.newInstance();
//                // Assuming that the class has a default (no-argument) constructor
//
//                // Iterate through the fields of the class
//                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
//                    // Make the field accessible (public, private, etc.)
//                    field.setAccessible(true);
//
//                    // Get the field name
//                    String fieldName = field.getName();
//
//                    // Check if the JSON object has a key with the field name
//                    if (jsonObject.has(fieldName)) {
//                        // Set the field value using reflection
//                        field.set(instance, jsonObject.get(fieldName));
//                        continue;
//                    }
//
//
//                    if (field.isAnnotationPresent(SerializedName.class)) {
//                        SerializedName annotation = field.getAnnotation(SerializedName.class);
//                        if (annotation != null) {
//                            String customName = annotation.value();
//                            field.set(instance, jsonObject.get(customName));
//                            continue;
//                        }
//
//                    }
//                }
//
//                return instance;
//            } catch (InstantiationException | IllegalAccessException e) {
//                e.printStackTrace(); // Handle the exception appropriately
//            } catch (JSONException e) {
//                throw new RuntimeException(e);
//            }
//
//            return null;
//        }
}
