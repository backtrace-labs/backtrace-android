package backtraceio.library.logger;

public class BacktraceMockLogger implements Logger {
    public final static int MOCK_VALUE = 123456789;
    @Override
    public int d(String tag, String message) {
        return MOCK_VALUE;
    }

    @Override
    public int w(String tag, String message) {
        return MOCK_VALUE;
    }

    @Override
    public int e(String tag, String message) {
        return MOCK_VALUE;
    }

    @Override
    public int e(String tag, String message, Throwable tr) {
        return MOCK_VALUE;
    }

    @Override
    public void setLevel(LogLevel level) { };
}
