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
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import backtraceio.library.BacktraceDatabase;
import backtraceio.library.logger.BacktraceLogger;

public class ThrowableTypeAdapterFactory implements TypeAdapterFactory {

    private static class ConstructorSpec<T extends Throwable> {
        final Class<?>[] paramTypes;
        final Object[] args;

        ConstructorSpec(Class<?>[] paramTypes, Object[] args) {
            this.paramTypes = paramTypes;
            this.args = args;
        }
    }

    private transient final String LOG_TAG = ThrowableTypeAdapterFactory.class.getSimpleName();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {

        final Class<? super T> rawType = typeToken.getRawType();
        if (!Throwable.class.isAssignableFrom(rawType)) {
            BacktraceLogger.d(LOG_TAG, "ThrowableTypeAdapterFactory doesn't handle " + rawType.getName() + " type");
            return null;
        }

        final TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
        final TypeAdapter<StackTraceElement> stackTraceElementAdapter = gson.getAdapter(StackTraceElement.class);
        final TypeAdapter<Throwable> causeAdapter = gson.getAdapter(Throwable.class);

        return new TypeAdapter<T>() {

            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }
                Throwable throwable = (Throwable) value;
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
                    causeAdapter.write(out, throwable.getCause());
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
                    BacktraceLogger.w(LOG_TAG, String.format("Could not instantiate specific Throwable type '%s'. Falling back by returning null.", throwableClassToInstantiate.getName()));
                    return null;
                }

                instance.setStackTrace(stackTraceList.toArray(new StackTraceElement[0]));

                // Ensure the created instance is assignable to T.
                // This should be true if logic for 'throwableClassToInstantiate' is correct.
                if (rawType.isInstance(instance)) {
                    return (T) instance;
                }
                // This case should ideally not be hit if actualClassName logic is sound
                // or if only rawType was used.
                // If it is, it means 'actualClass' was incompatible with the requested type 'T'.
                return createFallbackInstanceOrThrow(instance, message, stackTraceList);
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
                        BacktraceLogger.d(LOG_TAG, "Class "+ actualClassName +" not found, will fall back to using rawType (the type requested from Gson): " + rawType.getSimpleName());
                    }
                }
                return throwableClassToInstantiate;
            }

            @SuppressWarnings("unchecked")
            private <T extends Throwable> T tryInstantiateThrowable(
                    Class<T> exceptionClass,
                    String message,
                    Throwable cause
            ) {

                ConstructorSpec<T>[] specs = new ConstructorSpec[]{
                        new ConstructorSpec<T>(new Class<?>[]{String.class, Throwable.class}, new Object[]{message, cause}),
                        new ConstructorSpec<T>(new Class<?>[]{Throwable.class}, new Object[]{cause}),
                        new ConstructorSpec<T>(new Class<?>[]{String.class}, new Object[]{message}),
                        new ConstructorSpec<T>(new Class<?>[]{}, new Object[]{})
                };

                for (ConstructorSpec<T> spec : specs) {
                    try {
                        Constructor<T> constructor = exceptionClass.getDeclaredConstructor(spec.paramTypes);
                        constructor.setAccessible(true);
                        T instance = constructor.newInstance(spec.args);
                        if (cause != null) {
                            try {
                                instance.initCause(cause);
                            } catch (Exception e) {
                                BacktraceLogger.d(LOG_TAG, "Could not initCause for " + exceptionClass.getName() + " after constructor with args: " + Arrays.toString(spec.paramTypes));
                            }
                        }
                        return instance;
                    }
                    catch (Exception e) {
                        BacktraceLogger.d(LOG_TAG, "Failed to instantiate " + exceptionClass.getName() + " with constructor with args: " + Arrays.toString(spec.paramTypes) + ", error message: " + e);
                    }
                }

                return null;
            }

            @SuppressWarnings("unchecked")
            private T createFallbackInstanceOrThrow(Throwable instance, String message, List<StackTraceElement> stackTraceList) throws JsonParseException {
                String errorMessage = String.format(
                        "Deserialized to %s but expected assignable to %s. Message: %s",
                        (instance != null ? instance.getClass().getName() : "null"),
                        rawType.getName(),
                        message
                );
                // Create a new instance of the originally requested type (T or rawType) with the info
                try {
                    Constructor<? extends Throwable> tConstructor = ((Class<? extends Throwable>) rawType).getDeclaredConstructor(String.class, Throwable.class);
                    tConstructor.setAccessible(true);
                    // 'instance' becomes the cause
                    T fallbackT = (T) tConstructor.newInstance(errorMessage, instance);
                    ((Throwable) fallbackT).setStackTrace(stackTraceList.toArray(new StackTraceElement[0]));
                    return fallbackT;
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
                         InvocationTargetException e) {
                    throw new JsonParseException("Could not create fallback instance of " + rawType.getName() + ": " + e.getMessage(), e);
                }
            }

        };
    }
}
