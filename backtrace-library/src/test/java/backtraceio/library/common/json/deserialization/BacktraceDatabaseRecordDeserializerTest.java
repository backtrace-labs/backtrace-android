package backtraceio.library.common.json.deserialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.UUID;

import backtraceio.library.TestUtils;
import backtraceio.library.common.json.serialization.BacktraceOrgJsonDeserializer;
import backtraceio.library.models.database.BacktraceDatabaseRecord;

public class BacktraceDatabaseRecordDeserializerTest {
    @Test
    public void deserializeDatabaseRecord() {
        // GIVEN
        String json = TestUtils.readFileAsString(this, "backtraceDatabaseRecord.json");

        // WHEN
        BacktraceDatabaseRecord result = BacktraceOrgJsonDeserializer.deserialize(json, BacktraceDatabaseRecord.class);

        // THEN
        assertNotNull(result);
        assertEquals(25362, result.getSize());
        assertFalse(result.locked);
        assertEquals(UUID.fromString("ecdf418b-3e22-4c7c-8011-c85dc2b4386f"), result.id);
        assertEquals("/data/user/0/backtraceio.library.test/files/ecdf418b-3e22-4c7c-8011-c85dc2b4386f-attachment.json", result.getDiagnosticDataPath());
        assertEquals("/data/user/0/backtraceio.library.test/files/ecdf418b-3e22-4c7c-8011-c85dc2b4386f-record.json", result.getRecordPath());
        assertEquals("/data/user/0/backtraceio.library.test/files/ecdf418b-3e22-4c7c-8011-c85dc2b4386f-report.json", result.getReportPath());
    }
}
