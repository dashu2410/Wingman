package in.co.recex.wingman;

/**
 * Created by Ashutosh on 12/26/13.
 */

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.facebook.model.GraphUser;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressLint("SimpleDateFormat")
public class UserFunctions {

    private JSONParser jsonParser ;
    private String changeTime;
    Date changeDate;
    Date now;

    // Testing in localhost using wamp or xampp
    // use http://10.0.2.2/ to connect to your localhost ie http://localhost/
    private static String loginURL = "http://recex.co.in/android/TheWingman/index.php";
    private static String registerURL = "http://recex.co.in/android/TheWingman/index.php";

    private static String login_tag = "login";
    private static String register_tag = "register";

    public UserFunctions(){
        jsonParser = new JSONParser();
    }

    public JSONObject loginUser(String email, String password){
        // Building Parameters
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag", login_tag));
        params.add(new BasicNameValuePair("userName", email));
        params.add(new BasicNameValuePair("password", password));
        JSONObject json = jsonParser.getJSONFromUrl(loginURL, params);
        // return json

        return json;
    }

    public JSONObject registerUser(String name, String email, String password){
        // Building Parameters
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag", register_tag));
        params.add(new BasicNameValuePair("name", name));
        params.add(new BasicNameValuePair("userName", email));
        params.add(new BasicNameValuePair("password", password));

        // getting JSON Object
        JSONObject json = jsonParser.getJSONFromUrl(registerURL, params);
        // return json


        return json;
    }

    public boolean isConnectingToInternet(Context context){
        ConnectivityManager connectivity = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED)
                    {
                        return true;
                    }

        }
        return false;
    }

    public void checkConnection(Context context){
        if(!isConnectingToInternet(context)){
            AlertDialog.Builder builder= new AlertDialog.Builder(context).setNegativeButton("okay", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    System.exit(0);
                }
            }).setMessage("It seems you aren't connected to the internet right now, please come back when you have connectivity. Sorry for the inconvenience. ");
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    public boolean checkIfCrushAddable(String ID){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag", "checkIfCrushAddable"));
        params.add(new BasicNameValuePair("ID", ID));
        JSONObject json = jsonParser.getJSONFromUrl(registerURL, params);
        Log.d("checkIfCrushAddable","got the JSON from URL" + json.toString());

        try {
            @SuppressWarnings("unused")
            String changeTime= json.getString("TimeOfChange");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        SimpleDateFormat format = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");

        try {
            changeDate = format.parse(changeTime);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Calendar c = Calendar.getInstance();
        now = c.getTime();
        long diffDays = now.getTime() - changeDate.getTime();
        diffDays = diffDays/(1000*60*60*24)%24;
        if(diffDays>=7)
            return true;
        else
            return false;

    }

    public JSONObject addUserToDB(String ID, String User){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag","addUserToDB"));
        params.add(new BasicNameValuePair("ID",ID));
        params.add(new BasicNameValuePair("user",User));
        JSONObject json = jsonParser.getJSONFromUrl(loginURL, params);
        return json;
    }

    public JSONObject removeUserFromDB(String ID){
        List <NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag","removeUser"));
        params.add(new BasicNameValuePair("ID",ID));

        return jsonParser.getJSONFromUrl(loginURL,params);
    }

    public JSONObject checkIfUserExists (String ID){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag","checkIfUserExists"));
        params.add(new BasicNameValuePair("ID",ID));

        //returning the obtained JSON
        return jsonParser.getJSONFromUrl(loginURL,params);
    }

    public JSONObject getCrushFromUser (String ID){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag", "getCrushFromUser"));
        params.add(new BasicNameValuePair("ID", ID));

        //get the response JSON

        return jsonParser.getJSONFromUrl(loginURL, params);
    }

    public JSONObject removeCrushFromUser(String ID){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag","removeCrushFromUser"));
        params.add(new BasicNameValuePair("ID", ID));

        //get the response JSON
        return jsonParser.getJSONFromUrl(loginURL,params);
    }

    public JSONObject addCrushFromUser (String ID, String CrushList,Context context){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag","addCrushFromUser"));
        params.add(new BasicNameValuePair("ID", ID));
        params.add(new BasicNameValuePair("CrushList",CrushList));

        //get the response JSON
        return jsonParser.getJSONFromUrl(loginURL, params);
    }

    public  void checkCrush(String ID, Context context){

        JSONObject usersCrushJson = getCrushFromUser(ID);
        String usersCrushId = null;
        try {
             usersCrushId = DashboardActivity.getValueFromTag(usersCrushJson.getString("crushList"),"id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (usersCrushId==null){
            showErrorHavingMessage("Sorry, it seems the Wingman isn't able to retrieve your crush from his memory, please try again?", context);
            return;
        }

        JSONObject crushesCrushIdJson = getCrushFromUser(usersCrushId);
        String crushCrushIdString = null;
        try {
             crushCrushIdString = DashboardActivity.getValueFromTag(crushesCrushIdJson.getString("crushList"),"id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (crushCrushIdString == null){
            showErrorHavingMessage("It seems your crush hasn't told the Wingman about there crush. Please share the Wingman until your crush gets it too!",context);
            return;
        }
        if (crushCrushIdString.equals(ID)){
            showErrorHavingMessage("Wow! You are indeed very lucky! Your crush has told the Wingman that they like you too! Congratulations! Keep on sharing so that other people can Enjoy the happiness too! Best of luck now!",context);
        }
        else {
            showErrorHavingMessage("The Wingman is sad to tell you that unfortunately, your crush has a crush on someone else right now. Well, there are plenty of fish in the sea, so why worry? :)",context);
        }
    }

    public AlertDialog showErrorHavingMessage(String Message, Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle("The Wingman says:");
        builder.setMessage(Message);
        builder.setNegativeButton("Okay", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        return alertDialog;
    }

    public AlertDialog showErrorHavingMessage(String Message, Context context, AlertDialog.OnClickListener onClickListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle("The Wingman says:");
        builder.setMessage(Message);
        builder.setNegativeButton("Okay", onClickListener);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        return alertDialog;
    }

    public JSONObject changeCrushFromUser (GraphUser User, List<GraphUser> CrushList, Context context){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tag","changeCrushFromUser"));
        String ID = User.getId();
        params.add(new BasicNameValuePair("ID", ID));
        params.add(new BasicNameValuePair("CrushList", CrushList.toString()));

        //get the response JSON
        JSONObject json = jsonParser.getJSONFromUrl(loginURL, params);

        return json;

    }
}
