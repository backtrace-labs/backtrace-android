package backtraceio.backtraceio;

import backtraceio.coroner.CoronerClient;
import org.junit.BeforeClass;

public class InstrumentedTest {
    private static CoronerClient coronerClient;

    @BeforeClass
    public static void init() {
        String url = BuildConfig.BACKTRACE_CORONER_URL;
        String coronerToken = BuildConfig.BACKTRACE_CORONER_TOKEN;
        coronerClient = new CoronerClient(url, coronerToken);
    }

    public CoronerClient getCoronerClient() {
        return coronerClient;
    }

    public long getSecondsTimestampNowGMT() {
        return System.currentTimeMillis() / 1000;
    }
}
