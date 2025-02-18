package backtraceio.backtraceio;

import android.content.Context;
import android.content.SharedPreferences;
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
import backtraceio.library.anr.BacktraceAppExitInfoSenderHandler;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.models.BacktraceExceptionHandler;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_NAME = "myKey";

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

    private String readFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_NAME, "Default Value");
    }

    private void saveToSharedPreferences(String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_NAME, value);
        editor.apply(); // or editor.commit();
        editor.commit();
    }

    public void sharedPreferencesExample() {
        String val = readFromSharedPreferences();
        String val2 = val + "1";
        saveToSharedPreferences(val2);
        String val3 = readFromSharedPreferences();
        System.out.println(val3);
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
        sharedPreferencesExample();
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

//        backtraceClient.metrics.enable();
//        backtraceClient.enableAnr(AnrType.Event);
        BacktraceAppExitInfoSenderHandler backtraceAppExitInfoSender = new BacktraceAppExitInfoSenderHandler(backtraceClient, context);
        backtraceAppExitInfoSender.send();
        // Enable handling of native crashes
        database.setupNativeIntegration(backtraceClient, credentials, true);

        // Enable ANR detection
        backtraceClient.enableAnr(anrTimeout);
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
