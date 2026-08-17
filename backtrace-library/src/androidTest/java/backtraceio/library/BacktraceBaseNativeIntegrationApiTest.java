package backtraceio.library;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.enums.UnwindingMode;
import backtraceio.library.interfaces.Database;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies the result-returning native-integration overloads preserve every caller option. */
@RunWith(AndroidJUnit4.class)
public class BacktraceBaseNativeIntegrationApiTest {

    private Context context;
    private BacktraceCredentials credentials;

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        this.credentials = new BacktraceCredentials("https://test.sp.backtrace.io", "1231231231231");
    }

    @Test
    public void resultReturningOverloadDelegatesUnwindingOptions() {
        Database database = mock(Database.class);
        when(database.setupNativeIntegration(
                        any(BacktraceBase.class),
                        same(this.credentials),
                        eq(true),
                        eq(UnwindingMode.LOCAL_DUMPWITHOUTCRASH)))
                .thenReturn(true);
        BacktraceClient client = new BacktraceClient(this.context, this.credentials, database);

        assertTrue(client.tryEnableNativeIntegration(true, UnwindingMode.LOCAL_DUMPWITHOUTCRASH));

        verify(database)
                .setupNativeIntegration(
                        same(client), same(this.credentials), eq(true), eq(UnwindingMode.LOCAL_DUMPWITHOUTCRASH));
    }

    @Test
    public void voidBooleanOverloadDelegatesToMatchingDatabaseOperation() {
        Database database = mock(Database.class);
        BacktraceClient client = new BacktraceClient(this.context, this.credentials, database);

        client.enableNativeIntegration(false);

        verify(database).setupNativeIntegration(same(client), same(this.credentials), eq(false));
    }

    @Test
    public void voidUnwindingOverloadDelegatesEveryOption() {
        Database database = mock(Database.class);
        BacktraceClient client = new BacktraceClient(this.context, this.credentials, database);

        client.enableNativeIntegration(true, UnwindingMode.REMOTE_DUMPWITHOUTCRASH);

        verify(database)
                .setupNativeIntegration(
                        same(client), same(this.credentials), eq(true), eq(UnwindingMode.REMOTE_DUMPWITHOUTCRASH));
    }
}
