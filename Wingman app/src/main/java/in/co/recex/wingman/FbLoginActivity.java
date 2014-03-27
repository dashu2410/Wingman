package in.co.recex.wingman;

/**
 * Created by Ashutosh on 12/26/13.
 */


import android.app.ActionBar;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;

import com.searchboxsdk.android.StartAppSearch;
import com.startapp.android.publish.StartAppAd;

public class FbLoginActivity extends FragmentActivity {

    private FbLoginFragment mainFragment;
    private static final String TAG = "MainFragment";
    StartAppAd startAppAd;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        StartAppAd.init(this, "101242287", "201561630");
        StartAppSearch.init(this, "101242287", "201561630");
        startAppAd = new StartAppAd(this);



        if (savedInstanceState == null) {
            // Add the fragment on initial activity setup
            mainFragment = new FbLoginFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, mainFragment)
                    .commit();
        } else {
            // Or set the fragment from restored state info
            mainFragment = (FbLoginFragment) getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
        }
    }
    public boolean isConnectingToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (NetworkInfo anInfo : info)
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }
}
