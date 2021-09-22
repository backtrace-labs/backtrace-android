package backtraceio.library.common;

public class BacktraceStringHelper {
    public static boolean isNullOrEmpty(String input) {
        return (input == null || input.trim().isEmpty());
    }

    public static boolean isObjectValidString(Object input) {
        return (input != null && !input.toString().trim().isEmpty());
    }
}
