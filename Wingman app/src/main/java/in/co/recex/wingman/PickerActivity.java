package in.co.recex.wingman;

/**
 * Created by Ashutosh on 12/26/13.
 */


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.facebook.FacebookException;
import com.facebook.widget.FriendPickerFragment;
import com.facebook.widget.PickerFragment;

public class PickerActivity extends FragmentActivity {

    public static final Uri FRIEND_PICKER = Uri.parse("picker://friend");
    private FriendPickerFragment friendPickerFragment;
    private UserFunctions userFunctions;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pickers);
        userFunctions = new UserFunctions();
        if(!userFunctions.isConnectingToInternet(this)){
            userFunctions.showErrorHavingMessage("It seems you aren't connected to the internet or your connection is very unstable! Please try again later",this, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    System.exit(0);
                }
            });
        }
        Bundle args = getIntent().getExtras();
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragmentToShow = null;
        Uri intentUri = getIntent().getData();
        DashboardActivity.lastCalledActivity =1;


        if (FRIEND_PICKER.equals(intentUri)) {
            if (savedInstanceState == null) {
                friendPickerFragment = new FriendPickerFragment(args);
                friendPickerFragment.setMultiSelect(false);
            } else {
                friendPickerFragment =
                        (FriendPickerFragment) manager.findFragmentById(R.id.picker_fragment);
            }
            // Set the listener to handle errors
            friendPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
                @Override
                public void onError(PickerFragment<?> fragment,
                                    FacebookException error) {
                    PickerActivity.this.onError(error);
                }
            });
            // Set the listener to handle button clicks
            friendPickerFragment.setOnDoneButtonClickedListener(
                    new PickerFragment.OnDoneButtonClickedListener() {
                        @Override
                        public void onDoneButtonClicked(PickerFragment<?> fragment) {
                            finishActivity();
                        }
                    });
            fragmentToShow = friendPickerFragment;

        } else {
            // Nothing to do, finish
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        manager.beginTransaction()
                .replace(R.id.picker_fragment, fragmentToShow)
                .commit();
    }

    private void onError(Exception error) {
        onError(error.getLocalizedMessage(), true);
    }

    private void onError(String error, final boolean finishActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error_dialog_title).
                setMessage("An error has just occured. Sorry for the inconvenience!").
                setPositiveButton(R.string.error_dialog_button_text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (finishActivity) {
                                    System.exit(0);
                                }
                            }
                        });
        builder.show();
    }

    private void finishActivity() {
        WingmanApplication app = (WingmanApplication) getApplication();
        if (FRIEND_PICKER.equals(getIntent().getData())) {
            if (friendPickerFragment != null) {
                app.setSelectedUsers(friendPickerFragment.getSelection());
            }
        }
        setResult(RESULT_OK, null);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FRIEND_PICKER.equals(getIntent().getData())) {
            try {
                friendPickerFragment.loadData(false);
            } catch (Exception ex) {
                onError(ex);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //checking for continuity of internet
        if(!userFunctions.isConnectingToInternet(this)){
            userFunctions.showErrorHavingMessage("It seems you aren't connected to the internet or your connection is very unstable! Please try again later",this, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    System.exit(0);
                }
            });
        }
    }
}
