package backtraceio.library.common.serialization;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

class ThrowableAdapter extends TypeAdapter<Throwable> {
    @Override
    public void write(JsonWriter out, Throwable value) throws IOException {
        out.beginObject();
        out.name("message").value(value.getMessage());
        out.name("class").value(value.getClass().getName());
        out.name("stack-trace");
        out.beginArray();
        for (StackTraceElement element : value.getStackTrace()) {
            out.value(element.toString());
        }
        out.endArray();
        out.endObject();
    }

    @Override
    public Throwable read(JsonReader in) throws IOException {
        String message = null;
        String className = null;
        List<String> stackLines = new ArrayList<>();
        Throwable cause = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "message":
                    message = in.nextString();
                    break;
                case "class":
                    className = in.nextString();
                    break;
                case "stackTrace":
                    in.beginArray();
                    while (in.hasNext()) {
                        stackLines.add(in.nextString());
                    }
                    in.endArray();
                    break;
                case "cause":
                    cause = read(in); // recursive call
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        // Attempt to recreate Throwable (falling back to RuntimeException)
        Throwable t;
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(String.class);
            t = (Throwable) constructor.newInstance(message);
        } catch (Exception e) {
            t = new RuntimeException(message);
        }

        if (cause != null) {
            t.initCause(cause);
        }

        // Recreate stack trace (best effort)
        List<StackTraceElement> elements = new ArrayList<>();
        for (String line : stackLines) {
            elements.add(new StackTraceElement("unknown", line, null, -1));
        }
        t.setStackTrace(elements.toArray(new StackTraceElement[0]));

        return t;
    }
}
