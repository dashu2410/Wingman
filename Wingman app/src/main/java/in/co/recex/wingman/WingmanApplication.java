package in.co.recex.wingman;

/**
 * Created by Ashutosh on 12/26/13.
 */

import android.app.Application;
import android.content.Context;

import com.facebook.model.GraphUser;

import java.util.List;

public class WingmanApplication extends Application{

    private List<GraphUser> selectedUsers;
    public static boolean isCrushListDataSet=false;
    private static String crushName;
    private static boolean isDashboardDataSet=false;
    private static WingmanApplication wingmanApplication;

    public WingmanApplication(){
        wingmanApplication =this;
    }

    public static Context getContext(){
        return wingmanApplication;
    }

    public static String getstring(int resId)
    {
        return getContext().getString(resId);
    }

    public boolean isDashboardDataDataSet() {
        return isDashboardDataSet;
    }
    public void setIsDashboardDataSet(boolean isdashboardDataDataSet) {
        this.isDashboardDataSet = isdashboardDataDataSet;
    }
    public String getCrushName(){
        return crushName;
    }
    public void setCrushName(String Name){
        crushName = Name;
    }

    public List<GraphUser> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<GraphUser> users) {
        selectedUsers = users;
    }

}