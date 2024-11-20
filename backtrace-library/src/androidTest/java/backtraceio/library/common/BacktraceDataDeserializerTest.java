package backtraceio.library.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import backtraceio.library.models.database.BacktraceDatabaseRecord;

@RunWith(AndroidJUnit4.class)
public class BacktraceDataDeserializerTest {



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

        assertEquals(dataJson.getUuid(), "079e41e2-bef1-4062-9e20-f2e1b3a93582");
        assertEquals(dataJson.getReport(), null);
        assertEquals(dataJson.getLang(), "java");
        assertEquals(dataJson.getAgent(), "backtrace-android");
        assertEquals(dataJson.getLangVersion(), "0");
        assertEquals(dataJson.getAgentVersion(), "3.7.7-8-3f67d73-org-json-serializer");
        assertEquals(dataJson.getMainThread(), "instr: androidx.test.runner.androidjunitrunner");
        assertEquals(dataJson.getTimestamp(), 1694983723);
//        assertEquals(dataJson.);

//        assertEquals(dataJson.symbolication, "");
//        assertEquals(dataJson.classifiers, "");
//        assertEquals(dataJson.sourceCode, "");
//        assertEquals(dataJson.annotations, "");

//        BacktraceData backtraceData = BacktraceDataDeserializer.deserialize(context, jsonObj);

        System.out.println(dataJson.getReport());
        // THEN
//        assertEquals(backtraceData.report.message, );
    }
}
