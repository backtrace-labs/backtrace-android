package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;

public class BacktraceDatabaseRecordDeserializer implements Deserializable<BacktraceDatabaseRecord> {

    private final FieldNameLoader fieldNameLoader = new FieldNameLoader(BacktraceDatabaseRecord.class); // TODO: maybe we can reuse it
    static class Fields {
        final static String id = "id";
        final static String path = "path";
        final static String recordPath = "recordPath";
        final static String diagnosticDataPath = "diagnosticDataPath";
        final static String reportPath = "reportPath";
        final static String size = "size";
    }

    public BacktraceDatabaseRecord deserialize(JSONObject obj) throws JSONException {
        return new BacktraceDatabaseRecord(
                obj.optString(fieldNameLoader.get(Fields.id)),
                obj.optString(fieldNameLoader.get(Fields.path)),
                obj.optString(fieldNameLoader.get(Fields.recordPath)),
                obj.optString(fieldNameLoader.get(Fields.diagnosticDataPath)),
                obj.optString(fieldNameLoader.get(Fields.reportPath)),
                obj.optLong(fieldNameLoader.get(Fields.size))
        );
    }
}
