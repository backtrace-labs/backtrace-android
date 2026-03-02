package backtraceio.coroner.utils;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;

public class StringUtils {
    public static String normalizeSpace(String input) {
        return input.trim().replaceAll("\\s+", " ");
    }

    public static boolean compareJson(String json1, String json2) {

        final JsonParser parser = new JsonParser();
        final JsonElement o1 = parser.parse(json1);
        final JsonElement o2 = parser.parse(json2);
        final boolean compareResult = o1.equals(o2);

        if (!compareResult) {
            Gson g = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> json1Map = g.fromJson(json1, mapType);
            Map<String, Object> json2Map = g.fromJson(json2, mapType);

            System.out.println(Maps.difference(json1Map, json2Map));
            return false;
        }
        return true;
    }
}
