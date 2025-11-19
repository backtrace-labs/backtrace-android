package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import backtraceio.library.models.attributes.ReportDataAttributes;
import backtraceio.library.models.attributes.ReportDataBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ReportDataAttributeBuilderTest {

    private final String key = "attribute-key";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"String value"}, {123}, {45.67}, {true},
        });
    }

    private final Object primitiveValue;

    public ReportDataAttributeBuilderTest(Object primitiveValue) {
        this.primitiveValue = primitiveValue;
    }

    @Test
    public void correctlySetPrimitiveValueIntoAttribute() {
        ReportDataAttributes data = ReportDataBuilder.getReportAttributes(new HashMap<String, Object>() {
            {
                put(key, primitiveValue);
            }
        });
        assertEquals(data.getAttributes().get(key), primitiveValue.toString());
    }

    @Test
    public void shouldSetNullableValueAsAttribute() {
        ReportDataAttributes data = ReportDataBuilder.getReportAttributes(new HashMap<String, Object>() {
            {
                put(key, null);
            }
        });
        assertNull(data.getAttributes().get(key));
    }

    @Test
    public void shouldSkipNullableValue() {
        ReportDataAttributes data = ReportDataBuilder.getReportAttributes(
                new HashMap<String, Object>() {
                    {
                        put(key, null);
                    }
                },
                true);
        assertNull(data.getAttributes().get(key));
    }
}
