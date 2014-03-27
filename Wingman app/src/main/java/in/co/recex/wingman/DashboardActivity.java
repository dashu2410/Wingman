package in.co.recex.wingman;

/**
 * Created by Ashutosh on 12/26/13.
 */

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.ProfilePictureView;
import com.searchboxsdk.android.StartAppSearch;
import com.startapp.android.publish.StartAppAd;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends FragmentActivity {

    static Integer lastCalledActivity =-1;
    // 0 -> no activity
    // 1 -> friend picker
    // 2 -> RefreshActivity

    UserFunctions userFunctions;

    private List<GraphUser> selectedUsers;

    private StartAppAd startAppAd;
    private List<BaseListElement> listElements;

    private ProfilePictureView profilePictureView;
    private TextView userNameView;
    private static final int REAUTH_ACTIVITY_CODE = 100;

    private static final String TAG = "DashBoard";
    JSONObject crushListJson;
    WingmanApplication app = (WingmanApplication) getApplication();

    Request request;



    private UiLifecycleHelper uiHelper;

    private static GraphUser User;
    private static String userName;
    private static String userId;
    private static String userData;

    RequestAsyncTask requestasync = null;

    private static JSONObject crushListFromServer;

    private boolean isFirstTimer = true;

    private ProgressDialog pd;



    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        //Advertisement things
        startAppAd = new StartAppAd(this);
        StartAppSearch.showSearchBox(this);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // facebook things
        LoginButton authButton = (LoginButton) findViewById(R.id.authButton);
        Button checkCrush = (Button) findViewById(R.id.checkCrushButton);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
        // Find the user's profile picture custom view
        profilePictureView = (ProfilePictureView) findViewById(R.id.selection_profile_pic);
        profilePictureView.setCropped(false);

        // Find the user's name view
        userNameView = (TextView) findViewById(R.id.selection_user_name);
        checkCrush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userId != null) {
                    checkButtonAsyncRetriever check = new checkButtonAsyncRetriever();
                    check.execute(userId);
                } else {
                    userFunctions.showErrorHavingMessage("it seems the wingman can't retrieve your crush from his memory, please try again!", getActivity());
                }
            }
        });




        /**
         * Dashboard Screen for the application
         * */
         // Check login status in database
        userFunctions = new UserFunctions();

        //checking for internet connectivity
        if(userFunctions.isConnectingToInternet(DashboardActivity.this)){
            retrieveFromServerAsync newRetrieve = new retrieveFromServerAsync();
            newRetrieve.execute();
        }else {
            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
            builder.setMessage("It seems you dont have connectivity right now, please try again when you do.");
            builder.setTitle("Wingman says:");
            builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    System.exit(0);
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }



        // Find the list view
        ListView listView = (ListView) findViewById(R.id.selection_list);

        Session session = Session.getActiveSession();

        // Set up the list view items, based on a list of
        // BaseListElement items
        listElements = new ArrayList<BaseListElement>();
        // Add an item for the friend picker
        listElements.add(new PeopleListElement(0));

        if (savedInstanceState != null) {
            // Restore the state for each list element
            for (BaseListElement listElement : listElements) {
                listElement.restoreState(savedInstanceState);
            }
        }

        // Set the list view adapter
        listView.setAdapter(new ActionListAdapter(getApplicationContext(),
                R.id.selection_list, listElements));

        // user already logged in show dashboard
        // to check if facebook session is across all activities
        if (session != null) {
            Log.d("facebook session", "the session is active");
        }
    }

    // again, facebook things
    private void onSessionStateChange(Session session, SessionState state,
                                      Exception exception) {
        //TODO: check if this code works or not.
        if(exception != null){
            userFunctions.showErrorHavingMessage("It seems some error has occured, sorry for the inconvenience!",this,new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    System.exit(0);
                }
            });
        }
        Log.d("Session state","session state changed!");
        if (state.isOpened()) {
            Log.i(TAG, "Logged in...");
            makeMeRequest(session);

        } else if (state.isClosed()) {
            // to go back to login
            Intent login = new Intent(this, FbLoginActivity.class);

            // Close all views before launching Dashboard
            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(login);
            finish();
            Log.i(TAG, "Logged out...");
        }

    }

    @Override
    public void onBackPressed() {
        startAppAd.onBackPressed();
        super.onBackPressed();
    }

    private Session.StatusCallback callback = new Session.StatusCallback() {
        public void call(Session session, SessionState state,
                         Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    private DashboardActivity getActivity() {
        return this;

    }

    void makeMeRequest(final Session session) {
        // Make an API call to get user data and define a
        // new callback to handle the response.
        request = Request.newMeRequest(session,
                new Request.GraphUserCallback() {

                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        // If the response is successful
                        if (session == Session.getActiveSession()) {
                            if (user != null) {
                                // Set the id for the ProfilePictureView
                                // view that in turn displays the profile
                                // picture.
                                User = user;
                                userName = User.getName();
                                userId = User.getId();
                                userData = User.toString();

                                profilePictureView.setProfileId(userId);
                                // Set the Textview's text to the user's name.
                                userNameView.setText(userName);
                            }
                        }
                        if (response.getError() != null) {
                            // Handle errors, will do so later.
                            userFunctions.showErrorHavingMessage(response.getError().toString(), DashboardActivity.this);
                        }

                       /* //checking for internet connectivity
                        if(userFunctions.isConnectingToInternet(DashboardActivity.this)){
                            retrieveFromServerAsync newretrieve = new retrieveFromServerAsync();
                            newretrieve.execute();
                        }else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
                            builder.setMessage("It seems you dont have connectivity right now, please try again when you do.");
                            builder.setTitle("Wingman says:");
                            builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    System.exit(0);
                                }
                            });
                            AlertDialog alertDialog = builder.create();
                            alertDialog.show();
                        }*/
                    }
                });
        requestasync = request.executeAsync();
    }

    private List<GraphUser> restoreByteArray(byte[] bytes) {
        try {
            @SuppressWarnings("unchecked")
            List<String> usersAsString = (List<String>) (new ObjectInputStream(
                    new ByteArrayInputStream(bytes))).readObject();
            if (usersAsString != null) {
                List<GraphUser> users = new ArrayList<GraphUser>(
                        usersAsString.size());
                for (String user : usersAsString) {
                    GraphUser graphUser = GraphObject.Factory.create(
                            new JSONObject(user), GraphUser.class);
                    users.add(graphUser);
                }
                return users;
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to deserialize users.", e);
        } catch (IOException e) {
            Log.e(TAG, "Unable to deserialize users.", e);
        } catch (JSONException e) {
            Log.e(TAG, "Unable to deserialize users.", e);
        }
        return null;
    }

    private void startPickerActivity(Uri data, int requestCode) {
        Intent intent = new Intent();
        intent.setData(data);
        intent.setClass(getApplicationContext(), PickerActivity.class);
        startActivityForResult(intent, requestCode);
    }

    private void populateCrushListFromJson() {

        Intent refresh = new Intent(this, Refresh.class);
        int requestCode = 0;
        startActivityForResult(refresh, requestCode);
        Log.d("Refresh status", "refreshing now...");
    }

    public static String getValueFromTag(String mainString, String key) {
        int c = 0;
        StringBuilder keyString = new StringBuilder();
        StringBuilder valString = new StringBuilder();

        for (; c < mainString.length(); c++) {
            if (mainString.charAt(c) == '"') {
                c++;
                for (; c < mainString.length() && mainString.charAt(c) != '"'; c++) {

                    keyString.append(mainString.charAt(c));
                }
                c++;
                if (keyString.toString().equals(key)) {
                    //TODO: check for some error here, things seem slightly suspicous
                    for (; c < mainString.length()
                            && mainString.charAt(c) != '"'; c++);
                    c++; //to miss the semi-colon in between the value and key
                  //  c++;
                    for (; c < mainString.length()
                            && mainString.charAt(c) != '"'; c++) {
                        valString.append(mainString.charAt(c)) ;
                    }
                    return valString.toString();
                }

                keyString.delete(0, keyString.length());
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        startAppAd.onResume();
        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        if(!userFunctions.isConnectingToInternet(this)){
            userFunctions.showErrorHavingMessage("It seems you aren't connected to the internet or your connection is very unstable! Please try again later",DashboardActivity.this, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    System.exit(0);
                }
            });
        }
        Session session = Session.getActiveSession();
        if (session != null && (session.isOpened() || session.isClosed())) {
            onSessionStateChange(session, session.getState(), null);
        }

        uiHelper.onResume();

        //on screen rotation
        if (lastCalledActivity ==5){
            lastCalledActivity = 0;
            populateCrushListFromJson();
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REAUTH_ACTIVITY_CODE) {
            uiHelper.onActivityResult(requestCode, resultCode, data);
        } else if (resultCode == Activity.RESULT_OK && requestCode >= 0
                && requestCode < listElements.size()) {
            listElements.get(requestCode).onActivityResult(data);
        }
    }

    public byte[] getByteArray(List<GraphUser> users) {
        // convert the list of GraphUsers to a list of String
        // where each element is the JSON representation of the
        // GraphUser so it can be stored in a Bundle
        List<String> usersAsString = new ArrayList<String>(users.size());

        for (GraphUser user : users) {
            usersAsString.add(user.getInnerJSONObject().toString());
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            new ObjectOutputStream(outputStream).writeObject(usersAsString);
            return outputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Unable to serialize users.", e);
        }
        return null;
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
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        for (BaseListElement listElement : listElements) {
            listElement.onSaveInstanceState(bundle);
        }
        lastCalledActivity = 5; //code for refresh needed
        uiHelper.onSaveInstanceState(bundle);
    }

    private class ActionListAdapter extends ArrayAdapter<BaseListElement> {
        private List<BaseListElement> listElements;

        public ActionListAdapter(Context context, int resourceId,
                                 List<BaseListElement> listElements) {
            super(context, resourceId, listElements);
            this.listElements = listElements;
            // Set up as an observer for list item changes to
            // refresh the view.
            for (int i = 0; i < listElements.size(); i++) {
                listElements.get(i).setAdapter(this);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.listitem, null);
            }

            BaseListElement listElement = listElements.get(position);
            if (listElement != null) {
                view.setOnClickListener(listElement.getOnClickListener());
                ImageView icon = (ImageView) view.findViewById(R.id.icon);
                TextView text1 = (TextView) view.findViewById(R.id.text1);
                TextView text2 = (TextView) view.findViewById(R.id.text2);
                if (icon != null) {
                    icon.setImageDrawable(listElement.getIcon());
                }
                if (text1 != null) {
                    text1.setText(listElement.getText1());
                }
                if (text2 != null) {
                    text2.setText(listElement.getText2());
                }
            }
            return view;
        }

    }

    private class PeopleListElement extends BaseListElement {


        private static final String FRIENDS_KEY = "friends";
        Boolean isSetting;
        Boolean isRefreshing;
        String crushName;

        public PeopleListElement(int requestCode) {
            super(getResources().getDrawable(R.drawable.action_people),
                    getResources().getString(R.string.action_people),
                    getResources().getString(R.string.action_people_default),
                    requestCode);

        }

        @Override
        protected void onSaveInstanceState(Bundle bundle) {
            if (selectedUsers != null) {
                bundle.putByteArray(FRIENDS_KEY, getByteArray(selectedUsers));
            }
        }

        @Override
        protected boolean restoreState(Bundle savedState) {
            byte[] bytes = savedState.getByteArray(FRIENDS_KEY);
            if (bytes != null) {
                selectedUsers = restoreByteArray(bytes);
                setUsersText(getValueFromTag(selectedUsers.toString(),"name"));
                return true;
            }
            return false;
        }

        @Override
        protected View.OnClickListener getOnClickListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // only let the friend picker open if one week has passed
                    if (isFirstTimer)
                        startPickerActivity(PickerActivity.FRIEND_PICKER,
                                getRequestCode());
                    else if (userFunctions.checkIfCrushAddable(userId))
                        startPickerActivity(PickerActivity.FRIEND_PICKER,
                                getRequestCode());

                    else
                        Log.e("Friend Picker", "it hasnt been a week yet");
                }
            };
        }

        private void setUsersText() {
            String text = null;
            if (selectedUsers != null) {
                // If there is one friend
                if (selectedUsers.size() == 1) {
                    text = String.format(
                            WingmanApplication.getstring(
                                    R.string.single_user_selected),
                            selectedUsers.get(0).getName());
                } else if (selectedUsers.size() == 2) {
                    // If there are two friends
                    text = String.format(
                            getResources().getString(
                                    R.string.two_users_selected), selectedUsers
                            .get(0).getName(), selectedUsers.get(1)
                            .getName());
                } else if (selectedUsers.size() > 2) {
                    // If there are more than two friends
                    text = String.format(
                            getResources().getString(
                                    R.string.multiple_users_selected),
                            selectedUsers.get(0).getName(),
                            (selectedUsers.size() - 1));
                }
            }
            if (text == null) {
                // If no text, use the placeholder text
                text = getResources().getString(R.string.action_people_default);
            }
            // Set the text in list element. This will notify the
            // adapter that the data has changed to
            // refresh the list view.
            setText2(text);
        }

        private void setUsersText(String text) {

            if (text == null) {
                // If no text, use the placeholder text
                text = getResources().getString(R.string.action_people_default);
            }
            // Set the text in list element. This will notify the
            // adapter that the data has changed to
            // refresh the list view.
            setText2(text);
        }

        @Override
        protected void onActivityResult(Intent data) {
            isSetting = false;
            isRefreshing = false;
            crushName = null;
            crushName = ((WingmanApplication)getApplication()).getCrushName();
           // Log.d("crushName Value", crushName);
            isRefreshing = crushName!=null;

            if(!isRefreshing){
                Log.d("onActivity",
                        "no refreshing happening");
            }
            Log.d("isRefreshing",isRefreshing.toString() );

            selectedUsers = ((WingmanApplication) getActivity()
                    .getApplication()).getSelectedUsers();
            isSetting = selectedUsers != null;


            if (!isSetting) {
                Log.d("onActivityResult","data refreshing");

                if(crushListFromServer==null){
                    userFunctions.showErrorHavingMessage("It seems you aren't connected to the internet or your connection is very unstable! Please try again later",DashboardActivity.this, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            System.exit(0);
                        }
                    });
                }
                else{
                try {
                    if (crushListFromServer.getInt("error") == 0) {

                        setUsersText(crushName);
                        ((WingmanApplication) getActivity().getApplication())
                                .setIsDashboardDataSet(true);
                    }
                } catch (JSONException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }


                notifyDataChanged();
                }
            }

            if (isSetting) {
                Log.d("onActivityResult","data setting");
                setUsersText();
                notifyDataChanged();
                if (userFunctions.isConnectingToInternet(getApplicationContext()))
                {
                    setCrushOnServerAsync crushSetter = new setCrushOnServerAsync();
                    crushSetter.execute();
                }else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
                    builder.setMessage("It seems you dont have connectivity right now, please try again when you do.");
                    builder.setTitle("Wingman says:");
                    builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            System.exit(0);
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();                }
            }
        }
    }


    public class retrieveFromServerAsync extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd;
        GraphUser crush = null;
        Request getCrushRequest;
        @Override
        protected Void doInBackground(final Void... voids) {
            while(requestasync==null)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            while (requestasync.getStatus()!=Status.FINISHED)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            crushListJson = userFunctions.getCrushFromUser(userId);
            if (crushListJson==null){
                while (crushListJson==null){
                    crushListJson = userFunctions.getCrushFromUser(userId);
                }
            }
               //for construction of GraphUser of crush
            String crushListJsonString = "";
            try {
                crushListJsonString = crushListJson
                        .getString("crushList");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            final String tempCrushIdForGraphObject = getValueFromTag(crushListJsonString,"id");

           /* String crushUserPath = "me/friends?uid="+tempCrushIdForGraphObject;
            Bundle params = new Bundle();
            params.putString("access_token",Session.getActiveSession().getAccessToken());

            final Request.GraphUserCallback newgraphUserCallback = new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {
                    if (response.getError() != null) {
                        // Handle errors, will do so later.
                        Log.e("Facebook Error", response.getError().toString());
                        Log.e("Session state", Session.getActiveSession().getPermissions().toString());
                    }
                    else if (user != null) {
                        crush = user;
                        Log.d("crushGraphObject",crush.toString());
                        List<GraphUser> graphUserList = new ArrayList<GraphUser>();
                        graphUserList.add(user);
                        ((WingmanApplication) getApplication()).setSelectedUsers(graphUserList);
                    }
                }
            };
            Request.Callback newCallBack = new Request.Callback() {

                @Override
                public void onCompleted(Response response) {
                    newgraphUserCallback.onCompleted(response.getGraphObjectAs(GraphUser.class), response);
                }
            };

            Request makeCrushUserRequest = new Request(Session.getActiveSession(),crushUserPath,params, HttpMethod.GET, newCallBack);
            makeCrushUserRequest.executeAndWait();
            */

             getCrushRequest =Request.newMyFriendsRequest(Session.getActiveSession(), new Request.GraphUserListCallback() {
                 @Override
                 public void onCompleted(List<GraphUser> users, Response response) {
                     if (response.getError() != null) {
                         // Handle errors, will do so later.
                         userFunctions.showErrorHavingMessage("Some error occured! :(", DashboardActivity.this);
                     }
                     Integer counter = 0;
                     for (GraphUser user : users) {
                         counter++;
                         if (user.getId().equals(tempCrushIdForGraphObject)) {
                             crush = user;
                             Log.e("counter", counter.toString());
                         }
                     }
                     List<GraphUser> toSetToDBList = new ArrayList<GraphUser>();
                     toSetToDBList.add(crush);
                     ((WingmanApplication)getApplication()).setSelectedUsers(toSetToDBList);
                 }
             });
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(DashboardActivity.this);
            pd.setIndeterminate(true);
            pd.setTitle("Wingman Says");
            pd.setMessage("Loading...");
            pd.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            crushListFromServer = crushListJson;

            if (pd != null)
               pd.dismiss();
            getCrushRequest.executeAsync();

            try {
                if (!WingmanApplication.isCrushListDataSet
                        && crushListJson.getInt("error") != 1) {

                    String crushListJsonString = crushListJson
                            .getString("crushList");
                    app = (WingmanApplication)getApplication();


                    app.setCrushName(getValueFromTag(
                            crushListJsonString, "name"));

                    if (getValueFromTag( crushListJsonString,"name")!=null){

                        populateCrushListFromJson();
                    }
                    else{
                        userFunctions.removeCrushFromUser(userId);
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            super.onPostExecute(aVoid);
        }
    }

    public class checkButtonAsyncRetriever extends AsyncTask <String,Void,Void>{
        ProgressDialog pd = new ProgressDialog(DashboardActivity.this);
        String usersCrushId = null;
        String ID = new String();
        @Override
        protected Void doInBackground(String... strings) {
            ID = strings[0];
            JSONObject usersCrushJson = userFunctions.getCrushFromUser(ID);
            try {
                usersCrushId = DashboardActivity.getValueFromTag(usersCrushJson.getString("crushList"),"id");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(DashboardActivity.this);
            pd.setIndeterminate(true);
            pd.setTitle("Wingman Says");
            pd.setMessage("Checking...");
            pd.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(pd!=null){
                pd.dismiss();
            }
            if (usersCrushId==null){
                userFunctions.showErrorHavingMessage("Sorry, it seems the Wingman isn't able to retrieve your crush from his memory, please try again?", DashboardActivity.this);
                return;

            }

            JSONObject crushesCrushIdJson = userFunctions.getCrushFromUser(usersCrushId);
            String crushCrushIdString = null;
            try {
                crushCrushIdString = DashboardActivity.getValueFromTag(crushesCrushIdJson.getString("crushList"),"id");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (crushCrushIdString == null){
                userFunctions.showErrorHavingMessage("It seems your crush hasn't told the Wingman about their crush. Please share the Wingman until your crush gets it too!", DashboardActivity.this);
                return;
            }
            if (crushCrushIdString.equals(ID)){
                userFunctions.showErrorHavingMessage("Wow! You are indeed very lucky! Your crush has told the Wingman that they like you too! Congratulations! Keep on sharing so that other people can Enjoy the happiness too! Best of luck now!", DashboardActivity.this);
            }
            else {
                userFunctions.showErrorHavingMessage("The Wingman is sad to tell you that unfortunately, your crush has a crush on someone else right now. Well, there are plenty of fish in the sea, so why worry? :)", DashboardActivity.this);
            }
        }
    }

    public class setCrushOnServerAsync extends AsyncTask <Void,Void,Void>{

        ProgressDialog pd = new ProgressDialog(DashboardActivity.this);
        @Override
        protected Void doInBackground(Void... voids) {

            try {
                if (crushListFromServer.getInt("success") == 0) {
                    //Adding the crush to the server
                    JSONObject returnedCrushFromUserJSON = userFunctions
                            .addCrushFromUser(userId,
                                    selectedUsers.toString(),DashboardActivity.this);

                    JSONObject usersCrush = userFunctions.getCrushFromUser(userId);

                    //adding server code redundancy to check if crush adding is actually happening

                    while (usersCrush==null|| usersCrush.getString("crushList")==null||usersCrush.getInt("error")==1){
                        Log.d("crush adding error","some error occurred in adding the crush, retrying...");
                        userFunctions.removeCrushFromUser(userId);
                        userFunctions.addCrushFromUser(userId, selectedUsers.toString(), DashboardActivity.this);
                        usersCrush = userFunctions.getCrushFromUser(userId);
                    }


                    if (returnedCrushFromUserJSON != null
                            && returnedCrushFromUserJSON.getInt("error") != 1)
                        Log.d("addCrushFromUser","Crush added to DB successfully!");


                    //Adding user to the UserDB code
                    userFunctions.addUserToDB(userId, userData);
                    // another code redundancy
                    JSONObject ifUserExistsInDb = null;
                    ifUserExistsInDb = userFunctions.checkIfUserExists(userId);

                    if (ifUserExistsInDb==null){
                        //TODO: set some error handling mechanism here later
                       // userFunctions.showErrorHavingMessage("It seems you have weak or no connectivity to the net, please try again!", DashboardActivity.this);
                    }
                    if (ifUserExistsInDb != null) {
                        while (ifUserExistsInDb.getInt("success")==0){
                            userFunctions.removeUserFromDB(userId);
                            Log.d("adding user to database","some error has occured in the first try, trying again...");
                            userFunctions.addUserToDB(userId, userData);
                            ifUserExistsInDb = userFunctions.checkIfUserExists(userId);
                        }
                    }
                } else {
                    //Code to change crush already set in database
                    JSONObject changeCrushFromUser = userFunctions
                            .changeCrushFromUser(User, selectedUsers, DashboardActivity.this);
                    if (changeCrushFromUser.getInt("success") == 1) {
                        Log.d("changeCrushFromUser", "crush updated to DB successfully");
                    }
                    else while (changeCrushFromUser.getInt("success")!=1){
                        changeCrushFromUser = userFunctions.changeCrushFromUser(User,selectedUsers,DashboardActivity.this);
                        Log.d("change Crush","Some error occured in trying to change crush in database, trying again...");
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(DashboardActivity.this);
            pd.setIndeterminate(true);
            pd.setTitle("Wingman Says");
            pd.setMessage("Storing your crush...");
            pd.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(pd!=null)
                pd.dismiss();
        }
    }

}