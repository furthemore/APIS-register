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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextClock;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.squareup.sdk.pos.ChargeRequest;
import com.squareup.sdk.pos.CurrencyCode;
import com.squareup.sdk.pos.PosClient;
import com.squareup.sdk.pos.PosSdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLEncoder;
import java.util.EnumSet;
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
    static final int REQUEST_QR_CODE_JSON = 2;
    static final int REQUEST_SIGNATURE_CODE = 4;

    private final String DEFAULT_BASE_URL = "http://dawningbrooke.net/apis";
    private String base_url = "http://dawningbrooke.net/apis";

    private String apis_api_key = BuildConfig.APIS_API_KEY;

    private PosClient posClient;

    private String html = "";

    private String note = "";
    private int charge_total = 0;

    SharedPreferences prefs;

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
            webview.setVisibility(View.VISIBLE);

        } else {
            String url = prefs.getString(getResources().getString(R.string.pref_webview_url), null);
            if ((url != null) && (!"".equals(url)) && (!"null".equals(url))) {
                if (!url.equals(webview.getUrl())){
                    webview.setVisibility(View.VISIBLE);
                    webview.loadUrl(url);
                }
            } else {
                Log.d("webview", "No webview URL set - hiding");
                webview.setVisibility(View.GONE);
            }
        }

    }

    protected String formatHTML(SharedPreferences prefs, String body) {
        int background_color = prefs.getInt("background_color", Color.parseColor("#0099cc"));
        int foreground_color = prefs.getInt("foreground_color", Color.parseColor("#ffffff"));
        String bgcolor = String.format("#%06X", (0xFFFFFF & background_color));
        String fgcolor = String.format("#%06X", (0xFFFFFF & foreground_color));

        String html = "<html><head>" +
                "</head><body bgcolor='" + bgcolor + "' style='color: " + fgcolor + "'>" +
                body + "</body></html>";

        return html;
    }

    private class RegisterForPushNotificationsAsync extends AsyncTask<Void, Void, Exception> {
        protected Exception doInBackground(Void... params) {
            try {
                // Assign a unique token to this device
                final String deviceToken = Pushy.register(FullscreenActivity.this);

                // Log it for debugging purposes
                Log.d("APIS", "Pushy device token: " + deviceToken);

                String url = base_url + "/registration/firebase/register";

                final String terminal_name = URLEncoder.encode(prefs.getString("terminal_name", "Unnamed"), "utf-8");

                // this doesn't seem to work - defer registration until the name is set
                registerWithServer(deviceToken, terminal_name);

            } catch (Exception exc) {
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

    public void scanQrCode(View v) {
        String [] permissions = {Manifest.permission.CAMERA};
        int permissions_results = 0;

        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
        ActivityCompat.requestPermissions(this, permissions, permissions_results);
        int camerapermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (camerapermission == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(FullscreenActivity.this, ScanActivity.class);
            startActivityForResult(intent, REQUEST_QR_CODE_JSON);
        } else {
            Toast.makeText(getApplicationContext(), "Camera permissions denied.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        super.onCreate(savedInstanceState);

        // Dynamically create the PushReceiver so that the main activity is accessible to it
        pushreceiver = new PushReceiver();
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

        apis_api_key = prefs.getString(getResources().getString(R.string.pref_apis_api_key), BuildConfig.APIS_API_KEY);

        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d("ConfigChange", "Configuration change detected for " + key);
                onUpdate();
                SharedPreferences.Editor editor = prefs.edit();

                if (getResources().getString(R.string.pref_terminal_name).equals(key)) {
                    updateServerRegistration();
                }

                if (getResources().getString(R.string.pref_base_url).equals(key)) {
                    base_url = prefs.getString(getResources().getString(R.string.pref_base_url), DEFAULT_BASE_URL);
                    if ("/".equals(base_url.substring(base_url.length() - 1))) {
                        base_url = base_url.substring(0, base_url.length() - 1);
                        editor.putString(getResources().getString(R.string.pref_base_url), base_url);
                        editor.apply();
                    }
                    Log.d("ConfigChange", base_url);
                }

                if (getResources().getString(R.string.pref_square_client_id).equals(key)) {
                    posClient = PosSdk.createClient(getBaseContext(), prefs.getString("square_client_id", BuildConfig.SQUARE_CLIENT_ID));
                }

                if (getResources().getString(R.string.pref_apis_api_key).equals(key)) {
                    apis_api_key = prefs.getString(getResources().getString(R.string.pref_apis_api_key), BuildConfig.APIS_API_KEY);
                    updateServerRegistration();
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

        this.base_url = prefs.getString(getResources().getString(R.string.pref_base_url), DEFAULT_BASE_URL);


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

        posClient = PosSdk.createClient(this, prefs.getString("square_client_id", BuildConfig.SQUARE_CLIENT_ID));

        WebView webview = findViewById(R.id.web_view);
        webview.getSettings().setJavaScriptEnabled(true);

    }

    public void updateSettingsFromJson(String json) {
        SharedPreferences.Editor editor = prefs.edit();

        /*  Example settings JSON format

            {
              "v": 1,
              "client_id": "sq0idp-xxxxxxxxxxxxxxxxxxxxxx",
              "api_key": "b7b86d4d44743772532705d5d1d1e673",
              "endpoint": "https://stage.furthemore.org/apis",
              "name" : "Terminal 1,
              "location_id": "ABCDEFGHIJK",
              "force_location": true,
              "bg": "#0099cc",
              "fg": "#ffffff",
              "webview": "https://www.furthemore.org/code-of-conduct-embed/",
            }

         */

        try {
            JSONObject config_json = new JSONObject(json);
            int version = config_json.getInt("v");
            if (version != 1) {
                throw new JSONException("Incorrect version: "+version);
            }

            editor.putString(getResources().getString(R.string.pref_square_client_id),
                    config_json.getString("client_id"));
            editor.putString(getResources().getString(R.string.pref_base_url),
                    config_json.getString("endpoint"));
            editor.putString(getResources().getString(R.string.pref_apis_api_key),
                    config_json.getString("api_key"));
            editor.putString(getResources().getString(R.string.pref_terminal_name),
                    config_json.getString("name"));
            editor.putString(getResources().getString(R.string.pref_location_id),
                    config_json.getString("location_id"));
            editor.putBoolean(getResources().getString(R.string.pref_force_location),
                    config_json.getBoolean("force_location"));

            String fg_color_string = config_json.getString("fg");
            int fg_color = prefs.getInt(getResources().getString(R.string.pref_foreground_color), 0);
            try {
                fg_color = Color.parseColor(fg_color_string);
            } catch (IllegalArgumentException e) {
                Log.e("tag", "Unable to parse fg: "+e.toString());
            }

            String bg_color_string = config_json.getString("bg");
            int bg_color = prefs.getInt(getResources().getString(R.string.pref_background_color), 0);

            try {
                bg_color = Color.parseColor(bg_color_string);
            } catch (IllegalArgumentException e) {
                Log.e("tag", "Unable to parse bg: "+e.toString());
            }

            try {
                boolean closed = config_json.getBoolean("closed");
                editor.putBoolean(getResources().getString(R.string.pref_position_closed), closed);
            } catch (Exception e) {
            }

            editor.putInt(getResources().getString(R.string.pref_background_color), bg_color);

            editor.putInt(getResources().getString(R.string.pref_foreground_color), fg_color);


            String webview_url = config_json.getString("webview");
            if (webview_url == null) {
                webview_url = "";
            }
            Log.v("json", webview_url);
            editor.putString(getResources().getString(R.string.pref_webview_url), webview_url);

            editor.apply();
            onUpdate();

        } catch (JSONException e) {
            Log.e("json", "Problem while decoding QR code settings");
            Log.e("json", e.getMessage());
            Toast.makeText(getApplicationContext(), "QR Code did not contain settings we understood", Toast.LENGTH_LONG).show();
            return;
        }


        Toast.makeText(getApplicationContext(), "Settings were provisioned successfully", Toast.LENGTH_LONG).show();
    }

    protected void updateServerRegistration() {

        // Update the server with the pushy token and new terminal name
        String terminalName = prefs.getString(getResources().getString(R.string.pref_terminal_name), "Unnamed");
        String deviceToken = prefs.getString("pushyToken", "NO_TOKEN");

        registerWithServer(deviceToken, terminalName);
    }

    protected void registerWithServer(String token, String name) {
        String url = base_url + "/registration/firebase/register";

        url += "?token=" + token
                + "&key=" + apis_api_key
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

        // Schedule a runnable to display UI elements after a delayf
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

    public void getSignatureForBadge(int badgeId, String name, String agreement) {
        Intent intent = new Intent(this, Signature.class);
        intent.putExtra("agreement", agreement);
        intent.putExtra("name", name);
        intent.putExtra("badge_id", badgeId);
        startActivityForResult(intent, REQUEST_SIGNATURE_CODE);
    }

    public void getSignature(View view) {
        // do something when the button's pressed.
        Intent intent = new Intent(this, Signature.class);
        intent.putExtra("agreement", "I agree to the attendee code of conduct");
        intent.putExtra("name", "Kasper Finch");
        intent.putExtra("badge_id", -1);
        startActivity(intent);
    }

    public void testSquareCharge(View view) {
        int total = this.getCharge_total();

        boolean test_mode = prefs.getBoolean("test_mode", false);
        if (test_mode) {
            total = 1_00;
        }

        startTransaction(total, "Test transaction");
    }

    public void startTransaction(int dollarAmount, String note) {
        Set<ChargeRequest.TenderType> tenderTypes = EnumSet.noneOf(ChargeRequest.TenderType.class);

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

        boolean test_mode = prefs.getBoolean("test_mode", false);
        if (test_mode) {
            dollarAmount = 1_00;
        }

        ChargeRequest request = new ChargeRequest.Builder(dollarAmount, CurrencyCode.USD)
                .note(note)
                .autoReturn(3_200, MILLISECONDS)
                .restrictTendersTo(tenderTypes)
                .requestMetadata(this.getReference())
                .enforceBusinessLocation(location_id)
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
        String url = base_url + "/registration/onsite/square/complete";

        apis_api_key = prefs.getString(getResources().getString(R.string.pref_apis_api_key), BuildConfig.APIS_API_KEY);

        url += "?reference=" + reference
                + "&key=" + apis_api_key
                + "&clientTransactionId=" + clientTransactionId;

        if (serverTransactionId != null) {
            url += "&serverTransactionId=" + serverTransactionId;
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

    protected void uploadSignature(String reference, String clientTransactionId, String serverTransactionId) {
        String url = base_url + "/registration/onsite/square/complete";

        apis_api_key = prefs.getString(getResources().getString(R.string.pref_apis_api_key), BuildConfig.APIS_API_KEY);

        url += "?reference=" + reference
                + "&key=" + apis_api_key
                + "&clientTransactionId=" + clientTransactionId;

        if (serverTransactionId != null) {
            url += "&serverTransactionId=" + serverTransactionId;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                ChargeRequest.Error error = posClient.parseChargeError(data);
                Log.i("Square", "Square error: '" + error.code + "'");
                // TRANSACTION_CANCELED
                if (error.code != ChargeRequest.ErrorCode.TRANSACTION_CANCELED) {
                    showDialog("Error: " + error.code, error.debugDescription, null);
                }
            }

        } else if (requestCode == REQUEST_QR_CODE_JSON) {
            if (resultCode == Activity.RESULT_OK) {
                updateSettingsFromJson(data.getStringExtra("text"));
                updateServerRegistration();
            }

        } else if (requestCode == REQUEST_SIGNATURE_CODE) {
            if (resultCode == Activity.RESULT_OK) {

            }
        }
    }

}
