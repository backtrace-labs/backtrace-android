package backtraceio.coroner.response;

import java.util.List;

public class CoronerResponseGroup {
    private static final Integer EXPECTED_NUMBER_OF_EL = 3;
    @SuppressWarnings("unused")
    private final String groupIdentifier;
    private final List<Object> values;

    public CoronerResponseGroup(List<Object> obj) throws IllegalArgumentException {
        if (obj == null || obj.size() != EXPECTED_NUMBER_OF_EL) {
            throw new IllegalArgumentException("Wrong number of elements, expected number of elements: " + EXPECTED_NUMBER_OF_EL
                    + ", current value: " + (obj != null ? obj.size() : "null"));
        }

        this.groupIdentifier = obj.get(0).toString();
        this.values = (List<Object>) obj.get(1);
    }

    public Object getAttribute(int index) {
        return values.get(index);
    }

}
