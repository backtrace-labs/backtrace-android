package backtraceio.coroner.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class CoronerResponseGroupTest {

    @Test
    public void positiveNumericRowCountIsAccepted() {
        final CoronerResponseGroup responseGroup = createResponseGroup(2);

        assertEquals(2, responseGroup.getGroupRowCount());
    }

    @Test
    public void nonNumericRowCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> createResponseGroup("1"));
    }

    @Test
    public void zeroRowCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> createResponseGroup(0));
    }

    @Test
    public void negativeRowCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> createResponseGroup(-1));
    }

    @Test
    public void fractionalRowCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> createResponseGroup(1.5));
    }

    @Test
    public void overflowingRowCountIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createResponseGroup(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE)));
    }

    private static CoronerResponseGroup createResponseGroup(final Object rowCount) {
        return new CoronerResponseGroup(Arrays.asList("*", Collections.emptyList(), rowCount));
    }
}
