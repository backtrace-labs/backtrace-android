package backtraceio.coroner.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CoronerFieldFilterTest {
    @Test
    public void singleFilter() {
        // GIVEN
        final String name = "_rxid";
        final FilterOperator operator = FilterOperator.EQUAL;
        final String value = "03000000-4f0a-fd08-0000-000000000000";

        // WHEN
        final CoronerFieldFilter coronerFieldFilter = new CoronerFieldFilter(name, operator, value);
        final String result = coronerFieldFilter.toString();

        // THEN
        final String expectedResult = "\"_rxid\": [[ \"equal\", \"03000000-4f0a-fd08-0000-000000000000\" ]]";
        assertEquals(expectedResult, result);
    }

    @Test
    public void multipleFilters() {
        // GIVEN
        final String filterName = "_rxid";

        final FilterOperator filterOperator1 = FilterOperator.EQUAL;
        final String filterValue1 = "03000000-4f0a-fd08-0000-000000000000";
        final FilterOperator filterOperator2 = FilterOperator.AT_LEAST;
        final String filterValue2 = "4f0a0000-4f0a-fd08-0000-999000999000";

        // WHEN
        final CoronerFieldFilter coronerFieldFilter = new CoronerFieldFilter(filterName, filterOperator1, filterValue1);
        coronerFieldFilter.addValueFilter(filterOperator2, filterValue2);
        final String result = coronerFieldFilter.toString();

        // THEN
        final String expectedResult = "\"_rxid\": [[ \"equal\", \"03000000-4f0a-fd08-0000-000000000000\" ],[ \"at-least\", \"4f0a0000-4f0a-fd08-0000-999000999000\" ]]";
        assertEquals(expectedResult, result);
    }
}
