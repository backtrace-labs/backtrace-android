package backtraceio.library.common.json;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

@RunWith(Parameterized.class)
public class DeserializerGsonComparisonTest {
    private final Class<Object> clazz;
    private final String jsonPath;

    private final static int MAX_INCREASE_TIME_RATIO = 10;

    public DeserializerGsonComparisonTest(Class<Object> clazz, String jsonPath) {
        this.clazz = clazz;
        this.jsonPath = jsonPath;

    }
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BacktraceApiResult.class, "backtraceApiResult.json"},
                {BacktraceApiResult.class, "backtraceApiResult.error.json"},
                {BacktraceData.class, "backtraceData.json"},
                {BacktraceReport.class, "backtraceReport.json"},
                {BacktraceReport.class, "backtraceReport2.json"},
                {BacktraceResult.class, "backtraceResult.json"},
                {BacktraceStackFrame.class, "backtraceStackFrame.json"},
                {SourceCode.class, "sourceCode.json"},
                {ThreadInformation.class, "threadInformation.json"}
        });
    }
    @Test
    public void testGsonPerformanceDeserializer() {
        final String json = TestUtils.readFileAsString(this, jsonPath);
        final long norm = 1;
        assertNotNull(json);

        // WHEN ORG.JSON
        final long startTimeBacktrace = System.currentTimeMillis();
        final Object backtraceResult = BacktraceSerializeHelper.fromJson(json, clazz);
        final long endTimeBacktrace = System.currentTimeMillis();
        final long totalTimeBacktrace = endTimeBacktrace - startTimeBacktrace + norm;

        // WHEN GSON
        final long startTimeGson = System.currentTimeMillis();
        final Object gsonResult = new Gson().fromJson(json, clazz);
        final long endTimeGson = System.currentTimeMillis();
        final long totalTimeGson = endTimeGson - startTimeGson + norm;

        // THEN
        assertNotNull(backtraceResult);
        assertNotNull(gsonResult);

        System.out.println("Total deserialization time [GSON] of object " + clazz.getSimpleName() + ": " + totalTimeGson + " milliseconds, [Org.json]: " + totalTimeBacktrace + " milliseconds");

        assertTrue(totalTimeGson * MAX_INCREASE_TIME_RATIO > totalTimeBacktrace);
    }
}
