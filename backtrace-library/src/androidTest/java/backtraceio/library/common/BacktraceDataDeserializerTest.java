package backtraceio.library.common;

import static org.junit.Assert.assertEquals;

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
import backtraceio.library.common.serializers.SerializerHelper;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;

@RunWith(AndroidJUnit4.class)
public class BacktraceDataDeserializerTest {

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
