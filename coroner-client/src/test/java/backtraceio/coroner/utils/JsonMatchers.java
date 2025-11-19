package backtraceio.coroner.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.mockito.ArgumentMatcher;

public class JsonMatchers {

    public static ArgumentMatcher<String> jsonEquals(String expectedJson) {
        return actualJson -> {
            try {
                JsonElement expected = JsonParser.parseString(expectedJson);
                JsonElement actual = JsonParser.parseString(actualJson);
                return expected.equals(actual);
            } catch (Exception e) {
                return false;
            }
        };
    }
}
