package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import backtraceio.library.logger.BacktraceLogger;

public class GenericListDeserializer<T> {
    private static final String LOG_TAG = GenericListDeserializer.class.getSimpleName();

    List<T> deserialize(JSONArray array, Deserializable<T> deserializer) {
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null) {
                try {
                    result.add(deserializer.deserialize(obj));
                } catch (Exception e) {
                    BacktraceLogger.e(LOG_TAG, String.format("Can not deserialize object %s, reason is %s", obj, e.getMessage()), e);
                }
            }
        }
        return result;
    }
}
