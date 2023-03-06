package org.furthemore.apisregister;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class PushReceiver extends BroadcastReceiver {
    FullscreenActivity fa = null;

    void setActivityHandler(FullscreenActivity fa) {
        Log.i("pushy.me", "Registered main fragment activity on push handler");
        this.fa = fa;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.getStringExtra("message");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
