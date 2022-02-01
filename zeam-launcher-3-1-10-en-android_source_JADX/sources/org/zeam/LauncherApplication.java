package org.zeam;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class LauncherApplication extends Application {
    private static final String LOG_TAG = LauncherApplication.class.getSimpleName();

    public void onCreate() {
        Log.i(LOG_TAG, "\n--- " + getString(C0041R.string.application_name).toUpperCase() + " " + getVersionName(getApplicationContext()) + " ---");
        super.onCreate();
    }

    static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
