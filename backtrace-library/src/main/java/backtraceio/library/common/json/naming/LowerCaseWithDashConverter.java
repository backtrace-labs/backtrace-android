package backtraceio.library.common.json.naming;

import static backtraceio.library.common.json.naming.NamingUtils.separateCamelCase;

import java.util.Locale;

public class LowerCaseWithDashConverter implements NamingConverter {
    public String convert(String value) {
        if (value == null){
            return null;
        }
        return separateCamelCase(value, '-').toLowerCase(Locale.ENGLISH);
    }
}
