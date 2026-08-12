package backtraceio.backtraceio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.os.SystemClock;
import backtraceio.coroner.CoronerClient;
import backtraceio.coroner.query.CoronerQueryFields;
import backtraceio.coroner.response.CoronerResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GUID-correlated ingestion assertions for native qualification. GUID is the primary correlation
 * key (fatal reports carry no controllable {@code error.message}); the query limit exceeds every
 * expected count, so duplicate reports are detectable — with the historical limit of one they were
 * not. The count policy is injectable so a sabotage test can prove the exactly-N assertion rejects
 * duplicates.
 */
final class CoronerNativeReportAssertions {

    static final long POLL_DEADLINE_MS = 90_000;
    static final long POLL_INTERVAL_MS = 2_000;
    static final long STABILITY_WINDOW_MS = 10_000;
    static final int QUERY_LIMIT = 10;

    /**
     * Immutable test-only view of one result group. Coroner can collapse an {@code _rxid}-grouped
     * query into a single {@code "*"} group whose row count carries the real match total, so the
     * count policy sums {@code rowCount} instead of counting groups.
     */
    static final class NativeReport {
        final String guid;
        final String rxid;
        final String errorType;
        final String errorMessage;
        final int rowCount;

        NativeReport(String guid, String rxid, String errorType, String errorMessage) {
            this(guid, rxid, errorType, errorMessage, 1);
        }

        NativeReport(String guid, String rxid, String errorType, String errorMessage, int rowCount) {
            this.guid = guid;
            this.rxid = rxid;
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
        return (guid, timestampStartSeconds) -> {
            CoronerResponse response = client.guidTimestampFilter(
                    guid,
                    Long.toString(timestampStartSeconds),
                    Long.toString(System.currentTimeMillis() / 1000L),
                    Arrays.asList(CoronerQueryFields.ERROR_TYPE, CoronerQueryFields.ERROR_MESSAGE),
                    QUERY_LIMIT);

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
        };
    }

    static List<NativeReport> awaitExactly(
            CoronerClient client,
            String guid,
            long timestampStartSeconds,
            int expectedCount,
            Set<String> expectedMessages) {
        return awaitExactly(coronerSource(client), guid, timestampStartSeconds, expectedCount, expectedMessages);
    }

    static List<NativeReport> awaitExactly(
            ReportSource source,
            String guid,
            long timestampStartSeconds,
            int expectedCount,
            Set<String> expectedMessages) {
        return awaitExactly(
                source,
                guid,
                timestampStartSeconds,
                expectedCount,
                expectedMessages,
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
                    // expected count must fail, and the RXID set must not change.
                    SystemClock.sleep(stabilityMs);
                    List<NativeReport> stableReports = source.fetch(guid, timestampStartSeconds);
                    validate(stableReports, guid, expectedCount);
                    if (totalRowCount(stableReports) != expectedCount) {
                        fail("Report count changed during the stability window: expected " + expectedCount + ", found "
                                + totalRowCount(stableReports));
                    }
                    assertEquals(
                            "RXID set changed during the stability window", rxidSet(reports), rxidSet(stableReports));
                    if (expectedMessages != null) {
                        Set<String> messages = new HashSet<>();
                        for (NativeReport report : stableReports) {
                            messages.add(report.errorMessage);
                        }
                        if (totalRowCount(stableReports) == stableReports.size()) {
                            // Per-report identity available: require the exact message set.
                            assertEquals("Unexpected report messages", expectedMessages, messages);
                        } else {
                            // Collapsed grouping exposes one head value per group; every observed
                            // message must still be expected (the count assertion stays exact).
                            for (String message : messages) {
                                if (!expectedMessages.contains(message)) {
                                    fail("Unexpected report message: " + message);
                                }
                            }
                        }
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
        List<NativeReport> reports =
                awaitExactly(source, guid, timestampStartSeconds, 1, null, deadlineMs, intervalMs, stabilityMs);
        NativeReport report = reports.get(0);
        assertEquals("Fatal report must classify as Crash", "Crash", report.errorType);
        return report;
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
    private static int totalRowCount(List<NativeReport> reports) {
        int total = 0;
        for (NativeReport report : reports) {
            total += report.rowCount;
        }
        return total;
    }

    private static Set<String> rxidSet(List<NativeReport> reports) {
        Set<String> rxids = new HashSet<>();
        for (NativeReport report : reports) {
            rxids.add(report.rxid);
        }
        return rxids;
    }

    private CoronerNativeReportAssertions() {}
}
