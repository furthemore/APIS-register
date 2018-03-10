package org.furthemore.apisregister;

/**
 * Created by redxine on 3/4/18.
 */

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
        final int dollarAmount = Integer.parseInt(data.get("amount"));
        final String note = data.get("note");

        handler.post(new Runnable() {
            @Override public void run() {
                Context app = getApplicationContext();
                Toast.makeText(app, String.format("%s %s", dollarAmount, note), Toast.LENGTH_LONG).show();
                //startTransaction(dollarAmount, note);
            }
        });
    }

/*
    private void startTransaction(int dollarAmount, String note) {
        ChargeRequest request = new ChargeRequest.Builder(dollarAmount, CurrencyCode.USD).build();
        try {
            Intent intent = posClient
        }
        if (chargeActivity != null) {
            chargeActivity.startTransaction(dollarAmount, note);
        } else {
            Toast.makeText(app, "No resumed activity to start transaction", Toast.LENGTH_LONG).show();
        }
    }
    */
}

