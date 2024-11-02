package backtraceio.library.common.serializers.deserializers.cache;

import org.json.JSONObject;

public class JSONObjectExtensions {
    public static String optStringOrNull(JSONObject jsonObject, String key, String fallback) {
        Object obj = jsonObject.opt(key);
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj != null) {
            return obj.toString();
        }
        return fallback;
    }

    public static String optStringOrNull(JSONObject jsonObject, String key) {
        return JSONObjectExtensions.optStringOrNull(jsonObject, key, null);
    }
}
