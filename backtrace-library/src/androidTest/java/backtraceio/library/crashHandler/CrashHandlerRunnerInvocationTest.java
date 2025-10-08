package backtraceio.library.crashHandler;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import backtraceio.library.nativeCalls.BacktraceCrashHandlerWrapper;
import backtraceio.library.nativeCalls.SystemLoader;
import backtraceio.library.services.BacktraceCrashHandlerRunner;

@RunWith(MockitoJUnitRunner.class)
public class CrashHandlerRunnerInvocationTest {

    private final static String fakePathLibrary = "path/to/lib";
    @Mock
    BacktraceCrashHandlerWrapper backtraceCrashHandlerWrapper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void failIfEnvVariablesAreNotDefined() {
        BacktraceCrashHandlerRunner runner = new BacktraceCrashHandlerRunner();
        assertFalse(runner.run(new String[]{}, null));
    }

    @Test
    public void failIfEnvVariablesDontStoreHandlerPath() {
        BacktraceCrashHandlerRunner runner = new BacktraceCrashHandlerRunner();
        assertFalse(runner.run(new String[]{}, new HashMap<>()));
    }

    @Test
    public void shouldExecuteCorrectlyCrashpadHandlerMethod() {
        HashMap<String, String> envVariables = new HashMap<String, String>();
        envVariables.put(CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER, fakePathLibrary);

        when(backtraceCrashHandlerWrapper.handleCrash(any(String[].class))).thenReturn(true);

        BacktraceCrashHandlerRunner runner = new BacktraceCrashHandlerRunner(backtraceCrashHandlerWrapper, mock(SystemLoader.class));
        assertTrue(runner.run(new String[]{}, envVariables));
    }

    @Test
    public void shouldReturnFalseWhenCrashpadFails() {
        HashMap<String, String> envVariables = new HashMap<String, String>();
        envVariables.put(CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER, fakePathLibrary);

        when(backtraceCrashHandlerWrapper.handleCrash(any(String[].class))).thenReturn(true);

        BacktraceCrashHandlerRunner runner = new BacktraceCrashHandlerRunner(backtraceCrashHandlerWrapper, mock(SystemLoader.class));
        assertTrue(runner.run(new String[]{}, envVariables));
    }
}
