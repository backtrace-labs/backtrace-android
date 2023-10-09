package backtraceio.library.common;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import backtraceio.library.common.serializers.SerializerHelper;
import backtraceio.library.common.serializers.naming.NamingPolicy;

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
        JSONObject result = (JSONObject) SerializerHelper.serialize(namingPolicy, list);

        // THEN
        assertEquals(result.length(), 3);
        assertEquals(result.length(), 3);

    }

    public void serializeObjectList() {

    }

    public void serializeJSONObject() {

    }

    public void testIsPrimitiveType() {

    }

    public void testDecapitalizeString () {

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

        assertEquals(result.get("message"), "test");
    }
}
