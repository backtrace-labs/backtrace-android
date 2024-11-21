package backtraceio.library.deserializers;

import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import backtraceio.library.common.serializers.deserializers.ReflectionDeserializer;

public class ReflectionDeserializerTest {

    private class TestReflectionClass {
        public int a;
        public String b;

        public List<String> c;

        public HashMap<String, Object> d;

        public float e;
    }

    @Test
    public void deserializeObject () throws JSONException { // TODO: fix name
        // GIVEN
        ReflectionDeserializer deserializer = new ReflectionDeserializer();

        // WHEN
        TestReflectionClass object2 = new Gson().newBuilder().create().fromJson("{'a': 1, 'b': '2', 'c': [1, 2, 3], 'd': {'1': 'test'}, 'e': 123}", TestReflectionClass.class);
//        Object object = deserializer.deserialize(new JSONObject("{'a': 1, 'b': '2', 'c': [1, 2, 3], 'd': {'1': 'test'}, 'e': 123.123}"), TestReflectionClass.class);
        Object object = deserializer.deserialize(new JSONObject("{'d': {'1': 'test'}, 'e': 123}"), TestReflectionClass.class);


        // THEN
        assertNotNull(object);
    }
}
