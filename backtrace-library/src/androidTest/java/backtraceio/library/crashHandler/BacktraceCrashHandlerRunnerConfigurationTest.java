package backtraceio.library.crashHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;

@RunWith(AndroidJUnit4.class)
public class BacktraceCrashHandlerRunnerConfigurationTest {
    @Test
    public void unsupportedAbiVersion() {
        CrashHandlerConfiguration crashHandlerConfiguration = new CrashHandlerConfiguration();
        for (String unsupportedAbi :
                CrashHandlerConfiguration.UNSUPPORTED_ABIS) {
            assertFalse(crashHandlerConfiguration.isSupportedAbi(unsupportedAbi));
        }
    }

    @Test
    public void supportedAbiVersion() {
        CrashHandlerConfiguration crashHandlerConfiguration = new CrashHandlerConfiguration();
        for (String unsupportedAbi :
                new String[]{"armeabi", "arm64", "arm64-v8"}) {
            assertTrue(crashHandlerConfiguration.isSupportedAbi(unsupportedAbi));
        }
    }

    @Test
    public void environmentVariableHasCorrectPathToLibrary() {
        CrashHandlerConfiguration crashHandlerConfiguration = new CrashHandlerConfiguration();
        String fakePathToApk = "fake/path/to/apk/apk.apk";
        String fakePathToLib = "fake/path/to/lib/arm64";
        String fakeAbi = "fake-abi";
        List<String> environmentVariables = crashHandlerConfiguration.getCrashHandlerEnvironmentVariables(fakePathToApk, fakePathToLib, fakeAbi);

        Optional<String> crashHandlerEnv = environmentVariables.stream()
                .filter(n -> n.startsWith(CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER))
                .findFirst();

        if (!crashHandlerEnv.isPresent()) {
            fail("Backtrace crash handler env variable is not present");
        }
        assertEquals(crashHandlerEnv.get(), String.format("%s=%s!/lib/%s/libbacktrace-native.so", CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER, fakePathToApk, fakeAbi));
    }

    @Test
    public void environmentVariableContainsJavaEnvVariables() {
        CrashHandlerConfiguration crashHandlerConfiguration = new CrashHandlerConfiguration();
        String fakePathToApk = "fake/path/to/apk/apk.apk";
        String fakePathToLib = "fake/path/to/lib/arm64";
        String fakeAbi = "fake-abi";
        List<String> environmentVariables = crashHandlerConfiguration.getCrashHandlerEnvironmentVariables(fakePathToApk, fakePathToLib, fakeAbi);

        Map<String, String> systemEnvVariables = System.getenv();

        for (Map.Entry<String, String> envVariable : systemEnvVariables.entrySet()) {
            assertTrue(environmentVariables.indexOf(String.format("%s=%s", envVariable.getKey(), envVariable.getValue())) != -1);
        }
    }

}
