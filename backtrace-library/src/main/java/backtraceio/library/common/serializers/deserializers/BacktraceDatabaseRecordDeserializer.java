package backtraceio.library.common.serializers.deserializers;

import org.json.JSONException;
import org.json.JSONObject;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;

public class BacktraceDatabaseRecordDeserializer implements Deserializable<BacktraceDatabaseRecord>{

    public BacktraceDatabaseRecord deserialize(JSONObject obj) throws JSONException {
        return new BacktraceDatabaseRecord(); // TODO
    }
}
