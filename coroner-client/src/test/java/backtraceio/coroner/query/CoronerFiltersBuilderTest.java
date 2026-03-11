package backtraceio.coroner.query;

import static org.junit.Assert.assertEquals;

import backtraceio.coroner.utils.StringUtils;
import com.google.gson.JsonArray;
import org.junit.Test;

public class CoronerFiltersBuilderTest {

    @Test
    public void emptyFilters() {
        // GIVEN
        final CoronerFiltersBuilder filtersBuilder = new CoronerFiltersBuilder();

        // WHEN
        final JsonArray result = filtersBuilder.getJson();

        // THEN
        assertEquals("[{}]", StringUtils.normalizeSpace(result.toString()));
    }

    @Test
    public void singleFilter() {
        // GIVEN
        final String name = "_rxid";
        final FilterOperator operator = FilterOperator.EQUAL;
        final String value = "03000000-4f0a-fd08-0000-000000000000";
        final CoronerFiltersBuilder filtersBuilder = new CoronerFiltersBuilder();

        // WHEN
        filtersBuilder.addFilter(name, operator, value);
        final JsonArray result = filtersBuilder.getJson();

        // THEN
        final String expectedResult = "[{\"_rxid\":[[\"equal\",\"03000000-4f0a-fd08-0000-000000000000\"]]}]";
        assertEquals(expectedResult, result.toString());
    }

    @Test
    public void multipleFilters() {
        // GIVEN
        final CoronerFiltersBuilder filtersBuilder = new CoronerFiltersBuilder();

        // GIVEN FILTER 1
        final String filter1Name = "_rxid";
        final FilterOperator filter1Operator1 = FilterOperator.EQUAL;
        final String filter1Value1 = "03000000-4f0a-fd08-0000-000000000000";
        final FilterOperator filter1Operator2 = FilterOperator.AT_LEAST;
        final String filter1Value2 = "4f0a0000-4f0a-fd08-0000-999000999000";

        // GIVEN FILTER 2
        final String filter2Name = "example_field";
        final FilterOperator filter2Operator = FilterOperator.AT_MOST;
        final String filter2Value = "12345678-4f0b-fdp2-0001-000094000000";

        // WHEN
        filtersBuilder.addFilter(filter1Name, filter1Operator1, filter1Value1);
        filtersBuilder.addFilter(filter1Name, filter1Operator2, filter1Value2);
        filtersBuilder.addFilter(filter2Name, filter2Operator, filter2Value);

        final String result = filtersBuilder.getJson().toString();

        // THEN
        final String expectedResult =
                "[{\"_rxid\":[[\"equal\",\"03000000-4f0a-fd08-0000-000000000000\"],[\"at-least\",\"4f0a0000-4f0a-fd08-0000-999000999000\"]],\"example_field\":[[\"at-most\",\"12345678-4f0b-fdp2-0001-000094000000\"]]}]";
        assertEquals(expectedResult, result);
    }
}
