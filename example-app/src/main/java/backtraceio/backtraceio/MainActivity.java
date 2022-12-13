package backtraceio.backtraceio;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.View;

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
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceExceptionHandler;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;

public class MainActivity extends AppCompatActivity {

    private BacktraceClient backtraceClient;

    private final int anrTimeout = 3000;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set this value in your local.properties
        if (BuildConfig.BACKTRACE_SUBMISSION_URL != null) {
            backtraceClient = initializeBacktrace(BuildConfig.BACKTRACE_SUBMISSION_URL);
        }

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
        BacktraceCredentials credentials = new BacktraceCredentials(submissionUrl);

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

        List<String> attachments = new ArrayList<String>(){{
            add(context.getFilesDir() + "/" + "myCustomFile.txt");
        }};

        BacktraceDatabase database = new BacktraceDatabase(context, settings);
        BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database, attributes, attachments);

        BacktraceExceptionHandler.enable(backtraceClient);
        // backtraceClient.send("test");

        // Enable handling of native crashes
        database.setupNativeIntegration(backtraceClient, credentials, true);

        backtraceClient.metrics.enable();

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

    public List<String> getWarriorArmor()
    {
        return new ArrayList<String>(Arrays.asList("Tough Boots", "Strong Sword", "Sturdy Shield", "Magic Wand"));
    }

    int findEquipmentIndex(List<String> armor, String equipment)
    {
        return armor.indexOf(equipment);
    }

    void removeEquipment(List<String> armor, int index)
    {
        armor.remove(index);
    }

    void equipItem(List<String> armor, int index)
    {
        equippedItems.add(armor.get(index));
    }

    public void handledException(View view) {
        try {
            List<String> myWarriorArmor = getWarriorArmor();
            int magicWandIndex = findEquipmentIndex(myWarriorArmor, "Magic Wand");
            // I don't need a Magic Wand, I am a warrior
            removeEquipment(myWarriorArmor, magicWandIndex);
            // Where was that magic wand again?
            equipItem(myWarriorArmor, magicWandIndex);
        } catch (Exception e) {
            backtraceClient.send(new BacktraceReport(e));
        }
    }

    public void getSaveData() throws IOException {
        // I know for sure this file is there (spoiler alert, it's not)
        File mySaveData =  new File("mySave.sav");
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

    public void enableBreadcrumbs(View view) {
        backtraceClient.enableBreadcrumbs(view.getContext().getApplicationContext());
        registerNativeBreadcrumbs(backtraceClient); // Order should not matter
    }

    public void enableBreadcrumbsUserOnly(View view) {
        EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable = EnumSet.of(BacktraceBreadcrumbType.USER);
        backtraceClient.enableBreadcrumbs(view.getContext().getApplicationContext(), breadcrumbTypesToEnable);
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
        backtraceClient.send(report);
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