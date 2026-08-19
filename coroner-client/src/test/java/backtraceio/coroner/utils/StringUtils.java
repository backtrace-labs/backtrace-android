package backtraceio.coroner.utils;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;

public class StringUtils {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    public static String normalizeSpace(String input) {
        return input.trim().replaceAll("\\s+", " ");
    }

    public static boolean compareJson(String json1, String json2) {
        final JsonElement o1 = JsonParser.parseString(json1);
        final JsonElement o2 = JsonParser.parseString(json2);
        final boolean compareResult = o1.equals(o2);

        if (!compareResult) {
            Map<String, Object> json1Map = GSON.fromJson(json1, MAP_TYPE);
            Map<String, Object> json2Map = GSON.fromJson(json2, MAP_TYPE);
            MapDifference<String, Object> difference = Maps.difference(json1Map, json2Map);

            System.out.println("JSON comparison failed. Differences: " + difference);
            return false;
        }
        return true;
    }
}
