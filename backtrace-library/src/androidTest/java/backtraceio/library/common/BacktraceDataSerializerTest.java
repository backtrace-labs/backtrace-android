package backtraceio.library.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static backtraceio.library.common.BacktraceSerializationUtils.jsonEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.common.serializers.BacktraceDataSerializer;
import backtraceio.library.common.serializers.SerializerHelper;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.common.serializers.naming.NamingPolicy;

@RunWith(AndroidJUnit4.class)
public class BacktraceDataSerializerTest {

    @Test
    public void testGsonSerializer() throws JSONException, IllegalAccessException { // TODO: fix name
        // GIVEN
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        final Exception exception = new Exception("test-error");
        final Map<String, Object> attributes = new HashMap<String, Object>();

        attributes.put("string-key", "string-value");
        attributes.put("string-key-exception", exception);
//        attributes.put("complex-obj-value", new BacktraceResult(null, "sample-msg", BacktraceResultStatus.Ok));
        attributes.put("complex-obj-source-cd", new StackTraceElement("sample.class", "some.method", "file",123));

        final List<String> attachments = new ArrayList<>();
        attachments.add("test-path");
        attachments.add("test-path2");
        final BacktraceReport report = new BacktraceReport(exception, attributes, attachments);
        final BacktraceData  data = new BacktraceData.Builder(context, report, null).build();

        // WHEN
        final String jsonFromGson = BacktraceSerializeHelper.toJson(data);

        final BacktraceData dataGson = BacktraceSerializeHelper.fromJson(jsonFromGson, BacktraceData.class);
        BacktraceDataSerializer serializer = new BacktraceDataSerializer(new NamingPolicy());
        final String jsonFromOrgJson = serializer.toJson(data).toString();

        // THEN
        assertTrue(jsonEquals(jsonFromOrgJson, jsonFromGson));
    }

    @Test
    public void temp() throws JSONException {
//        try {

//        } catch (Exception e) {
            try{
                BacktraceReport r = null;
                r.toString();
            }
            catch (Exception e) {
                Object result2 = SerializerHelper.serialize(null, e);
                System.out.println(result2);
            }
            Object result = SerializerHelper.serialize(null, new BacktraceReport("test"));
            System.out.println(result);
//        }
    }

    @Test
    public void testGsonPerformanceSerializer() throws JSONException, IllegalAccessException { // TODO: fix name
        // GIVEN
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        long timeGson = 0;
        long timeOrgJson = 0;
        final int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            // INIT SAMPLE
            final Exception exception = new Exception(Integer.toString(i));

            final Map<String, Object> attributes = new HashMap<String, Object>();

            attributes.put("string-key", Integer.toString(i));
            attributes.put("string-key2", exception.getMessage());

            final List<String> attachments = new ArrayList<>();
            attachments.add(Integer.toString(i));
            attachments.add(Integer.toString(i * 10));
            final BacktraceReport report = new BacktraceReport(exception, attributes, attachments);
            final BacktraceData  data = new BacktraceData.Builder(context, report, null).build();

            // GSON
            long startTime = System.currentTimeMillis();
            BacktraceSerializeHelper.toJson(data);
            long endTime = System.currentTimeMillis();
            timeGson += endTime - startTime;


            // ORG JSON
            long startTimeOrg = System.currentTimeMillis();
            BacktraceDataSerializer serializer = new BacktraceDataSerializer(new NamingPolicy());
            serializer.toJson(data);
            long endTimeOrg = System.currentTimeMillis();
            timeOrgJson += endTimeOrg - startTimeOrg;
        }

        System.out.println("[GSON] Total execution time: " + timeGson + " milliseconds");
        System.out.println("[Org.json] total execution time: " + timeOrgJson + " milliseconds");

        double averageExecutionTimeGson = (double) timeGson / iterations;
        double averageExecutionTimeOrgJson = (double) timeOrgJson / iterations;

        System.out.println("[GSON] Average execution time: " + averageExecutionTimeGson + " milliseconds");
        System.out.println("[Org.json] Average execution time: " + averageExecutionTimeOrgJson + " milliseconds");
    }
}
