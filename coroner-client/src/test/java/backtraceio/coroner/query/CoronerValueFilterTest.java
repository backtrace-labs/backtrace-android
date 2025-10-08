package backtraceio.coroner.query;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonElement;
import org.junit.Test;

public class CoronerValueFilterTest {

    @Test
    public void serializationTest() {
        // GIVEN
        final CoronerValueFilter coronerValueFilter = new CoronerValueFilter(FilterOperator.EQUAL, "123");

        // WHEN
        JsonElement result = coronerValueFilter.get();

        // THEN
        String expectedResult = "[\"equal\",\"123\"]";
        assertEquals(expectedResult, result.toString());
    }
}
