package backtraceio.library.common.anr;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import net.jodah.concurrentunit.Waiter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.anr.BacktraceAppExitInfoSenderHandler;
import backtraceio.library.base.NativeLibraryLoader;
import backtraceio.library.logger.BacktraceInternalLogger;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.LogLevel;
import backtraceio.library.models.json.BacktraceReport;

//@RunWith(MockitoJUnitRunner.class)
@RunWith(MockitoJUnitRunner.class)
public class ABCTest {
    @Mock
    private Context mockContext;

    @Mock
    private ActivityManager mockActivityManager;

    private String PACKAGE_NAME = "backtrace.io";
    private final BacktraceCredentials credentials = new BacktraceCredentials("https://example-endpoint.com/");
//    private BacktraceClient backtraceClient;

    @Before
    public void setUp() {
        BacktraceClient backtraceClient1;
        try (MockedStatic<NativeLibraryLoader> mockedStatic = Mockito.mockStatic(NativeLibraryLoader.class)) {
            backtraceClient1 = mock(BacktraceClient.class);
            Mockito.doNothing().when(backtraceClient1).send((BacktraceReport) any(), any());//.thenReturn(null);
        }
//        BacktraceClient backtraceClient1 = mock(BacktraceClient.class);
//        Mockito.doNothing().when(backtraceClient1).send((BacktraceReport) any(), any());//.thenReturn(null);


//        ApplicationExitInfo x = mockApplicationExitInfo("random-text", System.currentTimeMillis(), ApplicationExitInfo.REASON_CRASH_NATIVE);
//        try (MockedStatic<NativeLibraryLoader> mockedStatic = Mockito.mockStatic(NativeLibraryLoader.class)) {
//            this.backtraceClient = new BacktraceClient(this.mockContext, credentials);
//
//        }

//        try (MockedStatic<BacktraceClient> mockedStatic = Mockito.mockStatic(BacktraceClient.class)) {
//            when(mockedStatic.se)
//            mockedStatic.when(BacktraceClient::send).thenReturn(null);
//            this.backtraceClient = new BacktraceClient(this.mockContext, credentials);
//
//        }
//        NativeLibraryLoader mockLoader = Mockito.mock(NativeLibraryLoader.class);
//        Mockito.doNothing().when(mockLoader).load();

        // Inject mock into BacktraceBase
//        BacktraceBase.setLibraryLoader(mockLoader);

//        this.mockNativeLibrary();
//        ApplicationExitInfo x = mockApplicationExitInfo("random-text", System.currentTimeMillis(), ApplicationExitInfo.REASON_CRASH_NATIVE);
        BacktraceLogger.setLogger(new BacktraceInternalLogger(LogLevel.DEBUG));
//        MockitoAnnotations.initMocks(this);
//        try (MockedStatic<System> mockedSystem = Mockito.mockStatic(System.class)) {
//            // Mock only the loadLibrary method, allowing other System methods to work normally
//            mockedSystem.when(() -> System.loadLibrary("backtrace-native")).thenAnswer(inv -> null);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        when(mockContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(mockActivityManager);
        when(mockContext.getApplicationContext()).th.getPackageName()).thenReturn(PACKAGE_NAME);
//        when(mockContext.getApplicationContext().getPackageName()).thenReturn(PACKAGE_NAME);
//        context.getApplicationContext().getPackageName()
        this.mockContext = mock(Context.class);
    }

//    private void mockNativeLibrary() {
//        NativeLibraryLoader loaderMock = Mockito.mock(NativeLibraryLoader.class);
////        Mockito.doNothing().when(loaderMock).load();
//        try (MockedStatic<NativeLibraryLoader> mockedLoader = Mockito.mockStatic(NativeLibraryLoader.class)) {
//            // Mock the static load method
//            // Option 1: Simply verify it was called without actually executing it
//            mockedLoader.when(NativeLibraryLoader::load).thenAnswer(invocation -> null);
////            mockedLoader.verify(NativeLibraryLoader::load);
//        }
//
//    }

    private void mockHistoricalProcessExitReasons(List<ApplicationExitInfo> exitInfoList) {
        when(mockActivityManager.getHistoricalProcessExitReasons(anyString(), eq(0), eq(0)))
                .thenReturn(exitInfoList);
    }

    private ApplicationExitInfo mockApplicationExitInfo(String description, Long timestamp, int reason, int pid, int importance, long pss, long rss) {
        ApplicationExitInfo mockAppExitInfo = mock(ApplicationExitInfo.class);
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
        final Waiter waiter = new Waiter();

        // MOCK
        Parcelable.Creator<ApplicationExitInfo> x = ApplicationExitInfo.CREATOR;

        BacktraceClient backtraceClient;
        try (MockedStatic<NativeLibraryLoader> mockedStatic = Mockito.mockStatic(NativeLibraryLoader.class)) {
            backtraceClient = mock(BacktraceClient.class);
            Mockito.doAnswer((Answer) invocation -> {
                waiter.resume();
                return null;
            }).when(backtraceClient).send((BacktraceReport) any(), any());//.thenReturn(null);
        }

        // GIVEN
        final List<ApplicationExitInfo> exitInfoList = new ArrayList<>();
        exitInfoList.add(mockApplicationExitInfo("random-text", System.currentTimeMillis(), ApplicationExitInfo.REASON_CRASH_NATIVE));
        exitInfoList.add(mockApplicationExitInfo("anr", System.currentTimeMillis(), ApplicationExitInfo.REASON_ANR));
        exitInfoList.add(mockApplicationExitInfo("random-description", System.currentTimeMillis(), ApplicationExitInfo.REASON_LOW_MEMORY));
        this.mockHistoricalProcessExitReasons(exitInfoList);

//        final Waiter waiter = new Waiter();
//        backtraceClient.setOnRequestHandler(new RequestHandler() {
//            @Override
//            public BacktraceResult onRequest(BacktraceData data) {
//                waiter.resume();
//                return new BacktraceResult(new BacktraceApiResult("_", "ok"));
//            }
//        });
        BacktraceAppExitInfoSenderHandler handler = new BacktraceAppExitInfoSenderHandler(backtraceClient, mockContext);
        // WHEN

        // THEN
        try {
            waiter.await(1000, TimeUnit.SECONDS); // Check if anr is detected and event was emitted
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        System.out.println("WAT");
    }
}
