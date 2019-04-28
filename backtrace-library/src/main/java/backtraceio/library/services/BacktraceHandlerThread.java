package backtraceio.library.services;

import android.os.HandlerThread;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceHandlerThread extends HandlerThread {

    BacktraceHandler mHandler;
    String url;

    public BacktraceHandlerThread(String name, String url) {
        super(name);
        this.url = url;
        this.start();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new BacktraceHandler(this.getLooper(), this.url);
    }

    public void sendReport(UUID requestId, String json, List<String>
            attachments, BacktraceReport report) {

        BacktraceHandlerInput mInput = new BacktraceHandlerInput(requestId, json, attachments, report);
        Message message = new Message();
        message.obj = mInput;
        mHandler.sendMessage(message);
    }

    private class BacktraceHandler extends Handler {
        String url;

        private BacktraceHandler(Looper looper, String url) {
            super(looper);
            this.url = url;
        }

        @Override
        public void handleMessage(Message msg) {
            BacktraceHandlerInput mInput = (BacktraceHandlerInput) msg.obj;
            BacktraceReportSender.sendReport(url, mInput.json, mInput.attachments, mInput.report);
        }
    }
}
