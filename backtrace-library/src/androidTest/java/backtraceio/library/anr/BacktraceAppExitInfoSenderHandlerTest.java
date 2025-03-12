package backtraceio.library.anr;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.logger.BacktraceInternalLogger;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.models.BacktraceApiResult;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;

//@RunWith(MockitoJUnitRunner.class)
//@RunWith(AndroidJUnit4.class)
//@RunWith(MockitoJUnitRunner.class)
@RunWith(AndroidJUnit4.class)
public class BacktraceAppExitInfoSenderHandlerTest {
    @Mock
    private Context mockContext;

//    @Mock
//    private ApplicationExitInfo mockAppExitInfo2;
    @Mock
    private ActivityManager mockActivityManager;

    private String PACKAGE_NAME = "backtrace.io";
    private final BacktraceCredentials credentials = new BacktraceCredentials("https://example-endpoint.com/", "");
    private BacktraceClient backtraceClient;



    @Before
    public void setUp() throws Exception {
                ApplicationExitInfo x = ApplicationExitInfoFactory.createApplicationExitInfo(0, "", 0,0L);
//        ApplicationExitInfo x = mockApplicationExitInfo("random-text", System.currentTimeMillis(), ApplicationExitInfo.REASON_CRASH_NATIVE);

//        Gson gson = new Gson();

//        ApplicationExitInfo obj = gson.fromJson("{\"mReason\": 5 }", ApplicationExitInfo.class);

        MockitoAnnotations.openMocks(this);
        BacktraceLogger.setLogger(new BacktraceInternalLogger(LogLevel.DEBUG));
        MockitoAnnotations.initMocks(this);
        this.backtraceClient = new BacktraceClient(this.mockContext, credentials);

        when(mockContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(mockActivityManager);
//        when(mockContext.getApplicationContext().getPackageName()).thenReturn(PACKAGE_NAME);
//        context.getApplicationContext().getPackageName()
        this.mockContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    private void mockHistoricalProcessExitReasons(List<ApplicationExitInfo> exitInfoList) {
        when(mockActivityManager.getHistoricalProcessExitReasons(anyString(), eq(0), eq(0)))
                .thenReturn(exitInfoList);
    }

    private ApplicationExitInfo mockApplicationExitInfo(String description, Long timestamp, int reason, int pid, int importance, long pss, long rss) {
        Gson gson = new Gson();
        ApplicationExitInfo gsonMockAppExitInfo = gson.fromJson("{}", ApplicationExitInfo.class);
        ApplicationExitInfo mockAppExitInfo = mock(gsonMockAppExitInfo);
//        ApplicationExitInfo mockAppExitInfo = mock(ApplicationExitInfo.class);
        when(mockAppExitInfo.getDescription()).thenReturn(description);
        when(mockAppExitInfo.getTimestamp()).thenReturn(timestamp);
        when(mockAppExitInfo.getReason()).thenReturn(reason);
        when(mockAppExitInfo.getPid()).thenReturn(pid);
        when(mockAppExitInfo.getImportance()).thenReturn(importance);
        when(mockAppExitInfo.getPss()).thenReturn(pss);
        when(mockAppExitInfo.getRss()).thenReturn(rss);
        return mockAppExitInfo;
    }

    private ApplicationExitInfo mockApplicationExitInfo(String description, Long timestamp, int reason) {
        return mockApplicationExitInfo(description, timestamp, reason, 0,0 , 0L, 0L);
    }

    @Test
    public void checkIfANRIsSentFromAppExitInfo() {
        // GIVEN
        final List<ApplicationExitInfo> exitInfoList = new ArrayList<>();
        exitInfoList.add(mockApplicationExitInfo("random-text", System.currentTimeMillis(), ApplicationExitInfo.REASON_CRASH_NATIVE));
        exitInfoList.add(mockApplicationExitInfo("anr", System.currentTimeMillis(), ApplicationExitInfo.REASON_ANR));
        exitInfoList.add(mockApplicationExitInfo("random-description", System.currentTimeMillis(), ApplicationExitInfo.REASON_LOW_MEMORY));
        this.mockHistoricalProcessExitReasons(exitInfoList);

        final Waiter waiter = new Waiter();
        backtraceClient.setOnRequestHandler(new RequestHandler() {
            @Override
            public BacktraceResult onRequest(BacktraceData data) {
                waiter.resume();
                return new BacktraceResult(new BacktraceApiResult("_", "ok"));
            }
        });
        BacktraceAppExitInfoSenderHandler handler = new BacktraceAppExitInfoSenderHandler(this.backtraceClient, mockContext);
        // WHEN

        // THEN
        try {
            waiter.await(1000, TimeUnit.SECONDS); // Check if anr is detected and event was emitted
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
