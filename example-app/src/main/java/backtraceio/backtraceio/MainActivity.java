package backtraceio.backtraceio;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceExceptionHandler;
import backtraceio.library.models.database.BacktraceDatabaseSettings;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BacktraceCredentials credentials =
                new BacktraceCredentials("<endpoint-url>", "<token>");

        Context context = getApplicationContext();
        String dbPath = context.getFilesDir().getAbsolutePath();

        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(dbPath);
        settings.setMaxRecordCount(100);
        settings.setMaxDatabaseSize(1000);
        settings.setRetryBehavior(RetryBehavior.ByInterval);
        settings.setAutoSendMode(true);
        settings.setRetryOrder(RetryOrder.Queue);

        BacktraceDatabase database = new BacktraceDatabase(context, settings);
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database);

        BacktraceExceptionHandler.enable(backtraceClient);
        backtraceClient.send("test");
    }
}