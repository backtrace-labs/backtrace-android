package backtraceio.library.deserializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import backtraceio.library.TestUtils;
import backtraceio.library.common.serializers.SerializedName;
import backtraceio.library.common.serializers.deserializers.ReflectionDeserializer;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

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
    public void deserializeObject() throws JSONException {
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

    @Test
    public void deserializeBacktraceResult() throws JSONException {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceResult.json");

        assertNotNull(json);
        ReflectionDeserializer deserializer = new ReflectionDeserializer();

        // WHEN
        BacktraceResult deserializedObject = (BacktraceResult) deserializer.deserialize(new JSONObject(json), BacktraceResult.class);

        // THEN
        assertEquals(BacktraceResultStatus.Ok, deserializedObject.getStatus());
        assertNull(deserializedObject.getMessage());
        assertEquals("95000000-eb43-390b-0000-000000000000", deserializedObject.getRxId());
    }

    @Test
    public void deserializeBacktraceApiResult() throws JSONException {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceApiResult.json");

        assertNotNull(json);
        ReflectionDeserializer deserializer = new ReflectionDeserializer();

        // WHEN
        BacktraceApiResult deserializedObject = (BacktraceApiResult) deserializer.deserialize(new JSONObject(json), BacktraceApiResult.class);

        // THEN
        assertNotNull(deserializedObject);
        assertEquals("ok", deserializedObject.getResponse());
        assertEquals("95000000-eb43-390b-0000-000000000000", deserializedObject.getRxId());
    }

    @Test
    public void deserializeBacktraceReport() throws JSONException {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceReport.json");

        assertNotNull(json);
        ReflectionDeserializer deserializer = new ReflectionDeserializer();

        // WHEN
        BacktraceReport deserializedObject = (BacktraceReport) deserializer.deserialize(new JSONObject(json), BacktraceReport.class);

        // THEN
        assertNotNull(deserializedObject);
        assertEquals("java.lang.IllegalAccessException", deserializedObject.classifier);
        assertEquals(1709680251, deserializedObject.timestamp);
        assertEquals(1, deserializedObject.attachmentPaths.size());
        assertEquals("abc.txt", deserializedObject.attachmentPaths.get(0));
        assertEquals(1, deserializedObject.attributes.size());
        assertEquals("Exception", deserializedObject.attributes.get("error.type"));
        assertEquals(2, deserializedObject.diagnosticStack.size());
        assertNotNull(deserializedObject.exception);
        assertEquals(UUID.fromString("a62a533a-a7b8-415c-9a99-253c51f00827"), deserializedObject.uuid);
        assertNull(deserializedObject.message);
        assertTrue(deserializedObject.exceptionTypeReport);
    }

    @Test
    public void deserializeBacktraceData() throws JSONException {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceData.json");

        assertNotNull(json);
        ReflectionDeserializer deserializer = new ReflectionDeserializer();

        // WHEN
        BacktraceData deserializedObject = (BacktraceData) deserializer.deserialize(new JSONObject(json), BacktraceData.class);

        // THEN
        assertNotNull(deserializedObject);
        assertEquals("backtrace-android", deserializedObject.agent);
        assertEquals("3.8.3", deserializedObject.agentVersion);
        assertEquals(2, deserializedObject.annotations.size());
        assertEquals(1, deserializedObject.attributes.size());
        assertEquals("4b965773-539e-4dd3-be1b-f8ab017c2c9f", deserializedObject.attributes.get("application.session"));
        assertEquals("java", deserializedObject.lang);
        assertEquals("0", deserializedObject.langVersion);
        assertEquals("instr: androidx.test.runner.androidjunitrunner", deserializedObject.mainThread);
        assertEquals(2, deserializedObject.sourceCode.size());
        assertEquals(2, deserializedObject.threadInformationMap.size());
        assertEquals(1720419610, deserializedObject.timestamp);
        assertEquals("ecdf418b-3e22-4c7c-8011-c85dc2b4386f", deserializedObject.uuid);
    }
}
