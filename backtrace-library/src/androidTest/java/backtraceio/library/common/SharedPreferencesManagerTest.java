package backtraceio.library.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SharedPreferencesManagerTest {

    private SharedPreferencesManager sharedPreferencesManager;

    @Mock
    private Context mockContext;

    @Mock
    private SharedPreferences mockSharedPreferences;

    @Mock
    private SharedPreferences.Editor mockEditor;

    @Before
    public void setUp() {
        sharedPreferencesManager = new SharedPreferencesManager(mockContext);
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor);
    }

    @Test
    public void testSaveLongToSharedPreferences() {
        String prefName = "test_prefs";
        String key = "test_key";
        Long value = 12345L;

        sharedPreferencesManager.saveLongToSharedPreferences(prefName, key, value);

        verify(mockContext).getSharedPreferences(prefName, Context.MODE_PRIVATE);
        verify(mockSharedPreferences).edit();
        verify(mockEditor).putLong(key, value);
        verify(mockEditor).apply();
        verify(mockEditor).commit();
    }

    @Test
    public void testReadLongFromSharedPreferences() {
        String prefName = "test_prefs";
        String key = "test_key";
        long defaultValue = 0L;
        Long expectedValue = 12345L;

        when(mockSharedPreferences.getLong(key, defaultValue)).thenReturn(expectedValue);

        Long result = sharedPreferencesManager.readLongFromSharedPreferences(prefName, key, defaultValue);

        verify(mockContext).getSharedPreferences(prefName, Context.MODE_PRIVATE);
        verify(mockSharedPreferences).getLong(key, defaultValue);
        assertEquals(expectedValue, result);
    }
}
