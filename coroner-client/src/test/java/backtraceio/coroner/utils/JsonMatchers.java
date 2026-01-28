package backtraceio.coroner.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.mockito.ArgumentMatcher;

public final class JsonMatchers {

    public static ArgumentMatcher<String> jsonEquals(String expectedJson) {
        JsonElement expected = JsonParser.parseString(expectedJson);

        return actualJson -> {
            if (actualJson == null) {
                return false;
            }
            JsonElement actual = JsonParser.parseString(actualJson);
            return expected.equals(actual);
        };
    }
}
