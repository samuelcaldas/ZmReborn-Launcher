package org.zmreborn;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class LauncherApplication extends Application {
    private static final String LOG_TAG = LauncherApplication.class.getSimpleName();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    public void onCreate() {
        Log.i(LOG_TAG, "\n--- " + getString(R.string.application_name).toUpperCase() + " " + getVersionName(getApplicationContext()) + " ---");
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
