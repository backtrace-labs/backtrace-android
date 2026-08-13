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
 * Sabotage coverage for the exactly-N ingestion policy: the same count, message, and error-type
 * policies the split-install tests use must reject duplicates, instability, collapsed groups that
 * cannot prove a message set, and misclassified reports. With the historical query limit of one
 * this class of defect was undetectable, because a response could never contain two reports. This
 * suite needs no credentials and runs in the secretless lanes.
 */
@RunWith(AndroidJUnit4.class)
public class CoronerNativeReportAssertionsTest {

    private static final String GUID = "11111111-2222-3333-4444-555555555555";
    private static final long FAST = 50;

    private static NativeReport report(String groupId, String message) {
        return new NativeReport(GUID, groupId, "Crash", message);
    }

    private static List<NativeReport> awaitFast(
            ReportSource source, int expected, HashSet<String> messages, String errorType) {
        return CoronerNativeReportAssertions.awaitExactly(
                source, GUID, 0, expected, messages, errorType, 2_000, FAST, FAST);
    }

    @Test
    public void twoReportsFailAnExpectedOneAssertion() {
        ReportSource duplicates = (guid, ts) -> Arrays.asList(report("rxid-a", "dump"), report("rxid-b", "dump"));

        assertThrows(AssertionError.class, () -> awaitFast(duplicates, 1, null, null));
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

        List<NativeReport> reports = awaitFast(flaky, 1, new HashSet<>(Arrays.asList("dump")), "Crash");
        assertEquals(1, reports.size());
        assertEquals("rxid-a", reports.get(0).groupId);
    }

    @Test
    public void duplicateArrivingDuringStabilityWindowFails() {
        AtomicInteger calls = new AtomicInteger();
        ReportSource lateDuplicate = (guid, ts) -> calls.incrementAndGet() == 1
                ? Arrays.asList(report("rxid-a", "dump"))
                : Arrays.asList(report("rxid-a", "dump"), report("rxid-b", "dump"));

        assertThrows(AssertionError.class, () -> awaitFast(lateDuplicate, 1, null, null));
    }

    @Test
    public void stableSingleReportPasses() {
        ReportSource stable = (guid, ts) -> Arrays.asList(report("rxid-a", "dump"));

        List<NativeReport> reports = awaitFast(stable, 1, null, null);
        assertEquals(1, reports.size());
    }

    @Test
    public void twoDistinctLifecycleMessagesPass() {
        ReportSource lifecycle =
                (guid, ts) -> Arrays.asList(report("rxid-a", "before-disable"), report("rxid-b", "after-reenable"));

        List<NativeReport> reports =
                awaitFast(lifecycle, 2, new HashSet<>(Arrays.asList("before-disable", "after-reenable")), "Crash");
        assertEquals(2, reports.size());
    }

    @Test
    public void wrongGuidFails() {
        ReportSource wrongGuid = (guid, ts) ->
                Arrays.asList(new NativeReport("99999999-0000-0000-0000-000000000000", "rxid-a", "Crash", "dump"));

        assertThrows(AssertionError.class, () -> awaitFast(wrongGuid, 1, null, null));
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

    /** Nonfatal and lifecycle reports must classify as Crash too, not only fatal reports. */
    @Test
    public void wrongErrorTypeFailsWhenCrashIsRequired() {
        ReportSource managed = (guid, ts) -> Arrays.asList(new NativeReport(GUID, "rxid-a", "Managed", "dump"));

        assertThrows(AssertionError.class, () -> awaitFast(managed, 1, new HashSet<>(Arrays.asList("dump")), "Crash"));
    }

    /**
     * Coroner can fold every matching row into one "*" group whose count carries the real total;
     * the policy must sum row counts, or duplicates hidden inside a collapsed group pass as one.
     */
    @Test
    public void collapsedGroupWithTwoRowsFailsAnExpectedOneAssertion() {
        ReportSource collapsed = (guid, ts) -> Arrays.asList(new NativeReport(GUID, "*", "Crash", "dump", 2));

        assertThrows(AssertionError.class, () -> awaitFast(collapsed, 1, null, null));
    }

    /** Count-only expectations (no message set) remain provable from a collapsed group. */
    @Test
    public void collapsedGroupWithTwoRowsSatisfiesACountOnlyExpectedTwoAssertion() {
        ReportSource collapsed = (guid, ts) -> Arrays.asList(new NativeReport(GUID, "*", "Crash", "before-disable", 2));

        List<NativeReport> reports = awaitFast(collapsed, 2, null, "Crash");
        assertEquals(1, reports.size());
        assertEquals(2, reports.get(0).rowCount);
    }

    /**
     * A collapsed group exposes one head message for many rows, so it can never prove a full
     * expected message set: two rows showing only "before-disable" could be a duplicate hiding a
     * missing "after-reenable" report. The policy must fail instead of relaxing the check.
     */
    @Test
    public void collapsedGroupCannotProveExpectedMessagesFails() {
        ReportSource collapsed = (guid, ts) -> Arrays.asList(new NativeReport(GUID, "*", "Crash", "before-disable", 2));

        assertThrows(
                AssertionError.class,
                () -> awaitFast(collapsed, 2, new HashSet<>(Arrays.asList("before-disable", "after-reenable")), null));
    }

    /** The per-message policy must reject a duplicated message, collapsed or not. */
    @Test
    public void duplicateRowsForOneMessageFailTheMessageAssertion() {
        ReportSource duplicated =
                (guid, ts) -> Arrays.asList(new NativeReport(GUID, "*", "Crash", "before-disable", 2));

        assertThrows(
                AssertionError.class,
                () -> CoronerNativeReportAssertions.awaitExactlyOneWithMessage(
                        duplicated, GUID, "before-disable", 0, 2_000, FAST, FAST));
    }

    @Test
    public void messageQuerySatisfiedByExactlyOneCrashReportPasses() {
        ReportSource single = (guid, ts) -> Arrays.asList(report("rxid-a", "before-disable"));

        NativeReport report = CoronerNativeReportAssertions.awaitExactlyOneWithMessage(
                single, GUID, "before-disable", 0, 2_000, FAST, FAST);
        assertEquals("rxid-a", report.groupId);
        assertEquals(1, report.rowCount);
    }
}
