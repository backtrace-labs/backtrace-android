package backtraceio.library.watchdog;

import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.logger.BacktraceInternalLogger;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;

@RunWith(AndroidJUnit4.class)
public class BacktraceAnrTest {
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
    @UiThreadTest
    public void checkIfANRIsDetectedCorrectly() {
        // GIVEN
        final Waiter waiter = new Waiter();
        BacktraceANRWatchdog watchdog = new BacktraceANRWatchdog(this.backtraceClient, 500);
        watchdog.setOnApplicationNotRespondingEvent(new OnApplicationNotRespondingEvent() {
            @Override
            public void onEvent(BacktraceWatchdogTimeoutException exception) {
                waiter.resume();
            }
        });

        // WHEN
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // THEN
        try {
            waiter.await(5, TimeUnit.SECONDS); // Check if anr is detected and event was emitted
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    @UiThreadTest
    public void checkIfANRIsDetectedCorrectlyWithBacktraceClient() {
        // GIVEN
        final Waiter waiter = new Waiter();
        this.backtraceClient.enableAnr(500, new OnApplicationNotRespondingEvent() {
            @Override
            public void onEvent(BacktraceWatchdogTimeoutException exception) {
                waiter.resume();
            }
        });

        // WHEN
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // THEN
        try {
            waiter.await(5, TimeUnit.SECONDS); // Check if anr is detected and event was emitted
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    @UiThreadTest
    public void checkIfANRIsNotDetected() {
        // GIVEN
        final int numberOfIterations = 5;
        final Waiter waiter = new Waiter();
        BacktraceANRWatchdog watchdog = new BacktraceANRWatchdog(this.backtraceClient, 5000);
        watchdog.setOnApplicationNotRespondingEvent(new OnApplicationNotRespondingEvent() {
            @Override
            public void onEvent(BacktraceWatchdogTimeoutException exception) {
                waiter.fail();
            }
        });

        // WHEN
        try {
            for (int i = 0; i < numberOfIterations; i++) {
                Thread.sleep(800);
                waiter.resume();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // THEN
        try {
            waiter.await(5, TimeUnit.SECONDS, numberOfIterations);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    @UiThreadTest
    public void checkIsDisableWorks() {
        // GIVEN
        final Waiter waiter = new Waiter();

        backtraceClient.enableAnr(1000, new OnApplicationNotRespondingEvent() {
            @Override
            public void onEvent(BacktraceWatchdogTimeoutException exception) {
                waiter.fail();
            }
        });

        // WHEN
        backtraceClient.disableAnr();

        // THEN
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}