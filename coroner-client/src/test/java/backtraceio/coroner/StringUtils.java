package backtraceio.coroner;

public class StringUtils {
    public static String normalizeSpace(String input) {
        return input.trim().replaceAll("\\s+"," ");
    }
}
