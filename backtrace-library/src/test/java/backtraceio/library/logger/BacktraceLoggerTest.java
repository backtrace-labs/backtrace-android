package backtraceio.library.logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BacktraceLoggerTest {
    @Before
    public void setUp() {
        BacktraceLogger.setLogger(new BacktraceMockLogger());
    }

    @Test
    public void testCustomLogger() {
        // GIVEN
        final String TEST_MSG = "TEST-MSG";
        final String TEST_TAG = "TEST-TAG";

        // WHEN
        final int debugResult = BacktraceLogger.d(TEST_TAG, TEST_MSG);
        final int warnResult = BacktraceLogger.w(TEST_TAG, TEST_MSG);
        final int errorResult = BacktraceLogger.e(TEST_TAG, TEST_MSG);
        final int errorResult2 = BacktraceLogger.e(TEST_TAG, TEST_MSG, new Exception());

        // THEN
        Assert.assertEquals(BacktraceMockLogger.MOCK_VALUE, debugResult);
        Assert.assertEquals(BacktraceMockLogger.MOCK_VALUE, warnResult);
        Assert.assertEquals(BacktraceMockLogger.MOCK_VALUE, errorResult);
        Assert.assertEquals(BacktraceMockLogger.MOCK_VALUE, errorResult2);
    }
}
