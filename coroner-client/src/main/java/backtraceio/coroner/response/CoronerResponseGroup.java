package backtraceio.coroner.response;

import java.math.BigDecimal;
import java.util.List;

public class CoronerResponseGroup {
    private static final Integer EXPECTED_NUMBER_OF_ELEMENTS = 3;

    private final String groupIdentifier;

    private final List<Object> values;

    private final int groupRowCount;

    public CoronerResponseGroup(final List<Object> obj) throws IllegalArgumentException {
        if (obj == null || obj.size() != EXPECTED_NUMBER_OF_ELEMENTS) {
            throw new IllegalArgumentException("Wrong number of elements, expected number of elements: "
                    + EXPECTED_NUMBER_OF_ELEMENTS + ", current value: " + (obj != null ? obj.size() : "null"));
        }

        this.groupIdentifier = obj.get(0).toString();
        this.values = (List<Object>) obj.get(1);
        this.groupRowCount = parseRowCount(obj.get(2));
    }

    private static int parseRowCount(final Object countElement) {
        if (!(countElement instanceof Number)) {
            throw new IllegalArgumentException("Coroner group row count is not numeric");
        }

        final int rowCount;
        try {
            rowCount = new BigDecimal(countElement.toString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException failure) {
            throw new IllegalArgumentException("Coroner group row count must be an exact 32-bit integer", failure);
        }
        if (rowCount <= 0) {
            throw new IllegalArgumentException("Coroner group row count must be positive");
        }
        return rowCount;
    }

    public Object getAttribute(final int index) {
        return values.get(index);
    }

    /** The grouped identifier for this result row; for {@code _rxid}-grouped queries, the RXID. */
    public String getGroupIdentifier() {
        return groupIdentifier;
    }

    /**
     * The number of underlying rows folded into this group. Coroner can collapse a grouped query
     * into a single {@code "*"} group whose count carries the real match total, so callers
     * counting reports must sum row counts rather than counting groups.
     */
    public int getGroupRowCount() {
        return groupRowCount;
    }
}
