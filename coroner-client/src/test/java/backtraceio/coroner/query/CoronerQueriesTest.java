package backtraceio.coroner.query;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import backtraceio.coroner.StringUtils;

public class CoronerQueriesTest {
    public CoronerQueries coronerQueries;

    @Before
    public void init() {
        coronerQueries = new CoronerQueries();
    }

    @Test
    public void filterByRxIdWithoutAttributesTest() {
        // GIVEN
        String rxId = "03000000-4f0a-fd08-0000-000000000000";

        // WHEN
        String result = coronerQueries.filterByRxId(rxId);

        // THEN
        String expectedResult = "{\"group\":[ [\"_rxid\"]],\"fold\": {}, \"offset\":0, \"limit\":1, \"filter\":[{ \"_rxid\": [[ \"equal\", \"03000000-4f0a-fd08-0000-000000000000\" ]] }]}";
        assertEquals(expectedResult, StringUtils.normalizeSpace(result));
    }

    @Test
    public void filterByRxIdTest() {
        // GIVEN
        String rxId = "03000000-4f0a-fd08-0000-000000000000";
        List<String> attributes = Arrays.asList("value1", "value2");

        // WHEN
        String result = coronerQueries.filterByRxId(rxId, attributes);

        // THEN
        String expectedResult = "{\"group\":[ [\"_rxid\"]],\"fold\": {\"value1\": [[\"head\"]],\"value2\": [[\"head\"]]}, \"offset\":0, \"limit\":1, \"filter\":[{ \"_rxid\": [[ \"equal\", \"03000000-4f0a-fd08-0000-000000000000\" ]] }]}";
        assertEquals(expectedResult, StringUtils.normalizeSpace(result));
    }

    @Test
    public void filterByErrorTypeAndTimestampTest() {
        // GIVEN
        final List<String> attributes = Arrays.asList("error.message", "example-attribute");

        final String errorType = "Crash";
        final String timestampStart = "1680943692";
        final String timestampEnd = "1681943692";

        // WHEN
        String result = coronerQueries.filterByErrorTypeAndTimestamp(errorType, timestampStart, timestampEnd, attributes);

        // THEN
        String expectedResult = "{\"group\":[ [\"_rxid\"]],\"fold\": {\"error.message\": [[\"head\"]],\"example-attribute\": [[\"head\"]]}, \"offset\":0, \"limit\":1, \"filter\":[{ \"error.type\": [[ \"equal\", \"Crash\" ]],\"timestamp\": [[ \"at-least\", \"1680943692.\" ],[ \"at-most\", \"1681943692.\" ]] }]}";
        assertEquals(expectedResult, StringUtils.normalizeSpace(result));
    }
}
