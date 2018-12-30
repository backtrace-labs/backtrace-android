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
                new BacktraceCredentials("https://yolo.sp.backtrace.io:6098/",
                        "2dd86e8e779d1fc7e22e7b19a9489abeedec3b1426abe7e2209888e92362fba4");
        BacktraceClient backtraceClient = new BacktraceClient(getApplicationContext(), credentials);
        new BacktraceExceptionHandler(getApplicationContext(), backtraceClient);
        Object x = null;
        x.toString();
    }
}
