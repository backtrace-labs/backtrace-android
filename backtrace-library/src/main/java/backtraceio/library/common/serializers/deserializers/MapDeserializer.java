package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import backtraceio.library.logger.BacktraceLogger;

public class MapDeserializer {
    private static final String LOG_TAG = BacktraceDataDeserializer.class.getSimpleName();
    public static Map<String, Object> toMap(JSONObject jsonObj) {
        if (jsonObj == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = jsonObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = jsonObj.get(key);
                if (value instanceof JSONArray) {
                    value = toList((JSONArray) value);
                } else if (value instanceof JSONObject) {
                    value = toMap((JSONObject) value);
                }
                map.put(key, value);
            } catch (JSONException e) {
                BacktraceLogger.e(LOG_TAG, String.format("Exception on deserialization map, " +
                        "key %s, object %s", key, jsonObj), e);
            }
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                Object value = array.get(i);
                if (value instanceof JSONArray) {
                    value = toList((JSONArray) value);
                } else if (value instanceof JSONObject) {
                    value = toMap((JSONObject) value);
                }
                list.add(value);
            } catch (JSONException e) {
                BacktraceLogger.e(LOG_TAG, String.format("Exception on deserialization list, " +
                        "index %s, object %s", i, array), e);
            }
        }
        return list;
    }
}
