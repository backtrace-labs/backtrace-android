package backtraceio.library.common.json.deserialization.cache;

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

    public static Integer optIntegerOrNull(JSONObject jsonObject, String key) {
        Object obj = jsonObject.opt(key);
        if (obj != null && obj instanceof Integer) {
            return (Integer) obj;
        }
        return null;
    }

    public static String optStringOrNull(JSONObject jsonObject, String key) {
        return JSONObjectExtensions.optStringOrNull(jsonObject, key, null);
    }
}
