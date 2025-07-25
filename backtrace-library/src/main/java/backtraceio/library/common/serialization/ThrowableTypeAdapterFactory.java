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
        final Class<? super T> rawType = typeToken.getRawType();
        if (!Throwable.class.isAssignableFrom(rawType)) {
            return null; // This factory doesn't handle this type.
        }

        final TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
        final TypeAdapter<StackTraceElement> stackTraceElementAdapter = gson.getAdapter(StackTraceElement.class);
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
                out.name("message").value(throwable.getMessage());
                out.name("class").value(throwable.getClass().getName());

                out.name("stack-trace");
                out.beginArray();
                for (StackTraceElement element : throwable.getStackTrace()){
                    stackTraceElementAdapter.write(out, element);
                }
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
                if (jsonObject.has("class")) {
                    actualClassName = jsonObject.get("class").getAsString();
                }

                String message = null;
                if (jsonObject.has("message") && jsonObject.get("message").isJsonPrimitive()) {
                    message = jsonObject.get("message").getAsString();
                }

                List<StackTraceElement> stackTraceList = new ArrayList<>();
                if (jsonObject.has("stack-trace") && jsonObject.get("stack-trace").isJsonArray()) {
                    for (JsonElement elementJson : jsonObject.getAsJsonArray("stack-trace")) {
                        stackTraceList.add(stackTraceElementAdapter.fromJsonTree(elementJson));
                    }
                }

                Throwable cause = null;
                if (jsonObject.has("cause") && jsonObject.get("cause").isJsonObject()) {
                    cause = causeAdapter.fromJsonTree(jsonObject.getAsJsonObject("cause"));
                }

                // Determine the class to instantiate
                Class<? extends Throwable> throwableClassToInstantiate = determineClassToInstantiate(actualClassName);

                Throwable instance = tryInstantiateThrowable(throwableClassToInstantiate, message, cause);

                if (instance == null) {
                    // TODO:
                    return null;
                }

                instance.setStackTrace(stackTraceList.toArray(new StackTraceElement[0]));

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

            private Class<? extends Throwable> determineClassToInstantiate(String actualClassName) {
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
                return throwableClassToInstantiate;
            }


            private <T extends Throwable> T tryInstantiateThrowable(
                    Class<T> exceptionClass,
                    String message,
                    Throwable cause
            ) {

                // Attempt 1: Constructor (String message, Throwable cause)
                try {
                    Constructor<T> constructor = exceptionClass.getDeclaredConstructor(String.class, Throwable.class);
                    constructor.setAccessible(true);
                    return constructor.newInstance(message, cause);
                } catch (NoSuchMethodException e) {
//                    if (logFailures) androidx.media3.common.util.Log.d("ThrowableFactory", exceptionClass.getName() + " lacks (String, Throwable) constructor.");
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
//                    if (logFailures) androidx.media3.common.util.Log.w("ThrowableFactory", "Failed to instantiate " + exceptionClass.getName() + " with (String, Throwable)", e);
                }

                // Attempt 2: Constructor (String message)
                try {
                    Constructor<T> constructor = exceptionClass.getDeclaredConstructor(String.class);
                    constructor.setAccessible(true);
                    T instance = constructor.newInstance(message);
                    if (cause != null) {
                        try {
                            instance.initCause(cause);
                        } catch (IllegalStateException ignored) {
                            // Cause might have already been set by a chained constructor, or not allowed
//                            if (logFailures) androidx.media3.common.util.Log.d("ThrowableFactory", "Could not initCause for " + exceptionClass.getName() + " after (String) constructor.");
                        }
                    }
                    return instance;
                } catch (NoSuchMethodException e) {
//                    if (logFailures) androidx.media3.common.util.Log.d("ThrowableFactory", exceptionClass.getName() + " lacks (String) constructor.");
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
//                    if (logFailures) androidx.media3.common.util.Log.w("ThrowableFactory", "Failed to instantiate " + exceptionClass.getName() + " with (String)", e);
                }

                // Attempt 3: Default constructor
                try {
                    Constructor<T> constructor = exceptionClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    T instance = constructor.newInstance();
                    // Message cannot be set via public API for standard Throwables after default construction.
                    // It will rely on the default message of the exception or be null.
                    if (cause != null) {
                        try {
                            instance.initCause(cause);
                        } catch (IllegalStateException ignored) {
//                            if (logFailures) androidx.media3.common.util.Log.d("ThrowableFactory", "Could not initCause for " + exceptionClass.getName() + " after default constructor.");
                        }
                    }
                    return instance;
                } catch (NoSuchMethodException e) {
//                    if (logFailures) androidx.media3.common.util.Log.d("ThrowableFactory", exceptionClass.getName() + " lacks default constructor.");
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
//                    if (logFailures) androidx.media3.common.util.Log.w("ThrowableFactory", "Failed to instantiate " + exceptionClass.getName() + " with default constructor", e);
                }

                return null; // All attempts failed
            }
        };
    }
}
