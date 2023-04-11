package backtraceio.coroner.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CoronerValueFilterTest {

    @Test
    public void serializationTest() {
        // GIVEN
        final CoronerValueFilter coronerValueFilter = new CoronerValueFilter(FilterOperator.EQUAL, "123");

        // WHEN
        String result = coronerValueFilter.toString();

        // THEN
        String expectedResult = "[ \"equal\", \"123\" ]";
        assertEquals(expectedResult, result);
    }
}
