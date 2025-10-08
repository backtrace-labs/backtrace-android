package backtraceio.library.anr;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.ApplicationExitInfo;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.TestUtils;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.jodah.concurrentunit.Waiter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(AndroidJUnit4.class)
public class BacktraceAppExitInfoSenderHandlerTest {
    @Mock
    private Context mockContext;

    private final String PACKAGE_NAME = "backtrace.io.tests";

    private final String ANR_APPEXIT_STACKTRACE_FILE = "anrAppExitInfoStacktrace.txt";

    private final BacktraceCredentials credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    private BacktraceClient backtraceClient;

    @Before
    public void setUp() throws Exception {
        this.mockContext = InstrumentationRegistry.getInstrumentation().getContext();
        this.backtraceClient = new BacktraceClient(this.mockContext, credentials);
    }

    private ExitInfo mockApplicationExitInfo(
            String description,
            Long timestamp,
            int reason,
            int pid,
            int importance,
            long pss,
            long rss,
            InputStream stacktrace)
            throws IOException {
        ExitInfo mockExitInfo = mock(ExitInfo.class);
        when(mockExitInfo.getDescription()).thenReturn(description);
        when(mockExitInfo.getTimestamp()).thenReturn(timestamp);
        when(mockExitInfo.getReason()).thenReturn(reason);
        when(mockExitInfo.getPid()).thenReturn(pid);
        when(mockExitInfo.getImportance()).thenReturn(importance);
        when(mockExitInfo.getPss()).thenReturn(pss);
        when(mockExitInfo.getRss()).thenReturn(rss);
        when(mockExitInfo.getTraceInputStream()).thenReturn(stacktrace);
        return mockExitInfo;
    }

    private ExitInfo mockApplicationExitInfo(String description, Long timestamp, int reason, InputStream stacktrace)
            throws IOException {
        return mockApplicationExitInfo(description, timestamp, reason, 0, 0, 0L, 0L, stacktrace);
    }

    private ProcessExitInfoProvider mockActivityManagerExitInfoProvider() throws IOException {
        ActivityManagerExitInfoProvider mock = mock(ActivityManagerExitInfoProvider.class);
        final List<ExitInfo> exitInfoList = new ArrayList<>();
        exitInfoList.add(mockApplicationExitInfo(
                "random-text", System.currentTimeMillis(), ApplicationExitInfo.REASON_CRASH_NATIVE, null));
        exitInfoList.add(mockApplicationExitInfo(
                "anr",
                System.currentTimeMillis(),
                ApplicationExitInfo.REASON_ANR,
                TestUtils.readFileAsStream(this, ANR_APPEXIT_STACKTRACE_FILE)));
        exitInfoList.add(mockApplicationExitInfo(
                "anr without stacktrace", System.currentTimeMillis(), ApplicationExitInfo.REASON_ANR, null));
        exitInfoList.add(mockApplicationExitInfo(
                "random-description", System.currentTimeMillis(), ApplicationExitInfo.REASON_LOW_MEMORY, null));

        when(mock.getHistoricalExitInfo(PACKAGE_NAME, 0, 0)).thenReturn(exitInfoList);
        when(mock.getSupportedTypesOfExitInfo()).thenReturn(Collections.singletonList(ApplicationExitInfo.REASON_ANR));
        return mock;
    }

    private AnrExitInfoState mockAnrExitInfoState() {
        AnrExitInfoState mock = mock(AnrExitInfoState.class);
        doNothing().when(mock).saveTimestamp(anyLong());
        when(mock.getLastTimestamp()).thenReturn(0L);
        return mock;
    }

    @Test
    @SdkSuppress(minSdkVersion = android.os.Build.VERSION_CODES.R)
    public void checkIfANRIsSentFromAppExitInfo() throws IOException {
        // GIVEN
        final ProcessExitInfoProvider mockProcessExitInfoProvider = mockActivityManagerExitInfoProvider();
        final AnrExitInfoState anrExitInfoState = mockAnrExitInfoState();
        final Waiter waiter = new Waiter();
        backtraceClient.setOnRequestHandler(new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {

                Map<String, String> attributes = data.getAttributes();
                Map<String, Object> annotations = data.getAnnotations();
                Map<String, Object> anrAnnotations = (Map<String, Object>) annotations.get("ANR annotations");

                waiter.assertEquals(data.getReport().getException().getStackTrace().length, 33);

                waiter.assertNotNull(anrAnnotations);
                waiter.assertNotNull(attributes);
                waiter.assertEquals("anr", anrAnnotations.get("description"));
                waiter.assertEquals(ApplicationExitInfo.REASON_ANR, anrAnnotations.get("reason-code"));
                waiter.assertEquals("anr", anrAnnotations.get("reason"));
                waiter.assertTrue(((Map<String, Object>) annotations.get("ANR parsed stacktrace")).size() > 0);
                waiter.assertEquals(
                        "backtraceio.library.anr.BacktraceANRExitInfoException", attributes.get("classifier"));
                waiter.assertEquals("Hang", attributes.get("error.type"));
                waiter.assertTrue(attributes.get("ANR stacktrace").length() > 0);
                waiter.resume();

                return new BacktraceResult(new BacktraceApiResult("_", "ok"));
            }
        });
        // WHEN
        new BacktraceAppExitInfoSenderHandler(
                this.backtraceClient, PACKAGE_NAME, anrExitInfoState, mockProcessExitInfoProvider);

        // THEN
        try {
            waiter.await(5, TimeUnit.SECONDS); // Check if anr is detected and event was emitted
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = android.os.Build.VERSION_CODES.Q)
    public void checkIfANRIsNotSentOnOldSDK() throws IOException {
        // GIVEN
        final int THREAD_SLEEP_TIME_MS = 3000;
        final ProcessExitInfoProvider mockProcessExitInfoProvider = mockActivityManagerExitInfoProvider();
        final AnrExitInfoState anrExitInfoState = mockAnrExitInfoState();
        final Waiter waiter = new Waiter();
        backtraceClient.setOnRequestHandler(new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                waiter.fail();
                return new BacktraceResult(new BacktraceApiResult("_", "ok"));
            }
        });
        // WHEN
        new BacktraceAppExitInfoSenderHandler(
                this.backtraceClient, PACKAGE_NAME, anrExitInfoState, mockProcessExitInfoProvider);

        // THEN
        try {
            Thread.sleep(THREAD_SLEEP_TIME_MS);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        System.out.println("wat");
    }
}
