package backtraceio.library.anr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import backtraceio.library.TestUtils;

public class ExitInfoStackTraceParserTest {
    private final String ANR_APPEXIT_STACKTRACE_FILE = "anrAppExitInfoStacktrace.txt";

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

        assertEquals(9207, anrStacktrace.get("pid"));
        assertNull(anrStacktrace.get("timestamp")); // TODO?

        List<Map<String, Object>> threads = (List<Map<String, Object>>) anrStacktrace.get("threads");
        assertEquals(20, threads.size());

        Map<String, Object> customThread4 = threads.get(18);
        List<String> thread4StackTrace = (List<String>) customThread4.get("stack_trace");

        assertEquals("Thread-4", threads.get(18).get("name"));
        assertEquals("at java.lang.Thread.sleep(Native method)", thread4StackTrace.get(0));
        assertEquals("at java.lang.Thread.sleep(Thread.java:442)", thread4StackTrace.get(1));
        assertEquals("at java.lang.Thread.sleep(Thread.java:358)", thread4StackTrace.get(2));
        assertEquals("at backtraceio.library.watchdog.BacktraceANRHandlerWatchdog.run(BacktraceANRHandlerWatchdog.java:118)", thread4StackTrace.get(3));
    }
    @Test
    public void parseAnrMainThreadStackTrace() {
        // GIVEN
        String anrStacktraceString = TestUtils.readFileAsString(this, ANR_APPEXIT_STACKTRACE_FILE);
        Map<String, Object> anrStacktrace = ExitInfoStackTraceParser.parseANRStackTrace(anrStacktraceString);

        // WHEN
        StackTraceElement[] anrMainThreadStacktrace = ExitInfoStackTraceParser.parseMainThreadStackTrace(anrStacktrace);

        // THEN

    }
}
