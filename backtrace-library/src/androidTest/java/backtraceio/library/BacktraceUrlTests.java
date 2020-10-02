package backtraceio.library;

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        String backtraceUrl = "https://submit.backtrace.io/universe/1234token/json";
        String expectedMinidumpUrl = "https://submit.backtrace.io/universe/1234token/minidump";

        BacktraceCredentials credentials = new BacktraceCredentials(backtraceUrl);
        Uri actualSubmissionUrl = credentials.getMinidumpSubmissionUrl();

        Assert.assertNotNull(actualSubmissionUrl);
        Assert.assertEquals(actualSubmissionUrl.toString(), expectedMinidumpUrl);
    }

    @Test
    public void createMinidumpSubmissionUrlForInvalidBacktraceUrl() {
        String backtraceUrl = "https://submit.backtrace.io/definetly/invalid/url";

        BacktraceCredentials credentials = new BacktraceCredentials(backtraceUrl);
        Uri actualSubmissionUrl = credentials.getMinidumpSubmissionUrl();

        Assert.assertNull(actualSubmissionUrl);
    }

    @Test
    public void createCorrectSubmissionUrl() {
        String backtraceUrl = "https://submit.backtrace.io/universe/1234token/json";

        BacktraceCredentials credentials = new BacktraceCredentials(backtraceUrl);
        Uri actualSubmissionUrl = credentials.getSubmissionUrl();

        Assert.assertNotNull(actualSubmissionUrl);
        Assert.assertEquals(backtraceUrl, actualSubmissionUrl.toString());
    }

    @Test
    public void createCorrectSubmissionUrlForLegacyUrlWithMissingServerSlash() {
        String backtraceServerUrl = "https://universe.sp.backtrace.io:6098";
        String token = "1234token";
        String expectedBacktraceUrl = "https://universe.sp.backtrace.io:6098/post?format=json&token=1234token";

        BacktraceCredentials credentials = new BacktraceCredentials(backtraceServerUrl, token);
        Uri actualSubmissionUrl = credentials.getSubmissionUrl();

        Assert.assertNotNull(actualSubmissionUrl);
        Assert.assertEquals(expectedBacktraceUrl, actualSubmissionUrl.toString());
    }

    @Test
    public void createCorrectSubmissionUrlForLegacyUrl() {
        String backtraceServerUrl = "https://universe.sp.backtrace.io:6098/";
        String token = "1234token";
        String expectedBacktraceUrl = "https://universe.sp.backtrace.io:6098/post?format=json&token=1234token";

        BacktraceCredentials credentials = new BacktraceCredentials(backtraceServerUrl, token);
        Uri actualSubmissionUrl = credentials.getSubmissionUrl();

        Assert.assertNotNull(actualSubmissionUrl);
        Assert.assertEquals(expectedBacktraceUrl, actualSubmissionUrl.toString());
    }
}
