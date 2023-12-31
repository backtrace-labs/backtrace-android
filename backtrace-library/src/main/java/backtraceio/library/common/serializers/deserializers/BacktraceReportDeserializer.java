package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceReportDeserializer implements Deserializable<BacktraceReport>{

    public BacktraceReport deserialize(JSONObject obj) throws JSONException {
//        return new BacktraceReport(); // TODO
        return null; // TODO:
    }
}
