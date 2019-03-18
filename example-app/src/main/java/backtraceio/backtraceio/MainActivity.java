package backtraceio.backtraceio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceExceptionHandler;
import backtraceio.library.models.database.BacktraceDatabaseSettings;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BacktraceCredentials credentials =
                new BacktraceCredentials("<endpoint-url>", "<token>");


        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings("/");
        BacktraceDatabase database = new BacktraceDatabase(settings);
        BacktraceClient backtraceClient = new BacktraceClient(getApplicationContext(), credentials, database);
        BacktraceExceptionHandler.enable(backtraceClient);
        backtraceClient.send("test");
    }
}
