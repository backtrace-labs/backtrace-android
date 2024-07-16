package backtraceio.library.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.json.AnnotationException;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

public class BacktraceDataJsonTest {
    private final String JSON_FILE = "backtraceData.json";
    @Test
    public void serialize() {
        // GIVEN
//            public BacktraceData(String uuid, String symbolication, long timestamp, String langVersion,
//                String agentVersion, Map<String, String> attributes, String mainThread,
//                String[] classifiers, BacktraceReport report, Map<String, Object> annotations,
//                Map<String, SourceCode > sourceCode,
//                Map<String, ThreadInformation > threadInformationMap)
//
        // GIVEN attributes
        final BacktraceData backtraceData = createTestBacktraceDataObject();

        // WHEN
        String json = BacktraceSerializeHelper.toJson(backtraceData);

        // THEN
        String expectedJson = TestUtils.readFileAsString(this, JSON_FILE);

        assertTrue(TestUtils.compareJson(json, expectedJson));
    }

    @NonNull
    private static BacktraceData createTestBacktraceDataObject() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("application.session", "4b965773-539e-4dd3-be1b-f8ab017c2c9f");

        // GIVEN annotations
        final Map<String, Object> annotations = new HashMap<String, Object>()
        {{
         put("Environment Variables", ImmutableMap.copyOf(new HashMap<String, String>() {{
                        put("SYSTEMSERVERCLASSPATH", "/system/framework/com.android.location.provider.jar:/system/framework/services.jar:/system/framework/ethernet-service.jar:/apex/com.android.permission/javalib/service-permission.jar:/apex/com.android.wifi/javalib/service-wifi.jar:/apex/com.android.ipsec/javalib/android.net.ipsec.ike.jar");
                    }}
                    ));
            put("Exception", new AnnotationException("Example test string"));
        }};

        // GIVEN other
        final Map<String, SourceCode> sourceCode = new HashMap<>();
        sourceCode.put("8751bea6-d6f6-48f4-9f96-1355c3408a9a", new SourceCode(null, "VMStack.java"));
        sourceCode.put("27948842-7c2b-4898-a74a-ba3ca4afe814", new SourceCode(17, "InvokeMethod.java"));

        final Map<String, ThreadInformation> threadInformationMap = new HashMap<>();

        threadInformationMap.put("profile saver", new ThreadInformation("profile saver", false, new ArrayList<>()));
        threadInformationMap.put("main", new ThreadInformation("main", false, new ArrayList<BacktraceStackFrame>() {{
            add(new BacktraceStackFrame("android.os.MessageQueue.nativePollOnce", null, null, "b1a3d84a-fcf3-4d10-90d5-994f1e397607" ));
            add(new BacktraceStackFrame("android.os.MessageQueue.next",  null, 335, "868c2d50-b00a-42a5-9aa0-e82cdea07bcd"));
        }}));

        // GIVEN BacktraceData
        final BacktraceData backtraceData = new BacktraceData(
                "ecdf418b-3e22-4c7c-8011-c85dc2b4386f",
                null,
                1720419610,
                "0",
                "3.8.3",
                attributes,
                "instr: androidx.test.runner.androidjunitrunner",
                null,
                null,
                annotations,
                sourceCode,
                threadInformationMap
        );
        return backtraceData;
    }

    @Test
    public void deserialize() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, JSON_FILE);

        // WHEN
        final BacktraceData obj = BacktraceSerializeHelper.fromJson(json, BacktraceData.class);

        // THEN
        assertNotNull(obj);
        assertEquals("ecdf418b-3e22-4c7c-8011-c85dc2b4386f", obj.getUuid());
        assertEquals(null, obj.getSymbolication());
        assertEquals(1720419610, obj.getTimestamp());
        assertEquals("0", obj.getLangVersion());
        assertEquals("java", obj.getLang());
        assertEquals("3.8.3", obj.getAgentVersion());
        assertEquals("backtrace-android", obj.getAgent());
        assertEquals("instr: androidx.test.runner.androidjunitrunner", obj.getMainThread());
        assertNull(obj.classifiers);
//        assertNull(obj.ge);
        // TODO: more
    }


    @Test
    public void serializeAndDeserialize() {
        // GIVEN
        final BacktraceData backtraceData = createTestBacktraceDataObject();

        // WHEN
        String json = BacktraceSerializeHelper.toJson(backtraceData);
        final BacktraceData obj = BacktraceSerializeHelper.fromJson(json, BacktraceData.class);

        // THEN
        assertNotNull(obj);
        assertEquals("ecdf418b-3e22-4c7c-8011-c85dc2b4386f", obj.getUuid());
        assertEquals(null, obj.getSymbolication());
        assertEquals(1720419610, obj.getTimestamp());
        assertEquals("0", obj.getLangVersion());
        assertEquals("java", obj.getLang());
        assertEquals("3.8.3", obj.getAgentVersion());
        assertEquals("backtrace-android", obj.getAgent());
        assertEquals("instr: androidx.test.runner.androidjunitrunner", obj.getMainThread());
        assertNull(obj.classifiers);
//        assertNull(obj.ge);
        // TODO: more
    }
}
