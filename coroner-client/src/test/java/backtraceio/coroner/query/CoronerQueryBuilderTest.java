package backtraceio.coroner.query;

import static org.junit.Assert.assertEquals;

import backtraceio.coroner.serialization.GsonWrapper;
import backtraceio.coroner.utils.StringUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class CoronerQueryBuilderTest {

    @Test
    public void testBuildRxIdGroup() {
        // GIVEN
        final CoronerQueryBuilder coronerQueryBuilder = new CoronerQueryBuilder();
        final JsonArray filters = GsonWrapper.fromJson(
                "[{\"_rxid\":[[\"equal\",\"03000000-4f0a-fd08-0000-000000000000\"]]}]", JsonArray.class);
        final List<String> headFolds = Arrays.asList("error.type", "callstack");

        // WHEN
        final JsonObject result = coronerQueryBuilder.buildRxIdGroup(filters, headFolds);

        // THEN
        final String expectedResult =
                "{\"fold\":{\"error.type\":[[\"head\"]],\"callstack\":[[\"head\"]]},\"group\":[[\"_rxid\"]],\"offset\":0,\"limit\":1,\"filter\":[{\"_rxid\":[[\"equal\",\"03000000-4f0a-fd08-0000-000000000000\"]]}]}";
        assertEquals(expectedResult, StringUtils.normalizeSpace(result.toString()));
    }
}
