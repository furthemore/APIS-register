package org.furthemore.apisregister;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextClock;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.squareup.sdk.pos.ChargeRequest;
import com.squareup.sdk.pos.CurrencyCode;
import com.squareup.sdk.pos.PosSdk;
import com.squareup.sdk.pos.PosClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private static final int CHARGE_REQUEST_CODE = 1;

    private PosClient posClient;

    private String html = "";

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    protected String getHtml() {
        return this.html;
    }

    protected void setHtml(String html) {
        this.html = html;
    }

    protected void onUpdate() {
        // Handle updating of preferences and settings
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d("ChargeAssistant", "Token: " + token);

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int background_color = prefs.getInt("background_color", Color.parseColor("#0099cc"));
        findViewById(R.id.fullscreen_content).setBackgroundColor(background_color);

        Button payment_button = findViewById(R.id.payment_button);

        int foreground_color = prefs.getInt("foreground_color", Color.parseColor("#ffffff"));
        TextClock textClock = findViewById(R.id.text_clock);
        textClock.setTextColor(foreground_color);

        WebView receipt = findViewById(R.id.receipt_view);
        String html = formatHTML(prefs, getHtml());
        receipt.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);

        WebView webview = findViewById(R.id.web_view);


        boolean closed = prefs.getBoolean("position_closed", false);
        if (closed) {
            html = formatHTML(prefs, "<h1 style='font-size: 96pt'>Closed</h1><h1>Next Register, Please</h1>");
            webview.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
            html = formatHTML(prefs, "");
            receipt.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);

            payment_button.setVisibility(View.GONE);

        } else {
            String url = prefs.getString("webview_url", null);
            if ((url != null) && (!url.equals(webview.getUrl()))){
                webview.loadUrl(url);
            }
        }

        updateFirebaseToken();

    }

    protected String formatHTML(SharedPreferences prefs, String body) {
        int background_color = prefs.getInt("background_color", Color.parseColor("#0099cc"));
        int foreground_color = prefs.getInt("foreground_color", Color.parseColor("#ffffff"));
        String bgcolor = String.format("#%06X", (0xFFFFFF & background_color));
        String fgcolor = String.format("#%06X", (0xFFFFFF & foreground_color));

        String html = "<html><head>" +
                "</head><body bgcolor='" + bgcolor + "' style='color: " + fgcolor+ "'>" +
                body + "</body></html>";

        return html;
    }

    protected void updateFirebaseToken() {
        final String token = FirebaseInstanceId.getInstance().getToken();
        Log.d("ChargeAssistant", "Token: " + token);

        String url = "http://dawningbrooke.net/apis/registration/firebase/register";

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        final String terminal_name = prefs.getString("terminal_name", "Unnamed");

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("VOLLEY", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("VOLLEY", error.toString());
                Toast.makeText(getApplicationContext(),"Register with server: " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        }) {


            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> data = new HashMap<String, String>();
                data.put("key", (String) BuildConfig.APIS_API_KEY.toString());
                data.put("token", token);
                data.put("name", terminal_name);
                return data;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                    // can get more details such as response.headers
                    //Toast.makeText(getApplicationContext(),"Register with server: " + responseString, Toast.LENGTH_SHORT).show();
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }
        };

        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("command");
            Log.d("mMessageReceiver", "Got Firebase command: " + message);

            Bundle extras = intent.getExtras();
            if (extras != null) {
                Button payment_button = findViewById(R.id.payment_button);
                String command = extras.getString("command");
                if ("clear".equals(command)) {
                    payment_button.setVisibility(View.GONE);
                    setHtml("");
                } else if ("display".equals(command)) {
                    setHtml(extras.getString("html"));

                    int amount = extras.getInt("amount");
                    String note = extras.getString("note");

                    payment_button.setVisibility(View.VISIBLE);
                }

                onUpdate();

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("onMessageReceived"));

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        Button payment_button = findViewById(R.id.payment_button);

        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d("ChargeAssistant", "Token: " + token);

        payment_button.setVisibility(View.GONE);

        // Set preference colours
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        onUpdate();


        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                onUpdate();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        posClient = PosSdk.createClient(this, BuildConfig.SQUARE_CLIENT_ID);

        WebView webview = findViewById(R.id.web_view);
        webview.getSettings().setJavaScriptEnabled(true);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }


    public void getSignature(View view) {
        // do something when the button's pressed.
        Intent intent = new Intent(this, Signature.class);
        startActivity(intent);
    }

    public void testSquareCharge(View view) {
        startTransaction(1_00, "Test transaction");
    }

    private void startTransaction(int dollarAmount, String note) {
        ChargeRequest request = new ChargeRequest.Builder(dollarAmount, CurrencyCode.USD)
                .note("note")
                .autoReturn(3_200, MILLISECONDS)
                .build();

        try {
            Intent intent = posClient.createChargeIntent(request);
            startActivityForResult(intent, CHARGE_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            showDialog("Error", "Square POS is not installed", null);
            posClient.openPointOfSalePlayStoreListing();
        }

    }

    private void showDialog(String title, String message, DialogInterface.OnClickListener listener) {
        Log.d("ChargeActivity", title + " " + message);
        new AlertDialog.Builder(this).setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, listener)
                .show();
    }

    public void openSettings(View view) {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHARGE_REQUEST_CODE) {
            if (data == null) {
                showDialog("Error", "Square POS was uninstalled or crashed", null);
                return;
            }

            if (resultCode == Activity.RESULT_OK) {
                ChargeRequest.Success success = posClient.parseChargeSuccess(data);
                String message = "Client transaction id: " + success.clientTransactionId;
                Toast.makeText(this, "Success, " + message, Toast.LENGTH_LONG).show();
            } else {
                ChargeRequest.Error error = posClient.parseChargeError(data);
                showDialog("Error: " + error.code, error.debugDescription, null);
            }
        }
    }
}
