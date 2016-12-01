package com.example.acme.kazoo.ui;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blackmoonit.androidbits.app.AppPrefsDebug;

/**
 * Allows the user to change the app preferences.
 */
public class SettingsActivity
extends AppPrefsDebug
implements SharedPreferences.OnSharedPreferenceChangeListener
{
    static private final String TAG = SettingsActivity.class.getSimpleName() ;

    @Override
    protected void setup()
    {
        super.setup() ;
        PreferenceManager.getDefaultSharedPreferences( this )
            .registerOnSharedPreferenceChangeListener( this ) ;
    }

    @Override
    protected void onResume()
    {
        super.onResume() ;
        this.resetPrefScreens() ;
    }

    @Override
    protected void onDestroy()
    {
        PreferenceManager.getDefaultSharedPreferences( this )
            .unregisterOnSharedPreferenceChangeListener( this ) ;
        super.onDestroy() ;
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences prefs, String sPrefKey )
    {
        Log.d(TAG, (new StringBuilder())
                .append( "Preference [" )
                .append( sPrefKey )
                .append( "] changed." )
                .toString()
            );
    }

}
