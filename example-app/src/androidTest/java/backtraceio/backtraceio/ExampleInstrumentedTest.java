package backtraceio.backtraceio;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.PerformException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Before
    public void enableMetricsAndBreadcrumbs() {
        onView(withId(R.id.enableBreadcrumbs)).perform(click());
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("backtraceio.backtraceio", appContext.getPackageName());
    }

    @Test
    public void dumpWithoutCrash() {
        onView(withId(R.id.dumpWithoutCrash)).perform(click());
    }

    @Test(expected = PerformException.class)
    public void unhandledException() {
        onView(withId(R.id.unhandledException)).perform(click());
    }

    @Test
    public void handledException() {
        onView(withId(R.id.handledException)).perform(click());
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
