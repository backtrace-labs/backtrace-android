package backtraceio.library.common;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    private final Context context;

    public SharedPreferencesManager(Context context) {
        this.context = context;
    }

    public void saveLongToSharedPreferences(String prefName, String key, Long value) {
        SharedPreferences sharedPreferences = this.context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public Long readLongFromSharedPreferences(String prefName, String key, Long defaultValue) {
        SharedPreferences sharedPreferences = this.context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return sharedPreferences.getLong(key, defaultValue);
    }
}
