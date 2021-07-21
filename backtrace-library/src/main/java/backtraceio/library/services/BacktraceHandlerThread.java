package backtraceio.library.services;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.List;
import java.util.Map;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.metrics.BacktraceHandlerInputEvents;
import backtraceio.library.metrics.SummedEventsHandler;
import backtraceio.library.metrics.SummedEventsPayload;
import backtraceio.library.metrics.UniqueEventsHandler;
import backtraceio.library.metrics.UniqueEventsPayload;
import backtraceio.library.models.BacktraceResult;

public class BacktraceHandlerThread extends HandlerThread {

    private static transient String LOG_TAG = BacktraceHandlerThread.class.getSimpleName();

    private BacktraceHandler mHandler;
    private String url;
    private UniqueEventsHandler mUniqueEventsHandler;
    private SummedEventsHandler mSummedEventsHandler;

    BacktraceHandlerThread(String name, String url) {
        super(name);
        this.url = url;
        this.start();
    }

    UniqueEventsHandler createUniqueEventsHandler(Context context, Map<String, Object> customAttributes, String universeName, String token, Api api, final long timeIntervalMillis, int timeBetweenRetriesMillis) {
        this.mUniqueEventsHandler = new UniqueEventsHandler(context, customAttributes, this, universeName, token, api, timeIntervalMillis, timeBetweenRetriesMillis);
        return mUniqueEventsHandler;
    }

    SummedEventsHandler createSummedEventsHandler(Context context, Map<String, Object> customAttributes, String universeName, String token, Api api, final long timeIntervalMillis, int timeBetweenRetriesMillis) {
        this.mSummedEventsHandler = new SummedEventsHandler(context, customAttributes, this, universeName, token, api, timeIntervalMillis, timeBetweenRetriesMillis);
        return mSummedEventsHandler;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new BacktraceHandler(this.getLooper(), this.url);
    }

    void sendReport(BacktraceHandlerInputReport data) {
        send(data);
    }

    void send(BacktraceHandlerInput data) {
        Message message = new Message();
        message.obj = data;
        if (data instanceof BacktraceHandlerInputReport) {
            mHandler.sendMessage(message);
        } else if (data instanceof BacktraceHandlerInputEvents) {
            if (((BacktraceHandlerInputEvents) data).payload instanceof UniqueEventsPayload &&
                    mUniqueEventsHandler != null) {
                mUniqueEventsHandler.sendMessage(message);
            } else if (((BacktraceHandlerInputEvents) data).payload instanceof SummedEventsPayload &&
                    mSummedEventsHandler != null) {
                mSummedEventsHandler.sendMessage(message);
            } else {
                BacktraceLogger.e(LOG_TAG, "BacktraceHandlerInput payload of type " + ((BacktraceHandlerInputEvents) data).payload.getClass().toString() + " not handled");
            }
        } else {
            BacktraceLogger.e(LOG_TAG, "BacktraceHandlerInput of type " + data.getClass().toString() + " not handled");
        }
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
