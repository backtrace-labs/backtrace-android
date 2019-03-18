package backtraceio.backtraceio;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceExceptionHandler;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BacktraceCredentials credentials =
                new BacktraceCredentials("<endpoint-url>", "<token>");


        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings("/data/user/0/backtraceio.backtraceio/files/");
        Context context = getApplicationContext();
        BacktraceDatabase database = new BacktraceDatabase(context, settings);
        Iterable<BacktraceDatabaseRecord> records = database.get();
        for(BacktraceDatabaseRecord r : records){
            Log.d("Test", r.Id.toString());
        }
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database);
        BacktraceExceptionHandler.enable(backtraceClient);
        backtraceClient.send("test");
    }
}
