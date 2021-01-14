package backtraceio.backtraceio;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.models.BacktraceExceptionHandler;
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

        BacktraceCredentials credentials =
                new BacktraceCredentials("<endpoint-url>", "<token>");

        Context context = getApplicationContext();
        String dbPath = context.getFilesDir().getAbsolutePath();

        BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(dbPath);
        settings.setMaxRecordCount(100);
        settings.setMaxDatabaseSize(1000);
        settings.setRetryBehavior(RetryBehavior.ByInterval);
        settings.setAutoSendMode(true);
        settings.setRetryOrder(RetryOrder.Queue);

        BacktraceDatabase database = new BacktraceDatabase(context, settings);
        backtraceClient = new BacktraceClient(context, credentials, database);

        BacktraceExceptionHandler.enable(backtraceClient);
        backtraceClient.send("test");

        // Enable handling of native crashes
        database.setupNativeIntegration(backtraceClient, credentials);

        // Enable ANR detection
        backtraceClient.enableAnr(anrTimeout);
    }

    public native void cppCrash();

    private ArrayList<String> equippedItems;

    public ArrayList<String> getWarriorArmor()
    {
        return new ArrayList<String>(Arrays.asList("Tough Boots", "Strong Sword", "Sturdy Shield", "Magic Wand"));
    }

    int findEquipmentIndex(ArrayList<String> armor, String equipment)
    {
        return armor.indexOf(equipment);
    }

    void removeEquipment(ArrayList<String> armor, int index)
    {
        armor.remove(index);
    }

    void equipItem(ArrayList<String> armor, int index)
    {
        equippedItems.add(armor.get(index));
    }

    public void handledException(View view) {
        try {
            ArrayList<String> myWarriorArmor = getWarriorArmor();
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
}
