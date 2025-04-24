package backtraceio.library.anr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import backtraceio.library.TestUtils;

public class ExitInfoStackTraceParserTest {
    private final String ANR_APPEXIT_STACKTRACE_FILE = "anrAppExitInfoStacktrace2.txt";

    @Test
    public void parseFrameJava() {
        // GIVEN
        String frame = "at backtraceio.backtraceio.MainActivity.handledException(MainActivity.java:157)";
        // WHEN
        StackTraceElement stackTraceElement = ExitInfoStackTraceParser.parseFrame(frame);
        // THEN
        assertEquals("backtraceio.backtraceio.MainActivity", stackTraceElement.getClassName());
        assertEquals("MainActivity.java", stackTraceElement.getFileName());
        assertEquals(157, stackTraceElement.getLineNumber());
        assertEquals("handledException", stackTraceElement.getMethodName());
    }

    @Test
    public void parseFrameNative() {
        // GIVEN
        String frame = "native: #19 pc 00630008  /apex/com.android.art/lib/libart.so (art::InvokeMethod(art::ScopedObjectAccessAlreadyRunnable const&, _jobject*, _jobject*, _jobject*, unsigned int)+1464)";
        // WHEN
        StackTraceElement stackTraceElement = ExitInfoStackTraceParser.parseFrame(frame);
        // THEN
        assertEquals("/apex/com.android.art/lib/libart.so", stackTraceElement.getClassName());
        assertEquals("address: 00630008", stackTraceElement.getFileName());
        assertEquals(0, stackTraceElement.getLineNumber());
        assertEquals("(art::InvokeMethod(art::ScopedObjectAccessAlreadyRunnable const&, _jobject*, _jobject*, _jobject*, unsigned int)+1464)", stackTraceElement.getMethodName());
    }

    @Test
    public void parseAnrStackTrace() {
        // GIVEN
        String anrStacktraceString = TestUtils.readFileAsString(this, ANR_APPEXIT_STACKTRACE_FILE);

        // WHEN
        Map<String, Object> anrStacktrace = ExitInfoStackTraceParser.parseANRStackTrace(anrStacktraceString);

        // THEN
        assertNotNull(anrStacktrace);
        assertNotNull(anrStacktrace.get("main_thread"));
        assertEquals("x86", anrStacktrace.get("abi"));
        assertEquals("74% free, 6892KB/25MB; 138095 objects", anrStacktrace.get("heap_info"));
        assertEquals("google/sdk_gphone_x86/generic_x86_arm:11/RSR1.201013.001/6903271:user/release-keys", anrStacktrace.get("build_fingerprint"));
        assertEquals("optimized", anrStacktrace.get("build_type"));
        assertEquals("backtraceio.backtraceio", anrStacktrace.get("command_line"));
        assertEquals("2025-03-27 21:02:38", anrStacktrace.get("timestamp"));
        assertEquals(9207, anrStacktrace.get("pid"));

        // THEN THREADS
        List<Map<String, Object>> threads = (List<Map<String, Object>>) anrStacktrace.get("threads");
        assertEquals(20, threads.size());

        Map<String, Object> customThread4 = threads.get(18);
        List<String> thread4StackTrace = (List<String>) customThread4.get("stack_trace");

        assertEquals("Thread-4", threads.get(18).get("name"));
        assertEquals("at java.lang.Thread.sleep(Native method)", thread4StackTrace.get(0));
        assertEquals("at java.lang.Thread.sleep(Thread.java:442)", thread4StackTrace.get(1));
        assertEquals("at java.lang.Thread.sleep(Thread.java:358)", thread4StackTrace.get(2));
        assertEquals("at backtraceio.library.watchdog.BacktraceANRHandlerWatchdog.run(BacktraceANRHandlerWatchdog.java:118)", thread4StackTrace.get(3));

        // THEN MAIN THREAD
        Map<String, Object> mainThread = (Map<String, Object>) anrStacktrace.get("main_thread");
        assertEquals(1, mainThread.get("flags"));
        assertEquals("0xf583f478", mainThread.get("handle"));
        assertEquals(5, mainThread.get("priority"));
        assertEquals("top-app", mainThread.get("cgrp"));
        assertEquals(1, mainThread.get("tid"));
        assertEquals(-10, mainThread.get("nice"));
        assertEquals(null, mainThread.get("daemon"));
        assertEquals(0, mainThread.get("dsCount"));
        assertEquals("0/0", mainThread.get("sched"));
        assertEquals("0x72287300", mainThread.get("obj"));
        assertEquals("main", mainThread.get("name"));
        assertEquals(1, mainThread.get("sCount"));
        assertEquals("0xe7380e10", mainThread.get("self"));
        assertEquals("S", mainThread.get("state"));

        ArrayList<?> stackTrace = (ArrayList<?>) mainThread.get("stack_trace");
        assertEquals(36, stackTrace.size());

        assertEquals(9207, mainThread.get("sysTid"));
        assertEquals("main", mainThread.get("group"));

        assertEquals("native: #20 pc 005886a0  /apex/com.android.art/lib/libart.so (art::Method_invoke(_JNIEnv*, _jobject*, _jobject*, _jobjectArray*)+80)", stackTrace.get(20));
        assertEquals("at androidx.appcompat.app.AppCompatViewInflater$DeclaredOnClickListener.onClick(AppCompatViewInflater.java:468)", stackTrace.get(24));
    }
    @Test
    public void parseAnrMainThreadStackTrace() {
        // GIVEN
        String anrStacktraceString = TestUtils.readFileAsString(this, ANR_APPEXIT_STACKTRACE_FILE);
        Map<String, Object> anrStacktrace = ExitInfoStackTraceParser.parseANRStackTrace(anrStacktraceString);

        // WHEN
        StackTraceElement[] anrMainThreadStacktrace = ExitInfoStackTraceParser.parseMainThreadStackTrace(anrStacktrace);

        // THEN
        assertEquals(33, anrMainThreadStacktrace.length);

        assertEquals("(__kernel_vsyscall+7)", anrMainThreadStacktrace[0].getMethodName());
        assertEquals(0, anrMainThreadStacktrace[0].getLineNumber());
        assertEquals("address: 00000b97", anrMainThreadStacktrace[0].getFileName());
        assertEquals("[vdso]", anrMainThreadStacktrace[0].getClassName());

        assertEquals("(art::interpreter::EnterInterpreterFromEntryPoint(art::Thread*, art::CodeItemDataAccessor const&, art::ShadowFrame*)+176)", anrMainThreadStacktrace[14].getMethodName());
        assertEquals(0, anrMainThreadStacktrace[14].getLineNumber());
        assertEquals("address: 00379b00", anrMainThreadStacktrace[14].getFileName());
        assertEquals("/apex/com.android.art/lib/libart.so", anrMainThreadStacktrace[14].getClassName());

        assertEquals("onClick", anrMainThreadStacktrace[22].getMethodName());
        assertEquals(468, anrMainThreadStacktrace[22].getLineNumber());
        assertEquals("AppCompatViewInflater.java", anrMainThreadStacktrace[22].getFileName());
        assertEquals("androidx.appcompat.app.AppCompatViewInflater$DeclaredOnClickListener", anrMainThreadStacktrace[22].getClassName());

        assertEquals("main", anrMainThreadStacktrace[32].getMethodName());
        assertEquals(947, anrMainThreadStacktrace[32].getLineNumber());
        assertEquals("ZygoteInit.java", anrMainThreadStacktrace[32].getFileName());
        assertEquals("com.android.internal.os.ZygoteInit", anrMainThreadStacktrace[32].getClassName());
    }
}
