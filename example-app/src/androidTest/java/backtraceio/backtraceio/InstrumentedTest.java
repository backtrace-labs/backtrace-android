package backtraceio.backtraceio;

import org.junit.BeforeClass;

import backtraceio.coroner.CoronerClient;

public class InstrumentedTest {
    private static CoronerClient coronerClient;

    @BeforeClass
    public static void init() {
        String url = BuildConfig.BACKTRACE_CORONER_URL;
        String coronerToken = BuildConfig.BACKTRACE_CORONER_TOKEN;
//        String url = "https://yolo.sp.backtrace.io/api/query?project=android-library";
//        String coronerToken = "4d1c52a829fa4b53c3c421d163f327e6bfaa5304e756a71249174ab2e1603f8a";
        coronerClient = new CoronerClient(url, coronerToken);
    }

    public CoronerClient getCoronerClient() {
        return coronerClient;
    }

    public long getSecondsTimestampNowGMT() {
        return System.currentTimeMillis() / 1000;
    }
}
