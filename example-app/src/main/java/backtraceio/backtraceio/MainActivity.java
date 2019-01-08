package backtraceio.backtraceio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.models.BacktraceExceptionHandler;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BacktraceCredentials credentials =
                new BacktraceCredentials("<endpoint-url>", "<token>");
        BacktraceClient backtraceClient = new BacktraceClient(getApplicationContext(), credentials);
        BacktraceExceptionHandler.enable(backtraceClient);
        Object x = null;
        x.toString();
    }
}
