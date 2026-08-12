package backtraceio.backtraceio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import backtraceio.backtraceio.CoronerNativeReportAssertions.NativeReport;
import backtraceio.backtraceio.CoronerNativeReportAssertions.ReportSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Sabotage coverage for the exactly-N ingestion policy: the same count policy the split-install
 * tests use must reject duplicates and instability. With the historical query limit of one this
 * class of defect was undetectable, because a response could never contain two reports.
 */
@RunWith(AndroidJUnit4.class)
public class CoronerNativeReportAssertionsTest {

    private static final String GUID = "11111111-2222-3333-4444-555555555555";
    private static final long FAST = 50;

    private static NativeReport report(String rxid, String message) {
        return new NativeReport(GUID, rxid, "Crash", message);
    }

    private static List<NativeReport> awaitFast(ReportSource source, int expected, HashSet<String> messages) {
        return CoronerNativeReportAssertions.awaitExactly(source, GUID, 0, expected, messages, 2_000, FAST, FAST);
    }

    @Test
    public void twoReportsFailAnExpectedOneAssertion() {
        ReportSource duplicates = (guid, ts) -> Arrays.asList(report("rxid-a", "dump"), report("rxid-b", "dump"));

        assertThrows(AssertionError.class, () -> awaitFast(duplicates, 1, null));
    }

    @Test
    public void transientFailureThenSuccessPasses() {
        AtomicInteger calls = new AtomicInteger();
        ReportSource flaky = (guid, ts) -> {
            if (calls.incrementAndGet() == 1) {
                throw new IOException("transient");
            }
            return Arrays.asList(report("rxid-a", "dump"));
        };

        List<NativeReport> reports = awaitFast(flaky, 1, new HashSet<>(Arrays.asList("dump")));
        assertEquals(1, reports.size());
        assertEquals("rxid-a", reports.get(0).rxid);
    }

    @Test
    public void duplicateArrivingDuringStabilityWindowFails() {
        AtomicInteger calls = new AtomicInteger();
        ReportSource lateDuplicate = (guid, ts) -> calls.incrementAndGet() == 1
                ? Arrays.asList(report("rxid-a", "dump"))
                : Arrays.asList(report("rxid-a", "dump"), report("rxid-b", "dump"));

        assertThrows(AssertionError.class, () -> awaitFast(lateDuplicate, 1, null));
    }

    @Test
    public void stableSingleReportPasses() {
        ReportSource stable = (guid, ts) -> Arrays.asList(report("rxid-a", "dump"));

        List<NativeReport> reports = awaitFast(stable, 1, null);
        assertEquals(1, reports.size());
    }

    @Test
    public void twoDistinctLifecycleMessagesPass() {
        ReportSource lifecycle =
                (guid, ts) -> Arrays.asList(report("rxid-a", "before-disable"), report("rxid-b", "after-reenable"));

        List<NativeReport> reports =
                awaitFast(lifecycle, 2, new HashSet<>(Arrays.asList("before-disable", "after-reenable")));
        assertEquals(2, reports.size());
    }

    @Test
    public void wrongGuidFails() {
        ReportSource wrongGuid = (guid, ts) ->
                Arrays.asList(new NativeReport("99999999-0000-0000-0000-000000000000", "rxid-a", "Crash", "dump"));

        assertThrows(AssertionError.class, () -> awaitFast(wrongGuid, 1, null));
    }

    @Test
    public void wrongErrorTypeFailsTheFatalAssertion() {
        ReportSource managedOnly =
                (guid, ts) -> new ArrayList<>(Arrays.asList(new NativeReport(GUID, "rxid-a", "Managed", "dump")));

        assertThrows(
                AssertionError.class,
                () -> CoronerNativeReportAssertions.awaitExactlyOneFatal(
                        (guid, ts) -> managedOnly.fetch(guid, ts), GUID, 0, 2_000, FAST, FAST));
    }

    /**
     * Coroner can fold every matching row into one "*" group whose count carries the real total;
     * the policy must sum row counts, or duplicates hidden inside a collapsed group pass as one.
     */
    @Test
    public void collapsedGroupWithTwoRowsFailsAnExpectedOneAssertion() {
        ReportSource collapsed = (guid, ts) -> Arrays.asList(new NativeReport(GUID, "*", "Crash", "dump", 2));

        assertThrows(AssertionError.class, () -> awaitFast(collapsed, 1, null));
    }

    @Test
    public void collapsedGroupWithTwoRowsSatisfiesAnExpectedTwoAssertion() {
        ReportSource collapsed = (guid, ts) -> Arrays.asList(new NativeReport(GUID, "*", "Crash", "before-disable", 2));

        List<NativeReport> reports =
                awaitFast(collapsed, 2, new HashSet<>(Arrays.asList("before-disable", "after-reenable")));
        assertEquals(1, reports.size());
        assertEquals(2, reports.get(0).rowCount);
    }
}
