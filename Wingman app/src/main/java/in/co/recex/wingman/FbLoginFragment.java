package in.co.recex.wingman;

/**
 * Created by Ashutosh on 12/26/13.
 */

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.startapp.android.publish.StartAppAd;

import java.util.Arrays;

public class FbLoginFragment extends Fragment {




    // facebook things
    private static final String TAG = "MainFragment";
    private UiLifecycleHelper uiHelper;
    Integer randomCounter;
    StartAppAd startAppAd;
    AlertDialog alertDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


         startAppAd = new StartAppAd(getActivity());
        //DashboardActivity.lastCalledActivity =6;

        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fblogin, container, false);
        randomCounter = 0;

        UserFunctions userFunctions = new UserFunctions();
        alertDialog=userFunctions.showErrorHavingMessage("Dear User, \n you may find the advertisements to be slightly intrusive, but please bear with us, as we are trying our level best to keep the application free so that it reaches as many people as possible. To show some support, please do CLICK on any ad to INSTALL the app so that your Wingman may stay alive and healthy. Thank you :)", getActivity());
        // facebook things
        LoginButton authButton = (LoginButton) view
                .findViewById(R.id.authButton);
        authButton.setFragment(this);
        // to disable the double login causing the dialog box leakage from the screen
        authButton.setHapticFeedbackEnabled(true);
        authButton.setReadPermissions(Arrays.asList("user_location"
                , "user_likes"));
        return view;
    }

    private void onSessionStateChange(Session session, SessionState state,
                                      Exception exception) {
        Log.d("Session state", state.toString());
       if (state.isClosed()) {
            Log.i(TAG, "Logged out...");
        }else if (state.isOpened()){
           Log.d("session state random counter", randomCounter.toString());
            if(randomCounter==0)
            loginfunction();
            randomCounter++;
        }
    }

    private Session.StatusCallback callback = new Session.StatusCallback() {
        public void call(Session session, SessionState state,
                         Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        startAppAd.onResume();
        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        Session session = Session.getActiveSession();
        if (session != null && (session.isOpened() || session.isClosed())) {
            onSessionStateChange(session, session.getState(), null);
        }

        uiHelper.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        startAppAd.onPause();
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    private void loginfunction() {

        startAppAd.showAd();
        startAppAd.loadAd();
        if (alertDialog!=null)
            alertDialog.dismiss();
        Intent dashboard = new Intent(getActivity(), DashboardActivity.class);

        // Close all views before launching Dashboard
        dashboard.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(dashboard);
        getActivity().finish();
    }


}

