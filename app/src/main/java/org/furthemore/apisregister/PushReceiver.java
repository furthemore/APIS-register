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

public class PushReceiver extends BroadcastReceiver {
    FullscreenActivity fa = null;

    void setActivityHandler(FullscreenActivity fa) {
        this.fa = fa;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.getStringExtra("message");

        String command = intent.getStringExtra("command");
        Button payment_button = this.fa.findViewById(R.id.payment_button);

        if ("clear".equals(command)) {
            // Clears the screen and hides the button
            this.fa.setHtml("");
            payment_button.setVisibility(View.GONE);

        } else if ("display".equals(command)) {
            // Update the information shown to the user
            String html = intent.getStringExtra("html");
            String note = intent.getStringExtra("note");
            String reference = intent.getStringExtra("reference");
            int amount = intent.getIntExtra("total", 0);
            Log.d("PushReceiver", "Dollar amount from server: "  + amount);
            this.fa.setHtml(html);
            this.fa.setNote(note);
            this.fa.setReference(reference);
            this.fa.setCharge_total(amount);

        } else if ("enable_payment".equals(command)) {
            // Update payment values and enable the user to launch square to take payment
            payment_button.setVisibility(View.VISIBLE);
            String note = intent.getStringExtra("note");
            if (note == null) {
                note = this.fa.getNote();
            }
            int amount = intent.getIntExtra("total", this.fa.getCharge_total());
            this.fa.setNote(note);
            this.fa.setCharge_total(amount);
            // do a thing

        } else if ("process_payment".equals(command)) {
            // Immediately trigger a Square charge
            String note = intent.getStringExtra("note");
            if (note == null) {
                note = this.fa.getNote();
            }
            int amount = intent.getIntExtra("total", this.fa.getCharge_total());

            this.fa.startTransaction(amount, note);

        } else if ("close".equals(command)) {
            // Close the terminal
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("position_closed", true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                editor.apply();
            }

        } else if ("open".equals(command)) {
            // Open for business
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("position_closed", false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                editor.apply();
            }

        } else if ("settings".equals(command)) {
            // Update terminal settings from JSON
            String json = intent.getStringExtra("json");
            this.fa.updateSettingsFromJson(json);
            Log.v("tag", "Server pushed new settings:");
            Log.v("tag", json);
        } else if ("signature".equals(command)) {
            Log.d("signature", "Prompting for signature for badge");
            int badgeId = intent.getIntExtra("badge_id", -1);
            String name = intent.getStringExtra("name");
            String agreement = intent.getStringExtra("agreement");
            Log.d("signature", "id: "+ badgeId);
            Log.d("signature", "name: " + name);
            this.fa.getSignatureForBadge(badgeId, name, agreement);
        }
    }
}
