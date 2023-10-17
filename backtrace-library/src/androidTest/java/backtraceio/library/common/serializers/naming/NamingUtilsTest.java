package backtraceio.library.common.serializers.naming;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

import backtraceio.library.common.serializers.naming.NamingUtils;

@RunWith(Parameterized.class)
public class NamingUtilsTest {

    private final String input;
    private final char separator;
    private final String expectedOutput;

    public NamingUtilsTest(String input, char separator, String expectedOutput) {
        this.input = input;
        this.separator = separator;
        this.expectedOutput = expectedOutput;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"camelCaseInput", '-', "camel-Case-Input"},
                {"camelcaseinput", '-', "camelcaseinput"},
                {"Camelcaseinput", '-', "Camelcaseinput"},
                {"UpperCaseInput", '_', "Upper_Case_Input"},
                {"snake_case_input", '*', "snake_case_input"},
                {"mixedCASEInput", '.', "mixed.C.A.S.E.Input"},
                {"some-dashed-input", '|', "some-dashed-input"},
                {"kebabCaseInput", ':', "kebab:Case:Input"},
                {"Space Separated Input", '+', "Space +Separated +Input"},
                {"123NumericInput", '_', "123_Numeric_Input"},
                {"!@#$%^SpecialChars", '!', "!@#$%^!Special!Chars"},
                {"", '*', ""},
                {null, '-', null}
        });
    }

    @Test
    public void separateCamelCase_shouldSeparateCamelCase() {
        // Arrange
        String actualOutput = NamingUtils.separateCamelCase(input, separator);

        // Assert
        assertEquals(expectedOutput, actualOutput);
    }
}