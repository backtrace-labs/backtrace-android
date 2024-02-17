package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.ThreadInformation;

public class ThreadInformationDeserializer implements Deserializable<ThreadInformation> {

    private final FieldNameLoader fieldNameLoader = new FieldNameLoader(ThreadInformation.class); // TODO: maybe we can reuse it

    static class Fields {
        final static String name = "name";
        final static String fault = "fault";
        final static String stack = "stack";
    }

    public ThreadInformation deserialize(JSONObject obj) throws JSONException {
        return new ThreadInformation(
                obj.optString(fieldNameLoader.get(Fields.name), null), // TODO: fallback warning
                obj.optBoolean(fieldNameLoader.get(Fields.fault), false),
                getBacktraceStackFrameList(obj.optJSONArray(fieldNameLoader.get(Fields.stack)))
                );
    }

    public List<BacktraceStackFrame> getBacktraceStackFrameList(JSONArray jsonArray) {
        final List<BacktraceStackFrame> result = new ArrayList<>();

//        StackTraceElement[] result = new StackTraceElement[array.length()];
        final BacktraceStackFrameDeserializer deserializer = new BacktraceStackFrameDeserializer(); // TODO: check how to resolve it
        for(int idx = 0; idx < jsonArray.length(); idx++) {
            try {
                JSONObject obj = (JSONObject) jsonArray.get(idx);
                result.add(deserializer.deserialize(obj));
            }
            catch (Exception ex) {
                // TODO: handle
            }
        }

        return result;
    }
}
