package backtraceio.library.common.serializers.naming;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

import backtraceio.library.common.serializers.naming.LowerCaseWithDashConverter;

@RunWith(Parameterized.class)
public class LowerCaseWithDashConverterTest {
    private final String input;
    private final String expectedOutput;

    public LowerCaseWithDashConverterTest(String input, String expectedOutput) {
        this.input = input;
        this.expectedOutput = expectedOutput;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"camelCaseInput", "camel-case-input"},
                {"UpperCaseInput", "upper-case-input"},
                {"snake_case_input", "snake_case_input"},
                {"mixedCASEInput", "mixed-c-a-s-e-input"},
                {"some-dashed-input", "some-dashed-input"},
                {"kebab-Case-Input", "kebab--case--input"},
                {"Space Separated Input", "space -separated -input"},
                {"123NumericInput", "123-numeric-input"},
                {"!@#$%^SpecialChars", "!@#$%^-special-chars"},
                {"", ""}
        });
    }

    @Test
    public void convertShouldConvertToLowerCaseWithDash() {
        // GIVEN
        LowerCaseWithDashConverter converter = new LowerCaseWithDashConverter();

        // WHEN
        String actualOutput = converter.convert(input);

        // THEN
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void convertShouldConvertNullToLowerCaseWithDash() {
        // GIVEN
        LowerCaseWithDashConverter converter = new LowerCaseWithDashConverter();

        // WHEN
        String actualOutput = converter.convert(null);

        // THEN
        assertNull(actualOutput);
    }
}
