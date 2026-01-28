package backtraceio.library.database;

import static backtraceio.library.TestUtils.compareJson;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;

import backtraceio.library.TestUtils;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import org.junit.Test;

public class BacktraceDatabaseRecordTest {
    private final String JSON_FILE = "backtraceDatabaseRecord.json";
    private final String uuid = "ecdf418b-3e22-4c7c-8011-c85dc2b4386f";
    private final String dbPath = "/data/user/0/backtraceio.library.test/files/";

    private final int expectedSize = 25362;

    @Test
    public void serialize() {
        // GIVEN
        final BacktraceDatabaseRecord obj = new BacktraceDatabaseRecord(
                uuid,
                dbPath + uuid + "-report.json",
                dbPath + uuid + "-record.json",
                dbPath + uuid + "-attachment.json",
                dbPath + uuid + "-report.json",
                expectedSize);

        // WHEN
        String json = BacktraceSerializeHelper.toJson(obj);

        // THEN
        String expectedJson = TestUtils.readFileAsString(this, JSON_FILE);
        compareJson(expectedJson, json);
    }

    @Test
    public void deserialize() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, JSON_FILE);
        // WHEN
        final BacktraceDatabaseRecord obj = BacktraceSerializeHelper.fromJson(json, BacktraceDatabaseRecord.class);
        // THEN
        assertEquals(dbPath + uuid + "-record.json", obj.getRecordPath());
        assertEquals(expectedSize, obj.getSize());
        assertEquals(dbPath + uuid + "-attachment.json", obj.getDiagnosticDataPath());
        assertNull(obj.getBacktraceData());
    }

    @Test
    public void serializeAndDeserialize() {
        // GIVEN
        final BacktraceDatabaseRecord obj = new BacktraceDatabaseRecord(
                uuid,
                dbPath + uuid + "-report.json",
                dbPath + uuid + "-record.json",
                dbPath + uuid + "-attachment.json",
                dbPath + uuid + "-report.json",
                expectedSize);
        String json = BacktraceSerializeHelper.toJson(obj);

        // WHEN
        final BacktraceDatabaseRecord output = BacktraceSerializeHelper.fromJson(json, BacktraceDatabaseRecord.class);

        // THEN
        assertEquals(dbPath + uuid + "-record.json", output.getRecordPath());
        assertEquals(expectedSize, output.getSize());
        assertEquals(dbPath + uuid + "-attachment.json", output.getDiagnosticDataPath());
        assertNull(output.getBacktraceData());
    }
}
