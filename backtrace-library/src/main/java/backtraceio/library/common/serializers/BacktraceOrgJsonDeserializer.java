package backtraceio.library.common.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.logger.BacktraceLogger;

public class BacktraceOrgJsonDeserializer {
    private final static String LOG_TAG = BacktraceOrgJsonDeserializer.class.getSimpleName();
    public static <T> T deserialize(String jsonString, Class<T> clazz) {
        try {
            return BacktraceDeserializer.deserialize(new JSONObject(jsonString), clazz);
        }
        catch (JSONException jsonException) {
            BacktraceLogger.e(LOG_TAG, String.format("Can not deserialize object %s", jsonString), jsonException); // TODO: test it
            return null;
        }
        catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, String.format("Exception during deserialization of object %s", jsonString), ex); // TODO: test it
            return null;
        }
    }
}
