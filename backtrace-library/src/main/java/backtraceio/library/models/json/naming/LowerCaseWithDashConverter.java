package backtraceio.library.models.json.naming;

import static backtraceio.library.models.json.naming.NamingUtils.separateCamelCase;

import java.util.Locale;

public class LowerCaseWithDashConverter implements NamingConverter {
    public String convert(String value) {
        return separateCamelCase(value, '-').toLowerCase(Locale.ENGLISH);
    }
}
