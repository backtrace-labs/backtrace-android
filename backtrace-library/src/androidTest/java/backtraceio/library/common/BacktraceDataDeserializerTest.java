package backtraceio.library.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static backtraceio.library.TestUtils.readFileAsString;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import backtraceio.library.common.serializers.BacktraceOrgJsonDeserializer;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.types.BacktraceResultStatus;

@RunWith(AndroidJUnit4.class)
public class BacktraceDataDeserializerTest {



    @Test
    public void deserializeDatabaseRecord() throws JSONException {
        // GIVEN
        String json = "{\"DataPath\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-attachment.json\",\"Id\":\"8fbde28b-aa64-4e2f-83d8-493a98446fc8\",\"RecordName\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-record.json\",\"ReportPath\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-report.json\",\"Size\":16996,\"size\":16996,\"record-path\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-record.json\",\"report-path\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-report.json\",\"diagnostic-data-path\":\"\\/data\\/user\\/0\\/backtraceio.backtraceio\\/files\\/8fbde28b-aa64-4e2f-83d8-493a98446fc8-attachment.json\"}"; // todo: incorrect json duplicated entries

        // WHEN
        BacktraceDatabaseRecord result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceDatabaseRecord.class);

        // THEN
        assertNotNull(result);
        assertEquals(16996, result.getSize());
        assertFalse(result.locked);
        assertEquals(UUID.fromString("8fbde28b-aa64-4e2f-83d8-493a98446fc8"), result.id);
        assertEquals("/data/user/0/backtraceio.backtraceio/files/8fbde28b-aa64-4e2f-83d8-493a98446fc8-attachment.json", result.getDiagnosticDataPath());
        assertEquals("/data/user/0/backtraceio.backtraceio/files/8fbde28b-aa64-4e2f-83d8-493a98446fc8-record.json", result.getRecordPath());
        assertEquals("/data/user/0/backtraceio.backtraceio/files/8fbde28b-aa64-4e2f-83d8-493a98446fc8-report.json", result.getReportPath());
    }

    @Test
    public void deserializeBacktraceApiResult() throws JSONException {
        // GIVEN

        // WHEN

        // THEN

    }

    @Test
    public void fromJson2() throws JSONException, IllegalAccessException {
        // GIVEN
        String fileName = "sample.json";
        String content = readFileAsString(this, fileName);

        // WHEN
        BacktraceData dataJson = BacktraceOrgJsonDeserializer.deserialize(content, BacktraceData.class);

        BacktraceData gsonDataJson = (new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)).create().fromJson(content, BacktraceData.class); // GSON backup

        assertEquals(dataJson.uuid, "079e41e2-bef1-4062-9e20-f2e1b3a93582");
        assertEquals(dataJson.report, null);
        assertEquals(dataJson.lang, "java");
        assertEquals(dataJson.agent, "backtrace-android");
        assertEquals(dataJson.langVersion, "0");
        assertEquals(dataJson.agentVersion, "3.7.7-8-3f67d73-org-json-serializer");
        assertEquals(dataJson.mainThread, "instr: androidx.test.runner.androidjunitrunner");
        assertEquals(dataJson.timestamp, 1694983723);
//        assertEquals(dataJson.);

//        assertEquals(dataJson.symbolication, "");
//        assertEquals(dataJson.classifiers, "");
//        assertEquals(dataJson.sourceCode, "");
//        assertEquals(dataJson.annotations, "");

//        BacktraceData backtraceData = BacktraceDataDeserializer.deserialize(context, jsonObj);

        System.out.println(dataJson.report);
        // THEN
//        assertEquals(backtraceData.report.message, );
    }
}
