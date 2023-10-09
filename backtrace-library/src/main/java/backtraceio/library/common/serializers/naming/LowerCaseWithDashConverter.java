package backtraceio.library.common.serializers.naming;

import static backtraceio.library.common.serializers.naming.NamingUtils.separateCamelCase;

import java.util.Locale;

public class LowerCaseWithDashConverter implements NamingConverter {
    public String convert(String value) {
        return separateCamelCase(value, '-').toLowerCase(Locale.ENGLISH);
    }
}
