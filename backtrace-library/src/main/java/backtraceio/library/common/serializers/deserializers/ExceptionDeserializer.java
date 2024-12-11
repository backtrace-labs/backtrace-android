package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.logger.BacktraceLogger;

public class ExceptionDeserializer implements Deserializable<Exception> {
    private final static String LOG_TAG = ExceptionDeserializer.class.getSimpleName();
    static class Fields {
        final static String detailMessage = "detail-message";
        final static String stackTrace = "stack-trace";
        final static String declaringClass = "declaring-class";
        final static String methodName = "method-name";
        final static String fileName = "file-name";
        final static String lineNumber = "line-number";
    }

    @Override
    public Exception deserialize(JSONObject obj) {
        final String message = obj.optString(Fields.detailMessage, "");

        final Exception exception = new Exception(message);

        try {
            exception.setStackTrace(getStacktrace(obj.optJSONArray(Fields.stackTrace)));
        } catch (JSONException jsonException) {
            BacktraceLogger.e(LOG_TAG, String.format("Error during setting exception stacktrace during deserialization of %s. Error %s", obj, jsonException));
        }
        return exception;
    }

    public StackTraceElement[] getStacktrace(JSONArray array) throws JSONException {
        if (array == null) {
            return null;
        }

        final StackTraceElement[] result = new StackTraceElement[array.length()];
        for (int idx = 0; idx < array.length(); idx++) {
            JSONObject obj = (JSONObject) array.get(idx);
            result[idx] = new StackTraceElement(
                    obj.optString(Fields.declaringClass),
                    obj.getString(Fields.methodName),
                    obj.optString(Fields.fileName),
                    obj.getInt(Fields.lineNumber)
            );
        }

        return result;
    }
}
