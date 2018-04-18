package org.furthemore.apisregister;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.squareup.sdk.pos.ChargeRequest;
import com.squareup.sdk.pos.CurrencyCode;
import com.squareup.sdk.pos.PosSdk;
import com.squareup.sdk.pos.PosClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import me.pushy.sdk.Pushy;

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

    private static final String base_url = "http://dawningbrooke.net/apis";

    private PosClient posClient;

    private String html = "";

    private String note = "";
    private int charge_total = 0;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    private String reference = "";

    private PushReceiver pushreceiver = null;

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
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
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

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return super.getSharedPreferences(name, mode);
    }

    protected void setHtml(String html) {
        this.html = html;

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        WebView receipt = findViewById(R.id.receipt_view);
        html = formatHTML(prefs, this.html);
        receipt.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getCharge_total() {
        return charge_total;
    }

    public void setCharge_total(int charge_total) {
        this.charge_total = charge_total;
    }

    protected void onUpdate() {
        // Handle updating of preferences and settings

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

    private class RegisterForPushNotificationsAsync extends AsyncTask<Void, Void, Exception> {
        protected Exception doInBackground(Void... params) {
            try {
                // Assign a unique token to this device
                final String deviceToken = Pushy.register(getApplicationContext());

                // Log it for debugging purposes
                Log.d("MyApp", "Pushy device token: " + deviceToken);

                String url = base_url + "/registration/firebase/register";

                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                final String terminal_name = URLEncoder.encode(prefs.getString("terminal_name", "Unnamed"));

                // this doesn't seem to work - defer registration until the name is set
                new URL(url + "?token=" + deviceToken
                            + "&key=" + BuildConfig.APIS_API_KEY.toString()
                            + "&name=" + terminal_name).openConnection();

            }
            catch (Exception exc) {
                // Return exc to onPostExecute
                return exc;
            }

            // Success
            return null;
        }

        @Override
        protected void onPostExecute(Exception exc) {
            // Failed?
            if (exc != null) {
                // Show error as toast message
                Toast.makeText(getApplicationContext(), exc.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            // Succeeded, do something to alert the user
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dynamically create the PushReceiver so that the main activity is accessible to it
        pushreceiver= new PushReceiver();
        pushreceiver.setActivityHandler(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction("pushy.me");
        registerReceiver(pushreceiver, filter);

        Pushy.toggleWifiPolicyCompliance(false, this);
        Pushy.listen(this);

        // Check whether the user has granted us the READ/WRITE_EXTERNAL_STORAGE permissions
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request both READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE so that the
            // Pushy SDK will be able to persist the device token in the external storage
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        Button payment_button = findViewById(R.id.payment_button);

        new RegisterForPushNotificationsAsync().execute();

        payment_button.setVisibility(View.GONE);

        // Set preference colours
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        onUpdate();


        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d("ConfigChange", "Configuration change detected for " + key);
                onUpdate();

                if ("terminal_name".equals(key)) {
                    // Update the server with the pushy token and new terminal name
                    String terminalName = prefs.getString("terminal_name", "Unnamed");
                    String deviceToken = prefs.getString("pushyToken", "NO_TOKEN");

                    registerWithServer(deviceToken, terminalName);
                }
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

    protected void registerWithServer(String token, String name) {
        String url = base_url + "/registration/firebase/register";

        url += "?token=" + token
                + "&key=" + BuildConfig.APIS_API_KEY.toString()
                + "&name=" + name;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("VOLLEY", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("VOLLEY", error.toString());
                //Toast.makeText(getApplicationContext(),"Register with server: " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                    Log.d("ConfigChange", "Register with server: " + responseString);
                    //Toast.makeText(getApplicationContext(),"Successfully registered new name with server", Toast.LENGTH_SHORT).show();
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }
        };

        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(stringRequest);

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
        if (pushreceiver != null) {
            unregisterReceiver(pushreceiver);
        }
        super.onDestroy();
    }


    public void getSignature(View view) {
        // do something when the button's pressed.
        Intent intent = new Intent(this, Signature.class);
        startActivity(intent);
    }

    public void testSquareCharge(View view) {
        int total = this.getCharge_total();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        boolean test_mode = prefs.getBoolean("test_mode", false);
        if (test_mode) {
            total = 1_00;
        }

        startTransaction(total, "Test transaction");
    }

    public void startTransaction(int dollarAmount, String note) {
        Set<ChargeRequest.TenderType> tenderTypes = EnumSet.noneOf(ChargeRequest.TenderType.class);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        tenderTypes.add(ChargeRequest.TenderType.CARD);
        boolean allow_cash = prefs.getBoolean("cash_payment", false);
        if (allow_cash) {
            tenderTypes.add(ChargeRequest.TenderType.CASH);
        }

        String location_id = prefs.getString("location_id", "");
        boolean force_location = prefs.getBoolean("force_location", false);
        if (force_location) {
            location_id = "";
        }

        ChargeRequest request = new ChargeRequest.Builder(dollarAmount, CurrencyCode.USD)
                .note("note")
                .autoReturn(3_200, MILLISECONDS)
                .restrictTendersTo(tenderTypes)
                .requestMetadata(this.getReference())
                .build();

        try {
            Intent intent = posClient.createChargeIntent(request);
            startActivityForResult(intent, CHARGE_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            showDialog("Error", "Square POS is not installed", null);
            posClient.openPointOfSalePlayStoreListing();
        }

    }

    protected void completeTransaction(String reference, String clientTransactionId, String serverTransactionId) {
        String url = base_url + "/registration/firebase/register";

        url += "?reference=" +  reference
                + "&key=" + BuildConfig.APIS_API_KEY.toString()
                + "&clientTransactionId=" + clientTransactionId;

        if (serverTransactionId != null) {
            url += "&serverTransactionId";
        }

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("VOLLEY", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("VOLLEY", error.toString());
            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                    Log.d("CompleteTransaction", "Complete transaction server response: " + responseString);
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }
        };

        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
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

                this.completeTransaction(success.requestMetadata, success.clientTransactionId, success.serverTransactionId);
            } else {
                ChargeRequest.Error error = posClient.parseChargeError(data);
                Log.i("Square", "Square error: '" + error.code + "'");
                // TRANSACTION_CANCELED
                if (error.code != ChargeRequest.ErrorCode.TRANSACTION_CANCELED) {
                    showDialog("Error: " + error.code, error.debugDescription, null);
                }
            }
        }
    }
}