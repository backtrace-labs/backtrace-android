package backtraceio.library.anr;

import android.content.Context;

import backtraceio.library.common.SharedPreferencesManager;

public class AnrAppExitInfoState {

    private final SharedPreferencesManager sharedPreferencesManager;

    private static final String PREFS_NAME = "ANR_APP_EXIT_INFO_STATE";

    private static final String TIMESTAMP_PREF_KEY = "LAST_ANR_TIMESTAMP";
    private static final long TIMESTAMP_DEFAULT = 0;

    public AnrAppExitInfoState(Context context) {
        this.sharedPreferencesManager = new SharedPreferencesManager(context);
    }

    public void saveTimestamp(long value) {
        this.sharedPreferencesManager.saveLongToSharedPreferences(PREFS_NAME, TIMESTAMP_PREF_KEY, value);
    }

    public long getLastTimestamp() {
        return this.sharedPreferencesManager.readLongFromSharedPreferences(
                PREFS_NAME, TIMESTAMP_PREF_KEY, TIMESTAMP_DEFAULT);
    }

}
