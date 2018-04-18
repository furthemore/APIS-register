package org.furthemore.apisregister;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextClock;

public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    ComponentName activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        activity = getCallingActivity();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        Log.d("SettingsActivity", "Setting Changed - Key: " + key);


        /*

        if (key.equals("foreground_color")) {
            int foreground_color = sharedPreferences.getInt("foreground_color", Color.parseColor("#ffffff"));
            TextClock textClock = findViewById(R.id.text_clock);
            textClock.setTextColor(foreground_color);
        }
        */

    }
}

