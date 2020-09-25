package backtraceio.library;

import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.jodah.concurrentunit.Waiter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceExceptionHandler;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.types.BacktraceResultStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@RunWith(AndroidJUnit4.class)
public class BacktraceUrlTests {
    @Test
    public void createMinidumpSubmissionUrlWithLegacyBacktraceUrl() {
        String backtraceLegacyUrl = "https://universe.sp.backtrace.io:6098/post?format=json&token=1234token";
        String expectedMinidumpUrl = "https://universe.sp.backtrace.io:6098/post?format=minidump&token=1234token";

        BacktraceCredentials credentials = new BacktraceCredentials(backtraceLegacyUrl);
        Uri actualSubmissionUrl = credentials.getMinidumpSubmissionUrl();

        Assert.assertNotNull(actualSubmissionUrl);
        Assert.assertEquals(actualSubmissionUrl.toString(), expectedMinidumpUrl);
    }

    @Test
    public void createMinidumpSubmissionUrlWithSubmitBacktraceUrl() {
        String backtraceLegacyUrl = "https://submit.backtrace.io/universe/1234token/json";
        String expectedMinidumpUrl = "https://submit.backtrace.io/universe/1234token/minidump";

        BacktraceCredentials credentials = new BacktraceCredentials(backtraceLegacyUrl);
        Uri actualSubmissionUrl = credentials.getMinidumpSubmissionUrl();

        Assert.assertNotNull(actualSubmissionUrl);
        Assert.assertEquals(actualSubmissionUrl.toString(), expectedMinidumpUrl);
    }

    @Test
    public void createMinidumpSubmissionUrlForInvalidBacktraceUrl() {
        String backtraceLegacyUrl = "https://submit.backtrace.io/definetly/invalid/url";

        BacktraceCredentials credentials = new BacktraceCredentials(backtraceLegacyUrl);
        Uri actualSubmissionUrl = credentials.getMinidumpSubmissionUrl();

        Assert.assertNull(actualSubmissionUrl);
    }
}
