package backtraceio.library.common.serializers;

import org.json.JSONException;
import org.json.JSONObject;

public class BacktraceOrgJsonDeserializer {

    public static <T> T deserialize(String jsonString, Class<T> clazz) throws JSONException {
        return BacktraceDeserializer.deserialize(new JSONObject(jsonString), clazz);
    }
}
