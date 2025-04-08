package backtraceio.backtraceio;

import android.content.Context;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.anr.AnrType;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.models.BacktraceExceptionHandler;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;

public class MainActivity extends AppCompatActivity {

    private BacktraceClient backtraceClient;
    private OnServerResponseEventListener listener;
    private final int anrTimeout = 3000;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public void setOnServerResponseEventListener(OnServerResponseEventListener e) {
        this.listener = e;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backtraceClient = initializeBacktrace(BuildConfig.BACKTRACE_SUBMISSION_URL);

        symlinkAndWriteFile();
    }

    /**
     * Example of how one would link a static filename to a more dynamic one, since you can only specify
     * the attachment to upload for a native crash once at startup
     */
    private void symlinkAndWriteFile() {
        Context context = getApplicationContext();
        final String fileName = context.getFilesDir() + "/" + "myCustomFile.txt";
        final String fileNameDateString = context.getFilesDir() + "/" + "myCustomFile06_11_2021.txt";
        try {
            Os.symlink(fileNameDateString, fileName);
        } catch (ErrnoException e) {
            e.printStackTrace();
        }
        writeMyCustomFile(fileNameDateString);
    }

    private BacktraceClient initializeBacktrace(final String submissionUrl) {



//        String weirdString = "\n----- pid 9207 at 2025-03-27 21:02:38 -----\nCmd line: backtraceio.backtraceio\nBuild fingerprint: \u0027google/sdk_gphone_x86/generic_x86_arm:11/RSR1.201013.001/6903271:user/release-keys\u0027\nABI: \u0027x86\u0027\nBuild type: optimized\nZygote loaded classes\u003d15747 post zygote classes\u003d1481\nDumping registered class loaders\n#0 dalvik.system.PathClassLoader: [], parent #1\n#1 java.lang.BootClassLoader: [], no parent\n#2 dalvik.system.PathClassLoader: [/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes15.dex:/data/data/backtraceio.backtraceio/code_cache/.overlay/base.apk/classes16.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes10.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes8.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes2.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes4.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes3.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes17.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes11.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes14.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes9.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes7.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes12.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes6.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes13.dex:/data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!classes5.dex], parent #1\nDone dumping class loaders\nClasses initialized: 760 in 140.777ms\nIntern table: 32164 strong; 519 weak\nJNI: CheckJNI is on; globals\u003d640 (plus 60 weak)\nLibraries: /data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!/lib/x86/libbacktrace-native.so /data/app/~~lbMB2_9XcyZkiC41RLVuMg\u003d\u003d/backtraceio.backtraceio-1XRWT_mS3AbJ9vbjbaCtkw\u003d\u003d/base.apk!/lib/x86/libnative-lib.so libandroid.so libaudioeffect_jni.so libcompiler_rt.so libicu_jni.so libjavacore.so libjavacrypto.so libjnigraphics.so libmedia_jni.so libopenjdk.so librs_jni.so libsfplugin_ccodec.so libsoundpool.so libstats_jni.so libwebviewchromium_loader.so (16)\nHeap: 74% free, 6892KB/25MB; 138095 objects\nDumping cumulative Gc timings\nAverage major GC reclaim bytes ratio inf over 0 GC cycles\nAverage major GC copied live bytes ratio 0.72272 over 4 major GCs\nCumulative bytes moved 11312128\nCumulative objects moved 212604\nPeak regions allocated 27 (6912KB) / 768 (192MB)\nStart Dumping histograms for 1 iterations for young concurrent copying\nGrayAllDirtyImmuneObjects:\tSum: 12.004ms 99% C.I. 12.004ms-12.004ms Avg: 12.004ms Max: 12.004ms\nInitializePhase:\tSum: 11.934ms 99% C.I. 11.934ms-11.934ms Avg: 11.934ms Max: 11.934ms\nProcessMarkStack:\tSum: 9.977ms 99% C.I. 9.977ms-9.977ms Avg: 9.977ms Max: 9.977ms\nScanImmuneSpaces:\tSum: 4.691ms 99% C.I. 4.691ms-4.691ms Avg: 4.691ms Max: 4.691ms\nEnqueueFinalizerReferences:\tSum: 4.233ms 99% C.I. 4.233ms-4.233ms Avg: 4.233ms Max: 4.233ms\nScanCardsForSpace:\tSum: 3.451ms 99% C.I. 3.451ms-3.451ms Avg: 3.451ms Max: 3.451ms\nVisitConcurrentRoots:\tSum: 3.005ms 99% C.I. 3.005ms-3.005ms Avg: 3.005ms Max: 3.005ms\nResetStack:\tSum: 1.680ms 99% C.I. 1.680ms-1.680ms Avg: 1.680ms Max: 1.680ms\nSweepSystemWeaks:\tSum: 1.050ms 99% C.I. 1.050ms-1.050ms Avg: 1.050ms Max: 1.050ms\nClearFromSpace:\tSum: 423us 99% C.I. 423us-423us Avg: 423us Max: 423us\nFlipOtherThreads:\tSum: 333us 99% C.I. 333us-333us Avg: 333us Max: 333us\nForwardSoftReferences:\tSum: 289us 99% C.I. 289us-289us Avg: 289us Max: 289us\nSweepArray:\tSum: 202us 99% C.I. 202us-202us Avg: 202us Max: 202us\nEmptyRBMarkBitStack:\tSum: 121us 99% C.I. 121us-121us Avg: 121us Max: 121us\nVisitNonThreadRoots:\tSum: 42us 99% C.I. 42us-42us Avg: 42us Max: 42us\nFlipThreadRoots:\tSum: 38us 99% C.I. 38us-38us Avg: 38us Max: 38us\nProcessReferences:\tSum: 26us 99% C.I. 1us-25us Avg: 13us Max: 25us\n(Paused)GrayAllNewlyDirtyImmuneObjects:\tSum: 25us 99% C.I. 25us-25us Avg: 25us Max: 25us\n(Paused)ClearCards:\tSum: 19us 99% C.I. 0.250us-13us Avg: 1.461us Max: 13us\nCopyingPhase:\tSum: 17us 99% C.I. 17us-17us Avg: 17us Max: 17us\nMarkZygoteLargeObjects:\tSum: 16us 99% C.I. 16us-16us Avg: 16us Max: 16us\nThreadListFlip:\tSum: 14us 99% C.I. 14us-14us Avg: 14us Max: 14us\nSwapBitmaps:\tSum: 9us 99% C.I. 9us-9us Avg: 9us Max: 9us\nFreeList:\tSum: 6us 99% C.I. 6us-6us Avg: 6us Max: 6us\nResumeRunnableThreads:\tSum: 4us 99% C.I. 4us-4us Avg: 4us Max: 4us\n(Paused)FlipCallback:\tSum: 2us 99% C.I. 2us-2us Avg: 2us Max: 2us\n(Paused)SetFromSpace:\tSum: 1us 99% C.I. 1us-1us Avg: 1us Max: 1us\nDone Dumping histograms\nyoung concurrent copying paused:\tSum: 63us 99% C.I. 63us-63us Avg: 63us Max: 63us\nyoung concurrent copying freed-bytes: Avg: 1905KB Max: 1905KB Min: 1905KB\nFreed-bytes histogram: 1600:1\nyoung concurrent copying total time: 53.653ms mean time: 53.653ms\nyoung concurrent copying freed: 25923 objects with total size 1905KB\nyoung concurrent copying throughput: 489113/s / 35MB/s  per cpu-time: 47582634/s / 45MB/s\nAverage minor GC reclaim bytes ratio 0.92539 over 1 GC cycles\nAverage minor GC copied live bytes ratio 0.133529 over 2 minor GCs\nCumulative bytes moved 870264\nCumulative objects moved 16602\nPeak regions allocated 27 (6912KB) / 768 (192MB)\nTotal time spent in GC: 53.653ms\nMean GC size throughput: 34MB/s per cpu-time: 44MB/s\nMean GC object throughput: 483160 objects/s\nTotal number of allocations 164018\nTotal bytes allocated 8797KB\nTotal bytes freed 1905KB\nFree memory 19MB\nFree memory until GC 19MB\nFree memory until OOME 185MB\nTotal memory 25MB\nMax memory 192MB\nZygote space size 2980KB\nTotal mutator paused time: 63us\nTotal time waiting for GC to complete: 4.459us\nTotal GC count: 1\nTotal GC time: 53.653ms\nTotal blocking GC count: 0\nTotal blocking GC time: 0\nNative bytes total: 15962696 registered: 108600\nTotal native bytes at last GC: 4019588\n/system/framework/oat/x86/android.hidl.manager-V1.0-java.odex: quicken\n/system/framework/oat/x86/android.test.base.odex: quicken\n/system/framework/oat/x86/android.hidl.base-V1.0-java.odex: quicken\nCurrent JIT code cache size (used / resident): 52KB / 60KB\nCurrent JIT data cache size (used / resident): 61KB / 96KB\nZygote JIT code cache size (at point of fork): 42KB / 44KB\nZygote JIT data cache size (at point of fork): 31KB / 36KB\nCurrent JIT mini-debug-info size: 50KB\nCurrent JIT capacity: 128KB\nCurrent number of JIT JNI stub entries: 2\nCurrent number of JIT code cache entries: 207\nTotal number of JIT compilations: 172\nTotal number of JIT compilations for on stack replacement: 7\nTotal number of JIT code cache collections: 1\nMemory used for stack maps: Avg: 85B Max: 660B Min: 12B\nMemory used for compiled code: Avg: 324B Max: 2604B Min: 17B\nMemory used for profiling info: Avg: 74B Max: 764B Min: 20B\nStart Dumping histograms for 212 iterations for JIT timings\nCompiling:\tSum: 540.855ms 99% C.I. 0.030ms-21.292ms Avg: 2.563ms Max: 24.444ms\nTrimMaps:\tSum: 77.018ms 99% C.I. 1.850us-4245us Avg: 365.014us Max: 9813us\nCode cache collection:\tSum: 3.847ms 99% C.I. 3.847ms-3.847ms Avg: 3.847ms Max: 3.847ms\nDone Dumping histograms\nMemory used for compilation: Avg: 36KB Max: 322KB Min: 0B\nProfileSaver total_bytes_written\u003d4974\nProfileSaver total_number_of_writes\u003d1\nProfileSaver total_number_of_code_cache_queries\u003d1\nProfileSaver total_number_of_skipped_writes\u003d0\nProfileSaver total_number_of_failed_writes\u003d0\nProfileSaver total_ms_of_sleep\u003d45000\nProfileSaver total_ms_of_work\u003d10\nProfileSaver total_number_of_hot_spikes\u003d4\nProfileSaver total_number_of_wake_ups\u003d3\n\nsuspend all histogram:\tSum: 301us 99% C.I. 0.090us-104us Avg: 11.148us Max: 104us\nDALVIK THREADS (20):\n\"Signal Catcher\" daemon prio\u003d10 tid\u003d4 Runnable\n  | group\u003d\"system\" sCount\u003d0 dsCount\u003d0 flags\u003d0 obj\u003d0x13040b28 self\u003d0xe738a810\n  | sysTid\u003d9216 nice\u003d-20 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xdc5da1e0\n  | state\u003dR schedstat\u003d( 17573906 201748 7 ) utm\u003d0 stm\u003d1 core\u003d0 HZ\u003d100\n  | stack\u003d0xdc4df000-0xdc4e1000 stackSize\u003d1008KB\n  | held mutexes\u003d \"mutator lock\"(shared held)\n  native: #00 pc 00542d9e  /apex/com.android.art/lib/libart.so (art::DumpNativeStack(std::__1::basic_ostream\u003cchar, std::__1::char_traits\u003cchar\u003e \u003e\u0026, int, BacktraceMap*, char const*, art::ArtMethod*, void*, bool)+110)\n  native: #01 pc 006a0897  /apex/com.android.art/lib/libart.so (art::Thread::DumpStack(std::__1::basic_ostream\u003cchar, std::__1::char_traits\u003cchar\u003e \u003e\u0026, bool, BacktraceMap*, bool) const+1015)\n  native: #02 pc 0069a171  /apex/com.android.art/lib/libart.so (art::Thread::Dump(std::__1::basic_ostream\u003cchar, std::__1::char_traits\u003cchar\u003e \u003e\u0026, bool, BacktraceMap*, bool) const+65)\n  native: #03 pc 006c61b4  /apex/com.android.art/lib/libart.so (art::DumpCheckpoint::Run(art::Thread*)+1172)\n  native: #04 pc 006bf266  /apex/com.android.art/lib/libart.so (art::ThreadList::RunCheckpoint(art::Closure*, art::Closure*)+630)\n  native: #05 pc 006be1ce  /apex/com.android.art/lib/libart.so (art::ThreadList::Dump(std::__1::basic_ostream\u003cchar, std::__1::char_traits\u003cchar\u003e \u003e\u0026, bool)+2446)\n  native: #06 pc 006bd70c  /apex/com.android.art/lib/libart.so (art::ThreadList::DumpForSigQuit(std::__1::basic_ostream\u003cchar, std::__1::char_traits\u003cchar\u003e \u003e\u0026)+1644)\n  native: #07 pc 0064d654  /apex/com.android.art/lib/libart.so (art::Runtime::DumpForSigQuit(std::__1::basic_ostream\u003cchar, std::__1::char_traits\u003cchar\u003e \u003e\u0026)+212)\n  native: #08 pc 00665b6a  /apex/com.android.art/lib/libart.so (art::SignalCatcher::HandleSigQuit()+1818)\n  native: #09 pc 0066496b  /apex/com.android.art/lib/libart.so (art::SignalCatcher::Run(void*)+587)\n  native: #10 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #11 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"main\" prio\u003d5 tid\u003d1 Native\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x72287300 self\u003d0xe7380e10\n  | sysTid\u003d9207 nice\u003d-10 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xf583f478\n  | state\u003dS schedstat\u003d( 44727379364 813394028 1292 ) utm\u003d1691 stm\u003d2781 core\u003d1 HZ\u003d100\n  | stack\u003d0xff020000-0xff022000 stackSize\u003d8192KB\n  | held mutexes\u003d\n  native: #00 pc 00000b97  [vdso] (__kernel_vsyscall+7)\n  native: #01 pc 0005ad68  /apex/com.android.runtime/lib/bionic/libc.so (syscall+40)\n  native: #02 pc 001d82ec  /apex/com.android.art/lib/libart.so (art::ConditionVariable::WaitHoldingLocks(art::Thread*)+108)\n  native: #03 pc 001d8273  /apex/com.android.art/lib/libart.so (art::ConditionVariable::Wait(art::Thread*)+35)\n  native: #04 pc 007894db  /apex/com.android.art/lib/libart.so (art::GoToRunnable(art::Thread*)+507)\n  native: #05 pc 007892a1  /apex/com.android.art/lib/libart.so (art::JniMethodEnd(unsigned int, art::Thread*)+33)\n  native: #06 pc 02008de3  /memfd:jit-cache (deleted) (offset 2000000) (art_jni_trampoline+163)\n  native: #07 pc 02008cee  /memfd:jit-cache (deleted) (offset 2000000) (backtraceio.backtraceio.MainActivity.handledException+46)\n  native: #08 pc 00142c04  /apex/com.android.art/lib/libart.so (art_quick_osr_stub+36)\n  native: #09 pc 003b138f  /apex/com.android.art/lib/libart.so (art::jit::Jit::MaybeDoOnStackReplacement(art::Thread*, art::ArtMethod*, unsigned int, int, art::JValue*)+415)\n  native: #10 pc 007b2bf9  /apex/com.android.art/lib/libart.so (MterpMaybeDoOnStackReplacement+185)\n  native: #11 pc 0013a2f5  /apex/com.android.art/lib/libart.so (MterpHelpers+294)\n  native: #12 pc 00001034  /data/data/backtraceio.backtraceio/code_cache/.overlay/base.apk/classes16.dex (backtraceio.backtraceio.MainActivity.handledException+8)\n  native: #13 pc 0036fb02  /apex/com.android.art/lib/libart.so (art::interpreter::Execute(art::Thread*, art::CodeItemDataAccessor const\u0026, art::ShadowFrame\u0026, art::JValue, bool, bool) (.llvm.16375758241455872412)+370)\n  native: #14 pc 00379b00  /apex/com.android.art/lib/libart.so (art::interpreter::EnterInterpreterFromEntryPoint(art::Thread*, art::CodeItemDataAccessor const\u0026, art::ShadowFrame*)+176)\n  native: #15 pc 0078b325  /apex/com.android.art/lib/libart.so (artQuickToInterpreterBridge+1061)\n  native: #16 pc 0014220d  /apex/com.android.art/lib/libart.so (art_quick_to_interpreter_bridge+77)\n  native: #17 pc 0013b922  /apex/com.android.art/lib/libart.so (art_quick_invoke_stub+338)\n  native: #18 pc 001d0381  /apex/com.android.art/lib/libart.so (art::ArtMethod::Invoke(art::Thread*, unsigned int*, unsigned int, art::JValue*, char const*)+241)\n  native: #19 pc 00630008  /apex/com.android.art/lib/libart.so (art::InvokeMethod(art::ScopedObjectAccessAlreadyRunnable const\u0026, _jobject*, _jobject*, _jobject*, unsigned int)+1464)\n  native: #20 pc 005886a0  /apex/com.android.art/lib/libart.so (art::Method_invoke(_JNIEnv*, _jobject*, _jobject*, _jobjectArray*)+80)\n  at java.lang.Thread.yield(Native method)\n  at backtraceio.backtraceio.MainActivity.handledException(MainActivity.java:157)\n  at java.lang.reflect.Method.invoke(Native method)\n  at androidx.appcompat.app.AppCompatViewInflater$DeclaredOnClickListener.onClick(AppCompatViewInflater.java:468)\n  at android.view.View.performClick(View.java:7448)\n  at android.view.View.performClickInternal(View.java:7425)\n  at android.view.View.access$3600(View.java:810)\n  at android.view.View$PerformClick.run(View.java:28305)\n  at android.os.Handler.handleCallback(Handler.java:938)\n  at android.os.Handler.dispatchMessage(Handler.java:99)\n  at android.os.Looper.loop(Looper.java:223)\n  at android.app.ActivityThread.main(ActivityThread.java:7656)\n  at java.lang.reflect.Method.invoke(Native method)\n  at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:592)\n  at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)\n\n\"ADB-JDWP Connection Control Thread\" daemon prio\u003d0 tid\u003d5 WaitingInMainDebuggerLoop\n  | group\u003d\"system\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040ba0 self\u003d0xe738c410\n  | sysTid\u003d9218 nice\u003d-20 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xdc3d81e0\n  | state\u003dS schedstat\u003d( 4694215 13986760 9 ) utm\u003d0 stm\u003d0 core\u003d1 HZ\u003d100\n  | stack\u003d0xdc2dd000-0xdc2df000 stackSize\u003d1008KB\n  | held mutexes\u003d\n  native: #00 pc 00000b99  [vdso] (__kernel_vsyscall+9)\n  native: #01 pc 000cf496  /apex/com.android.runtime/lib/bionic/libc.so (__ppoll+38)\n  native: #02 pc 00083979  /apex/com.android.runtime/lib/bionic/libc.so (poll+105)\n  native: #03 pc 0000a493  /apex/com.android.art/lib/libadbconnection.so (adbconnection::AdbConnectionState::RunPollLoop(art::Thread*)+1171)\n  native: #04 pc 000086d2  /apex/com.android.art/lib/libadbconnection.so (adbconnection::CallbackFunction(void*)+1666)\n  native: #05 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #06 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"perfetto_hprof_listener\" prio\u003d10 tid\u003d6 Native (still starting up)\n  | group\u003d\"\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x0 self\u003d0xe7385410\n  | sysTid\u003d9217 nice\u003d-20 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xdc4d91e0\n  | state\u003dS schedstat\u003d( 6280850 9339243 10 ) utm\u003d0 stm\u003d0 core\u003d0 HZ\u003d100\n  | stack\u003d0xdc3de000-0xdc3e0000 stackSize\u003d1008KB\n  | held mutexes\u003d\n  native: #00 pc 00000b97  [vdso] (__kernel_vsyscall+7)\n  native: #01 pc 000ccf9c  /apex/com.android.runtime/lib/bionic/libc.so (read+28)\n  native: #02 pc 0001aca2  /apex/com.android.art/lib/libperfetto_hprof.so (void* std::__1::__thread_proxy\u003cstd::__1::tuple\u003cstd::__1::unique_ptr\u003cstd::__1::__thread_struct, std::__1::default_delete\u003cstd::__1::__thread_struct\u003e \u003e, ArtPlugin_Initialize::$_29\u003e \u003e(void*)+306)\n  native: #03 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #04 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"HeapTaskDaemon\" daemon prio\u003d5 tid\u003d7 WaitingForTaskProcessor\n  | group\u003d\"system\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x130410c8 self\u003d0xe7384610\n  | sysTid\u003d9220 nice\u003d4 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc74921e0\n  | state\u003dS schedstat\u003d( 43450902 17904055 20 ) utm\u003d1 stm\u003d2 core\u003d1 HZ\u003d100\n  | stack\u003d0xc738f000-0xc7391000 stackSize\u003d1040KB\n  | held mutexes\u003d\n  native: #00 pc 00000b97  [vdso] (__kernel_vsyscall+7)\n  native: #01 pc 0005ad68  /apex/com.android.runtime/lib/bionic/libc.so (syscall+40)\n  native: #02 pc 001d82ec  /apex/com.android.art/lib/libart.so (art::ConditionVariable::WaitHoldingLocks(art::Thread*)+108)\n  native: #03 pc 001d8273  /apex/com.android.art/lib/libart.so (art::ConditionVariable::Wait(art::Thread*)+35)\n  native: #04 pc 0034a036  /apex/com.android.art/lib/libart.so (art::gc::TaskProcessor::GetTask(art::Thread*)+630)\n  native: #05 pc 0034aa64  /apex/com.android.art/lib/libart.so (art::gc::TaskProcessor::RunAllTasks(art::Thread*)+84)\n  native: #06 pc 005591f5  /apex/com.android.art/lib/libart.so (art::VMRuntime_runHeapTasks(_JNIEnv*, _jobject*)+53)\n  at dalvik.system.VMRuntime.runHeapTasks(Native method)\n  at java.lang.Daemons$HeapTaskDaemon.runInternal(Daemons.java:531)\n  at java.lang.Daemons$Daemon.run(Daemons.java:139)\n  at java.lang.Thread.run(Thread.java:923)\n\n\"Binder:9207_1\" prio\u003d5 tid\u003d8 Native\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040c18 self\u003d0xe7380010\n  | sysTid\u003d9224 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc6f701e0\n  | state\u003dS schedstat\u003d( 1496174 7615537 6 ) utm\u003d0 stm\u003d0 core\u003d1 HZ\u003d100\n  | stack\u003d0xc6e75000-0xc6e77000 stackSize\u003d1008KB\n  | held mutexes\u003d\n  native: #00 pc 00000b97  [vdso] (__kernel_vsyscall+7)\n  native: #01 pc 000cd46c  /apex/com.android.runtime/lib/bionic/libc.so (__ioctl+28)\n  native: #02 pc 00080e6a  /apex/com.android.runtime/lib/bionic/libc.so (ioctl+58)\n  native: #03 pc 00050edb  /system/lib/libbinder.so (android::IPCThreadState::talkWithDriver(bool)+331)\n  native: #04 pc 0005117a  /system/lib/libbinder.so (android::IPCThreadState::getAndExecuteCommand()+42)\n  native: #05 pc 00051cb8  /system/lib/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+72)\n  native: #06 pc 0007e309  /system/lib/libbinder.so (android::PoolThread::threadLoop()+41)\n  native: #07 pc 00015116  /system/lib/libutils.so (android::Thread::_threadLoop(void*)+374)\n  native: #08 pc 00098fee  /system/lib/libandroid_runtime.so (android::AndroidRuntime::javaThreadShell(void*)+174)\n  native: #09 pc 000147d9  /system/lib/libutils.so (thread_data_t::trampoline(thread_data_t const*)+457)\n  native: #10 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #11 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"FinalizerWatchdogDaemon\" daemon prio\u003d5 tid\u003d9 Waiting\n  | group\u003d\"system\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040c90 self\u003d0xe7382a10\n  | sysTid\u003d9223 nice\u003d4 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc71771e0\n  | state\u003dS schedstat\u003d( 398422 6565402 4 ) utm\u003d0 stm\u003d0 core\u003d1 HZ\u003d100\n  | stack\u003d0xc7074000-0xc7076000 stackSize\u003d1040KB\n  | held mutexes\u003d\n  at java.lang.Object.wait(Native method)\n  - waiting on \u003c0x02d3c777\u003e (a java.lang.Daemons$FinalizerWatchdogDaemon)\n  at java.lang.Object.wait(Object.java:442)\n  at java.lang.Object.wait(Object.java:568)\n  at java.lang.Daemons$FinalizerWatchdogDaemon.sleepUntilNeeded(Daemons.java:341)\n  - locked \u003c0x02d3c777\u003e (a java.lang.Daemons$FinalizerWatchdogDaemon)\n  at java.lang.Daemons$FinalizerWatchdogDaemon.runInternal(Daemons.java:321)\n  at java.lang.Daemons$Daemon.run(Daemons.java:139)\n  at java.lang.Thread.run(Thread.java:923)\n\n\"ReferenceQueueDaemon\" daemon prio\u003d5 tid\u003d10 Waiting\n  | group\u003d\"system\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040d08 self\u003d0xe7388c10\n  | sysTid\u003d9221 nice\u003d4 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc73891e0\n  | state\u003dS schedstat\u003d( 533566 3015911 3 ) utm\u003d0 stm\u003d0 core\u003d1 HZ\u003d100\n  | stack\u003d0xc7286000-0xc7288000 stackSize\u003d1040KB\n  | held mutexes\u003d\n  at java.lang.Object.wait(Native method)\n  - waiting on \u003c0x07fc5fe4\u003e (a java.lang.Class\u003cjava.lang.ref.ReferenceQueue\u003e)\n  at java.lang.Object.wait(Object.java:442)\n  at java.lang.Object.wait(Object.java:568)\n  at java.lang.Daemons$ReferenceQueueDaemon.runInternal(Daemons.java:217)\n  - locked \u003c0x07fc5fe4\u003e (a java.lang.Class\u003cjava.lang.ref.ReferenceQueue\u003e)\n  at java.lang.Daemons$Daemon.run(Daemons.java:139)\n  at java.lang.Thread.run(Thread.java:923)\n\n\"Jit thread pool worker thread 0\" daemon prio\u003d5 tid\u003d11 Native\n  | group\u003d\"system\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040d80 self\u003d0xe7387e10\n  | sysTid\u003d9219 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc7596d60\n  | state\u003dS schedstat\u003d( 468316720 500776964 211 ) utm\u003d6 stm\u003d39 core\u003d0 HZ\u003d100\n  | stack\u003d0xc7498000-0xc749a000 stackSize\u003d1023KB\n  | held mutexes\u003d\n  native: #00 pc 00000b97  [vdso] (__kernel_vsyscall+7)\n  native: #01 pc 0005ad68  /apex/com.android.runtime/lib/bionic/libc.so (syscall+40)\n  native: #02 pc 001d82ec  /apex/com.android.art/lib/libart.so (art::ConditionVariable::WaitHoldingLocks(art::Thread*)+108)\n  native: #03 pc 001d8273  /apex/com.android.art/lib/libart.so (art::ConditionVariable::Wait(art::Thread*)+35)\n  native: #04 pc 006c838f  /apex/com.android.art/lib/libart.so (art::ThreadPool::GetTask(art::Thread*)+143)\n  native: #05 pc 006c73e5  /apex/com.android.art/lib/libart.so (art::ThreadPoolWorker::Run()+133)\n  native: #06 pc 006c6e9d  /apex/com.android.art/lib/libart.so (art::ThreadPoolWorker::Callback(void*)+269)\n  native: #07 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #08 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"FinalizerDaemon\" daemon prio\u003d5 tid\u003d12 Waiting\n  | group\u003d\"system\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040df8 self\u003d0xe7386210\n  | sysTid\u003d9222 nice\u003d4 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc72801e0\n  | state\u003dS schedstat\u003d( 1102483 7783564 5 ) utm\u003d0 stm\u003d0 core\u003d1 HZ\u003d100\n  | stack\u003d0xc717d000-0xc717f000 stackSize\u003d1040KB\n  | held mutexes\u003d\n  at java.lang.Object.wait(Native method)\n  - waiting on \u003c0x0a047c4d\u003e (a java.lang.Object)\n  at java.lang.Object.wait(Object.java:442)\n  at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:190)\n  - locked \u003c0x0a047c4d\u003e (a java.lang.Object)\n  at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:211)\n  at java.lang.Daemons$FinalizerDaemon.runInternal(Daemons.java:273)\n  at java.lang.Daemons$Daemon.run(Daemons.java:139)\n  at java.lang.Thread.run(Thread.java:923)\n\n\"Binder:9207_2\" prio\u003d5 tid\u003d13 Native\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040e70 self\u003d0xe7389a10\n  | sysTid\u003d9225 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc6e6f1e0\n  | state\u003dS schedstat\u003d( 210561432 574003421 20 ) utm\u003d10 stm\u003d10 core\u003d1 HZ\u003d100\n  | stack\u003d0xc6d74000-0xc6d76000 stackSize\u003d1008KB\n  | held mutexes\u003d\n  native: #00 pc 00000b97  [vdso] (__kernel_vsyscall+7)\n  native: #01 pc 000cd46c  /apex/com.android.runtime/lib/bionic/libc.so (__ioctl+28)\n  native: #02 pc 00080e6a  /apex/com.android.runtime/lib/bionic/libc.so (ioctl+58)\n  native: #03 pc 00050edb  /system/lib/libbinder.so (android::IPCThreadState::talkWithDriver(bool)+331)\n  native: #04 pc 0005117a  /system/lib/libbinder.so (android::IPCThreadState::getAndExecuteCommand()+42)\n  native: #05 pc 00051cb8  /system/lib/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+72)\n  native: #06 pc 0007e309  /system/lib/libbinder.so (android::PoolThread::threadLoop()+41)\n  native: #07 pc 00015116  /system/lib/libutils.so (android::Thread::_threadLoop(void*)+374)\n  native: #08 pc 00098fee  /system/lib/libandroid_runtime.so (android::AndroidRuntime::javaThreadShell(void*)+174)\n  native: #09 pc 000147d9  /system/lib/libutils.so (thread_data_t::trampoline(thread_data_t const*)+457)\n  native: #10 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #11 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"Binder:9207_3\" prio\u003d5 tid\u003d14 Native\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040ee8 self\u003d0xe7391810\n  | sysTid\u003d9226 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc6d6e1e0\n  | state\u003dS schedstat\u003d( 17000557 50151632 14 ) utm\u003d0 stm\u003d1 core\u003d1 HZ\u003d100\n  | stack\u003d0xc6c73000-0xc6c75000 stackSize\u003d1008KB\n  | held mutexes\u003d\n  native: #00 pc 00000b97  [vdso] (__kernel_vsyscall+7)\n  native: #01 pc 000cd46c  /apex/com.android.runtime/lib/bionic/libc.so (__ioctl+28)\n  native: #02 pc 00080e6a  /apex/com.android.runtime/lib/bionic/libc.so (ioctl+58)\n  native: #03 pc 00050edb  /system/lib/libbinder.so (android::IPCThreadState::talkWithDriver(bool)+331)\n  native: #04 pc 0005117a  /system/lib/libbinder.so (android::IPCThreadState::getAndExecuteCommand()+42)\n  native: #05 pc 00051cb8  /system/lib/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+72)\n  native: #06 pc 0007e309  /system/lib/libbinder.so (android::PoolThread::threadLoop()+41)\n  native: #07 pc 00015116  /system/lib/libutils.so (android::Thread::_threadLoop(void*)+374)\n  native: #08 pc 00098fee  /system/lib/libandroid_runtime.so (android::AndroidRuntime::javaThreadShell(void*)+174)\n  native: #09 pc 000147d9  /system/lib/libutils.so (thread_data_t::trampoline(thread_data_t const*)+457)\n  native: #10 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #11 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"Binder:9207_4\" prio\u003d5 tid\u003d15 Native\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040f60 self\u003d0xe7390a10\n  | sysTid\u003d9227 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc68411e0\n  | state\u003dS schedstat\u003d( 8392907 55881465 12 ) utm\u003d0 stm\u003d0 core\u003d0 HZ\u003d100\n  | stack\u003d0xc6746000-0xc6748000 stackSize\u003d1008KB\n  | held mutexes\u003d\n  native: #00 pc 00000b97  [vdso] (__kernel_vsyscall+7)\n  native: #01 pc 000cd46c  /apex/com.android.runtime/lib/bionic/libc.so (__ioctl+28)\n  native: #02 pc 00080e6a  /apex/com.android.runtime/lib/bionic/libc.so (ioctl+58)\n  native: #03 pc 00050edb  /system/lib/libbinder.so (android::IPCThreadState::talkWithDriver(bool)+331)\n  native: #04 pc 0005117a  /system/lib/libbinder.so (android::IPCThreadState::getAndExecuteCommand()+42)\n  native: #05 pc 00051cb8  /system/lib/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+72)\n  native: #06 pc 0007e309  /system/lib/libbinder.so (android::PoolThread::threadLoop()+41)\n  native: #07 pc 00015116  /system/lib/libutils.so (android::Thread::_threadLoop(void*)+374)\n  native: #08 pc 00098fee  /system/lib/libandroid_runtime.so (android::AndroidRuntime::javaThreadShell(void*)+174)\n  native: #09 pc 000147d9  /system/lib/libutils.so (thread_data_t::trampoline(thread_data_t const*)+457)\n  native: #10 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #11 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"Profile Saver\" daemon prio\u003d5 tid\u003d16 Native\n  | group\u003d\"system\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13040fd8 self\u003d0xe738ee10\n  | sysTid\u003d9228 nice\u003d9 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc67401e0\n  | state\u003dS schedstat\u003d( 14918009 13675595 16 ) utm\u003d1 stm\u003d0 core\u003d1 HZ\u003d100\n  | stack\u003d0xc6645000-0xc6647000 stackSize\u003d1008KB\n  | held mutexes\u003d\n  native: #00 pc 00000b97  [vdso] (__kernel_vsyscall+7)\n  native: #01 pc 0005ad68  /apex/com.android.runtime/lib/bionic/libc.so (syscall+40)\n  native: #02 pc 001d82ec  /apex/com.android.art/lib/libart.so (art::ConditionVariable::WaitHoldingLocks(art::Thread*)+108)\n  native: #03 pc 001d8273  /apex/com.android.art/lib/libart.so (art::ConditionVariable::Wait(art::Thread*)+35)\n  native: #04 pc 003cc829  /apex/com.android.art/lib/libart.so (art::ProfileSaver::Run()+633)\n  native: #05 pc 003d2e8f  /apex/com.android.art/lib/libart.so (art::ProfileSaver::RunProfileSaverThread(void*)+175)\n  native: #06 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #07 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"RenderThread\" daemon prio\u003d7 tid\u003d17 Native\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x13041050 self\u003d0xe7392610\n  | sysTid\u003d9229 nice\u003d-10 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc663f1e0\n  | state\u003dS schedstat\u003d( 236695461 71606987 112 ) utm\u003d4 stm\u003d19 core\u003d1 HZ\u003d100\n  | stack\u003d0xc6544000-0xc6546000 stackSize\u003d1008KB\n  | held mutexes\u003d\n  native: #00 pc 00000b99  [vdso] (__kernel_vsyscall+9)\n  native: #01 pc 000cf2cb  /apex/com.android.runtime/lib/bionic/libc.so (__epoll_pwait+43)\n  native: #02 pc 00088f9d  /apex/com.android.runtime/lib/bionic/libc.so (epoll_wait+45)\n  native: #03 pc 0001a003  /system/lib/libutils.so (android::Looper::pollInner(int)+259)\n  native: #04 pc 00019e96  /system/lib/libutils.so (android::Looper::pollOnce(int, int*, int*, void**)+118)\n  native: #05 pc 002452c5  /system/lib/libhwui.so (android::uirenderer::ThreadBase::waitForWork()+149)\n  native: #06 pc 0026cab7  /system/lib/libhwui.so (android::uirenderer::renderthread::RenderThread::threadLoop()+119)\n  native: #07 pc 00015116  /system/lib/libutils.so (android::Thread::_threadLoop(void*)+374)\n  native: #08 pc 000147d9  /system/lib/libutils.so (thread_data_t::trampoline(thread_data_t const*)+457)\n  native: #09 pc 000e6974  /apex/com.android.runtime/lib/bionic/libc.so (__pthread_start(void*)+100)\n  native: #10 pc 00078567  /apex/com.android.runtime/lib/bionic/libc.so (__start_thread+71)\n  (no managed stack frames)\n\n\"BacktraceHandlerThread\" prio\u003d5 tid\u003d18 Native\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x12cf4660 self\u003d0xe738fc10\n  | sysTid\u003d9232 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc64281e0\n  | state\u003dS schedstat\u003d( 313706628 570346891 185 ) utm\u003d20 stm\u003d11 core\u003d0 HZ\u003d100\n  | stack\u003d0xc6325000-0xc6327000 stackSize\u003d1040KB\n  | held mutexes\u003d\n  native: #00 pc 00000b99  [vdso] (__kernel_vsyscall+9)\n  native: #01 pc 000cf2cb  /apex/com.android.runtime/lib/bionic/libc.so (__epoll_pwait+43)\n  native: #02 pc 00088f9d  /apex/com.android.runtime/lib/bionic/libc.so (epoll_wait+45)\n  native: #03 pc 0001a003  /system/lib/libutils.so (android::Looper::pollInner(int)+259)\n  native: #04 pc 00019e96  /system/lib/libutils.so (android::Looper::pollOnce(int, int*, int*, void**)+118)\n  native: #05 pc 0010ef8b  /system/lib/libandroid_runtime.so (android::android_os_MessageQueue_nativePollOnce(_JNIEnv*, _jobject*, long long, int)+59)\n  at android.os.MessageQueue.nativePollOnce(Native method)\n  at android.os.MessageQueue.next(MessageQueue.java:335)\n  at android.os.Looper.loop(Looper.java:183)\n  at android.os.HandlerThread.run(HandlerThread.java:67)\n\n\"Timer-0\" prio\u003d5 tid\u003d19 TimedWaiting\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x12cf5498 self\u003d0xe738e010\n  | sysTid\u003d9233 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc62461e0\n  | state\u003dS schedstat\u003d( 7524451 0 14 ) utm\u003d0 stm\u003d0 core\u003d0 HZ\u003d100\n  | stack\u003d0xc6143000-0xc6145000 stackSize\u003d1040KB\n  | held mutexes\u003d\n  at java.lang.Object.wait(Native method)\n  - waiting on \u003c0x02569302\u003e (a java.util.TaskQueue)\n  at java.lang.Object.wait(Object.java:442)\n  at java.util.TimerThread.mainLoop(Timer.java:559)\n  - locked \u003c0x02569302\u003e (a java.util.TaskQueue)\n  at java.util.TimerThread.run(Timer.java:512)\n\n\"WifiManagerThread\" prio\u003d5 tid\u003d20 Native\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x12cff1c0 self\u003d0xe7399610\n  | sysTid\u003d9236 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc613d1e0\n  | state\u003dS schedstat\u003d( 581694 0 1 ) utm\u003d0 stm\u003d0 core\u003d0 HZ\u003d100\n  | stack\u003d0xc603a000-0xc603c000 stackSize\u003d1040KB\n  | held mutexes\u003d\n  native: #00 pc 00000b99  [vdso] (__kernel_vsyscall+9)\n  native: #01 pc 000cf2cb  /apex/com.android.runtime/lib/bionic/libc.so (__epoll_pwait+43)\n  native: #02 pc 00088f9d  /apex/com.android.runtime/lib/bionic/libc.so (epoll_wait+45)\n  native: #03 pc 0001a003  /system/lib/libutils.so (android::Looper::pollInner(int)+259)\n  native: #04 pc 00019e96  /system/lib/libutils.so (android::Looper::pollOnce(int, int*, int*, void**)+118)\n  native: #05 pc 0010ef8b  /system/lib/libandroid_runtime.so (android::android_os_MessageQueue_nativePollOnce(_JNIEnv*, _jobject*, long long, int)+59)\n  at android.os.MessageQueue.nativePollOnce(Native method)\n  at android.os.MessageQueue.next(MessageQueue.java:335)\n  at android.os.Looper.loop(Looper.java:183)\n  at android.os.HandlerThread.run(HandlerThread.java:67)\n\n\"Thread-4\" prio\u003d5 tid\u003d21 Sleeping\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x12d8a710 self\u003d0xe7394210\n  | sysTid\u003d9237 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc60341e0\n  | state\u003dS schedstat\u003d( 76232117 47640734 71 ) utm\u003d5 stm\u003d2 core\u003d0 HZ\u003d100\n  | stack\u003d0xc5f31000-0xc5f33000 stackSize\u003d1040KB\n  | held mutexes\u003d\n  at java.lang.Thread.sleep(Native method)\n  - sleeping on \u003c0x05336413\u003e (a java.lang.Object)\n  at java.lang.Thread.sleep(Thread.java:442)\n  - locked \u003c0x05336413\u003e (a java.lang.Object)\n  at java.lang.Thread.sleep(Thread.java:358)\n  at backtraceio.library.watchdog.BacktraceANRHandlerWatchdog.run(BacktraceANRHandlerWatchdog.java:118)\n\n\"OkHttp ConnectionPool\" daemon prio\u003d5 tid\u003d22 TimedWaiting\n  | group\u003d\"main\" sCount\u003d1 dsCount\u003d0 flags\u003d1 obj\u003d0x12d39c50 self\u003d0xe739b210\n  | sysTid\u003d9239 nice\u003d0 cgrp\u003dtop-app sched\u003d0/0 handle\u003d0xc56251e0\n  | state\u003dS schedstat\u003d( 442059 1414330 1 ) utm\u003d0 stm\u003d0 core\u003d0 HZ\u003d100\n  | stack\u003d0xc5522000-0xc5524000 stackSize\u003d1040KB\n  | held mutexes\u003d\n  at java.lang.Object.wait(Native method)\n  - waiting on \u003c0x09119650\u003e (a com.android.okhttp.ConnectionPool)\n  at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:106)\n  - locked \u003c0x09119650\u003e (a com.android.okhttp.ConnectionPool)\n  at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)\n  at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)\n  at java.lang.Thread.run(Thread.java:923)\n\n----- end 9207 -----\n\n----- Waiting Channels: pid 9207 at 2025-03-27 21:02:38 -----\nCmd line: backtraceio.backtraceio\n\nsysTid\u003d9207      0\nsysTid\u003d9216      do_sigtimedwait\nsysTid\u003d9217      pipe_read\nsysTid\u003d9218      do_sys_poll\nsysTid\u003d9219      futex_wait_queue_me\nsysTid\u003d9220      futex_wait_queue_me\nsysTid\u003d9221      futex_wait_queue_me\nsysTid\u003d9222      futex_wait_queue_me\nsysTid\u003d9223      futex_wait_queue_me\nsysTid\u003d9224      binder_thread_read\nsysTid\u003d9225      binder_thread_read\nsysTid\u003d9226      binder_thread_read\nsysTid\u003d9227      binder_thread_read\nsysTid\u003d9228      futex_wait_queue_me\nsysTid\u003d9229      do_epoll_wait\nsysTid\u003d9232      do_epoll_wait\nsysTid\u003d9233      futex_wait_queue_me\nsysTid\u003d9236      do_epoll_wait\nsysTid\u003d9237      futex_wait_queue_me\nsysTid\u003d9239      futex_wait_queue_me\n\n----- end 9207 -----\n\n";
        // System.out.println("Try programiz.pro");
//        System.out.println(StackTraceParser.parseStackTrace(weirdString));

        BacktraceCredentials credentials = new BacktraceCredentials("https://yolo.sp.backtrace.io:6098/",
                "2dd86e8e779d1fc7e22e7b19a9489abeedec3b1426abe7e2209888e92362fba4");

        Context context = getApplicationContext();
        String dbPath = context.getFilesDir().getAbsolutePath();

        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(dbPath);
        settings.setMaxRecordCount(100);
        settings.setMaxDatabaseSize(1000);
        settings.setRetryBehavior(RetryBehavior.ByInterval);
        settings.setAutoSendMode(true);
        settings.setRetryOrder(RetryOrder.Queue);

        Map<String, Object> attributes = new HashMap<String, Object>() {{
            put("custom.attribute", "My Custom Attribute");
        }};

        List<String> attachments = new ArrayList<String>() {{
            add(context.getFilesDir() + "/" + "myCustomFile.txt");
        }};

        BacktraceDatabase database = new BacktraceDatabase(context, settings);
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database, attributes, attachments);

