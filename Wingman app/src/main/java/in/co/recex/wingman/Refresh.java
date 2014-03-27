package in.co.recex.wingman;

/**
 * Created by Ashutosh on 12/26/13.
 */

import android.app.Activity;
import android.os.Bundle;

public class Refresh extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        finishActivity();
        DashboardActivity.lastCalledActivity =2;
    }

    private void finishActivity() {
        // TODO Auto-generated method stub

        WingmanApplication app = (WingmanApplication) getApplication();
        app.isCrushListDataSet=true;
        setResult(RESULT_OK, null);
        finish();

    }


}
