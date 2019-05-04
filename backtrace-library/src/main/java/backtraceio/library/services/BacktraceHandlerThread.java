package backtraceio.library.services;

import android.os.HandlerThread;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.List;
import java.util.UUID;

import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceHandlerThread extends HandlerThread {

    private BacktraceHandler mHandler;
    private String url;

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

    public void sendReport(String json, List<String>
            attachments, BacktraceReport report, OnServerResponseEventListener serverResponseEventListener, OnServerErrorEventListener serverErrorEventListener) {

        Message message = new Message();
        message.obj = new BacktraceHandlerInput(json, attachments, report, serverResponseEventListener, serverErrorEventListener);
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
            BacktraceResult result = BacktraceReportSender.sendReport(url, mInput.json, mInput.attachments, mInput.report, mInput.serverErrorEventListener);

            if(mInput.serverResponseEventListener != null) {
                    mInput.serverResponseEventListener.onEvent(result);
            }
        }
    }
}
