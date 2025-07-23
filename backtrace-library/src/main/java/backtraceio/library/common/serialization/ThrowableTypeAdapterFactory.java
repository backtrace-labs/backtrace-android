package backtraceio.library.common.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class ThrowableTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        // We are interested in creating adapters for Throwable and its subtypes.
        final Class<? super T> rawType = typeToken.getRawType();
        if (!Throwable.class.isAssignableFrom(rawType)) {
            return null; // This factory doesn't handle this type.
        }

        // This adapter will handle T, where T is Throwable or a subclass.
        final TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
//        final TypeAdapter<StackTraceElement> stackTraceElementAdapter = gson.getAdapter(StackTraceElement.class);
        // Adapter for handling the 'cause' field recursively.
        // We request a TypeAdapter for Throwable itself for the cause.
        final TypeAdapter<Throwable> causeAdapter = gson.getAdapter(Throwable.class);


        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }
                Throwable throwable = (Throwable) value; // Safe cast due to the factory's check

                out.beginObject();
                // Store the actual class name for more accurate deserialization
                out.name("actualClass").value(throwable.getClass().getName());
                out.name("message").value(throwable.getMessage());

                out.name("stackTrace");
                out.beginArray();
//                for (StackTraceElement element : throwable.getStackTrace()) {
//                    stackTraceElementAdapter.write(out, element);
//                }
                out.endArray();

                if (throwable.getCause() != null) {
                    out.name("cause");
                    causeAdapter.write(out, throwable.getCause()); // Use the causeAdapter
                }
                out.endObject();
            }

            @Override
            @SuppressWarnings("unchecked") // For the final cast to T
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                JsonElement jsonElement = jsonElementAdapter.read(in);
                if (!jsonElement.isJsonObject()) {
                    throw new JsonParseException("Expected a JSON object for Throwable deserialization");
                }
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                String actualClassName = null;
                if (jsonObject.has("actualClass")) {
                    actualClassName = jsonObject.get("actualClass").getAsString();
                }

                String message = null;
                if (jsonObject.has("message") && jsonObject.get("message").isJsonPrimitive()) {
                    message = jsonObject.get("message").getAsString();
                }

                List<StackTraceElement> stackTraceList = new ArrayList<>();
                if (jsonObject.has("stackTrace") && jsonObject.get("stackTrace").isJsonArray()) {
                    for (JsonElement elementJson : jsonObject.getAsJsonArray("stackTrace")) {
//                        stackTraceList.add(stackTraceElementAdapter.fromJsonTree(elementJson));
                    }
                }

                Throwable cause = null;
                if (jsonObject.has("cause") && jsonObject.get("cause").isJsonObject()) {
                    cause = causeAdapter.fromJsonTree(jsonObject.getAsJsonObject("cause"));
                }

                // Determine the class to instantiate
                Class<? extends Throwable> throwableClassToInstantiate = (Class<? extends Throwable>) rawType;
                if (actualClassName != null) {
                    try {
                        Class<?> loadedClass = Class.forName(actualClassName);
                        if (Throwable.class.isAssignableFrom(loadedClass)) {
                            // Only use if it's compatible with the requested type T (rawType)
                            if (rawType.isAssignableFrom(loadedClass)) {
                                throwableClassToInstantiate = (Class<? extends Throwable>) loadedClass;
                            }
                            // If actualClassName is more specific but not assignable to rawType,
                            // we stick to rawType to avoid ClassCastException later.
                            // e.g. if deserializing into `Exception.class` but `actualClass` was `Error`.
                        }
                    } catch (ClassNotFoundException ignored) {
                        // Class not found, will fall back to using rawType (the type requested from Gson)
                    }
                }

                Throwable instance = null;
                try {
                    // Try constructor with (String message, Throwable cause)
                    Constructor<? extends Throwable> constructor = throwableClassToInstantiate.getDeclaredConstructor(String.class, Throwable.class);
                    constructor.setAccessible(true);
                    instance = constructor.newInstance(message, cause);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e1) {
                    try {
                        // Try constructor with (String message)
                        Constructor<? extends Throwable> constructor = throwableClassToInstantiate.getDeclaredConstructor(String.class);
                        constructor.setAccessible(true);
                        instance = constructor.newInstance(message);
                        if (cause != null && instance != null) {
                            try {
                                instance.initCause(cause);
                            } catch (IllegalStateException ignored) {
                                // Cause might have already been set by a chained constructor, or not allowed
                            }
                        }
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e2) {
                        try {
                            // Try default constructor
                            Constructor<? extends Throwable> constructor = throwableClassToInstantiate.getDeclaredConstructor();
                            constructor.setAccessible(true);
                            instance = constructor.newInstance();
                            // Manually initialize if possible (though Throwable doesn't have public setters for message/cause after construction)
                            // This part is tricky. For custom exceptions, you might need more specific logic.
                            // For standard exceptions, (String) and (String, Throwable) constructors are common.
                            if (cause != null && instance != null) {
                                try {
                                    instance.initCause(cause);
                                } catch (IllegalStateException ignored) {}
                            }
                        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e3) {
                            // Fallback: create a generic RuntimeException if specific instantiation fails
                            // This is a last resort to avoid losing the exception data entirely.
                            String fallbackMessage = String.format(
                                    "Failed to instantiate %s. Original message: %s. Cause: %s",
                                    throwableClassToInstantiate.getName(), message, (cause != null ? cause.getMessage() : "null")
                            );
                            instance = new RuntimeException(fallbackMessage, cause);
                        }
                    }
                }

                if (instance != null) {
                    instance.setStackTrace(stackTraceList.toArray(new StackTraceElement[0]));
                }

                // Ensure the created instance is assignable to T.
                // This should be true if logic for 'throwableClassToInstantiate' is correct.
                if (rawType.isInstance(instance)) {
                    return (T) instance;
                } else {
                    // This case should ideally not be hit if actualClassName logic is sound
                    // or if only rawType was used.
                    // If it is, it means 'actualClass' was incompatible with the requested type 'T'.
                    // We might return a generic instance of T or throw an error.
                    // For now, let's wrap the incompatible instance if possible, or throw.
                    String errorMessage = String.format(
                            "Deserialized to %s but expected assignable to %s. Message: %s",
                            (instance != null ? instance.getClass().getName() : "null"),
                            rawType.getName(),
                            message
                    );
                    // Create a new instance of the originally requested type (T or rawType) with the info
                    try {
                        Constructor<? extends Throwable> tConstructor = ((Class<? extends Throwable>)rawType).getDeclaredConstructor(String.class, Throwable.class);
                        tConstructor.setAccessible(true);
                        T fallbackT = (T) tConstructor.newInstance(errorMessage, instance); // 'instance' becomes the cause
                        ((Throwable)fallbackT).setStackTrace(stackTraceList.toArray(new StackTraceElement[0]));
                        return fallbackT;
                    } catch (Exception fallbackEx) {
                        throw new JsonParseException("Could not create fallback instance of " + rawType.getName() + ": " + fallbackEx.getMessage(), fallbackEx);
                    }
                }
            }
        };
    }
}
