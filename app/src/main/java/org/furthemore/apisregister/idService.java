package org.furthemore.apisregister;

import android.util.Log;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by redxine on 3/4/18.
 */

public class idService extends FirebaseInstanceIdService {
    @Override public void onTokenRefresh() {
        Log.d("ChargeAssistant", "Token: " + FirebaseInstanceId.getInstance().getToken());
    }
}
