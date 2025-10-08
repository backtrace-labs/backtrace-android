package backtraceio.library.watchdog;

import android.content.Context;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;

@RunWith(AndroidJUnit4.class)
public class BacktraceWatchdogTest {
    private Context context;
    private final BacktraceCredentials credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    private BacktraceClient backtraceClient;

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getContext();
        this.backtraceClient = new BacktraceClient(this.context, credentials);
    }

    @Test
    @UiThreadTest
    public void checkIfRegisteredThreadCorrectlyUsesWatchdog() {
        // GIVEN
        final BacktraceWatchdog watchdog = new BacktraceWatchdog(this.backtraceClient);
        final Waiter waiter = new Waiter();
        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000); // Long operation
                    watchdog.tick(Thread.currentThread());
                    waiter.resume();
                } catch (Exception e) {
                    waiter.fail(e);
                }
            }
        };
        watchdog.registerThread(t, 2000, 500);
        t.start();
        try {
            // WHEN
            waiter.await(2500);
            boolean result = watchdog.checkIsAnyThreadIsBlocked();

            // THEN
            Assert.assertFalse(result);
        } catch (Exception e) {
            waiter.fail(e);
        }
    }

    @Test
    @UiThreadTest
    public void checkIfItCorrectlyDetectsBlockedThread() {
        // GIVEN
        final BacktraceWatchdog watchdog = new BacktraceWatchdog(this.backtraceClient);
        final Waiter waiter = new Waiter();
        Thread t = new Thread() {
            public void run() {
                while (true) {
                }
            }
        };
        watchdog.registerThread(t, 200, 50);
        t.start();
        try {
            // WHEN
            Thread.sleep(300);
            boolean result = watchdog.checkIsAnyThreadIsBlocked();

            //THEN
            Assert.assertTrue(result);
        } catch (Exception e) {
            waiter.fail(e);
        }
    }

    @Test
    @UiThreadTest
    public void checkIfUnregisterThreadWorksCorrectly() {
        // GIVEN
        final BacktraceWatchdog watchdog = new BacktraceWatchdog(this.backtraceClient);
        final Waiter waiter = new Waiter();
        Thread t = new Thread() {
            public void run() {
                while (true) {
                }
            }
        };
        watchdog.registerThread(t, 200, 50);
        t.start();
        try {
            // WHEN
            Thread.sleep(3000);
            watchdog.unRegisterThread(t);
            boolean result = watchdog.checkIsAnyThreadIsBlocked();

            // THEN
            Assert.assertFalse(result);
        } catch (Exception e) {
            waiter.fail(e);
        }
    }

    @Test
    @UiThreadTest
    public void checkIfDeactivateThreadWatcherWorksCorrectly() {
        // GIVEN
        final BacktraceWatchdog watchdog = new BacktraceWatchdog(this.backtraceClient);
        final Waiter waiter = new Waiter();
        Thread t = new Thread() {
            public void run() {
                while (true) {
                }
            }
        };
        t.start();
        watchdog.registerThread(t, 200, 50);

        try {
            // WHEN
            Thread.sleep(500);
            watchdog.deactivateWatcher(t);
            boolean result = watchdog.checkIsAnyThreadIsBlocked();

            // THEN
            Assert.assertFalse(result);
        } catch (Exception e) {
            waiter.fail(e);
        }
    }

    @Test
    @UiThreadTest
    public void checkIfActivateThreadWatcherWorksCorrectly() {
        // GIVEN
        final Waiter waiter = new Waiter();
        final BacktraceWatchdog watchdog = new BacktraceWatchdog(this.backtraceClient);
        Thread t = new Thread() {
            public void run() {
                while (true) {
                }
            }
        };
        t.start();
        watchdog.registerThread(t, 200, 50);

        try {
            // WHEN
            Thread.sleep(500);
            watchdog.deactivateWatcher(t);

            // THEN
            boolean result = watchdog.checkIsAnyThreadIsBlocked();
            Assert.assertFalse(result);

            // WHEN
            Thread.sleep(500);
            watchdog.activateWatcher(t);
            result = watchdog.checkIsAnyThreadIsBlocked();

            // THEN
            Assert.assertTrue(result);
        } catch (Exception e) {
            waiter.fail(e);
        }
    }

    @Test
    @UiThreadTest
    public void checkIsCustomEventWorksCorrectly() {
        // GIVEN
        final Waiter waiter = new Waiter();
        final BacktraceWatchdog watchdog = new BacktraceWatchdog(this.backtraceClient);
        watchdog.setOnApplicationNotRespondingEvent(new OnApplicationNotRespondingEvent() {
            @Override
            public void onEvent(BacktraceWatchdogTimeoutException exception) {
                waiter.resume();
            }
        });
        Thread t = new Thread() {
            public void run() {
                while (true) {
                }
            }
        };
        t.start();
        watchdog.registerThread(t, 200, 50);

        try {
            // WHEN
            Thread.sleep(500);
            boolean result = watchdog.checkIsAnyThreadIsBlocked();

            // THEN
            Assert.assertTrue(result);
            waiter.await(500);
        } catch (Exception e) {
            waiter.fail(e);
        }
    }
}
