package backtraceio.library.anr;

import org.junit.Test;

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
        assertEquals(stackTraceElement.getClassName());
        assertEquals(stackTraceElement.getFileName());
        assertEquals(stackTraceElement.getLineNumber());
        assertEquals(stackTraceElement.getMethodName());
    }

    @Test
    public void parseFrameNative() {
        // GIVEN
        String frame = "native: #19 pc 00630008  /apex/com.android.art/lib/libart.so (art::InvokeMethod(art::ScopedObjectAccessAlreadyRunnable const&, _jobject*, _jobject*, _jobject*, unsigned int)+1464)";
        // WHEN
        StackTraceElement stackTraceElement = ExitInfoStackTraceParser.parseFrame(frame);
        // THEN
        assertEquals(stackTraceElement.getClassName());
        assertEquals(stackTraceElement.getFileName());
        assertEquals(stackTraceElement.getLineNumber());
        assertEquals(stackTraceElement.getMethodName());
    }

    public void parseAnrStackTrace() {
        // GIVEN
        String anrStacktraceString = TestUtils.readFileAsString(this, ANR_APPEXIT_STACKTRACE_FILE);

        // WHEN
        Map<String, Object> anrStacktrace = ExitInfoStackTraceParser.parseANRStackTrace(anrStacktraceString);

        // THEN
    }

    public void parseAnrMainThreadStackTrace() {
        // GIVEN
        String anrStacktraceString = TestUtils.readFileAsString(this, ANR_APPEXIT_STACKTRACE_FILE);
        Map<String, Object> anrStacktrace = ExitInfoStackTraceParser.parseANRStackTrace(anrStacktraceString);

        // WHEN
        StackTraceElement[] anrMainThreadStacktrace = ExitInfoStackTraceParser.parseMainThreadStackTrace(anrStacktrace);

        // THEN

    }
}
