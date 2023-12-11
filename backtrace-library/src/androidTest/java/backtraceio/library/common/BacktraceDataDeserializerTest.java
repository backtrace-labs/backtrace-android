package backtraceio.library.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static backtraceio.library.TestUtils.readFileAsString;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.R;
import backtraceio.library.common.serializers.BacktraceDataDeserializer;
import backtraceio.library.common.serializers.BacktraceDataSerializer;
import backtraceio.library.common.serializers.BacktraceOrgJsonDeserializer;
import backtraceio.library.common.serializers.SerializerHelper;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceDataDeserializerTest {

    @Test
    public void deserializeCoronerJsonResponse() throws JSONException {
        // GIVEN
        String json = "{\"response\":\"ok\",\"_rxid\":\"01000000-5360-240b-0000-000000000000\"}";

        // WHEN
        BacktraceResult result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceResult.class);

        // THEN
        assertNotNull(result);
        assertNull(result.getBacktraceReport());
        assertNull(result.message);
        assertEquals(BacktraceResultStatus.Ok, result.status);
        assertEquals("01000000-5360-240b-0000-000000000000", result.rxId);
    }

    @Test
    public void deserializeDatabaseRecord() throws JSONException {
        // GIVEN
        String json = "{\"DataPath\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-attachment.json\",\"Id\":\"8fbde28b-aa64-4e2f-83d8-493a98446fc8\",\"RecordName\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-record.json\",\"ReportPath\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-report.json\",\"Size\":16996,\"size\":16996,\"record-path\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-record.json\",\"report-path\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-report.json\",\"diagnostic-data-path\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-attachment.json\"}";

        // WHEN
        BacktraceDatabaseRecord result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceDatabaseRecord.class);

        // THEN
        assertNotNull(result);
        assertEquals(16996, result.getSize());
        assertFalse(result.locked);
        assertEquals("/data/user/0/backtraceio.backtraceio/files/8fbde28b-aa64-4e2f-83d8-493a98446fc8-attachment.json", result.getDiagnosticDataPath());
        assertEquals("/data/user/0/backtraceio.backtraceio/files/8fbde28b-aa64-4e2f-83d8-493a98446fc8-record.json", result.getRecordPath());
        assertEquals("/data/user/0/backtraceio.backtraceio/files/8fbde28b-aa64-4e2f-83d8-493a98446fc8-report.json", result.getReportPath());
    }
    @Test
    public void fromJson2() throws JSONException, IllegalAccessException {
        // GIVEN
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();

        String fileName = "sample.json";

        String path = "resources/";
        String jsonPath =  path + "sample.json";

        // WHEN

        String content = readFileAsString(this, fileName);
//        File x = getFileFromPath(this, "resources/sample.json");
        JSONObject jsonObj = new JSONObject(content);

        BacktraceData dataJson = BacktraceOrgJsonDeserializer.deserialize(content, BacktraceData.class);

        BacktraceData backtraceData = BacktraceDataDeserializer.deserialize(context, jsonObj);

        System.out.println(backtraceData.report);
        // THEN
//        assertEquals(backtraceData.report.message, );
    }

    @Test
    public void fromJson() throws JSONException, IllegalAccessException {
        // GIVEN
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();

        String fileName = "sample.json";

        String path = "resources/";
        String jsonPath =  path + "sample.json";

        // WHEN

        String content = readFileAsString(this, fileName);
//        File x = getFileFromPath(this, "resources/sample.json");
        JSONObject jsonObj = new JSONObject(content);

        BacktraceData backtraceData = BacktraceDataDeserializer.deserialize(context, jsonObj);

        System.out.println(backtraceData.report);
        // THEN
//        assertEquals(backtraceData.report.message, );
    }
}
