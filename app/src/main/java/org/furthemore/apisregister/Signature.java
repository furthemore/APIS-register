package org.furthemore.apisregister;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.github.gcacace.signaturepad.views.SignaturePad;

import java.io.ByteArrayOutputStream;

public class Signature extends AppCompatActivity {

    private View mContentView;
    private SignaturePad mSignaturePad;
    private TextView mAgreementText;
    private TextView mNameText;
    private Button mCancelButton;
    private Button mClearButton;
    private Button mSaveButton;

    private int badgeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);
        mContentView = findViewById(R.id.signature_pad_container);

        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mSignaturePad = findViewById(R.id.signature_pad);
        mSignaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {
                //Toast.makeText(Signature.this, "OnStartSigning", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSigned() {
                mSaveButton.setEnabled(true);
                mClearButton.setEnabled(true);
            }

            @Override
            public void onClear() {
                mSaveButton.setEnabled(false);
                mClearButton.setEnabled(false);
            }
        });

        mAgreementText = (TextView) findViewById(R.id.signature_pad_description);
        mNameText = (TextView) findViewById(R.id.signature_pad_name);

        String agreement = getIntent().getStringExtra("agreement");
        if (agreement != null) {
            mAgreementText.setText(agreement);
        }

        String name_text = getIntent().getStringExtra("name");
        if (name_text != null) {
            mNameText.setText(name_text);
        }

        badgeId = getIntent().getIntExtra("badge_id", -1);

        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mClearButton = (Button) findViewById(R.id.clear_button);
        mSaveButton = (Button) findViewById(R.id.save_button);

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
                //onBackPressed();
            }
        });

        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignaturePad.clear();
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //mSignaturePad.clear();
                Log.d("signature", mSignaturePad.getSignatureSvg());


                Intent resultIntent = new Intent();
                resultIntent.putExtra("signature_svg", mSignaturePad.getSignatureSvg());

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mSignaturePad.getSignatureBitmap().compress(Bitmap.CompressFormat.PNG, 90, stream);
                byte[] bytes = stream.toByteArray();

                resultIntent.putExtra("signature_bmp", bytes);

                Log.d("signature", "Finish intent RESULT_OK");
                setResult(Activity.RESULT_OK, resultIntent);
                finish();

                //onBackPressed();
            }
        });
    }
}
