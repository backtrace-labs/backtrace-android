package backtraceio.library.common.json.deserialization.cache;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import backtraceio.library.common.json.serialization.SerializedName;

public class FieldNameCache {
        // Map to store annotation information
        private final static Map<String, String> fieldNameMap = new HashMap<>();

        // Method to get annotation for a given class and field
        public static String getAnnotation(Class<?> clazz, @NonNull String fieldName) {
            // Generate a unique key for the class and field combination
            String key = clazz.getName() + "_" + fieldName;

            // Check if the annotation is already cached
            String cachedFieldName = fieldNameMap.get(key);
            if (cachedFieldName == null) {
                // If not cached, retrieve the annotation and store it in the map
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    if (field.isAnnotationPresent(SerializedName.class)) {
                        SerializedName annotation = field.getAnnotation(SerializedName.class);
                        if (annotation != null) {
                            cachedFieldName = annotation.value();
                            fieldNameMap.put(key, cachedFieldName);
                        }
                    } else {
                        cachedFieldName = field.getName();
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
            return cachedFieldName;
        }
    }
