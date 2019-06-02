package backtraceio.library.watchdog;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.anr.BacktraceWatchdog;

@RunWith(AndroidJUnit4.class)
public class BacktraceWatchdogTest {
    private Context context;
    private BacktraceCredentials credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    private BacktraceClient backtraceClient;

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getContext();
        this.backtraceClient = new BacktraceClient(this.context, credentials);
    }

    @Test
    @UiThreadTest
    public void checkIfRegisteredThreadCorrectlyUsesWatchdog() {
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
            waiter.await(2500);
            Assert.assertFalse(watchdog.checkIsAnyThreadIsBlocked());
        } catch (Exception e) {
            waiter.fail(e);
        }
    }

    @Test
    @UiThreadTest
    public void checkIfItCorrectlyDetectsBlockedThread() {
        final BacktraceWatchdog watchdog = new BacktraceWatchdog(this.backtraceClient);
        final Waiter waiter = new Waiter();
        Thread t = new Thread() {
            public void run() {
                try {
                    while(true){

                    }
                } catch (Exception e) {
                    waiter.fail(e);
                }
            }
        };
        watchdog.registerThread(t, 200, 50);
        t.start();
        try {
            Thread.sleep(300);
            Assert.assertTrue(watchdog.checkIsAnyThreadIsBlocked());
        } catch (Exception e) {
            waiter.fail(e);
        }
    }
}