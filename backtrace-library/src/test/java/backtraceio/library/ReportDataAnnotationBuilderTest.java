package backtraceio.library;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

import backtraceio.library.models.attributes.ReportDataAttributes;
import backtraceio.library.models.attributes.ReportDataBuilder;

@RunWith(Parameterized.class)
public class ReportDataAnnotationBuilderTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new HashMap<String, String>() {{
                    put("add", "value");
                }}},
                {new HashSet<String>() {{
                    add("value");
                }}},
                {new Object()},
        });
    }

    private final Object annotation;

    public ReportDataAnnotationBuilderTest(Object annotation) {
        this.annotation = annotation;
    }

    @Test
    public void correctlySetComplexObjectAsAnnotation() {
        String key = "annotation-key";
        ReportDataAttributes data = ReportDataBuilder.getReportAttributes(new HashMap<String, Object>() {
            {
                put(key, annotation);
            }
        });
        assertEquals(data.getAnnotations().get(key), annotation);
    }
}
