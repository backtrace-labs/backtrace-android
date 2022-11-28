package backtraceio.backtraceio;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.espresso.PerformException;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.jodah.concurrentunit.Waiter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import backtraceio.coroner.response.CoronerResponse;
import backtraceio.coroner.response.CoronerResponseProcessingException;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.models.BacktraceResult;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest extends InstrumentedTest {
    private final int THREAD_SLEEP_TIME_MS = 2000;

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void enableMetricsAndBreadcrumbs() {
        try {
            onView(withId(R.id.enableBreadcrumbs)).perform(click());
        }
        catch (Exception ex) {
            System.out.println("Before enableMetricsAndBreadcrumbs");
            System.out.println(ex.getMessage());
            throw ex;
        }
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("backtraceio.backtraceio", appContext.getPackageName());
    }

    @Test
    public void handledException() throws TimeoutException, CoronerResponseProcessingException, InterruptedException {
        // GIVEN
        final String[] rxId = new String[1];
        final Waiter waiter = new Waiter();
        mActivityRule.getScenario().onActivity(activity ->
                activity.setOnServerResponseEventListener(backtraceResult -> {
            rxId[0] = backtraceResult.rxId;
            waiter.resume();
        }));

        // WHEN
        onView(withId(R.id.handledException)).perform(click()); // UI action
        waiter.await(5, TimeUnit.SECONDS, 1);
        Thread.sleep(THREAD_SLEEP_TIME_MS);

        // THEN
        CoronerResponse response = null;

        try {
            response = this.getCoronerClient().rxIdFilter(rxId[0], Arrays.asList("error.message"));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getResultsNumber());

        String resultErrorMsg = response.getAttribute(0, "error.message", String.class);
        Assert.assertEquals("Invalid index of selected element!", resultErrorMsg);

        String resultClassifier = response.getAttribute(0, "classifiers", String.class);
        Assert.assertEquals("java.lang.IndexOutOfBoundsException", resultClassifier);
    }

    @Test
    public void dumpWithoutCrash() throws CoronerResponseProcessingException, InterruptedException {
        // GIVEN
        CoronerResponse response = null;
        long timestampStart = this.getSecondsTimestampNowGMT();

        // WHEN
        onView(withId(R.id.dumpWithoutCrash)).perform(click()); // UI action
        Thread.sleep(THREAD_SLEEP_TIME_MS * 3);

        // THEN
        try {
            response = this.getCoronerClient().errorTypeTimestampFilter("Crash",
                    Long.toString(timestampStart),
                    Long.toString(this.getSecondsTimestampNowGMT()),
                    Arrays.asList("error.message"));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getResultsNumber());
        String val = response.getAttribute(0, "error.message", String.class);
        Assert.assertEquals("DumpWithoutCrash", val);
    }

    //@Test
    public void unhandledException() throws CoronerResponseProcessingException, InterruptedException {
        // GIVEN
        CoronerResponse response = null;
        long timestampStart = this.getSecondsTimestampNowGMT();

        // WHEN
        // UnhandledException crashes the app, so don't actually click the button
        // onView(withId(R.id.unhandledException)).perform(click()); // UI action

        // call BacktraceExceptionHandler directly instead
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new NullPointerException());

        Thread.sleep(THREAD_SLEEP_TIME_MS);

        // THEN
        try {
            response = this.getCoronerClient().errorTypeTimestampFilter("Crash",
                    Long.toString(timestampStart),
                    Long.toString(this.getSecondsTimestampNowGMT()),
                    Arrays.asList("error.message"));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getResultsNumber());
        String val = response.getAttribute(0, "error.message", String.class);
        Assert.assertEquals("Dump without crash", val);
    }

    @Test
    public void anr() {
        onView(withId(R.id.anr)).perform(click());
    }

    // Will break build, obviously.
    //@Test
    public void nativeCrash() {
        onView(withId(R.id.nativeCrash)).perform(click());
    }
}
