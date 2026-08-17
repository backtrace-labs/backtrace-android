package backtraceio.library;

import backtraceio.gson.Gson;
import backtraceio.gson.JsonElement;
import backtraceio.gson.JsonParser;
import backtraceio.gson.reflect.TypeToken;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class TestUtils {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    public static String readFileAsString(Object obj, String fileName) {
        ClassLoader classLoader = obj.getClass().getClassLoader();
        if (classLoader == null) {
            return null;
        }
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder jsonStringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonStringBuilder.append(line);
                    if (reader.ready()) {
                        jsonStringBuilder.append("\n");
                    }
                }
                return jsonStringBuilder.toString();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String minifyJsonString(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        return jsonObject.toString();
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
