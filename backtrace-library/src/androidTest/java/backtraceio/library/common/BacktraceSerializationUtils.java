package backtraceio.library.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class BacktraceSerializationUtils {

    public static boolean jsonEquals(String json1, String json2) {
        JsonParser parser = new JsonParser();
        JsonElement o1 = parser.parse(json1);
        JsonElement o2 = parser.parse(json2);
        return o1.equals(o2);
    }
}
