package backtraceio.library;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        JsonParser parser = new JsonParser();
        JsonElement o1 = parser.parse(json1);
        JsonElement o2 = parser.parse(json2);
//        System.out.println(o1.equals(o2));
        return o1.equals(o2);
    }
}
