package backtraceio.library.common.json.serialization;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.logger.BacktraceLogger;

public class BacktraceOrgJsonDeserializer {
    private final static String LOG_TAG = BacktraceOrgJsonDeserializer.class.getSimpleName();

    public static <T> T deserialize(String jsonString, Class<T> clazz) {
        try {
            return BacktraceDeserializer.deserialize(new JSONObject(jsonString), clazz);
        } catch (JSONException jsonException) {
            BacktraceLogger.e(LOG_TAG,
                    String.format("Can not deserialize object %s because of %s", jsonString, jsonException.getMessage()), jsonException);
            return null;
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG,
                    String.format("Exception during deserialization of object %s because of %s", jsonString, ex.getMessage()), ex);
            return null;
        }
    }
}
