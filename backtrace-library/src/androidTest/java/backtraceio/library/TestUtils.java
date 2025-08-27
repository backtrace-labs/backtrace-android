package backtraceio.library;

import java.io.InputStream;

public class TestUtils {

    public static InputStream readFileAsStream(Object obj, String fileName) {
        ClassLoader classLoader = obj.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        if (inputStream != null) {
            return inputStream;
        }
        return null;
    }
}
