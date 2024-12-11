package backtraceio.library.common.json.naming;

public class NamingUtils {
    static String separateCamelCase(String name, char separator) {
        StringBuilder translation = new StringBuilder();
        if (name == null) {
            return null;
        }
        for (int i = 0, length = name.length(); i < length; i++) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(separator);
            }
            translation.append(character);
        }
        return translation.toString();
    }
}
