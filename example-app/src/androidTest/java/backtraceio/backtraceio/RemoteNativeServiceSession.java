package backtraceio.backtraceio;

import static org.junit.Assert.fail;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Instrumentation-side session around one debug-only native qualification service running in its
 * own process. Bundles carry only strings, ints, booleans, and longs; a reported
 * {@code EVENT_FAILED} fails immediately with the service's safe error class and scenario instead
 * of waiting for a timeout.
 */
final class RemoteNativeServiceSession implements AutoCloseable {

    private static final class Event {
        final int what;
        final Bundle data;

        Event(int what, Bundle data) {
            this.what = what;
            this.data = data;
        }
    }

    private final Context context;
    private final ServiceConnection connection;
    private final Messenger remote;
    private final Messenger replyMessenger;
    private final HandlerThread replyThread;
    private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
    private final CountDownLatch binderDeath = new CountDownLatch(1);
    private final AtomicInteger remotePid = new AtomicInteger(-1);

    private RemoteNativeServiceSession(
            Context context, ServiceConnection connection, IBinder binder, HandlerThread replyThread) {
        this.context = context;
        this.connection = connection;
        this.remote = new Messenger(binder);
        this.replyThread = replyThread;
        Handler replyHandler = new Handler(replyThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                Bundle data = message.getData() == null ? new Bundle() : new Bundle(message.getData());
                if (data.containsKey(NativeTestProtocol.KEY_PID)) {
                    remotePid.set(data.getInt(NativeTestProtocol.KEY_PID));
                }
                events.add(new Event(message.what, data));
            }
        };
        this.replyMessenger = new Messenger(replyHandler);
        try {
            binder.linkToDeath(binderDeath::countDown, 0);
        } catch (RemoteException alreadyDead) {
            binderDeath.countDown();
        }
    }

    static RemoteNativeServiceSession bind(Context context, Class<? extends Service> serviceClass, long timeoutMs) {
        final CountDownLatch connected = new CountDownLatch(1);
        final IBinder[] binderHolder = new IBinder[1];

        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binderHolder[0] = service;
                connected.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // Binder death is tracked through the DeathRecipient.
            }
        };

        Intent intent = new Intent(context, serviceClass);
        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            fail("Unable to bind " + serviceClass.getSimpleName());
        }
        try {
            if (!connected.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                context.unbindService(connection);
                fail("Timed out binding " + serviceClass.getSimpleName());
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            fail("Interrupted while binding " + serviceClass.getSimpleName());
        }

        HandlerThread replyThread = new HandlerThread(serviceClass.getSimpleName() + "-replies");
        replyThread.start();
        return new RemoteNativeServiceSession(context, connection, binderHolder[0], replyThread);
    }

    Bundle request(int command, Bundle data, int expectedEvent, long timeoutMs) {
        send(command, data);
        return awaitEvent(expectedEvent, timeoutMs);
    }

    void send(int command, Bundle data) {
        Message message = Message.obtain(null, command);
        message.replyTo = replyMessenger;
        if (data != null) {
            message.setData(data);
        }
        try {
            remote.send(message);
        } catch (RemoteException failure) {
            fail("Remote service is unreachable for command " + command);
        }
    }

    Bundle awaitEvent(int expectedEvent, long timeoutMs) {
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        while (true) {
            long remaining = deadline - SystemClock.elapsedRealtime();
            if (remaining <= 0) {
                fail("Timed out waiting for event " + expectedEvent);
            }
            Event event;
            try {
                event = events.poll(remaining, TimeUnit.MILLISECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                fail("Interrupted waiting for event " + expectedEvent);
                return null;
            }
            if (event == null) {
                fail("Timed out waiting for event " + expectedEvent);
                return null;
            }
            if (event.what == NativeTestProtocol.EVENT_FAILED) {
                fail("Remote service reported failure: "
                        + event.data.getString(NativeTestProtocol.KEY_ERROR_TYPE) + " in scenario "
                        + event.data.getString(NativeTestProtocol.KEY_SCENARIO));
            }
            if (event.what == expectedEvent) {
                return event.data;
            }
            // Unrelated event (for example a stale COMPLETED); keep waiting for the expected one.
        }
    }

    void awaitBinderDeath(long timeoutMs) {
        try {
            if (!binderDeath.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                fail("Remote service process did not die within " + timeoutMs + " ms");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            fail("Interrupted waiting for remote process death");
        }
    }

    int getRemotePid() {
        return remotePid.get();
    }

    @Override
    public void close() {
        try {
            context.unbindService(connection);
        } catch (IllegalArgumentException alreadyUnbound) {
            // The connection died with the remote process; nothing to unbind.
        }
        replyThread.quitSafely();
    }
}
