package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExceptionDeserializer implements Deserializable<Exception> {
    static class Fields {
        final static String startLine = "startLine";
        final static String sourceCodeFileName = "sourceCodeFileName";
    }
    @Override
    public Exception deserialize(JSONObject obj) {
        final String message = obj.optString("detail-message", "");

        final Exception exception = new Exception(message);

        try {
            exception.setStackTrace(getStacktrace(obj.optJSONArray("stack-trace")));
        }
        catch (JSONException jsonException) {
            //TODO: handle
            System.out.println(jsonException.toString());
        }
        return exception;
    }

    public StackTraceElement[] getStacktrace(JSONArray array) throws JSONException {
        if (array == null || array.length() == 0) {
            // TODO:
            return null;
        }

        StackTraceElement[] result = new StackTraceElement[array.length()];
        for(int idx = 0; idx < array.length(); idx++) {
            JSONObject obj = (JSONObject) array.get(idx);
            result[idx] = new StackTraceElement(
                obj.optString("declaring-class"), // TODO: make something to not hardcode in this way
                    obj.getString("method-name"), // TODO: make something to not hardcode in this way
                    obj.optString("file-name"), // TODO: make something to not hardcode in this way
                    obj.getInt("line-number") // TODO: make something to not hardcode in this way
            );
        }

        return result;
    }
}