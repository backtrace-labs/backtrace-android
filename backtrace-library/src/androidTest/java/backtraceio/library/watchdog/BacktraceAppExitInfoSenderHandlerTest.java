package backtraceio.library.watchdog;

import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.anr.BacktraceAppExitInfoSenderHandler;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.logger.BacktraceInternalLogger;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;

@RunWith(AndroidJUnit4.class)
public class BacktraceAppExitInfoSenderHandlerTest {
    private Context context;
    private final BacktraceCredentials credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    private BacktraceClient backtraceClient;

    @Before
    public void setUp() {
        BacktraceLogger.setLogger(new BacktraceInternalLogger(LogLevel.DEBUG));
        this.context = InstrumentationRegistry.getInstrumentation().getContext();
        this.backtraceClient = new BacktraceClient(this.context, credentials);
    }

    @Test
    public void checkIfANRIsSentFromAppExitInfo() {
        // GIVEN
        final Waiter waiter = new Waiter();
        backtraceClient.setOnRequestHandler(new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                waiter.resume();
                return new BacktraceResult(new BacktraceApiResult("_", "ok"));
            }
        });
        BacktraceAppExitInfoSenderHandler handler = new BacktraceAppExitInfoSenderHandler(this.backtraceClient, context);
        // WHEN

        // THEN
        try {
            waiter.await(1000, TimeUnit.SECONDS); // Check if anr is detected and event was emitted
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
