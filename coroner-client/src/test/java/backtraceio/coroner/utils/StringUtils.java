package backtraceio.coroner.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class StringUtils {
    public static String normalizeSpace(String input) {
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Compares two JSON strings for structural equality using Gson.
     *
     * @param json1 First JSON string
     * @param json2 Second JSON string
     * @return true if both represent the same JSON structure, false otherwise
     */
    public static boolean assertJson(String json1, String json2) {
        try {
            Gson gson = new Gson();

            JsonElement element1 = JsonParser.parseString(json1);
            JsonElement element2 = JsonParser.parseString(json2);

            return element1.equals(element2);
        } catch (Exception e) {
            // Invalid JSON or parsing error
            return false;
        }
    }
}
