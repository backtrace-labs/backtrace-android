package backtraceio.library.deserializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import backtraceio.library.TestUtils;
import backtraceio.library.common.serializers.SerializedName;
import backtraceio.library.common.serializers.deserializers.ReflectionDeserializer;

public class ReflectionDeserializerTest {
    private class TmpObject {
        double first;

        @SerializedName("second-renamed")
        String second;
    }
    private class TestReflectionClass {
        public int a;

        public Integer A;
        public String b;

        public List<String> c;

        @SerializedName("d-renamed")
        public HashMap<String, TmpObject> d;

        public float e;

        public String[] stringArray;

        public TmpObject[] objArray;

        public List<IllegalArgumentException> exceptions;
    }

    @Test
    public void deserializeObject() throws JSONException { // TODO: fix name
        // GIVEN
        String json = TestUtils.readFileAsString(this, "testObject.json");

        assertNotNull(json);
        ReflectionDeserializer deserializer = new ReflectionDeserializer();

        // WHEN
        TestReflectionClass deserializedObject = (TestReflectionClass) deserializer.deserialize(new JSONObject(json), TestReflectionClass.class);

        // THEN
        assertNotNull(deserializedObject);
        // Assert primitive int
        assertEquals(42, deserializedObject.a);

        // Assert boxed Integer
        assertEquals(Integer.valueOf(100), deserializedObject.A);

        // Assert string
        assertEquals("Sample string", deserializedObject.b);

        // Assert list of strings
        assertNotNull(deserializedObject.c);
        assertEquals(3, deserializedObject.c.size());
        assertEquals("string1", deserializedObject.c.get(0));
        assertEquals("string2", deserializedObject.c.get(1));
        assertEquals("string3", deserializedObject.c.get(2));

        // Assert HashMap
        assertNotNull(deserializedObject.d);
        assertEquals(2, deserializedObject.d.size());

        TmpObject tmpObject1 = deserializedObject.d.get("key1");
        assertNotNull(tmpObject1);
        assertEquals(1.1, tmpObject1.first, 0.001);
        assertEquals("value1", tmpObject1.second);

        TmpObject tmpObject2 = deserializedObject.d.get("key2");
        assertNotNull(tmpObject2);
        assertEquals(2.2, tmpObject2.first, 0.001);
        assertEquals("value2", tmpObject2.second);

        // Assert primitive float
        assertEquals(3.14f, deserializedObject.e, 0.001);

        // Assert string array
        assertNotNull(deserializedObject.stringArray);
        assertEquals(2, deserializedObject.stringArray.length);
        assertEquals("arrayItem1", deserializedObject.stringArray[0]);
        assertEquals("arrayItem2", deserializedObject.stringArray[1]);

        // Assert object array
        assertNotNull(deserializedObject.objArray);
        assertEquals(2, deserializedObject.objArray.length);

        TmpObject objArrayItem1 = deserializedObject.objArray[0];
        assertNotNull(objArrayItem1);
        assertEquals(4.4, objArrayItem1.first, 0.001);
        assertEquals("arrayValue1", objArrayItem1.second);

        TmpObject objArrayItem2 = deserializedObject.objArray[1];
        assertNotNull(objArrayItem2);
        assertEquals(5.5, objArrayItem2.first, 0.001);
        assertEquals("arrayValue2", objArrayItem2.second);

        // Assert exceptions list
        assertNotNull(deserializedObject.exceptions);
        assertEquals(2, deserializedObject.exceptions.size());
        assertEquals("Exception 1", deserializedObject.exceptions.get(0).getMessage());
        assertEquals("Exception 2", deserializedObject.exceptions.get(1).getMessage());
    }
}