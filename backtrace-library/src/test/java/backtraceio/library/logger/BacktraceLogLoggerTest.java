package backtraceio.library.logger;

import static org.mockito.Mockito.mockStatic;

import android.util.Log;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class BacktraceLogLoggerTest {
    private final int LOG_ENABLED = 12345;
    private final int LOG_DISABLED = 0;

    private final String TEST_TAG = "TEST-TAG";
    private final String TEST_MSG = "TEST-MSG";

    public class LogMockAnswer implements Answer<Integer> {
        @Override
        public Integer answer(InvocationOnMock invocation) { return LOG_ENABLED; }
    }

    @Mock
    private static MockedStatic<Log> mockLog;
    private BacktraceLogLogger logger;

    @BeforeClass
    public static void init() {
        mockLog = mockStatic(Log.class);
    }

    @AfterClass
    public static void close() {
        mockLog.close();
    }

    @Before
    public void setUp() {
        logger = new BacktraceLogLogger();
        mockLogMethods();
    }

    private void mockLogMethods() {
        try {
            mockLog.when(() -> Log.d(Mockito.any(), Mockito.any())).thenAnswer(
                    new LogMockAnswer()
            );
            mockLog.when(() -> Log.w(Mockito.any(), Mockito.anyString())).thenAnswer(
                    new LogMockAnswer()
            );
            mockLog.when(() -> Log.e(Mockito.any(), Mockito.any())).thenAnswer(
                    new LogMockAnswer()
            );
            mockLog.when(() -> Log.e(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(
                    new LogMockAnswer()
            );
        }
        catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testDebugLog() {
        // GIVEN
        logger.setLevel(LogLevel.DEBUG);

        // WHEN
        final int debugResult = logger.d(TEST_TAG, TEST_MSG);
        final int warnResult = logger.w(TEST_TAG, TEST_MSG);
        final int errorResult = logger.e(TEST_TAG, TEST_MSG);
        final int errorResult2 = logger.e(TEST_TAG, TEST_MSG, new Exception());

        // THEN
        Assert.assertEquals(LOG_ENABLED, debugResult);
        Assert.assertEquals(LOG_ENABLED, warnResult);
        Assert.assertEquals(LOG_ENABLED, errorResult);
        Assert.assertEquals(LOG_ENABLED, errorResult2);
    }

    @Test
    public void testWarnLog() {
        // GIVEN
        logger.setLevel(LogLevel.WARN);

        // WHEN
        final int debugResult = logger.d(TEST_TAG, TEST_MSG);
        final int warnResult = logger.w(TEST_TAG, TEST_MSG);
        final int errorResult = logger.e(TEST_TAG, TEST_MSG);
        final int errorResult2 = logger.e(TEST_TAG, TEST_MSG, new Exception());

        // THEN
        Assert.assertEquals(LOG_DISABLED, debugResult);
        Assert.assertEquals(LOG_ENABLED, warnResult);
        Assert.assertEquals(LOG_ENABLED, errorResult);
        Assert.assertEquals(LOG_ENABLED, errorResult2);
    }

    @Test
    public void testErrorLog() {
        // GIVEN
        logger.setLevel(LogLevel.ERROR);

        // WHEN
        final int debugResult = logger.d(TEST_TAG, TEST_MSG);
        final int warnResult = logger.w(TEST_TAG, TEST_MSG);
        final int errorResult = logger.e(TEST_TAG, TEST_MSG);
        final int errorResult2 = logger.e(TEST_TAG, TEST_MSG, new Exception());

        // THEN
        Assert.assertEquals(LOG_DISABLED, debugResult);
        Assert.assertEquals(LOG_DISABLED, warnResult);
        Assert.assertEquals(LOG_ENABLED, errorResult);
        Assert.assertEquals(LOG_ENABLED, errorResult2);
    }

    @Test
    public void testLogOff() {
        // GIVEN
        logger.setLevel(LogLevel.OFF);

        // WHEN
        final int debugResult = logger.d(TEST_TAG, TEST_MSG);
        final int warnResult = logger.w(TEST_TAG, TEST_MSG);
        final int errorResult = logger.e(TEST_TAG, TEST_MSG);
        final int errorResult2 = logger.e(TEST_TAG, TEST_MSG, new Exception());

        // THEN
        Assert.assertEquals(LOG_DISABLED, debugResult);
        Assert.assertEquals(LOG_DISABLED, warnResult);
        Assert.assertEquals(LOG_DISABLED, errorResult);
        Assert.assertEquals(LOG_DISABLED, errorResult2);
    }
}