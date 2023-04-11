package backtraceio.coroner.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import backtraceio.coroner.utils.StringUtils;

public class CoronerQueryBuilderTest {

    @Test
    public void testBuildRxIdGroup() {
        // GIVEN
        final CoronerQueryBuilder coronerQueryBuilder = new CoronerQueryBuilder();
        final String filters = "{\"_rxid\":[[\"equal\",\"03000000-4f0a-fd08-0000-000000000000\"]]}";
        final List<String> headFolds = Arrays.asList("error.type", "callstack");

        // WHEN
        final String result = coronerQueryBuilder.buildRxIdGroup(filters, headFolds);

        // THEN
        final String expectedResult = "{\"group\":[ [\"_rxid\"]],\"fold\": {\"error.type\": [[\"head\"]],\"callstack\": [[\"head\"]]}, \"offset\":0, \"limit\":1, \"filter\":[{\"_rxid\":[[\"equal\",\"03000000-4f0a-fd08-0000-000000000000\"]]}]}";
        assertEquals(expectedResult, StringUtils.normalizeSpace(result));
    }
}
