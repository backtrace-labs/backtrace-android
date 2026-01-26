package backtraceio.library.common.serialization;

import backtraceio.gson.TypeAdapter;
import backtraceio.gson.stream.JsonReader;
import backtraceio.gson.stream.JsonToken;
import backtraceio.gson.stream.JsonWriter;
import java.io.IOException;

public class StackTraceElementTypeAdapter extends TypeAdapter<StackTraceElement> {

    @Override
    public void write(JsonWriter out, StackTraceElement value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        out.name("declaring-class").value(value.getClassName());
        out.name("method-name").value(value.getMethodName());
        out.name("file-name").value(value.getFileName());
        out.name("line-number").value(value.getLineNumber());
        // Do NOT write classLoaderName (for compatibility)
        out.endObject();
    }

    @Override
    public StackTraceElement read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String className = null;
        String methodName = null;
        String fileName = null;
        int lineNumber = -1;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "declaring-class":
                    className = in.nextString();
                    break;
                case "method-name":
                    methodName = in.nextString();
                    break;
                case "file-name":
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        fileName = null;
                    } else {
                        fileName = in.nextString();
                    }
                    break;
                case "line-number":
                    lineNumber = in.nextInt();
                    break;
                    // Ignore any unknown fields (including classLoaderName)
                default:
                    in.skipValue();
            }
        }
        in.endObject();
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }
}
