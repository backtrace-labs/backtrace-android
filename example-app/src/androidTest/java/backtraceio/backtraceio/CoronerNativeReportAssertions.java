package backtraceio.backtraceio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.os.SystemClock;
import backtraceio.coroner.CoronerClient;
import backtraceio.coroner.query.CoronerQueryFields;
import backtraceio.coroner.response.CoronerResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GUID-correlated ingestion assertions for native qualification. GUID is the primary correlation
 * key (fatal reports carry no controllable {@code error.message}); the query limit exceeds every
 * expected count, so duplicate reports are detectable — with the historical limit of one they were
 * not. The count policy sums group row counts because Coroner can collapse an {@code _rxid}-grouped
 * query into a single {@code "*"} group whose row count carries the real match total. A collapsed
 * group exposes only one head value per fold, so it can never prove an expected message set: when
 * specific messages must be proven, callers use {@link #awaitExactlyOneWithMessage}, which filters
 * on the message itself. The report source is injectable so sabotage tests can prove every policy
 * rejects its failure case.
 */
final class CoronerNativeReportAssertions {

    static final String CRASH_ERROR_TYPE = "Crash";
    static final long POLL_DEADLINE_MS = 90_000;
    static final long POLL_INTERVAL_MS = 2_000;
    static final long STABILITY_WINDOW_MS = 10_000;
    static final int QUERY_LIMIT = 10;

    /**
     * Immutable test-only view of one result group. {@code groupId} is the RXID when the backend
     * returns per-report groups and the literal {@code "*"} when it collapses the grouping;
     * {@code rowCount} is the number of underlying reports folded into the group.
     */
    static final class NativeReport {
        final String guid;
        final String groupId;
        final String errorType;
        final String errorMessage;
        final int rowCount;

        NativeReport(String guid, String groupId, String errorType, String errorMessage) {
            this(guid, groupId, errorType, errorMessage, 1);
        }

        NativeReport(String guid, String groupId, String errorType, String errorMessage, int rowCount) {
            this.guid = guid;
            this.groupId = groupId;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
            this.rowCount = rowCount;
        }
    }

    /** Injectable report source; production queries Coroner, sabotage tests inject fakes. */
    interface ReportSource {
        List<NativeReport> fetch(String guid, long timestampStartSeconds) throws Exception;
    }

    static ReportSource coronerSource(final CoronerClient client) {
        return (guid, timestampStartSeconds) -> toReports(client.guidTimestampFilter(
                guid,
                Long.toString(timestampStartSeconds),
                Long.toString(System.currentTimeMillis() / 1000L),
                Arrays.asList(CoronerQueryFields.ERROR_TYPE, CoronerQueryFields.ERROR_MESSAGE),
                QUERY_LIMIT));
    }

    /** Source filtered on an exact {@code error.message}, immune to collapsed grouping. */
    static ReportSource coronerMessageSource(final CoronerClient client, final String message) {
        return (guid, timestampStartSeconds) -> toReports(client.guidMessageTimestampFilter(
                guid,
                message,
                Long.toString(timestampStartSeconds),
                Long.toString(System.currentTimeMillis() / 1000L),
                Arrays.asList(CoronerQueryFields.ERROR_TYPE, CoronerQueryFields.ERROR_MESSAGE),
                QUERY_LIMIT));
    }

    private static List<NativeReport> toReports(CoronerResponse response) throws Exception {
        List<NativeReport> reports = new ArrayList<>();
        for (int index = 0; index < response.getResultsNumber(); index++) {
            reports.add(new NativeReport(
                    response.getAttribute(index, CoronerQueryFields.GUID, String.class),
                    response.values.get(index).getGroupIdentifier(),
                    response.getAttribute(index, CoronerQueryFields.ERROR_TYPE, String.class),
                    response.getAttribute(index, CoronerQueryFields.ERROR_MESSAGE, String.class),
                    response.values.get(index).getGroupRowCount()));
        }
        return reports;
    }

    static List<NativeReport> awaitExactly(
            CoronerClient client,
            String guid,
            long timestampStartSeconds,
            int expectedCount,
            Set<String> expectedMessages,
            String expectedErrorType) {
        return awaitExactly(
                coronerSource(client), guid, timestampStartSeconds, expectedCount, expectedMessages, expectedErrorType);
    }

    static List<NativeReport> awaitExactly(
            ReportSource source,
            String guid,
            long timestampStartSeconds,
            int expectedCount,
            Set<String> expectedMessages,
            String expectedErrorType) {
        return awaitExactly(
                source,
                guid,
                timestampStartSeconds,
                expectedCount,
                expectedMessages,
                expectedErrorType,
                POLL_DEADLINE_MS,
                POLL_INTERVAL_MS,
                STABILITY_WINDOW_MS);
    }

    /** Timing-injectable variant so sabotage tests run in milliseconds, not the CI deadlines. */
    static List<NativeReport> awaitExactly(
            ReportSource source,
            String guid,
            long timestampStartSeconds,
            int expectedCount,
            Set<String> expectedMessages,
            String expectedErrorType,
            long deadlineMs,
            long intervalMs,
            long stabilityMs) {
        long deadline = SystemClock.elapsedRealtime() + deadlineMs;
        Exception lastPollFailure = null;
        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                List<NativeReport> reports = source.fetch(guid, timestampStartSeconds);
                validate(reports, guid, expectedCount);

                if (totalRowCount(reports) == expectedCount) {
                    // Duplicate-stability window: a delayed duplicate arriving right after the
                    // expected count must fail, and the group-identifier set must not change.
                    SystemClock.sleep(stabilityMs);
                    List<NativeReport> stableReports = source.fetch(guid, timestampStartSeconds);
                    validate(stableReports, guid, expectedCount);
                    if (totalRowCount(stableReports) != expectedCount) {
                        fail("Report count changed during the stability window: expected " + expectedCount + ", found "
                                + totalRowCount(stableReports));
                    }
                    assertEquals(
                            "Group-identifier set changed during the stability window",
                            groupIdSet(reports),
                            groupIdSet(stableReports));
                    if (expectedErrorType != null) {
                        for (NativeReport report : stableReports) {
                            if (!expectedErrorType.equals(report.errorType)) {
                                fail("Report must classify as " + expectedErrorType + ", found " + report.errorType);
                            }
                        }
                    }
                    if (expectedMessages != null) {
                        if (totalRowCount(stableReports) != stableReports.size()) {
                            // A collapsed group exposes one head message for many rows; it can
                            // never prove the full expected set, so relaxing here would let a
                            // duplicate of one message pass for a missing other message.
                            fail("Collapsed grouping cannot prove the expected message set; "
                                    + "use per-message queries for guid " + guid);
                        }
                        Set<String> messages = new HashSet<>();
                        for (NativeReport report : stableReports) {
                            messages.add(report.errorMessage);
                        }
                        assertEquals("Unexpected report messages", expectedMessages, messages);
                    }
                    return stableReports;
                }
                lastPollFailure = null;
            } catch (AssertionError hardFailure) {
                throw hardFailure;
            } catch (Exception pollFailure) {
                lastPollFailure = pollFailure;
            }
            SystemClock.sleep(intervalMs);
        }
        fail("Expected " + expectedCount + " report(s) for guid " + guid + " before the deadline"
                + (lastPollFailure == null
                        ? ""
                        : "; last poll failure: " + lastPollFailure.getClass().getName()));
        return null;
    }

    /**
     * Proves one specific {@code error.message} was ingested exactly once for this GUID, through a
     * message-filtered query: correct even when GUID-wide grouping collapses. The report must
     * classify as {@code Crash}.
     */
    static NativeReport awaitExactlyOneWithMessage(
            CoronerClient client, String guid, String message, long timestampStartSeconds) {
        return awaitExactlyOneWithMessage(
                coronerMessageSource(client, message),
                guid,
                message,
                timestampStartSeconds,
                POLL_DEADLINE_MS,
                POLL_INTERVAL_MS,
                STABILITY_WINDOW_MS);
    }

    /** Timing-injectable variant; {@code source} must already filter on the message. */
    static NativeReport awaitExactlyOneWithMessage(
            ReportSource source,
            String guid,
            String message,
            long timestampStartSeconds,
            long deadlineMs,
            long intervalMs,
            long stabilityMs) {
        List<NativeReport> reports = awaitExactly(
                source,
                guid,
                timestampStartSeconds,
                1,
                Collections.singleton(message),
                CRASH_ERROR_TYPE,
                deadlineMs,
                intervalMs,
                stabilityMs);
        return reports.get(0);
    }

    static NativeReport awaitExactlyOneFatal(ReportSource source, String guid, long timestampStartSeconds) {
        return awaitExactlyOneFatal(
                source, guid, timestampStartSeconds, POLL_DEADLINE_MS, POLL_INTERVAL_MS, STABILITY_WINDOW_MS);
    }

    /** Timing-injectable variant so sabotage tests avoid the production stability window. */
    static NativeReport awaitExactlyOneFatal(
            ReportSource source,
            String guid,
            long timestampStartSeconds,
            long deadlineMs,
            long intervalMs,
            long stabilityMs) {
        List<NativeReport> reports = awaitExactly(
                source, guid, timestampStartSeconds, 1, null, CRASH_ERROR_TYPE, deadlineMs, intervalMs, stabilityMs);
        return reports.get(0);
    }

    static NativeReport awaitExactlyOneFatal(CoronerClient client, String guid, long timestampStartSeconds) {
        return awaitExactlyOneFatal(coronerSource(client), guid, timestampStartSeconds);
    }

    private static void validate(List<NativeReport> reports, String guid, int expectedCount) {
        for (NativeReport report : reports) {
            if (!guid.equals(report.guid)) {
                fail("Report guid mismatch: expected " + guid + ", found " + report.guid);
            }
        }
        if (totalRowCount(reports) > expectedCount) {
            fail("Duplicate reports detected: expected " + expectedCount + ", found " + totalRowCount(reports));
        }
    }

    /** Reports are groups; Coroner may fold many rows into one group, so sum the row counts. */
    static int totalRowCount(List<NativeReport> reports) {
        int total = 0;
        for (NativeReport report : reports) {
            total += report.rowCount;
        }
        return total;
    }

    private static Set<String> groupIdSet(List<NativeReport> reports) {
        Set<String> groupIds = new HashSet<>();
        for (NativeReport report : reports) {
            groupIds.add(report.groupId);
        }
        return groupIds;
    }

    private CoronerNativeReportAssertions() {}
}
