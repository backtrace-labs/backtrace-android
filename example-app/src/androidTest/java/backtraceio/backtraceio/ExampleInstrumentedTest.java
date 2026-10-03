package backtraceio.backtraceio;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.SystemClock;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.coroner.response.CoronerResponse;
import backtraceio.coroner.response.CoronerResponseProcessingException;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.types.BacktraceResultStatus;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest extends InstrumentedTest {
    private final int THREAD_SLEEP_TIME_MS = 2000;
    private static final long SUBMISSION_TIMEOUT_MS = 60_000;
    private static final long INGESTION_TIMEOUT_MS = 60_000;

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void enableMetricsAndBreadcrumbs() {
        BacktraceLogger.setLevel(LogLevel.DEBUG);
        // Re-assert RESUMED: some old real devices background the activity right after launch.
        mActivityRule.getScenario().moveToState(Lifecycle.State.RESUMED);
        onView(withId(R.id.enableBreadcrumbs)).perform(click());
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
        final BacktraceResult[] result = new BacktraceResult[1];
        final Waiter waiter = new Waiter();
        mActivityRule
                .getScenario()
                .onActivity(activity -> activity.setOnServerResponseEventListener(backtraceResult -> {
                    result[0] = backtraceResult;
                    waiter.resume();
                }));

        // WHEN
        onView(withId(R.id.handledException)).perform(click()); // UI action
        // A live round trip from a Sauce device to the submission endpoint, queued behind any other
        // upload on the single sender thread, routinely exceeds a few seconds.
        waiter.await(SUBMISSION_TIMEOUT_MS, TimeUnit.MILLISECONDS, 1);
        Assert.assertNotNull("No submission result", result[0]);
        Assert.assertEquals("Submission failed: " + result[0].message, BacktraceResultStatus.Ok, result[0].status);
        String rxId = result[0].rxId;

        // THEN
        CoronerResponse response = awaitIngestedReport(rxId);

        Assert.assertEquals(1, response.getResultsNumber());

        String resultErrorMsg = response.getAttribute(0, "error.message", String.class);
        Assert.assertEquals("Invalid index of selected element!", resultErrorMsg);

        String resultClassifier = response.getAttribute(0, "classifiers", String.class);
        Assert.assertEquals("java.lang.IndexOutOfBoundsException", resultClassifier);
    }

    /**
     * Polls Coroner until the report with the given rxId is queryable. Ingestion is asynchronous on
     * the server, so a single query right after the upload can miss a report that was accepted.
     */
    private CoronerResponse awaitIngestedReport(String rxId) throws InterruptedException {
        long deadline = SystemClock.elapsedRealtime() + INGESTION_TIMEOUT_MS;
        Exception lastPollFailure = null;
        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                CoronerResponse response = this.getCoronerClient().rxIdFilter(rxId, Arrays.asList("error.message"));
                lastPollFailure = null;
                if (response != null && response.getResultsNumber() > 0) {
                    return response;
                }
            } catch (Exception ex) {
                lastPollFailure = ex;
            }
            Thread.sleep(THREAD_SLEEP_TIME_MS);
        }
        Assert.fail("Report " + rxId + " not ingested within " + INGESTION_TIMEOUT_MS + " ms"
                + (lastPollFailure == null ? "" : "; last poll failure: " + lastPollFailure.getMessage()));
        return null;
    }

    @Test
    public void dumpWithoutCrash() throws CoronerResponseProcessingException, InterruptedException {
        // GIVEN
        CoronerResponse response = null;
        long timestampStart = this.getSecondsTimestampNowGMT();

        // WHEN
        onView(withId(R.id.dumpWithoutCrash)).perform(click()); // UI action
        Thread.sleep(THREAD_SLEEP_TIME_MS * 10);

        // THEN
        try {
            response = this.getCoronerClient()
                    .errorTypeTimestampFilter(
                            "Crash",
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

    // @Test
    public void unhandledException() throws CoronerResponseProcessingException, InterruptedException {
        // GIVEN
        CoronerResponse response = null;
        long timestampStart = this.getSecondsTimestampNowGMT();

        // WHEN
        // UnhandledException crashes the app, so don't actually click the button
        // onView(withId(R.id.unhandledException)).perform(click()); // UI action

        // call BacktraceExceptionHandler directly instead
        Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), new NullPointerException());

        Thread.sleep(THREAD_SLEEP_TIME_MS);

        // THEN
        try {
            response = this.getCoronerClient()
                    .errorTypeTimestampFilter(
                            "Crash",
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
    // @Test
    public void nativeCrash() {
        onView(withId(R.id.nativeCrash)).perform(click());
    }
}
