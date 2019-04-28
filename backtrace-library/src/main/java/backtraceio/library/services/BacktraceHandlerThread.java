package backtraceio.library.services;

import android.os.HandlerThread;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.List;
import java.util.UUID;

import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceHandlerThread extends HandlerThread {

    private BacktraceHandler mHandler;
    private String url;
    private OnServerResponseEventListener serverResponseEventListener;

    public BacktraceHandlerThread(String name, String url, OnServerResponseEventListener serverResponseEventListener) {
        super(name);
        this.url = url;
        this.serverResponseEventListener = serverResponseEventListener;
        this.start();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new BacktraceHandler(this.getLooper(), this.url, serverResponseEventListener);
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
        OnServerResponseEventListener serverResponseEventListener;

        private BacktraceHandler(Looper looper, String url, OnServerResponseEventListener serverResponseEventListener) {
            super(looper);
            this.url = url;
            this.serverResponseEventListener = serverResponseEventListener;
        }

        @Override
        public void handleMessage(Message msg) {
            BacktraceHandlerInput mInput = (BacktraceHandlerInput) msg.obj;
            BacktraceReportSender.sendReport(url, mInput.json, mInput.attachments, mInput.report);
        }
    }
}
