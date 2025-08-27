package backtraceio.library;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import backtraceio.gson.Gson;
import backtraceio.gson.JsonElement;
import backtraceio.gson.JsonParser;
import backtraceio.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

public class TestUtils {

    public static String readFileAsString(Object obj, String fileName) {
        ClassLoader classLoader = obj.getClass().getClassLoader();
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

        final JsonParser parser = new JsonParser();
        final JsonElement o1 = parser.parse(json1);
        final JsonElement o2 = parser.parse(json2);
        final boolean compareResult = o1.equals(o2);

        if (!compareResult) {
            Gson g = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> json1Map = g.fromJson(json1, mapType);
            Map<String, Object> json2Map = g.fromJson(json2, mapType);
            MapDifference<String, Object> x = Maps.difference(json1Map, json2Map);
            
            System.out.println(Maps.difference(json1Map, json2Map));
            return false;
        }
        return true;
    }
}
