package backtraceio.coroner.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CommonTest {
    @Test
    public void stringIsNull() {
        final String input = null;

        boolean result = Common.isNullOrEmpty(input);

        assertTrue(result);
    }

    @Test
    public void stringIsEmpty() {
        final String input = "";

        boolean result = Common.isNullOrEmpty(input);

        assertTrue(result);
    }

    @Test
    public void stringInNotEmpty() {
        final String input = "test-string";

        boolean result = Common.isNullOrEmpty(input);

        assertFalse(result);
    }
}
