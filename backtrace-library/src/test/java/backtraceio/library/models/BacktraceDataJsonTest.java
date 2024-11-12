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
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.ThreadInformation;

public class BacktraceDataJsonTest {
    private final String JSON_FILE = "backtraceData.json";

    @Test
    public void serialize() {
        // GIVEN
        final BacktraceData backtraceData = createTestBacktraceDataObject();

        // WHEN
        String json = BacktraceSerializeHelper.toJson(backtraceData);

        // THEN
        String expectedJson = TestUtils.readFileAsString(this, JSON_FILE);

        assertTrue(TestUtils.compareJson(json, expectedJson));
    }

    @NonNull
    private static BacktraceData createTestBacktraceDataObject() {
        final Map<String, String> attributes = ImmutableMap.of("application.session", "4b965773-539e-4dd3-be1b-f8ab017c2c9f");

        // GIVEN annotations
        final Map<String, Object> annotations = ImmutableMap.of(
                "Environment Variables", ImmutableMap.of("SYSTEMSERVERCLASSPATH", "/system/framework/com.android.location.provider.jar:/system/framework/services.jar:/system/framework/ethernet-service.jar:/apex/com.android.permission/javalib/service-permission.jar:/apex/com.android.wifi/javalib/service-wifi.jar:/apex/com.android.ipsec/javalib/android.net.ipsec.ike.jar"),
                "Exception", ImmutableMap.of("message", "Example test string")
        );
        // GIVEN other
        final Map<String, SourceCode> sourceCode = ImmutableMap.of(
                "8751bea6-d6f6-48f4-9f96-1355c3408a9a", new SourceCode(null, "VMStack.java"),
                "27948842-7c2b-4898-a74a-ba3ca4afe814", new SourceCode(17, "InvokeMethod.java")
        );

        final Map<String, ThreadInformation> threadInformationMap = new HashMap<>();

        threadInformationMap.put("profile saver", new ThreadInformation("profile saver", false, new ArrayList<>()));
        threadInformationMap.put("main", new ThreadInformation("main", false, new ArrayList<BacktraceStackFrame>() {{
            add(new BacktraceStackFrame("android.os.MessageQueue.nativePollOnce", null, null, "b1a3d84a-fcf3-4d10-90d5-994f1e397607"));
            add(new BacktraceStackFrame("android.os.MessageQueue.next", null, 335, "868c2d50-b00a-42a5-9aa0-e82cdea07bcd"));
        }}));

        // GIVEN BacktraceData
        return new BacktraceData(
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
        assertNull(obj.getClassifiers());
        assertEquals(2, obj.getAnnotations().size());
        assertEquals("/system/framework/com.android.location.provider.jar:/system/framework/services.jar:/system/framework/ethernet-service.jar:/apex/com.android.permission/javalib/service-permission.jar:/apex/com.android.wifi/javalib/service-wifi.jar:/apex/com.android.ipsec/javalib/android.net.ipsec.ike.jar", ((Map<String, Object>) obj.getAnnotations().get("Environment Variables")).get("SYSTEMSERVERCLASSPATH"));
        assertEquals("Example test string", ((Map<String, Object>) obj.getAnnotations().get("Exception")).get("message"));
        assertEquals(1, obj.getAttributes().size());
        assertEquals("4b965773-539e-4dd3-be1b-f8ab017c2c9f", obj.getAttributes().get("application.session"));
        assertNull(obj.getReport());

        // THEN source-code
        assertEquals(2, obj.getSourceCode().size());
        assertNull(obj.getSourceCode().get("8751bea6-d6f6-48f4-9f96-1355c3408a9a").getStartLine());
        assertEquals("VMStack.java", obj.getSourceCode().get("8751bea6-d6f6-48f4-9f96-1355c3408a9a").getSourceCodeFileName());
        assertEquals(new Integer(17), obj.getSourceCode().get("27948842-7c2b-4898-a74a-ba3ca4afe814").getStartLine());
        assertEquals("InvokeMethod.java", obj.getSourceCode().get("27948842-7c2b-4898-a74a-ba3ca4afe814").getSourceCodeFileName());
        assertEquals(2, obj.getThreadInformationMap().size());

        // THEN 'profile saver' thread
        ThreadInformation resultProfileSaverThread = obj.getThreadInformationMap().get("profile saver");
        assertEquals(false, resultProfileSaverThread.getFault());
        assertEquals("profile saver", resultProfileSaverThread.getName());
        assertEquals(0, resultProfileSaverThread.getStack().size());

        // THEN 'main' thread
        ThreadInformation resultMainThread = obj.getThreadInformationMap().get("main");
        assertEquals(false, resultMainThread.getFault());
        assertEquals("main", resultMainThread.getName());
        assertEquals(2, resultMainThread.getStack().size());
        assertEquals(null, resultMainThread.getStack().get(0).sourceCodeFileName);
        assertEquals(null, resultMainThread.getStack().get(0).line);
        assertEquals("b1a3d84a-fcf3-4d10-90d5-994f1e397607", resultMainThread.getStack().get(0).sourceCode);
        assertEquals("android.os.MessageQueue.nativePollOnce", resultMainThread.getStack().get(0).functionName);

        assertEquals(null, resultMainThread.getStack().get(1).sourceCodeFileName);
        assertEquals(new Integer(335), resultMainThread.getStack().get(1).line);
        assertEquals("868c2d50-b00a-42a5-9aa0-e82cdea07bcd", resultMainThread.getStack().get(1).sourceCode);
        assertEquals("android.os.MessageQueue.next", resultMainThread.getStack().get(1).functionName);
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
        assertNull(null);
        assertEquals(1720419610, obj.getTimestamp());
        assertEquals("0", obj.getLangVersion());
        assertEquals("java", obj.getLang());
        assertEquals("3.8.3", obj.getAgentVersion());
        assertEquals("backtrace-android", obj.getAgent());
        assertEquals("instr: androidx.test.runner.androidjunitrunner", obj.getMainThread());
        assertNull(obj.getClassifiers());
        assertEquals(2, obj.getAnnotations().size());
        assertEquals("/system/framework/com.android.location.provider.jar:/system/framework/services.jar:/system/framework/ethernet-service.jar:/apex/com.android.permission/javalib/service-permission.jar:/apex/com.android.wifi/javalib/service-wifi.jar:/apex/com.android.ipsec/javalib/android.net.ipsec.ike.jar", ((Map<String, Object>) obj.getAnnotations().get("Environment Variables")).get("SYSTEMSERVERCLASSPATH"));
        assertEquals("Example test string", ((Map<String, Object>) obj.getAnnotations().get("Exception")).get("message"));
        assertEquals(1, obj.getAttributes().size());
        assertEquals("4b965773-539e-4dd3-be1b-f8ab017c2c9f", obj.getAttributes().get("application.session"));
        assertNull(obj.getReport());

        // THEN source-code
        assertEquals(2, obj.getSourceCode().size());
        SourceCode sourceCode1 = obj.getSourceCode().get("8751bea6-d6f6-48f4-9f96-1355c3408a9a");
        assertNotNull(sourceCode1);
        assertNull(sourceCode1.getStartLine());
        assertEquals("VMStack.java", sourceCode1.getSourceCodeFileName());

        SourceCode sourceCode2 = obj.getSourceCode().get("27948842-7c2b-4898-a74a-ba3ca4afe814");
        assertNotNull(sourceCode2);
        assertEquals(Integer.valueOf(17), sourceCode2.getStartLine());
        assertEquals("InvokeMethod.java", sourceCode2.getSourceCodeFileName());
        assertEquals(2, obj.getThreadInformationMap().size());

        // THEN 'profile saver' thread
        ThreadInformation resultProfileSaverThread = obj.getThreadInformationMap().get("profile saver");
        assertEquals(false, resultProfileSaverThread.getFault());
        assertEquals("profile saver", resultProfileSaverThread.getName());
        assertEquals(0, resultProfileSaverThread.getStack().size());

        // THEN 'main' thread
        ThreadInformation resultMainThread = obj.getThreadInformationMap().get("main");
        assertEquals(false, resultMainThread.getFault());
        assertEquals("main", resultMainThread.getName());
        assertEquals(2, resultMainThread.getStack().size());
        assertNull(resultMainThread.getStack().get(0).sourceCodeFileName);
        assertNull(resultMainThread.getStack().get(0).line);
        assertEquals("b1a3d84a-fcf3-4d10-90d5-994f1e397607", resultMainThread.getStack().get(0).sourceCode);
        assertEquals("android.os.MessageQueue.nativePollOnce", resultMainThread.getStack().get(0).functionName);

        assertNull(resultMainThread.getStack().get(1).sourceCodeFileName);
        assertEquals(new Integer(335), resultMainThread.getStack().get(1).line);
        assertEquals("868c2d50-b00a-42a5-9aa0-e82cdea07bcd", resultMainThread.getStack().get(1).sourceCode);
        assertEquals("android.os.MessageQueue.next", resultMainThread.getStack().get(1).functionName);
    }
}
