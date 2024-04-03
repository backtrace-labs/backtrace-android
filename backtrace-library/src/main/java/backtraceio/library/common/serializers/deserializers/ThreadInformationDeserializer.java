package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import backtraceio.library.common.serializers.deserializers.cache.FieldNameLoader;
import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.ThreadInformation;

public class ThreadInformationDeserializer implements Deserializable<ThreadInformation> {

    private final FieldNameLoader fieldNameLoader = new FieldNameLoader(ThreadInformation.class); // TODO: maybe we can reuse it
    final BacktraceStackFrameDeserializer stackFrameDeserializer;
    static class Fields {
        final static String name = "name";
        final static String fault = "fault";
        final static String stack = "stack";
    }

    public ThreadInformationDeserializer() {
        this.stackFrameDeserializer = new BacktraceStackFrameDeserializer();
    }

    public ThreadInformation deserialize(JSONObject obj) throws JSONException {
        return new ThreadInformation(
                obj.optString(fieldNameLoader.get(Fields.name), null), // TODO: fallback warning
                obj.optBoolean(fieldNameLoader.get(Fields.fault), false),
                getBacktraceStackFrameList(obj.optJSONArray(fieldNameLoader.get(Fields.stack))));
    }

    public List<BacktraceStackFrame> getBacktraceStackFrameList(JSONArray array) {
        if (array == null) {
            return null; // todo: Check if we should return empty or null
        }

        GenericListDeserializer<BacktraceStackFrame> deserializer = new GenericListDeserializer<>();
        return deserializer.deserialize(array, this.stackFrameDeserializer);
    }
}
