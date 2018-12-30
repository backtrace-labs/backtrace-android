package backtraceio.library.interfaces;

import android.os.AsyncTask;

import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;

public interface IBacktraceClient {
    BacktraceResult send(BacktraceReport report);
    AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceReport report);
}
