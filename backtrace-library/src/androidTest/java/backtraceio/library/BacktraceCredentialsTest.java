package backtraceio.library;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BacktraceCredentialsTest {

    private final String fakeUniverse = "universe";
    private final String fakeToken = "aaaaabbbbbccccf82668682e69f59b38e0a853bed941e08e85f4bf5eb2c5458";
    private final String legacyUrl = "https://" + fakeUniverse + ".sp.backtrace.io:6098/post?format=json&token=" + fakeToken;
    private final String url = "https://submit.backtrace.io/" + fakeUniverse + "/" + fakeToken + "/json";
    private final String urlPrefix = "https://" + fakeUniverse + ".sp.backtrace.io:6098";

    @Test
    public void createBacktraceCredentialsWithLegacyUrlAndTokenAndGetUniverseName() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(urlPrefix, fakeToken);
        assertEquals(fakeUniverse, backtraceCredentials.getUniverseName());
    }

    @Test
    public void createBacktraceCredentialsWithLegacyUrlAndTokenAndGetSubmissionToken() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(urlPrefix, fakeToken);
        assertEquals(fakeToken, backtraceCredentials.getSubmissionToken());
    }

    @Test
    public void createBacktraceCredentialsWithUriStringAndGetUniverseName() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(url);
        assertEquals(fakeUniverse, backtraceCredentials.getUniverseName());
    }

    @Test
    public void createBacktraceCredentialsWithUriStringAndGetSubmissionToken() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(url);
        assertEquals(fakeToken, backtraceCredentials.getSubmissionToken());
    }

    @Test
    public void createBacktraceCredentialsWithLegacyUriStringAndGetUniverseName() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(legacyUrl);
        assertEquals(fakeUniverse, backtraceCredentials.getUniverseName());
    }

    @Test
    public void createBacktraceCredentialsWithLegacyUriStringAndGetSubmissionToken() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(legacyUrl);
        assertEquals(fakeToken, backtraceCredentials.getSubmissionToken());
    }

    @Test
    public void createBacktraceCredentialsWithUriAndGetUniverseName() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(Uri.parse(url));
        assertEquals(fakeUniverse, backtraceCredentials.getUniverseName());
    }

    @Test
    public void createBacktraceCredentialsWithUriAndGetSubmissionToken() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(Uri.parse(url));
        assertEquals(fakeToken, backtraceCredentials.getSubmissionToken());
    }

    @Test
    public void createBacktraceCredentialsWithLegacyUriAndGetUniverseName() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(Uri.parse(legacyUrl));
        assertEquals(fakeUniverse, backtraceCredentials.getUniverseName());
    }

    @Test
    public void createBacktraceCredentialsWithLegacyUriAndGetSubmissionToken() {
        BacktraceCredentials backtraceCredentials = new BacktraceCredentials(Uri.parse(legacyUrl));
        assertEquals(fakeToken, backtraceCredentials.getSubmissionToken());
    }
}
