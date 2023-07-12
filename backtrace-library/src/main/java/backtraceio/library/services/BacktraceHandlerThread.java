package backtraceio.library.services;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.List;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceNativeData;
import backtraceio.library.models.BacktraceResult;

public class BacktraceHandlerThread extends HandlerThread {

    private static final transient String LOG_TAG = BacktraceHandlerThread.class.getSimpleName();

    private BacktraceHandler mHandler;
    private final String url;
    private UniqueEventsHandler mUniqueEventsHandler;
    private SummedEventsHandler mSummedEventsHandler;

    BacktraceHandlerThread(String name, String url) {
        super(name);
        this.url = url;
        this.start();
    }

    UniqueEventsHandler createUniqueEventsHandler(BacktraceMetrics backtraceMetrics, Api api) {
        this.mUniqueEventsHandler = new UniqueEventsHandler(backtraceMetrics, api, this);
        return mUniqueEventsHandler;
    }

    SummedEventsHandler createSummedEventsHandler(BacktraceMetrics backtraceMetrics, Api api) {
        this.mSummedEventsHandler = new SummedEventsHandler(backtraceMetrics, api, this);
        return mSummedEventsHandler;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        if (mHandler == null) {
            mHandler = new BacktraceHandler(this.getLooper(), this.url);
        }
    }

    Message createMessage(BacktraceHandlerInput data) {
        Message message = new Message();
        message.obj = data;
        return message;
    }

    void sendReport(BacktraceHandlerInputReport data) {
        // Sometimes, sendReport gets called before the Looper is ready.
        // getLooper will wait for the Looper to be ready: https://stackoverflow.com/questions/30300555/android-what-happens-after-a-handlerthread-is-started
        if (mHandler == null) {
            mHandler = new BacktraceHandler(this.getLooper(), this.url);
        }
        mHandler.sendMessage(createMessage(data));
    }

    void sendUniqueEvents(BacktraceHandlerInputEvents data) {
        mUniqueEventsHandler.sendMessage(createMessage(data));
    }

    void sendSummedEvents(BacktraceHandlerInputEvents data) {
        mSummedEventsHandler.sendMessage(createMessage(data));
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
            BacktraceHandlerInputReport mInput = (BacktraceHandlerInputReport) msg.obj;
            BacktraceResult result;
            if (mInput.requestHandler != null) {
                BacktraceLogger.d(LOG_TAG, "Sending using custom request handler");
                if (mInput.data.report.message == "" && mInput.data.containsMinidump()) {
                    BacktraceLogger.d(LOG_TAG, "Minidump report detected");
                    result = mInput.requestHandler.onNativeRequest(new BacktraceNativeData(mInput.data.report));
                } else
                    result = mInput.requestHandler.onRequest(mInput.data);
            } else {
                BacktraceLogger.d(LOG_TAG, "Sending report using default request handler");
                String json = BacktraceSerializeHelper.toJson(mInput.data);
                List<String> attachments = mInput.data.getAttachments();
                result = BacktraceReportSender.sendReport(url, json, attachments, mInput.data.report,
                        mInput.serverErrorEventListener);
            }

            if (mInput.serverResponseEventListener != null) {
                BacktraceLogger.d(LOG_TAG, "Processing result using custom event");
                mInput.serverResponseEventListener.onEvent(result);
            }
        }
    }
}
