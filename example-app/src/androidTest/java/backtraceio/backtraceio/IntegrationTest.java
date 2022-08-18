package backtraceio.backtraceio;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import backtraceio.library.BacktraceClient;
import backtraceio.library.models.BacktraceData;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class IntegrationTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    private BacktraceClient client;

    @Before
    public void enableMetricsAndBreadcrumbs() {
        onView(withId(R.id.enableBreadcrumbs)).perform(click());
        client = mActivityRule.getActivity().backtraceClient;
        client.attributes.put("testClass", IntegrationTest.class.getName());
    }

    @After
    public void cleanAttributes() {
        client.attributes.remove("testName");
        client.attributes.remove("testClass");
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("backtraceio.backtraceio", appContext.getPackageName());
    }

    @Test
    public void dumpWithoutCrash() {
        client.attributes.put("testName", "dumpWithoutCrash");
        final boolean[] invoked = {false};
        client.setOnBeforeSendEventListener(data -> {
            invoked[0] = true;
            assertEquals(data.attributes.get("testName"), "unhandledExceptionx");
            return data;
        });


        onView(withId(R.id.dumpWithoutCrash)).perform(click());
    }

    @Test(timeout = 2000)
    public void unhandledException() throws InterruptedException {
        client.attributes.put("testName", "unhandledException");

        final BacktraceData[] sentData = new BacktraceData[1];
        client.setOnBeforeSendEventListener(data -> {
            System.out.println("hererererere" + data);
            sentData[0] = data;
            return data;
        });

        // invoking this outside from another thread will prevent espresso from catching it
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        executor.execute(() -> onView(withId(R.id.unhandledException)).perform(click()));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // The uncaught exception is handled from a different thread, so we have to wait
        // consider using awaitilty in the future (new dependency)
        while (sentData[0] == null) {
            Thread.sleep(10);
        }
        final BacktraceData data = sentData[0];
        assertEquals("unhandledException", data.attributes.get("testName"));
    }

    @Test
    public void handledException() {
        client.attributes.put("testName", "handledException");

        final BacktraceData[] sentData = new BacktraceData[1];
        client.setOnBeforeSendEventListener(data -> {
            sentData[0] = data;
            return data;
        });

        onView(withId(R.id.handledException)).perform(click());

        final BacktraceData data = sentData[0];
        assertNotNull("onEvent wasn't called!", data);
        assertEquals("handledException", data.attributes.get("testName"));
    }

    @Test
    public void anr() {
        client.attributes.put("testName", "anr");
        onView(withId(R.id.anr)).perform(click());
    }

    // Will break build, obviously.
    //@Test
    public void nativeCrash() {
        onView(withId(R.id.nativeCrash)).perform(click());
    }

    // Will break build, obviously.
    //@Test
    public void triggerOom() throws InterruptedException {
        int iteratorValue = 20;
        for (int outerIterator = 1; outerIterator < 20; outerIterator++) {
            int loop1 = 2;
            int[] memoryFillIntVar = new int[iteratorValue];
            do {
                memoryFillIntVar[loop1] = 0;
                loop1--;
            } while (loop1 > 0);
            iteratorValue = iteratorValue * 5;
            Thread.sleep(100);
        }
    }
}
