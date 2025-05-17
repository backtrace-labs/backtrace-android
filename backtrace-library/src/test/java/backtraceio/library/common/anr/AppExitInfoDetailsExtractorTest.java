package backtraceio.library.common.anr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.ApplicationExitInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;

import backtraceio.library.anr.AppExitInfoDetailsExtractor;
import backtraceio.library.anr.ExitInfo;

@RunWith(MockitoJUnitRunner.class)
public class AppExitInfoDetailsExtractorTest {
    @Mock
    private ExitInfo mockAppExitInfo;

    @Before
    public void setUp() {
        mockAppExitInfo = mock(ExitInfo.class);
    }

    @Test
    public void testGetANRAttributesNullInput() {
        // WHEN
        HashMap<String, Object> result = AppExitInfoDetailsExtractor.getANRAttributes(null);

        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetANRAttributesValidInput() {
        // GIVEN
        when(mockAppExitInfo.getDescription()).thenReturn("Test ANR");
        when(mockAppExitInfo.getTimestamp()).thenReturn(System.currentTimeMillis());
        when(mockAppExitInfo.getReason()).thenReturn(ApplicationExitInfo.REASON_ANR);
        when(mockAppExitInfo.getPid()).thenReturn(1234);
        when(mockAppExitInfo.getImportance()).thenReturn(100);
        when(mockAppExitInfo.getPss()).thenReturn(200L);
        when(mockAppExitInfo.getRss()).thenReturn(300L);

        // WHEN
        HashMap<String, Object> result = AppExitInfoDetailsExtractor.getANRAttributes(mockAppExitInfo);

        // THEN
        assertNotNull(result);
        assertEquals("Test ANR", result.get("description"));
        assertEquals("anr", result.get("reason"));
        assertEquals(1234, result.get("PID"));
        assertEquals(100, result.get("Importance"));
        assertEquals(200L, result.get("PSS"));
        assertEquals(300L, result.get("RSS"));
    }

    @Test
    public void testGetANRMessage() {
        // GIVEN
        Long timestamp = 1745473156000L;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestampString = dateFormat.format(timestamp);

        // GIVEN
        when(mockAppExitInfo.getDescription()).thenReturn("Test ANR");
        when(mockAppExitInfo.getTimestamp()).thenReturn(timestamp);

        // WHEN
        String result = AppExitInfoDetailsExtractor.getANRMessage(mockAppExitInfo);

        // THEN
        assertEquals("Application Not Responding | Description: Test ANR | Timestamp: " + timestampString, result);
    }

    @Test
    public void testGetStackTraceInfoValidStream() throws IOException {
        // GIVEN
        String mockStackTrace = "Exception: Test Stack Trace";
        InputStream inputStream = new ByteArrayInputStream(mockStackTrace.getBytes());
        when(mockAppExitInfo.getTraceInputStream()).thenReturn(inputStream);

        // WHEN
        String stackTrace = AppExitInfoDetailsExtractor.getStackTraceInfo(mockAppExitInfo);

        // THEN
        assertNotNull(stackTrace);
        assertTrue(stackTrace.contains("Exception: Test Stack Trace"));
    }

    @Test
    public void testGetStackTraceInfoNullStream() {
        // WHEN
        Object stackTrace = AppExitInfoDetailsExtractor.getANRAttributes(mockAppExitInfo).get("stackTrace");

        // THEN
        assertNull(stackTrace);
    }
}
