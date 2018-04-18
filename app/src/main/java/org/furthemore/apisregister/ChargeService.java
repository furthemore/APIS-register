package org.furthemore.apisregister;

/**
 * Created by redxine on 3/4/18.
 */

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.squareup.sdk.pos.ChargeRequest;
import com.squareup.sdk.pos.CurrencyCode;

import java.util.Map;

public class ChargeService extends FirebaseMessagingService {
    final Handler handler = new Handler(Looper.getMainLooper());

    @Override public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        if ("clear".equals(data.get("command"))) {
            Intent main = new Intent(ChargeService.this, FullscreenActivity.class);
            main.putExtra("command", "clear");
            main.setAction("onMessageReceived");
            LocalBroadcastManager.getInstance(this).sendBroadcast(main);

        } else if ("display".equals(data.get("command"))) {
            // Update the information shown to the user

            Intent main = new Intent(ChargeService.this, FullscreenActivity.class);
            main.putExtra("command", "display");
            main.putExtra("html", data.get("html"));
            main.putExtra("amount", Integer.parseInt(data.get("amount")));
            main.putExtra("note", data.get("note"));
            main.setAction("onMessageReceived");
            LocalBroadcastManager.getInstance(this).sendBroadcast(main);

        } else if ("payment".equals(data.get("command"))) {

            final int dollarAmount = Integer.parseInt(data.get("amount"));
            final String note = data.get("note");

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Context app = getApplicationContext();
                    Toast.makeText(app, String.format("%s %s", dollarAmount, note), Toast.LENGTH_LONG).show();
                    //startTransaction(dollarAmount, note);
                }
            });
        }
    }

}