        BacktraceExceptionHandler.enable(backtraceClient);

        backtraceClient.metrics.enable();

        // Enable handling of native crashes
        database.setupNativeIntegration(backtraceClient, credentials, true);

        // Enable ANR detection
        backtraceClient.enableAnr(AnrType.ApplicationExit);
        return backtraceClient;
    }

    public native void cppCrash();

    public native boolean registerNativeBreadcrumbs(BacktraceBase backtraceBase);

    public native boolean addNativeBreadcrumb();

    public native boolean addNativeBreadcrumbUserError();

    public native void cleanupNativeBreadcrumbHandler();

    private List<String> equippedItems;

    public List<String> getWarriorArmor() {
        return new ArrayList<String>(Arrays.asList("Tough Boots", "Strong Sword", "Sturdy Shield", "Magic Wand"));
    }

    int findEquipmentIndex(List<String> armor, String equipment) {
        return armor.indexOf(equipment);
    }

    void removeEquipment(List<String> armor, int index) {
        armor.remove(index);
    }

    void equipItem(List<String> armor, int index) {
        equippedItems.add(armor.get(index));
    }

    public void handledException(View view) {
        try {
            try {
                List<String> myWarriorArmor = getWarriorArmor();
                int magicWandIndex = findEquipmentIndex(myWarriorArmor, "Magic Wand");
                // I don't need a Magic Wand, I am a warrior
                removeEquipment(myWarriorArmor, magicWandIndex);
                // Where was that magic wand again?
                equipItem(myWarriorArmor, magicWandIndex);
            } catch (IndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException("Invalid index of selected element!");
            }
        } catch (IndexOutOfBoundsException e) {
            backtraceClient.send(new BacktraceReport(e), this.listener);
        }
    }

    public void getSaveData() throws IOException {
        // I know for sure this file is there (spoiler alert, it's not)
        File mySaveData = new File("mySave.sav");
        FileReader mySaveDataReader = new FileReader(mySaveData);
        char[] saveDataBuffer = new char[255];
        mySaveDataReader.read(saveDataBuffer);
    }

    public void unhandledException(View view) throws IOException {
        getSaveData();
    }

    public void nativeCrash(View view) {
        cppCrash();
    }

    public void anr(View view) throws InterruptedException {
        Thread.sleep(anrTimeout + 2000);
    }

    public void enableBreadcrumbs(View view) throws Exception {
        Context appContext = view.getContext().getApplicationContext();
        if (backtraceClient == null) {
            throw new Exception("Backtrace client integration is not initialized");
        }

        if (appContext == null) {
            throw new Exception("App context is null");
        }

        backtraceClient.enableBreadcrumbs(view.getContext().getApplicationContext());
        registerNativeBreadcrumbs(backtraceClient); // Order should not matter
    }

    public void enableBreadcrumbsUserOnly(View view) throws Exception {
        EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable = EnumSet.of(BacktraceBreadcrumbType.USER);
        Context appContext = view.getContext().getApplicationContext();
        backtraceClient.enableBreadcrumbs(appContext, breadcrumbTypesToEnable);
        registerNativeBreadcrumbs(backtraceClient); // Order should not matter
    }

    public void sendReport(View view) {
        final long id = Thread.currentThread().getId();
        Map<String, Object> attributes = new HashMap<String, Object>() {{
            put("Caller thread", id);
        }};
        backtraceClient.addBreadcrumb("About to send Backtrace report", attributes, BacktraceBreadcrumbType.LOG);
        addNativeBreadcrumb();
        addNativeBreadcrumbUserError();
        BacktraceReport report = new BacktraceReport("Test");
        backtraceClient.send(report, this.listener);
    }

    private void writeMyCustomFile(String filePath) {
        String fileData = "My custom data\nMore of my data\nEnd of my data";
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(filePath));
            outputStreamWriter.write(fileData);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("BacktraceAndroid", "File write failed due to: " + e.toString());
        }
    }

    public BacktraceClient getBacktraceClient() {
        return backtraceClient;
    }

    public void exit(View view) {
        System.exit(0);
    }

    public void dumpWithoutCrash(View view) {
        backtraceClient.dumpWithoutCrash("DumpWithoutCrash");
    }

    public void disableNativeIntegration(View view) {
        backtraceClient.disableNativeIntegration();
    }

    public void enableNativeIntegration(View view) {
        backtraceClient.enableNativeIntegration();
    }
}
