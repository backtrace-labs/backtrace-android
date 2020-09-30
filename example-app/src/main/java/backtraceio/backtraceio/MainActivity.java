package backtraceio.backtraceio;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceExceptionHandler;
import backtraceio.library.models.database.BacktraceDatabaseSettings;

public class MainActivity extends AppCompatActivity {
    BacktraceClient backtraceClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BacktraceCredentials credentials = new BacktraceCredentials(
                "https://yolo.sp.backtrace.io:6098",
                "533c6e267998b8562e4b878c891bf7fc509beec7839f991bdaa1d43220d0f497"
        );

        Context context = getApplicationContext();
        String dbPath = context.getFilesDir().getAbsolutePath();

        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(dbPath);
        settings.setMaxRecordCount(100);
        settings.setMaxDatabaseSize(1000);
        settings.setRetryBehavior(RetryBehavior.ByInterval);
        settings.setAutoSendMode(true);
        settings.setRetryOrder(RetryOrder.Queue);

        BacktraceDatabase database = new BacktraceDatabase(context, settings);
        backtraceClient = new BacktraceClient(context, credentials, database);
        database.setupNativeIntegration(backtraceClient, credentials);

        BacktraceExceptionHandler.enable(backtraceClient);
        backtraceClient.send("test");


        Button button = (Button) findViewById(R.id.button1);

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                backtraceClient.nativeCrash();
            }
        });
    }
}
