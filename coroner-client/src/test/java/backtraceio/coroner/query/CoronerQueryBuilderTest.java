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

    /** The no-limit overload must keep emitting {@code limit: 1} for existing callers. */
    @Test
    public void defaultOverloadKeepsLimitOne() {
        final CoronerQueryBuilder coronerQueryBuilder = new CoronerQueryBuilder();
        final JsonArray filters = GsonWrapper.fromJson("[{\"guid\":[[\"equal\",\"abc\"]]}]", JsonArray.class);

        final JsonObject result = coronerQueryBuilder.buildRxIdGroup(filters, Arrays.asList("error.type"));

        assertEquals(1, result.get("limit").getAsInt());
    }

    @Test
    public void explicitLimitIsEmitted() {
        final CoronerQueryBuilder coronerQueryBuilder = new CoronerQueryBuilder();
        final JsonArray filters = GsonWrapper.fromJson("[{\"guid\":[[\"equal\",\"abc\"]]}]", JsonArray.class);

        final JsonObject result = coronerQueryBuilder.buildRxIdGroup(filters, Arrays.asList("error.type"), 10);

        assertEquals(10, result.get("limit").getAsInt());
    }

    @Test
    public void invalidLimitsThrow() {
        final CoronerQueryBuilder coronerQueryBuilder = new CoronerQueryBuilder();
        final JsonArray filters = GsonWrapper.fromJson("[{\"guid\":[[\"equal\",\"abc\"]]}]", JsonArray.class);
        final List<String> headFolds = Arrays.asList("error.type");

        org.junit.Assert.assertThrows(
                IllegalArgumentException.class, () -> coronerQueryBuilder.buildRxIdGroup(filters, headFolds, 0));
        org.junit.Assert.assertThrows(
                IllegalArgumentException.class, () -> coronerQueryBuilder.buildRxIdGroup(filters, headFolds, -1));
        org.junit.Assert.assertThrows(
                IllegalArgumentException.class,
                () -> coronerQueryBuilder.buildRxIdGroup(filters, headFolds, CoronerQueryBuilder.MAX_LIMIT + 1));
    }
}
