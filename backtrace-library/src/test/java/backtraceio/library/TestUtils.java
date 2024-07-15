package backtraceio.library;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

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
                }
                return jsonStringBuilder.toString();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String unifyJsonString(String json) {
        return json.replace("\n", "")
                .replace(" ", "")
                .replace("\t", "");
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

            System.out.println(Maps.difference(json1Map, json2Map)); // TODO: improve print
            return false;
        }
        return true;
    }
}
