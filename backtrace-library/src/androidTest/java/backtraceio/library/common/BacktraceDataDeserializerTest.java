package backtraceio.library.common;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String path = "src/test/resources/";
        String jsonPath =  path + "sample.json";
        String json = FileHelper.readFile(new File(jsonPath));
        // WHEN
        JSONObject jsonObj = new JSONObject(json);

        // THEN
//        BacktraceData backtraceData = BacktraceDataDeserializer.
    }
}
