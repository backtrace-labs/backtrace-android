package backtraceio.library.services;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.List;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceResult;

public class BacktraceHandlerThread extends HandlerThread {

    private BacktraceHandler mHandler;
    private String url;

    BacktraceHandlerThread(String name, String url) {
        super(name);
        this.url = url;
        this.start();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new BacktraceHandler(this.getLooper(), this.url);
    }

    void sendReport(BacktraceHandlerInput data) {

        Message message = new Message();
        message.obj = data;
        mHandler.sendMessage(message);
    }

    private class BacktraceHandler extends Handler {
        private final transient String LOG_TAG = BacktraceHandler.class.getSimpleName();
        String url;

        private BacktraceHandler(Looper looper, String url) {
            super(looper);
            this.url = url;
        }

        @Override
        public void handleMessage(Message msg) {
            BacktraceHandlerInput mInput = (BacktraceHandlerInput) msg.obj;
            BacktraceResult result;

            if (mInput.requestHandler != null) {
                BacktraceLogger.d(LOG_TAG, "Sending using custom request handler");
                result = mInput.requestHandler.onRequest(mInput.data);
            } else {
                BacktraceLogger.d(LOG_TAG, "Sending report using default request handler");
                String json = BacktraceSerializeHelper.toJson(mInput.data);
                List<String> attachments = mInput.data.getAttachments();
                result = BacktraceReportSender.sendReport(url, json, attachments, mInput.data
                                .report,
                        mInput.serverErrorEventListener);
            }

            if (mInput.serverResponseEventListener != null) {
                BacktraceLogger.d(LOG_TAG, "Processing result using custom event");
                mInput.serverResponseEventListener.onEvent(result);
            }
        }
    }
}
