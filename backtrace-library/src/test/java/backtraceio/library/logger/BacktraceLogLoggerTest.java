package backtraceio.library.logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class BacktraceLogLoggerTest {

    @Mock
    private Log mockLog;

    private BacktraceLogLogger logger;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // We need to mock the Log class's static methods
        mockStatic(Log.class);
    }

    @Test
    public void testDebugLog() {
        try (MockedStatic<BacktraceLogLogger> dummyStatic = Mockito.mockStatic(BacktraceLogLogger.class)) {
            dummyStatic.when(BacktraceLogLogger::d)
                    .thenReturn(1);
            // when
            System.out.println(SomePublicClass.myPublicStaticFunc());
            //then
            dummyStatic.verify(
                    SomePublicClass::myPublicStaticFunc, times(1));
        }

        logger = new BacktraceLogLogger(LogLevel.DEBUG);
        String tag = "TestTag";
        String message = "Debug message";

        when(Log.d(anyString(), anyString())).thenReturn(10);

        int result = logger.d(tag, message);

        // Verify that Log.d was called
        verifyStatic(Log.class);
        Log.d(eq("BacktraceLogger: " + tag), eq(message));

        assertEquals(10, result);
    }

    @Test
    public void testWarningLog() {
        logger = new BacktraceLogLogger(LogLevel.WARN);
        String tag = "TestTag";
        String message = "Warning message";

        when(Log.w(anyString(), anyString())).thenReturn(10);

        int result = logger.w(tag, message);

        // Verify that Log.w was called
        verifyStatic(Log.class);
        Log.w(eq("BacktraceLogger: " + tag), eq(message));

        assertEquals(10, result);
    }

    @Test
    public void testErrorLog() {
        logger = new BacktraceLogLogger(LogLevel.ERROR);
        String tag = "TestTag";
        String message = "Error message";

        when(Log.e(anyString(), anyString())).thenReturn(10);

        int result = logger.e(tag, message);

        // Verify that Log.e was called
        verifyStatic(Log.class);
        Log.e(eq("BacktraceLogger: " + tag), eq(message));

        assertEquals(10, result);
    }

    @Test
    public void testErrorLogWithThrowable() {
        logger = new BacktraceLogLogger(LogLevel.ERROR);
        String tag = "TestTag";
        String message = "Error message";
        Throwable tr = new Throwable("Test Exception");

        when(Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(10);

        int result = logger.e(tag, message, tr);

        // Verify that Log.e was called with throwable
        verifyStatic(Log.class);
        Log.e(eq("BacktraceLogger: " + tag), eq(message), eq(tr));

        assertEquals(10, result);
    }

    @Test
    public void testLogLevelFiltering() {
        logger = new BacktraceLogLogger(LogLevel.WARN);
        String tag = "TestTag";
        String message = "Debug message";

        int result = logger.d(tag, message);

        // Verify that Log.d was not called due to log level filtering
        verifyStatic(Log.class, never());
        Log.d(anyString(), anyString());

        assertEquals(0, result);
    }
}