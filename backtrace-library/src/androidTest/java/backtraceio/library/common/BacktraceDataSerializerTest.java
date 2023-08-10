package backtraceio.library.common;

import static org.junit.Assert.assertEquals;

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
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;

@RunWith(AndroidJUnit4.class)
public class BacktraceDataSerializerTest {

    @Test
    public void testGsonSerializer() throws JSONException, IllegalAccessException { // TODO: fix name
        // GIVEN
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        final Exception exception = new Exception("test-error");
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("string-key", "string-value");
        attributes.put("string-key2", exception);

        final List<String> attachments = new ArrayList<>();
        attachments.add("test-path");
        attachments.add("test-path2");
        final BacktraceReport report = new BacktraceReport(exception, attributes, attachments);
        final BacktraceData  data = new BacktraceData(context, report, null);

        // WHEN
        final String jsonFromGson = BacktraceSerializeHelper.toJson(data);
        final String jsonFromOrgJson = BacktraceDataSerializer.toJson(data);

        // THEN
        assertEquals(jsonFromOrgJson, jsonFromGson);
    }
}
