package backtraceio.coroner.response;

import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class CoronerResponseTest {

    @Test
    public void elementIndexEqualToResultCountIsRejected() {
        final CoronerResponse response = createResponse(
                Collections.singletonList(createColumn("guid")),
                Collections.singletonList(createResponseGroup("guid-value")));

        assertThrows(CoronerResponseProcessingException.class, () -> response.getAttribute(1, "guid", String.class));
    }

    @Test
    public void nullColumnsDescriptionIsRejected() {
        final CoronerResponse response =
                createResponse(null, Collections.singletonList(createResponseGroup("guid-value")));

        assertThrows(CoronerResponseProcessingException.class, () -> response.getAttribute(0, "guid", String.class));
    }

    @Test
    public void nullSelectedResponseGroupIsRejected() {
        final CoronerResponse response =
                createResponse(Collections.singletonList(createColumn("guid")), Collections.singletonList(null));

        assertThrows(CoronerResponseProcessingException.class, () -> response.getAttribute(0, "guid", String.class));
    }

    private static CoronerResponse createResponse(
            final java.util.List<ColumnDescElement> columnsDesc, final java.util.List<CoronerResponseGroup> values) {
        return new CoronerResponse(columnsDesc, values);
    }

    private static ColumnDescElement createColumn(final String name) {
        return new ColumnDescElement(name, null, null, null);
    }

    private static CoronerResponseGroup createResponseGroup(final Object attribute) {
        return new CoronerResponseGroup(
                Arrays.asList("*", Collections.singletonList(Collections.singletonList(attribute)), 1));
    }
}
