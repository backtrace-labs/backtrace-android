package backtraceio.library.common;

public class BacktraceStringHelper {
    public static boolean isNullOrEmpty(String input) {
        return (input == null || input.trim().isEmpty());
    }

    public static boolean isObjectNotNullOrNotEmptyString(Object input) {
        if (input instanceof String) {
            return (input != null && !input.toString().trim().isEmpty());
        } else {
            return (input != null);
        }
    }
}
