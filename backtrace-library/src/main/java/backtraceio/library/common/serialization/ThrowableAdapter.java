package backtraceio.library.common.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

class ThrowableAdapter<T extends Throwable> extends TypeAdapter<T> {
    @Override
    public void write(JsonWriter out, T value) throws IOException {
        JsonObject json = new JsonObject();

        try {
            json.addProperty("message", value.getMessage());
            json.addProperty("class", value.getClass().getName());
            json.add("stack-trace", parseStacktraceElements(value.getStackTrace()));

        } catch (JSONException e) {
            // TODO: improve
            e.printStackTrace();
        }

        out.jsonValue(json.toString());
    }

    public JsonArray parseStacktraceElements(StackTraceElement[] stackTraceElements) throws JSONException {
        JsonArray stackTraceArray = new JsonArray();
        for (StackTraceElement element : stackTraceElements) {
            JsonObject elementJson = new JsonObject();
            elementJson.addProperty("declaring-class", element.getClassName());
            elementJson.addProperty("method-name", element.getMethodName());
            elementJson.addProperty("file-name", element.getFileName());
            elementJson.addProperty("line-number", element.getLineNumber());
            stackTraceArray.add(elementJson);
        }
        return stackTraceArray;
    }

    public String getExceptionMessage(JsonObject json) {
        if (json.has("detail-message")) {
            return json.get("detail-message").getAsString();
        }

        if (json.has("message")) {
            return json.get("message").getAsString();
        }
        return null;
    }

    @Override
    public T read(JsonReader in) throws IOException {
        JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
        JsonArray stackTraceArray = json.getAsJsonArray("stack-trace");
        String message = getExceptionMessage(json);
        List<StackTraceElement> stackTraceElementList = new ArrayList<>();
        for (JsonElement element : stackTraceArray) {
            JsonObject elementJson = element.getAsJsonObject();
            String lineNumber = elementJson.get("line-number").getAsString();
            String fileName = elementJson.get("file-name").getAsString();
            String methodName = elementJson.get("method-name").getAsString();
            String declaringClass = elementJson.get("declaring-class").getAsString();
            stackTraceElementList.add(new StackTraceElement(declaringClass, methodName, fileName, Integer.parseInt(lineNumber)));
        }

        Throwable throwable = new Throwable(message);

        throwable.setStackTrace(stackTraceElementList.toArray(new StackTraceElement[0]));
        return (T) throwable;

//        t.setStackTrace(elements.toArray(new StackTraceElement[0]));
//        return this.fromJson(in.toString());
//        in.
//        String message = null;
//        String className = null;
//        List<String> stackLines = new ArrayList<>();
//        Throwable cause = null;
//
//        in.beginObject();
//        while (in.hasNext()) {
//            switch (in.nextName()) {
//                case "message":
//                    message = in.nextString();
//                    break;
//                case "class":
//                    className = in.nextString();
//                    break;
//                case "stackTrace":
//                    in.beginArray();
//                    while (in.hasNext()) {
//                        stackLines.add(in.nextString());
//                    }
//                    in.endArray();
//                    break;
//                case "cause":
//                    cause = read(in); // recursive call
//                    break;
//                default:
//                    in.skipValue();
//            }
//        }
//        in.endObject();
//
//        // Attempt to recreate Throwable (falling back to RuntimeException)
//        Throwable t;
//        try {
//            Class<?> clazz = Class.forName(className);
//            Constructor<?> constructor = clazz.getConstructor(String.class);
//            t = (Throwable) constructor.newInstance(message);
//        } catch (Exception e) {
//            t = new RuntimeException(message);
//        }
//
//        if (cause != null) {
//            t.initCause(cause);
//        }
//
//        // Recreate stack trace (best effort)
//        List<StackTraceElement> elements = new ArrayList<>();
//        for (String line : stackLines) {
//            elements.add(new StackTraceElement("unknown", line, null, -1));
//        }
//        t.setStackTrace(elements.toArray(new StackTraceElement[0]));
//
//        return t;
    }
}
