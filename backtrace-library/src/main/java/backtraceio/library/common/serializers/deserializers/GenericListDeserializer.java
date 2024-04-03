package backtraceio.library.common.serializers.deserializers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GenericListDeserializer<T> {
    List<T> deserialize(JSONArray array, Deserializable<T> deserializer) {
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null) {
                try {
                    result.add(deserializer.deserialize(obj));
                } catch (Exception e) {
                    // TODO: handle
                }
            }
        }
        return result;
    }
}
