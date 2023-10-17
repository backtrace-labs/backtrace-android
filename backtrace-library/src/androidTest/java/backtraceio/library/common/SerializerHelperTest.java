package backtraceio.library.common;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import backtraceio.library.common.serializers.SerializerHelper;
import backtraceio.library.common.serializers.naming.NamingPolicy;
import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.json.SourceCode;

@RunWith(AndroidJUnit4.class)
public class SerializerHelperTest {
    public final static NamingPolicy namingPolicy = new NamingPolicy();



    @Test
    public void serializeList() throws JSONException {
        // GIVEN
        final List<Integer> list = new ArrayList<Integer>() {{
            add(1);
            add(2);
            add(3);
        }};
        // WHEN
        JSONArray result = (JSONArray) SerializerHelper.serialize(namingPolicy, list);

        // THEN
        assertEquals("[1,2,3]", result.toString());
        assertEquals(result.length(), 3);
    }

    @Test
    public void serializeObjectList() throws JSONException {
        // GIVEN
        final SourceCode sourceCode = new SourceCode(new BacktraceStackFrame(new StackTraceElement("sample-class", "sample-method", "sample-file", 123)));

        // WHEN
        JSONObject jsonObject = (JSONObject) SerializerHelper.serialize(namingPolicy, sourceCode);

        // THEN
        assertEquals("{\"startLine\": 123, \"path\": \"sample-file\" }", jsonObject.toString());
    }

    @Test
    public void serializeJSONObject() throws JSONException {
        // GIVEN
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("sample-int", 1);
        jsonObject.put("sample-boolean", true);
        jsonObject.put("sample-string", "example");
        jsonObject.put("sample-array", new ArrayList<String>(){{
            add("1");
            add("2");
            add("3");
        }});
        jsonObject.put("sample-exception", new Exception("msg"));

        // WHEN
        final JSONObject result = (JSONObject) SerializerHelper.serialize(namingPolicy, jsonObject);

        // THEN
        assertEquals("", result.toString()); // TODO:
    }



    public void serializeMap() {

    }

    public void serializeObject() {

    }

    public void serializeException() throws JSONException {
        // GIVEN
        final ArrayIndexOutOfBoundsException exception = new ArrayIndexOutOfBoundsException("test");

        // WHEN
        JSONObject result = (JSONObject) SerializerHelper.serialize(namingPolicy, exception);

        // THEN
        assertNotNull(result);

        assertEquals("test", result.get("message"));
        assertEquals("\"message\":\"test\"", result.toString());
    }
}
