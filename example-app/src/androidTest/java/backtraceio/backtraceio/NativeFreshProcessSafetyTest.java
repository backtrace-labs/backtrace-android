package backtraceio.backtraceio;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Clean-process failure safety: the safety service runs in its own process that has never
 * initialized the native backend, so — unlike in-process instrumentation tests, where another test
 * may already have initialized Crashpad — a dump call after a failed setup genuinely exercises the
 * uninitialized native guards. Requires no backend credentials; runs in every lane, including
 * secretless forks.
 */
@RunWith(AndroidJUnit4.class)
public class NativeFreshProcessSafetyTest {

    private static final long BIND_TIMEOUT_MS = 20_000;
    private static final long SAFETY_TIMEOUT_MS = 120_000;

    @Test
    public void freshProcessSurvivesFailedSetupAndDumpCalls() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (RemoteNativeServiceSession session =
                RemoteNativeServiceSession.bind(context, NativeSafetyTestService.class, BIND_TIMEOUT_MS)) {

            Bundle completed = session.request(
                    NativeTestProtocol.CMD_RUN_FRESH_PROCESS_SAFETY,
                    null,
                    NativeTestProtocol.EVENT_COMPLETED,
                    SAFETY_TIMEOUT_MS);
            assertNotNull(completed);
            assertTrue("Safety process must report a PID", session.getRemotePid() > 0);

            // A post-run PING proves the clean process survived every scenario and both dump
            // overloads; a native crash would have killed the process and failed the bind.
            Bundle pong = session.request(
                    NativeTestProtocol.CMD_PING, null, NativeTestProtocol.EVENT_COMPLETED, BIND_TIMEOUT_MS);
            assertNotNull(pong);
        }
    }
}
