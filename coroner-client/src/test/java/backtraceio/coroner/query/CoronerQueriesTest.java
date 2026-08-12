package backtraceio.coroner.query;

import static org.junit.Assert.assertEquals;

import backtraceio.coroner.utils.StringUtils;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

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
        JsonObject result = coronerQueries.filterByRxId(rxId);

        // THEN
        String expectedResult =
                "{\"fold\":{},\"group\":[[\"_rxid\"]],\"offset\":0,\"limit\":1,\"filter\":[{\"_rxid\":[[\"equal\",\"03000000-4f0a-fd08-0000-000000000000\"]]}]}";
        assertEquals(expectedResult, StringUtils.normalizeSpace(result.toString()));
    }

    @Test
    public void filterByRxIdTest() {
        // GIVEN
        String rxId = "03000000-4f0a-fd08-0000-000000000000";
        List<String> attributes = Arrays.asList("value1", "value2");

        // WHEN
        JsonObject result = coronerQueries.filterByRxId(rxId, attributes);

        // THEN
        String expectedResult =
                "{\"fold\":{\"value1\":[[\"head\"]],\"value2\":[[\"head\"]]},\"group\":[[\"_rxid\"]],\"offset\":0,\"limit\":1,\"filter\":[{\"_rxid\":[[\"equal\",\"03000000-4f0a-fd08-0000-000000000000\"]]}]}";
        assertEquals(expectedResult, StringUtils.normalizeSpace(result.toString()));
    }

    @Test
    public void filterByGuidAndTimestampTest() {
        // GIVEN
        final String guid = "1f4a2b3c-0000-4f0a-fd08-00000000dead";
        final String timestampStart = "1680943692";
        final String timestampEnd = "1681943692";
        final List<String> attributes = Arrays.asList("error.type", "error.message");

        // WHEN
        JsonObject result = coronerQueries.filterByGuidAndTimestamp(guid, timestampStart, timestampEnd, attributes, 10);

        // THEN
        String expectedResult = "{\"fold\":{\"error.type\":[[\"head\"]],\"error.message\":[[\"head\"]]},"
                + "\"group\":[[\"_rxid\"]],\"offset\":0,\"limit\":10,"
                + "\"filter\":[{\"guid\":[[\"equal\",\"1f4a2b3c-0000-4f0a-fd08-00000000dead\"]],"
                + "\"timestamp\":[[\"at-least\",\"1680943692.\"],[\"at-most\",\"1681943692.\"]]}]}";
        assertEquals(expectedResult, StringUtils.normalizeSpace(result.toString()));
    }

    @Test
    public void filterByGuidEscapesJsonSpecialCharacters() {
        // A GUID is validated as non-blank only; prove special characters cannot break the JSON.
        JsonObject result =
                coronerQueries.filterByGuidAndTimestamp("we\"ird\\guid", "1", "2", Arrays.asList("error.type"), 10);

        String serialized = result.toString();
        org.junit.Assert.assertTrue(serialized.contains("we\\\"ird\\\\guid"));
        // Round-trips through the parser without corruption.
        com.google.gson.JsonParser.parseString(serialized);
    }

    @Test
    public void filterByGuidRejectsBlankGuid() {
        org.junit.Assert.assertThrows(
                IllegalArgumentException.class,
                () -> coronerQueries.filterByGuidAndTimestamp(null, "1", "2", Arrays.asList("a"), 10));
        org.junit.Assert.assertThrows(
                IllegalArgumentException.class,
                () -> coronerQueries.filterByGuidAndTimestamp("  ", "1", "2", Arrays.asList("a"), 10));
    }

    @Test
    public void filterByErrorTypeAndTimestampTest() {
        // GIVEN
        final List<String> attributes = Arrays.asList("error.message", "example-attribute");

        final String errorType = "Crash";
        final String timestampStart = "1680943692";
        final String timestampEnd = "1681943692";

        // WHEN
        JsonObject result =
                coronerQueries.filterByErrorTypeAndTimestamp(errorType, timestampStart, timestampEnd, attributes);

        // THEN
        String expectedResult =
                "{\"fold\":{\"error.message\":[[\"head\"]],\"example-attribute\":[[\"head\"]]},\"group\":[[\"_rxid\"]],\"offset\":0,\"limit\":1,\"filter\":[{\"error.type\":[[\"equal\",\"Crash\"]],\"timestamp\":[[\"at-least\",\"1680943692.\"],[\"at-most\",\"1681943692.\"]]}]}";
        assertEquals(expectedResult, StringUtils.normalizeSpace(result.toString()));
    }
}
