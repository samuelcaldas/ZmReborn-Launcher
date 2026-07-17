package org.zeam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Parcelable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import org.zeam.CellLayout;
import org.zeam.LauncherSettings;

public final class Launcher extends Activity implements View.OnClickListener, View.OnLongClickListener {
    static final int APPWIDGET_HOST_ID = 1024;
    static final int DEFAULT_SCREEN = 2;
    private static final int DIALOG_ADD = 2;
    private static final int DIALOG_LAUNCHER = 1;
    static final int DIALOG_RENAME_FOLDER = 3;
    static final String EXTRA_CUSTOM_WIDGET = "custom_widget";
    static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";
    static final boolean LOGD = true;
    static final String LOG_TAG = Launcher.class.getSimpleName();
    private static final int MENU_ADD = 2;
    private static final int MENU_APPLICATIONS = 5;
    private static final int MENU_GROUP_ADD = 1;
    private static final int MENU_GROUP_APPLICATIONS = 3;
    private static final int MENU_GROUP_WORKSPACE = 2;
    private static final int MENU_MANAGE_APPS = 8;
    private static final int MENU_PREFERENCES = 6;
    private static final int MENU_SEARCH = 4;
    private static final int MENU_SETTINGS = 7;
    private static final int MENU_UNINSTALL_APPS = 9;
    private static final int MENU_WALLPAPER = 3;
    private static final String PREFERENCES = "launcher.preferences";
    private static final boolean PROFILE_ROTATE = false;
    private static final boolean PROFILE_STARTUP = false;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_CREATE_LIVE_FOLDER = 4;
    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_PICK_APPLICATION = 6;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_PICK_LIVE_FOLDER = 8;
    private static final int REQUEST_PICK_SHORTCUT = 7;
    private static final String RUNTIME_STATE_ALL_APPS_FOLDER = "launcher.all_apps_folder";
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cellX";
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cellY";
    private static final String RUNTIME_STATE_PENDING_ADD_COUNT_X = "launcher.add_countX";
    private static final String RUNTIME_STATE_PENDING_ADD_COUNT_Y = "launcher.add_countY";
    private static final String RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS = "launcher.add_occupied_cells";
    private static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_spanX";
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_spanY";
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";
    private static final String RUNTIME_STATE_USER_FOLDERS = "launcher.user_folder";
    static final int SCREEN_COUNT = 5;
    static final String SEARCH_WIDGET = "search_widget";
    static final int WALLPAPER_SCREEN_SPAN = 2;
    /* access modifiers changed from: private */
    public static final LauncherModel sLauncherModel = new LauncherModel();
    private static final Object sLock = new Object();
    static boolean sRestart = false;
    static boolean sRestartLoaders = false;
    private static int sScreen = 2;
    private static WallpaperIntentReceiver sWallpaperReceiver;
    /* access modifiers changed from: private */
    public CellLayout.CellInfo mAddItemCellInfo;
    private boolean mAllowAppsGridAnimations = LOGD;
    /* access modifiers changed from: private */
    public LauncherAppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private boolean mApplicationsGridOpen = false;
    private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver(this, (ApplicationsIntentReceiver) null);
    /* access modifiers changed from: private */
    public ApplicationsView mApplicationsView;
    private DesktopBinder mBinder;
    private boolean mBootstrap = false;
    private final int[] mCellCoordinates = new int[2];
    private final BroadcastReceiver mCloseSystemDialogsReceiver = new CloseSystemDialogsIntentReceiver(this, (CloseSystemDialogsIntentReceiver) null);
    private SpannableStringBuilder mDefaultKeySsb = null;
    /* access modifiers changed from: private */
    public boolean mDesktopLocked = LOGD;
    private boolean mDestroyed;
    private DisplayMetrics mDisplayMetrics;
    /* access modifiers changed from: private */
    public Dock mDock;
    private DragLayer mDragLayer;
    /* access modifiers changed from: private */
    public FolderInfo mFolderInfo;
    private boolean mFullScreenPreviews = LOGD;
    private boolean mFullscreen;
    private ImageButton mHomeButton;
    private LayoutInflater mInflater;
    private boolean mIsNewIntent;
    private boolean mLocaleChanged;
    private CellLayout.CellInfo mMenuAddInfo;
    private final ContentObserver mObserver = new FavoritesChangeObserver();
    private boolean mPreviewsShowing = false;
    private boolean mRestoring;
    private int mRotation;
    private Bundle mSavedInstanceState;
    private Bundle mSavedState;
    private ScreenIndicator mScreenIndicator;
    /* access modifiers changed from: private */
    public boolean mWaitingForResult;
    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();
    /* access modifiers changed from: private */
    public Workspace mWorkspace;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mInflater = getLayoutInflater();
        this.mAppWidgetManager = AppWidgetManager.getInstance(this);
        this.mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        this.mAppWidgetHost.startListening();
        this.mBootstrap = mustBootstrapDock();
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(displayMetrics);
        this.mRotation = displayMetrics.widthPixels < displayMetrics.heightPixels ? 0 : 1;
        this.mDisplayMetrics = displayMetrics;
        checkForLocaleChange();
        setWallpaperDimension();
        setContentView(R.layout.launcher);
        setupViews();
        registerIntentReceivers();
        registerContentObservers();
        this.mSavedState = savedInstanceState;
        restoreState(this.mSavedState);
        if (!this.mRestoring) {
            startLoaders();
        }
        this.mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(this.mDefaultKeySsb, 0);
    }

    private void setRequestedOrientation() {
        if (PreferencesUtil.useSensorOrientation(this)) {
            setRequestedOrientation(4);
        } else {
            setRequestedOrientation(5);
        }
    }

    private void setRequestedFullscreen() {
        if (PreferencesUtil.isFullscreenEnabled(this)) {
            setFullscreen(LOGD);
        } else {
            setFullscreen(false);
        }
    }

    private void toggleFullscreen() {
        if (PreferencesUtil.isFullscreenEnabled(this)) {
            setFullscreen(false);
        } else {
            setFullscreen(LOGD);
        }
    }

    private void setFullscreen(boolean fullscreen, boolean save) {
        if (fullscreen) {
            getWindow().setFlags(APPWIDGET_HOST_ID, APPWIDGET_HOST_ID);
        } else {
            getWindow().clearFlags(APPWIDGET_HOST_ID);
        }
        this.mFullscreen = fullscreen;
        if (save) {
            PreferencesUtil.setFullscreenEnabled(this, fullscreen);
        }
    }

    private void setFullscreen(boolean fullscreen) {
        setFullscreen(fullscreen, LOGD);
    }

    private boolean isFullscreen() {
        return this.mFullscreen;
    }

    /* access modifiers changed from: package-private */
    public DisplayMetrics getDisplayMetrics() {
        return this.mDisplayMetrics;
    }

    private void checkForLocaleChange() {
        LocaleConfiguration localeConfiguration = new LocaleConfiguration((LocaleConfiguration) null);
        readConfiguration(this, localeConfiguration);
        Configuration configuration = getResources().getConfiguration();
        String previousLocale = localeConfiguration.locale;
        String locale = configuration.locale.toString();
        int previousMcc = localeConfiguration.mcc;
        int mcc = configuration.mcc;
        int previousMnc = localeConfiguration.mnc;
        int mnc = configuration.mnc;
        this.mLocaleChanged = (locale.equals(previousLocale) && mcc == previousMcc && mnc == previousMnc) ? false : LOGD;
        if (this.mLocaleChanged) {
            localeConfiguration.locale = locale;
            localeConfiguration.mcc = mcc;
            localeConfiguration.mnc = mnc;
            writeConfiguration(this, localeConfiguration);
        }
    }

    private static class LocaleConfiguration {
        public String locale;
        public int mcc;
        public int mnc;

        private LocaleConfiguration() {
            this.mcc = -1;
            this.mnc = -1;
        }

        /* synthetic */ LocaleConfiguration(LocaleConfiguration localeConfiguration) {
            this();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x002b A[SYNTHETIC, Splitter:B:12:0x002b] */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0034 A[SYNTHETIC, Splitter:B:17:0x0034] */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x003d A[SYNTHETIC, Splitter:B:22:0x003d] */
    /* JADX WARNING: Removed duplicated region for block: B:37:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:38:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void readConfiguration(android.content.Context r4, org.zeam.Launcher.LocaleConfiguration r5) {
        /*
            if (r4 != 0) goto L_0x0003
        L_0x0002:
            return
        L_0x0003:
            r0 = 0
            java.io.DataInputStream r1 = new java.io.DataInputStream     // Catch:{ FileNotFoundException -> 0x0028, IOException -> 0x0031, all -> 0x003a }
            java.lang.String r2 = "launcher.preferences"
            java.io.FileInputStream r2 = r4.openFileInput(r2)     // Catch:{ FileNotFoundException -> 0x0028, IOException -> 0x0031, all -> 0x003a }
            r1.<init>(r2)     // Catch:{ FileNotFoundException -> 0x0028, IOException -> 0x0031, all -> 0x003a }
            java.lang.String r2 = r1.readUTF()     // Catch:{ FileNotFoundException -> 0x004c, IOException -> 0x0049, all -> 0x0046 }
            r5.locale = r2     // Catch:{ FileNotFoundException -> 0x004c, IOException -> 0x0049, all -> 0x0046 }
            int r2 = r1.readInt()     // Catch:{ FileNotFoundException -> 0x004c, IOException -> 0x0049, all -> 0x0046 }
            r5.mcc = r2     // Catch:{ FileNotFoundException -> 0x004c, IOException -> 0x0049, all -> 0x0046 }
            int r2 = r1.readInt()     // Catch:{ FileNotFoundException -> 0x004c, IOException -> 0x0049, all -> 0x0046 }
            r5.mnc = r2     // Catch:{ FileNotFoundException -> 0x004c, IOException -> 0x0049, all -> 0x0046 }
            if (r1 == 0) goto L_0x004f
            r1.close()     // Catch:{ IOException -> 0x0041 }
            r0 = r1
            goto L_0x0002
        L_0x0028:
            r2 = move-exception
        L_0x0029:
            if (r0 == 0) goto L_0x0002
            r0.close()     // Catch:{ IOException -> 0x002f }
            goto L_0x0002
        L_0x002f:
            r2 = move-exception
            goto L_0x0002
        L_0x0031:
            r2 = move-exception
        L_0x0032:
            if (r0 == 0) goto L_0x0002
            r0.close()     // Catch:{ IOException -> 0x0038 }
            goto L_0x0002
        L_0x0038:
            r2 = move-exception
            goto L_0x0002
        L_0x003a:
            r2 = move-exception
        L_0x003b:
            if (r0 == 0) goto L_0x0040
            r0.close()     // Catch:{ IOException -> 0x0044 }
        L_0x0040:
            throw r2
        L_0x0041:
            r2 = move-exception
            r0 = r1
            goto L_0x0002
        L_0x0044:
            r3 = move-exception
            goto L_0x0040
        L_0x0046:
            r2 = move-exception
            r0 = r1
            goto L_0x003b
        L_0x0049:
            r2 = move-exception
            r0 = r1
            goto L_0x0032
        L_0x004c:
            r2 = move-exception
            r0 = r1
            goto L_0x0029
        L_0x004f:
            r0 = r1
            goto L_0x0002
        */
        throw new UnsupportedOperationException("Method not decompiled: org.zeam.Launcher.readConfiguration(android.content.Context, org.zeam.Launcher$LocaleConfiguration):void");
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x002c A[SYNTHETIC, Splitter:B:12:0x002c] */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x003e A[SYNTHETIC, Splitter:B:20:0x003e] */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0047 A[SYNTHETIC, Splitter:B:25:0x0047] */
    /* JADX WARNING: Removed duplicated region for block: B:40:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:41:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void writeConfiguration(android.content.Context r5, org.zeam.Launcher.LocaleConfiguration r6) {
        /*
            if (r5 != 0) goto L_0x0003
        L_0x0002:
            return
        L_0x0003:
            r0 = 0
            java.io.DataOutputStream r1 = new java.io.DataOutputStream     // Catch:{ FileNotFoundException -> 0x0029, IOException -> 0x0032 }
            java.lang.String r3 = "launcher.preferences"
            r4 = 0
            java.io.FileOutputStream r3 = r5.openFileOutput(r3, r4)     // Catch:{ FileNotFoundException -> 0x0029, IOException -> 0x0032 }
            r1.<init>(r3)     // Catch:{ FileNotFoundException -> 0x0029, IOException -> 0x0032 }
            java.lang.String r3 = r6.locale     // Catch:{ FileNotFoundException -> 0x0056, IOException -> 0x0053, all -> 0x0050 }
            r1.writeUTF(r3)     // Catch:{ FileNotFoundException -> 0x0056, IOException -> 0x0053, all -> 0x0050 }
            int r3 = r6.mcc     // Catch:{ FileNotFoundException -> 0x0056, IOException -> 0x0053, all -> 0x0050 }
            r1.writeInt(r3)     // Catch:{ FileNotFoundException -> 0x0056, IOException -> 0x0053, all -> 0x0050 }
            int r3 = r6.mnc     // Catch:{ FileNotFoundException -> 0x0056, IOException -> 0x0053, all -> 0x0050 }
            r1.writeInt(r3)     // Catch:{ FileNotFoundException -> 0x0056, IOException -> 0x0053, all -> 0x0050 }
            r1.flush()     // Catch:{ FileNotFoundException -> 0x0056, IOException -> 0x0053, all -> 0x0050 }
            if (r1 == 0) goto L_0x0059
            r1.close()     // Catch:{ IOException -> 0x004b }
            r0 = r1
            goto L_0x0002
        L_0x0029:
            r3 = move-exception
        L_0x002a:
            if (r0 == 0) goto L_0x0002
            r0.close()     // Catch:{ IOException -> 0x0030 }
            goto L_0x0002
        L_0x0030:
            r3 = move-exception
            goto L_0x0002
        L_0x0032:
            r2 = move-exception
        L_0x0033:
            java.lang.String r3 = "launcher.preferences"
            java.io.File r3 = r5.getFileStreamPath(r3)     // Catch:{ all -> 0x0044 }
            r3.delete()     // Catch:{ all -> 0x0044 }
            if (r0 == 0) goto L_0x0002
            r0.close()     // Catch:{ IOException -> 0x0042 }
            goto L_0x0002
        L_0x0042:
            r3 = move-exception
            goto L_0x0002
        L_0x0044:
            r3 = move-exception
        L_0x0045:
            if (r0 == 0) goto L_0x004a
            r0.close()     // Catch:{ IOException -> 0x004e }
        L_0x004a:
            throw r3
        L_0x004b:
            r3 = move-exception
            r0 = r1
            goto L_0x0002
        L_0x004e:
            r4 = move-exception
            goto L_0x004a
        L_0x0050:
            r3 = move-exception
            r0 = r1
            goto L_0x0045
        L_0x0053:
            r2 = move-exception
            r0 = r1
            goto L_0x0033
        L_0x0056:
            r3 = move-exception
            r0 = r1
            goto L_0x002a
        L_0x0059:
            r0 = r1
            goto L_0x0002
        */
        throw new UnsupportedOperationException("Method not decompiled: org.zeam.Launcher.writeConfiguration(android.content.Context, org.zeam.Launcher$LocaleConfiguration):void");
    }

    static int getScreen() {
        int i;
        synchronized (sLock) {
            i = sScreen;
        }
        return i;
    }

    static void setScreen(int screen) {
        synchronized (sLock) {
            sScreen = screen;
        }
    }

    private void startLoaders() {
        boolean z = LOGD;
        sLauncherModel.loadApplications(LOGD, this.mApplicationsView);
        LauncherModel launcherModel = sLauncherModel;
        if (this.mLocaleChanged) {
            z = false;
        }
        launcherModel.loadUserItems(z, this, this.mLocaleChanged);
        this.mRestoring = false;
    }

    private void setWallpaperDimension() {
        WallpaperManager wallpaperManager = (WallpaperManager) getSystemService("wallpaper");
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        boolean isPortrait = defaultDisplay.getWidth() < defaultDisplay.getHeight() ? LOGD : false;
        wallpaperManager.suggestDesiredDimensions((isPortrait ? defaultDisplay.getWidth() : defaultDisplay.getHeight()) * 2, isPortrait ? defaultDisplay.getHeight() : defaultDisplay.getWidth());
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        int appWidgetId;
        boolean z = false;
        this.mWaitingForResult = false;
        if (resultCode == -1 && this.mAddItemCellInfo != null) {
            switch (requestCode) {
                case 1:
                    CellLayout.CellInfo cellInfo = this.mAddItemCellInfo;
                    if (!this.mDesktopLocked) {
                        z = true;
                    }
                    completeAddShortcut(data, cellInfo, z);
                    return;
                case 4:
                    CellLayout.CellInfo cellInfo2 = this.mAddItemCellInfo;
                    if (!this.mDesktopLocked) {
                        z = true;
                    }
                    completeAddLiveFolder(data, cellInfo2, z);
                    return;
                case 5:
                    CellLayout.CellInfo cellInfo3 = this.mAddItemCellInfo;
                    if (!this.mDesktopLocked) {
                        z = true;
                    }
                    completeAddAppWidget(data, cellInfo3, z);
                    return;
                case 6:
                    CellLayout.CellInfo cellInfo4 = this.mAddItemCellInfo;
                    if (!this.mDesktopLocked) {
                        z = true;
                    }
                    completeAddApplication(this, data, cellInfo4, z);
                    return;
                case 7:
                    processShortcut(data, 6, 1);
                    return;
                case 8:
                    addLiveFolder(data);
                    return;
                case 9:
                    addAppWidget(data);
                    return;
                default:
                    return;
            }
        } else if ((requestCode == 9 || requestCode == 5) && resultCode == 0 && data != null && (appWidgetId = data.getIntExtra("appWidgetId", -1)) != -1) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        }
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        if (sRestart) {
            Preferences.alertRestart(this);
        }
        loadPreferences();
        int rotation = this.mRotation;
        if (this.mApplicationsView instanceof ApplicationsGridView) {
            if (rotation == 0) {
                this.mApplicationsView.setNumColumns(PreferencesUtil.getAppsGridVerticalScrollingContentColumnsPortrait(this));
            } else {
                this.mApplicationsView.setNumColumns(PreferencesUtil.getAppsGridVerticalScrollingContentColumnsLandscape(this));
            }
        } else if (this.mApplicationsView instanceof ApplicationsPagingView) {
            ApplicationsPagingView applicationsPagingView = (ApplicationsPagingView) this.mApplicationsView;
            if (rotation == 0) {
                applicationsPagingView.setNumRows(PreferencesUtil.getAppsGridHorizontalPagingContentRowsPortrait(this));
                applicationsPagingView.setNumColumns(PreferencesUtil.getAppsGridHorizontalPagingContentColumnsPortrait(this));
            } else {
                applicationsPagingView.setNumRows(PreferencesUtil.getAppsGridHorizontalPagingContentRowsLandscape(this));
                applicationsPagingView.setNumColumns(PreferencesUtil.getAppsGridHorizontalPagingContentColumnsLandscape(this));
            }
        }
        this.mApplicationsView.setBackgroundAlpha(PreferencesUtil.getAppsGridBackgroundAlpha(this));
        this.mWorkspace.setWallpaper(false);
        if (this.mRestoring) {
            startLoaders();
        }
        if (this.mIsNewIntent) {
            this.mWorkspace.post(new Runnable() {
                public void run() {
                    try {
                        ((SearchManager) Launcher.this.getSystemService("search")).stopSearch();
                    } catch (Exception e) {
                        Log.e(Launcher.LOG_TAG, "error stopping search", e);
                    }
                }
            });
        }
        this.mIsNewIntent = false;
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
    }

    public Object onRetainNonConfigurationInstance() {
        if (this.mBinder == null) {
            return null;
        }
        this.mBinder.mTerminate = LOGD;
        return null;
    }

    private boolean acceptFilter() {
        if (((InputMethodManager) getSystemService("input_method")).isFullscreenMode()) {
            return false;
        }
        return LOGD;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = super.onKeyDown(keyCode, event);
        if (handled || !acceptFilter() || keyCode == 66 || !TextKeyListener.getInstance().onKeyDown(this.mWorkspace, this.mDefaultKeySsb, keyCode, event) || this.mDefaultKeySsb == null || this.mDefaultKeySsb.length() <= 0) {
            return handled;
        }
        return onSearchRequested();
    }

    private String getTypedText() {
        return this.mDefaultKeySsb.toString();
    }

    private void clearTypedText() {
        this.mDefaultKeySsb.clear();
        this.mDefaultKeySsb.clearSpans();
        Selection.setSelection(this.mDefaultKeySsb, 0);
    }

    private void restoreState(Bundle savedState) {
        if (savedState != null) {
            int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN, -1);
            if (currentScreen > -1) {
                this.mWorkspace.setCurrentScreen(currentScreen);
            }
            int addScreen = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SCREEN, -1);
            if (addScreen > -1) {
                this.mAddItemCellInfo = new CellLayout.CellInfo();
                CellLayout.CellInfo addItemCellInfo = this.mAddItemCellInfo;
                addItemCellInfo.valid = LOGD;
                addItemCellInfo.screen = addScreen;
                addItemCellInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
                addItemCellInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
                addItemCellInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
                addItemCellInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
                addItemCellInfo.findVacantCellsFromOccupied(savedState.getBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS), savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_X), savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y));
                this.mRestoring = LOGD;
            }
            if (savedState.getBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, false)) {
                this.mFolderInfo = sLauncherModel.getFolderById(this, savedState.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID));
                this.mRestoring = LOGD;
            }
        }
    }

    private void setupViews() {
        this.mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        DragLayer dragLayer = this.mDragLayer;
        this.mWorkspace = (Workspace) dragLayer.findViewById(R.id.workspace);
        Workspace workspace = this.mWorkspace;
        ViewStub appsGridStub = (ViewStub) dragLayer.findViewById(R.id.apps_grid_stub);
        switch (PreferencesUtil.getAppsGridType(this)) {
            case 1:
                appsGridStub.setLayoutResource(R.layout.apps_grid_view);
                break;
            case 2:
                appsGridStub.setLayoutResource(R.layout.apps_paging_view);
                break;
            default:
                appsGridStub.setLayoutResource(R.layout.apps_paging_view);
                break;
        }
        this.mApplicationsView = (ApplicationsView) appsGridStub.inflate();
        ApplicationsView applicationsView = this.mApplicationsView;
        ViewStub dockbarStub = (ViewStub) dragLayer.findViewById(R.id.dock_stub);
        dockbarStub.setLayoutResource(R.layout.dockbar);
        dockbarStub.inflate();
        this.mHomeButton = (ImageButton) findViewById(R.id.home_button);
        this.mHomeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Launcher.this.isApplicationsGridOpen()) {
                    Launcher.this.closeAllApplications();
                }
            }
        });
        DeleteZone deleteZone = (DeleteZone) dragLayer.findViewById(R.id.delete_zone);
        applicationsView.setDragController(dragLayer);
        applicationsView.setLauncher(this);
        workspace.setOnLongClickListener(this);
        workspace.setDragger(dragLayer);
        workspace.setLauncher(this);
        deleteZone.setLauncher(this);
        deleteZone.setDragController(dragLayer);
        dragLayer.setIgnoredDropTarget(applicationsView.getImplementingView());
        dragLayer.setDragScoller(workspace);
        dragLayer.addDragListener(deleteZone);
        this.mDock = (Dock) dragLayer.findViewById(R.id.dock);
        this.mDock.setDragger(dragLayer);
        this.mDock.setLauncher(this);
        dragLayer.addDragListener(this.mDock);
        deleteZone.setHandle(new View(this));
        this.mScreenIndicator = (ScreenIndicator) findViewById(R.id.workspace_screen_indicator);
    }

    private void loadPreferences() {
        Log.d(LOG_TAG, "-- loadPreferences()");
        setRequestedOrientation();
        setRequestedFullscreen();
        if (sRestartLoaders) {
            sRestartLoaders = false;
            startLoaders();
        }
        this.mFullScreenPreviews = LOGD;
        this.mAllowAppsGridAnimations = PreferencesUtil.isAnimateAppsGridEnabled(this);
        this.mWorkspace.setElasticScrolling(PreferencesUtil.isElasticScrollingEnabled(this));
        this.mWorkspace.setDrawWallpaper(PreferencesUtil.isManageWallpaperEnabled(this));
        this.mWorkspace.setScrollWallpaper(PreferencesUtil.isScrollWallpaperEnabled(this));
        loadIndicator();
        loadDockWidths();
        loadDockBackground();
    }

    private void loadDockWidths() {
        Dock dock = this.mDock;
        Resources resources = getResources();
        String dockItemWidth = PreferencesUtil.getDockItemWidth(this);
        String[] dockItemWidths = resources.getStringArray(R.array.preferences_values_dock_item_widths);
        for (int i = 0; i < dockItemWidths.length; i++) {
            if (dockItemWidths[i].equals(dockItemWidth)) {
                switch (i) {
                    case 0:
                        dock.setItemWidth(0);
                        continue;
                    case 2:
                        dock.setItemWidth(2);
                        break;
                }
                dock.setItemWidth(1);
            }
        }
        dock.invalidate();
    }

    private void loadDockBackground() {
        Dock dock = this.mDock;
        Resources resources = getResources();
        String dockBackgroundType = PreferencesUtil.getDockBackgroundType(this);
        String[] dockBackgrounds = resources.getStringArray(R.array.preferences_values_dock_backgrounds);
        dock.setBackgroundColor(1610612736);
        for (int i = 0; i < dockBackgrounds.length; i++) {
            if (dockBackgrounds[i].equals(dockBackgroundType)) {
                switch (i) {
                    case 0:
                        dock.setBackgroundColor(-2013265920);
                        break;
                    case 1:
                        dock.setBackgroundResource(R.drawable.dock_bg_fade_to_black);
                        break;
                    case 2:
                        dock.setBackgroundDrawable((Drawable) null);
                        break;
                    case 3:
                        dock.setBackgroundColor(-16777216);
                        break;
                    case 4:
                        dock.setBackgroundColor(-12303292);
                        break;
                    case 5:
                        dock.setBackgroundResource(R.drawable.dock_bg_bar_grey);
                        break;
                    case 6:
                        dock.setBackgroundColor(1358954495);
                        break;
                    default:
                        dock.setBackgroundResource(R.drawable.dock_bg_bar_grey);
                        break;
                }
            }
        }
        dock.invalidate();
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:5:0x0019  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadIndicator() {
        /*
            r9 = this;
            r8 = 0
            r7 = 1
            android.content.res.Resources r4 = r9.getResources()
            java.lang.String r2 = org.zeam.PreferencesUtil.getScreenIndicator(r9)
            r5 = 2131296259(0x7f090003, float:1.821043E38)
            java.lang.String[] r3 = r4.getStringArray(r5)
            r1 = 0
        L_0x0012:
            int r5 = r3.length
            if (r1 < r5) goto L_0x0033
        L_0x0015:
            org.zeam.ScreenIndicator r5 = r9.mScreenIndicator
            if (r5 == 0) goto L_0x0032
            org.zeam.ScreenIndicator r5 = r9.mScreenIndicator
            r5.setAutoHide(r7)
            org.zeam.Workspace r5 = r9.mWorkspace
            if (r5 == 0) goto L_0x0032
            org.zeam.ScreenIndicator r5 = r9.mScreenIndicator
            org.zeam.Workspace r6 = r9.mWorkspace
            int r6 = r6.getChildCount()
            r5.setItems(r6)
            org.zeam.Workspace r5 = r9.mWorkspace
            r5.indicateCurrent()
        L_0x0032:
            return
        L_0x0033:
            r5 = r3[r1]
            boolean r5 = r5.equals(r2)
            if (r5 == 0) goto L_0x004f
            switch(r1) {
                case 0: goto L_0x0052;
                case 1: goto L_0x005a;
                case 2: goto L_0x006c;
                default: goto L_0x003e;
            }
        L_0x003e:
            r5 = 2131099677(0x7f06001d, float:1.7811714E38)
            android.view.View r5 = r9.findViewById(r5)     // Catch:{ ClassCastException -> 0x0056 }
            org.zeam.ScreenIndicator r5 = (org.zeam.ScreenIndicator) r5     // Catch:{ ClassCastException -> 0x0056 }
            r9.mScreenIndicator = r5     // Catch:{ ClassCastException -> 0x0056 }
            org.zeam.ScreenIndicator r5 = r9.mScreenIndicator     // Catch:{ ClassCastException -> 0x0056 }
            r6 = 2
            r5.setType(r6)     // Catch:{ ClassCastException -> 0x0056 }
        L_0x004f:
            int r1 = r1 + 1
            goto L_0x0012
        L_0x0052:
            r5 = 0
            r9.mScreenIndicator = r5     // Catch:{ ClassCastException -> 0x0056 }
            goto L_0x004f
        L_0x0056:
            r0 = move-exception
            r9.mScreenIndicator = r8
            goto L_0x0015
        L_0x005a:
            r5 = 2131099677(0x7f06001d, float:1.7811714E38)
            android.view.View r5 = r9.findViewById(r5)     // Catch:{ ClassCastException -> 0x0056 }
            org.zeam.ScreenIndicator r5 = (org.zeam.ScreenIndicator) r5     // Catch:{ ClassCastException -> 0x0056 }
            r9.mScreenIndicator = r5     // Catch:{ ClassCastException -> 0x0056 }
            org.zeam.ScreenIndicator r5 = r9.mScreenIndicator     // Catch:{ ClassCastException -> 0x0056 }
            r6 = 2
            r5.setType(r6)     // Catch:{ ClassCastException -> 0x0056 }
            goto L_0x004f
        L_0x006c:
            r5 = 2131099677(0x7f06001d, float:1.7811714E38)
            android.view.View r5 = r9.findViewById(r5)     // Catch:{ ClassCastException -> 0x0056 }
            org.zeam.ScreenIndicator r5 = (org.zeam.ScreenIndicator) r5     // Catch:{ ClassCastException -> 0x0056 }
            r9.mScreenIndicator = r5     // Catch:{ ClassCastException -> 0x0056 }
            org.zeam.ScreenIndicator r5 = r9.mScreenIndicator     // Catch:{ ClassCastException -> 0x0056 }
            r6 = 1
            r5.setType(r6)     // Catch:{ ClassCastException -> 0x0056 }
            goto L_0x004f
        */
        throw new UnsupportedOperationException("Method not decompiled: org.zeam.Launcher.loadIndicator():void");
    }

    /* access modifiers changed from: package-private */
    public View createShortcut(ApplicationItemInfo info) {
        return createShortcut(R.layout.application, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentScreen()), info);
    }

    /* access modifiers changed from: package-private */
    public View createShortcut(int layoutResId, ViewGroup parent, ApplicationItemInfo info) {
        TextView favorite = (TextView) this.mInflater.inflate(layoutResId, parent, false);
        if (!info.filtered) {
            info.icon = Utilities.createIconThumbnail(info.icon, this);
            info.filtered = LOGD;
        }
        favorite.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, info.icon, (Drawable) null, (Drawable) null);
        if (PreferencesUtil.isShowShortcutTitlesEnabled(this)) {
            favorite.setText(info.title);
        }
        favorite.setTag(info);
        favorite.setOnClickListener(this);
        return favorite;
    }

    /* access modifiers changed from: package-private */
    public void completeAddApplication(Context context, Intent data, CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
        ApplicationItemInfo info;
        cellInfo.screen = this.mWorkspace.getCurrentScreen();
        if (((cellInfo.cellX != -1 && cellInfo.cellY != -1) || findSingleSlot(cellInfo)) && (info = infoFromApplicationIntent(context, data)) != null) {
            this.mWorkspace.addApplicationShortcut(info, cellInfo, insertAtFirst);
        }
    }

    private static ApplicationItemInfo infoFromApplicationIntent(Context context, Intent data) {
        ComponentName component = data.getComponent();
        PackageManager packageManager = context.getPackageManager();
        ActivityInfo activityInfo = null;
        try {
            activityInfo = packageManager.getActivityInfo(component, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Couldn't find ActivityInfo for selected application", e);
        }
        if (activityInfo == null) {
            return null;
        }
        ApplicationItemInfo itemInfo = new ApplicationItemInfo();
        itemInfo.title = activityInfo.loadLabel(packageManager);
        if (itemInfo.title == null) {
            itemInfo.title = activityInfo.name;
        }
        itemInfo.setActivity(component, 270532608);
        itemInfo.container = -1;
        itemInfo.icon = activityInfo.loadIcon(packageManager);
        return itemInfo;
    }

    private void addApplicationsGridItem() {
        CellLayout.CellInfo cellInfo = this.mAddItemCellInfo;
        cellInfo.screen = this.mWorkspace.getCurrentScreen();
        if ((cellInfo.cellX != -1 && cellInfo.cellY != -1) || findSingleSlot(cellInfo)) {
            ApplicationsGridItemInfo applicationsGridItemInfo = new ApplicationsGridItemInfo(this);
            LauncherModel.addItemToDatabase(this, applicationsGridItemInfo, -100, cellInfo.screen, cellInfo.cellX, cellInfo.cellY, false);
            if (!this.mRestoring) {
                sLauncherModel.addDesktopItem(applicationsGridItemInfo);
                this.mWorkspace.addInCurrentScreen(createApplicationsGridItemView(applicationsGridItemInfo), cellInfo.cellX, cellInfo.cellY, 1, 1, false);
            } else if (sLauncherModel.isDesktopLoaded()) {
                sLauncherModel.addDesktopItem(applicationsGridItemInfo);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public View createApplicationsGridItemView(ApplicationsGridItemInfo applicationsGridItemInfo) {
        TextView textView = (TextView) this.mInflater.inflate(R.layout.application, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentScreen()), false);
        textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, Utilities.createIconThumbnail(applicationsGridItemInfo.icon, this), (Drawable) null, (Drawable) null);
        if (PreferencesUtil.isShowShortcutTitlesEnabled(this)) {
            textView.setText(applicationsGridItemInfo.title);
        }
        textView.setTag(applicationsGridItemInfo);
        textView.setOnClickListener(this);
        return textView;
    }

    private void completeAddShortcut(Intent data, CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
        cellInfo.screen = this.mWorkspace.getCurrentScreen();
        if ((cellInfo.cellX != -1 && cellInfo.cellY != -1) || findSingleSlot(cellInfo)) {
            ApplicationItemInfo info = addShortcut(this, data, cellInfo, false);
            if (!this.mRestoring) {
                sLauncherModel.addDesktopItem(info);
                this.mWorkspace.addInCurrentScreen(createShortcut(info), cellInfo.cellX, cellInfo.cellY, 1, 1, insertAtFirst);
            } else if (sLauncherModel.isDesktopLoaded()) {
                sLauncherModel.addDesktopItem(info);
            }
        }
    }

    private void completeAddAppWidget(Intent data, CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
        Bundle extras = data.getExtras();
        final int appWidgetId = extras.getInt("appWidgetId", -1);
        Log.d(LOG_TAG, "dumping extras content=" + extras.toString());
        final AppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        final int[] spans = ((CellLayout) this.mWorkspace.getChildAt(cellInfo.screen)).rectToCell(appWidgetInfo.minWidth, appWidgetInfo.minHeight);
        final CellLayout.CellInfo cInfo = cellInfo;
        View widgetSpanView = View.inflate(this, R.layout.widget_span, (ViewGroup) null);
        final NumberPicker columnsPicker = (NumberPicker) widgetSpanView.findViewById(R.id.widget_columns_span);
        columnsPicker.setRange(1, this.mWorkspace.getCurrentDesktopColumns());
        columnsPicker.setCurrent(spans[0]);
        final NumberPicker rowsPicker = (NumberPicker) widgetSpanView.findViewById(R.id.widget_rows_span);
        rowsPicker.setRange(1, this.mWorkspace.getCurrentDesktopRows());
        rowsPicker.setCurrent(spans[1]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(widgetSpanView);
        builder.setView(scrollView);
        AlertDialog alertDialog = builder.create();
        alertDialog.setTitle(getString(R.string.dialog_title_widget_span));
        final boolean z = insertAtFirst;
        alertDialog.setButton(-1, getString(R.string.button_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int which) {
                spans[1] = rowsPicker.getCurrent();
                spans[0] = columnsPicker.getCurrent();
                Launcher.this.realAddWidget(appWidgetInfo, cInfo, spans, appWidgetId, z);
            }
        });
        alertDialog.setButton(-2, getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return this.mAppWidgetHost;
    }

    static ApplicationItemInfo addShortcut(Context context, Intent data, CellLayout.CellInfo cellInfo, boolean notify) {
        ApplicationItemInfo info = infoFromShortcutIntent(context, data);
        LauncherModel.addItemToDatabase(context, info, -100, cellInfo.screen, cellInfo.cellX, cellInfo.cellY, notify);
        return info;
    }

    private static ApplicationItemInfo infoFromShortcutIntent(Context context, Intent data) {
        Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
        String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
        Bitmap bitmap = (Bitmap) data.getParcelableExtra("android.intent.extra.shortcut.ICON");
        Drawable icon = null;
        boolean filtered = false;
        boolean customIcon = false;
        Intent.ShortcutIconResource iconResource = null;
        if (bitmap != null) {
            icon = new FastBitmapDrawable(Utilities.createBitmapThumbnail(bitmap, context));
            filtered = LOGD;
            customIcon = LOGD;
        } else {
            Parcelable extra = data.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
            if (extra != null && (extra instanceof Intent.ShortcutIconResource)) {
                try {
                    iconResource = (Intent.ShortcutIconResource) extra;
                    Resources resources = context.getPackageManager().getResourcesForApplication(iconResource.packageName);
                    icon = resources.getDrawable(resources.getIdentifier(iconResource.resourceName, (String) null, (String) null));
                } catch (Exception e) {
                    Log.w(LOG_TAG, "Could not load shortcut icon: " + extra);
                }
            }
        }
        if (icon == null) {
            icon = context.getPackageManager().getDefaultActivityIcon();
        }
        ApplicationItemInfo info = new ApplicationItemInfo();
        info.icon = icon;
        info.filtered = filtered;
        info.title = name;
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;
        return info;
    }

    /* access modifiers changed from: package-private */
    public void closeSystemDialogs() {
        getWindow().closeAllPanels();
        try {
            dismissDialog(2);
            this.mWorkspace.unlock();
        } catch (Exception e) {
        }
        try {
            dismissDialog(3);
            this.mWorkspace.unlock();
        } catch (Exception e2) {
        }
    }

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("android.intent.action.MAIN".equals(intent.getAction())) {
            closeSystemDialogs();
            this.mIsNewIntent = LOGD;
            if ((intent.getFlags() & 4194304) != 4194304) {
                if (!isApplicationsGridOpen()) {
                    performAction(PreferencesUtil.getActionBindingForHomeButton(this));
                    if (PreferencesUtil.getDockResetHome(this)) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Launcher.this.mDock.scrollReset();
                            }
                        });
                    }
                } else {
                    closeApplications(LOGD);
                }
                View view = getWindow().peekDecorView();
                if (view != null && view.getWindowToken() != null) {
                    ((InputMethodManager) getSystemService("input_method")).hideSoftInputFromWindow(view.getWindowToken(), 0);
                    return;
                }
                return;
            }
            closeApplications(false);
        }
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Bundle windowState = savedInstanceState.getBundle("android:viewHierarchyState");
        SparseArray<Parcelable> savedStates = null;
        int focusedViewId = -1;
        if (windowState != null) {
            savedStates = windowState.getSparseParcelableArray("android:views");
            windowState.remove("android:views");
            focusedViewId = windowState.getInt("android:focusedViewId", -1);
            windowState.remove("android:focusedViewId");
        }
        super.onRestoreInstanceState(savedInstanceState);
        if (windowState != null) {
            windowState.putSparseParcelableArray("android:views", savedStates);
            windowState.putInt("android:focusedViewId", focusedViewId);
            windowState.remove("android:Panels");
        }
        this.mSavedInstanceState = savedInstanceState;
    }

    /* access modifiers changed from: protected */
    public void onSaveInstanceState(Bundle outState) {
        closeOptionsMenu();
        super.onSaveInstanceState(outState);
        outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, this.mWorkspace.getCurrentScreen());
        ArrayList<Folder> folders = this.mWorkspace.getOpenFolders();
        if (folders.size() > 0) {
            int count = folders.size();
            long[] ids = new long[count];
            for (int i = 0; i < count; i++) {
                ids[i] = folders.get(i).getInfo().f3id;
            }
            outState.putLongArray(RUNTIME_STATE_USER_FOLDERS, ids);
        }
        boolean isConfigurationChange = getChangingConfigurations() != 0;
        if (this.mApplicationsGridOpen && isConfigurationChange) {
            outState.putBoolean(RUNTIME_STATE_ALL_APPS_FOLDER, LOGD);
        }
        if (this.mAddItemCellInfo != null && this.mAddItemCellInfo.valid && this.mWaitingForResult) {
            CellLayout.CellInfo addItemCellInfo = this.mAddItemCellInfo;
            CellLayout layout = (CellLayout) this.mWorkspace.getChildAt(addItemCellInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN, addItemCellInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, addItemCellInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, addItemCellInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, addItemCellInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, addItemCellInfo.spanY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_X, layout.getCountX());
            outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y, layout.getCountY());
            outState.putBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS, layout.getOccupiedCells());
        }
        if (this.mFolderInfo != null && this.mWaitingForResult) {
            outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, LOGD);
            outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID, this.mFolderInfo.f3id);
        }
    }

    public void onDestroy() {
        this.mDestroyed = LOGD;
        super.onDestroy();
        try {
            this.mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(LOG_TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        TextKeyListener.getInstance().release();
        this.mApplicationsView.onDestroy();
        sLauncherModel.unbind();
        sLauncherModel.abortLoaders();
        getContentResolver().unregisterContentObserver(this.mObserver);
        getContentResolver().unregisterContentObserver(this.mWidgetObserver);
        unregisterReceiver(this.mApplicationsReceiver);
        unregisterReceiver(this.mCloseSystemDialogsReceiver);
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        closeApplications(false);
        if (requestCode >= 0) {
            this.mWaitingForResult = LOGD;
        }
        super.startActivityForResult(intent, requestCode);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(1, 2, 0, R.string.menu_add).setIcon(17301555).setAlphabeticShortcut('A');
        menu.add(2, 3, 0, R.string.menu_wallpaper).setIcon(17301567).setAlphabeticShortcut('W');
        menu.add(2, 4, 0, R.string.menu_search).setIcon(17301600).setAlphabeticShortcut('s');
        menu.add(2, 5, 0, R.string.menu_applications).setIcon(17301591).setAlphabeticShortcut('M');
        menu.add(2, 6, 0, R.string.menu_preferences).setIcon(17301570).setAlphabeticShortcut('P');
        menu.add(2, 7, 0, R.string.menu_settings).setIntent(new Intent("android.settings.SETTINGS")).setIcon(17301577).setAlphabeticShortcut('S');
        menu.add(3, 8, 0, R.string.menu_manage_apps).setIcon(17301570).setAlphabeticShortcut('M');
        menu.add(3, 9, 0, R.string.menu_uninstall_apps).setIcon(17301564).setAlphabeticShortcut('U');
        return LOGD;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean z = false;
        super.onPrepareOptionsMenu(menu);
        if (this.mApplicationsGridOpen) {
            menu.setGroupVisible(1, false);
            menu.setGroupVisible(2, false);
            menu.setGroupVisible(3, LOGD);
        } else {
            menu.setGroupVisible(1, LOGD);
            menu.setGroupVisible(2, LOGD);
            menu.setGroupVisible(3, false);
        }
        setFullscreen(false, false);
        this.mMenuAddInfo = this.mWorkspace.findAllVacantCells((boolean[]) null);
        if (this.mMenuAddInfo != null && this.mMenuAddInfo.valid) {
            z = true;
        }
        menu.setGroupEnabled(1, z);
        return LOGD;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 2:
                showAddDialog(this.mWorkspace.findAllVacantCells((boolean[]) null));
                return LOGD;
            case 3:
                startWallpaperChooser();
                return LOGD;
            case 4:
                onSearchRequested();
                return LOGD;
            case 5:
                if (this.mApplicationsGridOpen) {
                    closeApplicationsGrid(LOGD);
                    return LOGD;
                }
                openApplicationsGrid(LOGD);
                return LOGD;
            case 6:
                startPreferences();
                return LOGD;
            case 8:
                startActivitySafely(new Intent("android.intent.action.MANAGE_PACKAGE_STORAGE"));
                return LOGD;
            case 9:
                this.mApplicationsView.setMode(1);
                return LOGD;
            default:
                if (PreferencesUtil.isFullscreenEnabled(this) && !isFullscreen()) {
                    setFullscreen(LOGD, false);
                }
                return super.onOptionsItemSelected(item);
        }
    }

    /* access modifiers changed from: package-private */
    public void uninstallApplication(ApplicationItemInfo applicationItemInfo) {
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(applicationItemInfo.intent, 0);
        if (resolveInfo == null) {
            return;
        }
        if (resolveInfo.activityInfo != null) {
            startActivity(new Intent("android.intent.action.DELETE", Uri.parse("package:" + resolveInfo.activityInfo.packageName)));
            return;
        }
        Toast.makeText(this, "Unable to find ActivityInfo for " + applicationItemInfo.title, 1).show();
    }

    public boolean onSearchRequested() {
        startSearch((String) null, false, (Bundle) null, LOGD);
        return LOGD;
    }

    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
        closeAllApplications();
        Search searchWidget = this.mWorkspace.findSearchWidgetOnCurrentScreen();
        if (searchWidget == null) {
            showSearchDialog(initialQuery, selectInitialQuery, appSearchData, globalSearch);
            return;
        }
        searchWidget.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
        searchWidget.setQuery(getTypedText());
    }

    /* access modifiers changed from: package-private */
    public void showSearchDialog(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
        if (initialQuery == null) {
            initialQuery = getTypedText();
            clearTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
        }
        final SearchManager searchManager = (SearchManager) getSystemService("search");
        if (this.mWorkspace.findSearchWidgetOnCurrentScreen() != null) {
            searchManager.setOnCancelListener(new SearchManager.OnCancelListener() {
                public void onCancel() {
                    searchManager.setOnCancelListener((SearchManager.OnCancelListener) null);
                    Launcher.this.stopSearch();
                }
            });
        }
        searchManager.startSearch(initialQuery, selectInitialQuery, getComponentName(), appSearchData, globalSearch);
    }

    /* access modifiers changed from: package-private */
    public void stopSearch() {
        ((SearchManager) getSystemService("search")).stopSearch();
        Search searchWidget = this.mWorkspace.findSearchWidgetOnCurrentScreen();
        if (searchWidget != null) {
            searchWidget.stopSearch(false);
        }
    }

    /* access modifiers changed from: private */
    public void removeShortcutsForPackage(String packageName) {
        if (packageName != null && packageName.length() > 0) {
            this.mWorkspace.removeShortcutsForPackage(packageName);
            this.mDock.removeShortcutsForPackage(packageName);
        }
    }

    /* access modifiers changed from: private */
    public void updateShortcutsForPackage(String packageName) {
        if (packageName != null && packageName.length() > 0) {
            this.mWorkspace.updateShortcutsForPackage(packageName);
            this.mDock.updateShortcutsForPackage(packageName);
        }
    }

    /* access modifiers changed from: package-private */
    public void addAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra("appWidgetId", -1);
        if (SEARCH_WIDGET.equals(data.getStringExtra(EXTRA_CUSTOM_WIDGET))) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            addSearch();
            return;
        }
        AppWidgetProviderInfo appWidget = this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidget.configure != null) {
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_CONFIGURE");
            intent.putExtra("appWidgetId", appWidgetId);
            intent.setComponent(appWidget.configure);
            startActivityForResult(intent, 5);
            return;
        }
        onActivityResult(5, -1, data);
    }

    /* access modifiers changed from: package-private */
    public void addSearch() {
        Widget info = Widget.makeSearch();
        CellLayout.CellInfo cellInfo = this.mAddItemCellInfo;
        int[] xy = this.mCellCoordinates;
        int spanX = info.spanX;
        int spanY = info.spanY;
        if (findSlot(cellInfo, xy, spanX, spanY)) {
            info.spanX = spanX;
            info.spanY = spanY;
            sLauncherModel.addDesktopItem(info);
            LauncherModel.addItemToDatabase(this, info, -100, this.mWorkspace.getCurrentScreen(), xy[0], xy[1], false);
            View view = this.mInflater.inflate(info.layoutResource, (ViewGroup) null);
            view.setTag(info);
            ((Search) view.findViewById(R.id.widget_search)).setLauncher(this);
            this.mWorkspace.addInCurrentScreen(view, xy[0], xy[1], spanX, spanY);
        }
    }

    /* access modifiers changed from: package-private */
    public void processShortcut(Intent intent, int requestCodeApplication, int requestCodeShortcut) {
        String applicationName = getResources().getString(R.string.group_applications);
        String applicationsGridName = getResources().getString(R.string.group_add_apps_grid);
        String shortcutName = intent.getStringExtra("android.intent.extra.shortcut.NAME");
        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent("android.intent.action.MAIN", (Uri) null);
            Intent pickIntent = new Intent("android.intent.action.PICK_ACTIVITY");
            mainIntent.addCategory("android.intent.category.LAUNCHER");
            pickIntent.putExtra("android.intent.extra.INTENT", mainIntent);
            startActivityForResult(pickIntent, requestCodeApplication);
        } else if (applicationName == null || !applicationsGridName.equals(shortcutName)) {
            startActivityForResult(intent, requestCodeShortcut);
        } else {
            addApplicationsGridItem();
        }
    }

    /* access modifiers changed from: package-private */
    public void addLiveFolder(Intent intent) {
        String folderName = getResources().getString(R.string.group_folder);
        String shortcutName = intent.getStringExtra("android.intent.extra.shortcut.NAME");
        if (folderName == null || !folderName.equals(shortcutName)) {
            startActivityForResult(intent, 4);
        } else {
            addFolder(this.mDesktopLocked ? false : LOGD);
        }
    }

    /* access modifiers changed from: package-private */
    public void addFolder(boolean insertAtFirst) {
        CellLayout.CellInfo cellInfo = this.mAddItemCellInfo;
        cellInfo.screen = this.mWorkspace.getCurrentScreen();
        if ((cellInfo.cellX != -1 && cellInfo.cellY != -1) || findSingleSlot(cellInfo)) {
            UserFolderInfo folderInfo = new UserFolderInfo();
            folderInfo.title = getText(R.string.folder_name);
            LauncherModel.addItemToDatabase(this, folderInfo, -100, this.mWorkspace.getCurrentScreen(), cellInfo.cellX, cellInfo.cellY, false);
            sLauncherModel.addDesktopItem(folderInfo);
            sLauncherModel.addFolder(folderInfo);
            this.mWorkspace.addInCurrentScreen(FolderIcon.fromXml(R.layout.folder_icon, this, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentScreen()), folderInfo), cellInfo.cellX, cellInfo.cellY, 1, 1, insertAtFirst);
        }
    }

    private void completeAddLiveFolder(Intent data, CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
        cellInfo.screen = this.mWorkspace.getCurrentScreen();
        if ((cellInfo.cellX != -1 && cellInfo.cellY != -1) || findSingleSlot(cellInfo)) {
            LiveFolderInfo info = addLiveFolder(this, data, cellInfo, false);
            if (!this.mRestoring) {
                sLauncherModel.addDesktopItem(info);
                this.mWorkspace.addInCurrentScreen(LiveFolderIcon.fromXml(R.layout.live_folder_icon, this, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentScreen()), info), cellInfo.cellX, cellInfo.cellY, 1, 1, insertAtFirst);
            } else if (sLauncherModel.isDesktopLoaded()) {
                sLauncherModel.addDesktopItem(info);
            }
        }
    }

    static LiveFolderInfo addLiveFolder(Context context, Intent data, CellLayout.CellInfo cellInfo, boolean notify) {
        Intent baseIntent = (Intent) data.getParcelableExtra("android.intent.extra.livefolder.BASE_INTENT");
        String name = data.getStringExtra("android.intent.extra.livefolder.NAME");
        Drawable icon = null;
        Intent.ShortcutIconResource iconResource = null;
        Parcelable extra = data.getParcelableExtra("android.intent.extra.livefolder.ICON");
        if (extra != null && (extra instanceof Intent.ShortcutIconResource)) {
            try {
                iconResource = (Intent.ShortcutIconResource) extra;
                Resources resources = context.getPackageManager().getResourcesForApplication(iconResource.packageName);
                icon = resources.getDrawable(resources.getIdentifier(iconResource.resourceName, (String) null, (String) null));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Could not load live folder icon: " + extra);
            }
        }
        if (icon == null) {
            icon = context.getResources().getDrawable(R.drawable.ic_launcher_folder);
        }
        LiveFolderInfo liveFolderInfo = new LiveFolderInfo();
        liveFolderInfo.icon = icon;
        liveFolderInfo.filtered = false;
        liveFolderInfo.title = name;
        liveFolderInfo.iconResource = iconResource;
        liveFolderInfo.uri = data.getData();
        liveFolderInfo.baseIntent = baseIntent;
        liveFolderInfo.displayMode = data.getIntExtra("android.intent.extra.livefolder.DISPLAY_MODE", 1);
        LauncherModel.addItemToDatabase(context, liveFolderInfo, -100, cellInfo.screen, cellInfo.cellX, cellInfo.cellY, notify);
        sLauncherModel.addFolder(liveFolderInfo);
        return liveFolderInfo;
    }

    private boolean findSingleSlot(CellLayout.CellInfo cellInfo) {
        int[] xy = new int[2];
        if (!findSlot(cellInfo, xy, 1, 1)) {
            return false;
        }
        cellInfo.cellX = xy[0];
        cellInfo.cellY = xy[1];
        return LOGD;
    }

    private boolean findSlot(CellLayout.CellInfo cellInfo, int[] xy, int spanX, int spanY) {
        if (!cellInfo.findCellForSpan(xy, spanX, spanY)) {
            if (!this.mWorkspace.findAllVacantCells(this.mSavedState != null ? this.mSavedState.getBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS) : null).findCellForSpan(xy, spanX, spanY)) {
                Toast.makeText(this, getString(R.string.out_of_space), 0).show();
                return false;
            }
        }
        return LOGD;
    }

    private void expandNotifications() {
        try {
            Object service = getSystemService("statusbar");
            if (service != null) {
                service.getClass().getMethod("expand", new Class[0]).invoke(service, new Object[0]);
            }
        } catch (Exception e) {
        }
        getWorkspace().postDelayed(new Runnable() {
            public void run() {
                Launcher.this.getWindow().clearFlags(Launcher.APPWIDGET_HOST_ID);
            }
        }, 100);
    }

    /* access modifiers changed from: private */
    public void startWallpaperChooser() {
        Intent chooser = Intent.createChooser(new Intent("android.intent.action.SET_WALLPAPER"), getText(R.string.chooser_wallpaper));
        WallpaperInfo wallpaperInfo = ((WallpaperManager) getSystemService("wallpaper")).getWallpaperInfo();
        if (!(wallpaperInfo == null || wallpaperInfo.getSettingsActivity() == null)) {
            LabeledIntent labeledIntent = new LabeledIntent(getPackageName(), R.string.configure_wallpaper, 0);
            labeledIntent.setClassName(wallpaperInfo.getPackageName(), wallpaperInfo.getSettingsActivity());
            chooser.putExtra("android.intent.extra.INITIAL_INTENTS", new Intent[]{labeledIntent});
        }
        startActivity(chooser);
    }

    private void registerIntentReceivers() {
        if (sWallpaperReceiver == null) {
            Application application = getApplication();
            sWallpaperReceiver = new WallpaperIntentReceiver(application, this);
            application.registerReceiver(sWallpaperReceiver, new IntentFilter("android.intent.action.WALLPAPER_CHANGED"));
        } else {
            sWallpaperReceiver.setLauncher(this);
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme("package");
        registerReceiver(this.mApplicationsReceiver, intentFilter);
        registerReceiver(this.mCloseSystemDialogsReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        registerReceiver(this.mApplicationsReceiver, intentFilter2);
    }

    private void registerContentObservers() {
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, LOGD, this.mObserver);
        resolver.registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, LOGD, this.mWidgetObserver);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 0) {
            switch (event.getKeyCode()) {
                case 3:
                case 4:
                    return LOGD;
            }
        } else if (event.getAction() == 1) {
            switch (event.getKeyCode()) {
                case 3:
                    return LOGD;
                case 4:
                    if (event.isCanceled()) {
                        return LOGD;
                    }
                    this.mWorkspace.dispatchKeyEvent(event);
                    if (this.mApplicationsGridOpen) {
                        closeApplications();
                    } else {
                        closeFolder();
                    }
                    if (!isPreviewsShowing()) {
                        return LOGD;
                    }
                    dismissPreviews();
                    return LOGD;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void closeApplications() {
        closeApplications(LOGD);
    }

    private void closeApplications(boolean animated) {
        if (this.mApplicationsGridOpen) {
            if (animated) {
                closeApplicationsGrid(LOGD);
            } else {
                closeApplicationsGrid(false);
            }
            if (this.mApplicationsView.getImplementingView().hasFocus()) {
                this.mWorkspace.getChildAt(this.mWorkspace.getCurrentScreen()).requestFocus();
            }
        }
    }

    private void closeFolder() {
        Folder folder = this.mWorkspace.getOpenFolder();
        if (folder != null) {
            closeFolder(folder);
        }
    }

    /* access modifiers changed from: package-private */
    public void closeFolder(Folder folder) {
        folder.getInfo().opened = false;
        ViewGroup parent = (ViewGroup) folder.getParent();
        if (parent != null) {
            parent.removeView(folder);
        }
        folder.onClose();
    }

    /* access modifiers changed from: private */
    public void onFavoritesChanged() {
        this.mDesktopLocked = LOGD;
        sLauncherModel.loadUserItems(false, this, false);
    }

    /* access modifiers changed from: private */
    public void onAppWidgetReset() {
        this.mAppWidgetHost.startListening();
    }

    /* access modifiers changed from: package-private */
    public void onDesktopItemsLoaded(ArrayList<ItemInfo> shortcuts, ArrayList<LauncherAppWidgetInfo> appWidgets) {
        if (!this.mDestroyed) {
            bindDesktopItems(shortcuts, appWidgets);
        }
    }

    private boolean mustBootstrapDock() {
        File file = getBootstrapFile(this);
        if (file.exists()) {
            return false;
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
        }
        return LOGD;
    }

    private static File getBootstrapFile(Context context) {
        return new File(String.valueOf(Preferences.getApplicationDataPath(context)) + "x");
    }

    /* access modifiers changed from: package-private */
    public void bootstrapDock() {
        ApplicationItemInfo applicationItemInfo;
        Log.d(LOG_TAG, "-- bootstrapDock()");
        Dock dock = this.mDock;
        ArrayList<ApplicationItemInfo> applications = new ArrayList<>();
        ArrayList<Intent> launchIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        ApplicationItemInfo dialerApplicationItemInfo = getApplicationItemInfoOrNull(new ComponentName("com.android.contacts", "com.android.contacts.DialtactsActivity"));
        if (dialerApplicationItemInfo != null) {
            applications.add(dialerApplicationItemInfo);
        }
        ApplicationItemInfo dialerApplicationItemInfo2 = getApplicationItemInfoOrNull(new ComponentName("com.android.contacts", "com.sec.android.app.contacts.DialerEntryActivity"));
        if (dialerApplicationItemInfo2 != null) {
            applications.add(dialerApplicationItemInfo2);
        }
        ApplicationItemInfo dialerApplicationItemInfo3 = getApplicationItemInfoOrNull(new ComponentName("com.android.htccontacts", "com.android.htccontacts.DialerTabActivity"));
        if (dialerApplicationItemInfo3 != null) {
            applications.add(dialerApplicationItemInfo3);
        }
        launchIntents.add(packageManager.getLaunchIntentForPackage("com.android.htccontacts"));
        launchIntents.add(packageManager.getLaunchIntentForPackage("com.android.mms"));
        launchIntents.add(packageManager.getLaunchIntentForPackage("com.google.android.gm"));
        launchIntents.add(packageManager.getLaunchIntentForPackage("com.google.android.talk"));
        Intent browserIntent = packageManager.getLaunchIntentForPackage("com.android.browser");
        if (browserIntent == null) {
            browserIntent = packageManager.getLaunchIntentForPackage("com.google.android.browser");
        }
        if (browserIntent != null) {
            launchIntents.add(browserIntent);
        }
        for (int i = 0; i < launchIntents.size(); i++) {
            Intent launchIntent = launchIntents.get(i);
            if (!(launchIntent == null || (applicationItemInfo = getApplicationItemInfoOrNull(launchIntent.getComponent())) == null)) {
                applications.add(applicationItemInfo);
            }
        }
        if (applications.size() > 4) {
            for (int i2 = 0; i2 < applications.size(); i2++) {
                if (i2 == 4) {
                    dock.sendDrop(applications.get(i2));
                    dock.sendDrop(new ApplicationsGridItemInfo(this));
                } else {
                    dock.sendDrop(applications.get(i2));
                }
            }
            return;
        }
        dock.sendDrop(new ApplicationsGridItemInfo(this));
        for (int i3 = 0; i3 < applications.size(); i3++) {
            dock.sendDrop(applications.get(i3));
        }
    }

    private ApplicationItemInfo getApplicationItemInfoOrNull(ComponentName componentName) {
        PackageManager packageManager = getPackageManager();
        try {
            ActivityInfo activityInfo = packageManager.getActivityInfo(componentName, 0);
            ApplicationItemInfo applicationItemInfo = new ApplicationItemInfo();
            applicationItemInfo.icon = Utilities.createIconThumbnail(activityInfo.loadIcon(packageManager), this);
            applicationItemInfo.container = -1;
            applicationItemInfo.filtered = false;
            applicationItemInfo.title = activityInfo.loadLabel(packageManager);
            applicationItemInfo.setActivity(componentName, 270532608);
            return applicationItemInfo;
        } catch (Exception e) {
            return null;
        }
    }

    private void bindDesktopItems(ArrayList<ItemInfo> shortcuts, ArrayList<LauncherAppWidgetInfo> appWidgets) {
        if (shortcuts != null && appWidgets != null) {
            Workspace workspace = this.mWorkspace;
            int count = workspace.getChildCount();
            for (int i = 0; i < count; i++) {
                ((ViewGroup) workspace.getChildAt(i)).removeAllViewsInLayout();
            }
            ((Dock) this.mDragLayer.findViewById(R.id.dock)).removeAllViewsInLayout();
            if (this.mBinder != null) {
                this.mBinder.mTerminate = LOGD;
            }
            this.mBinder = new DesktopBinder(this, shortcuts, appWidgets);
            this.mBinder.startBindingItems();
        }
    }

    /* access modifiers changed from: private */
    public void bindItems(DesktopBinder binder, ArrayList<ItemInfo> shortcuts, int start, int count) {
        Workspace workspace = this.mWorkspace;
        boolean desktopLocked = this.mDesktopLocked;
        Dock dock = (Dock) this.mDragLayer.findViewById(R.id.dock);
        ArrayList<ItemInfo> dockItems = new ArrayList<>();
        int end = Math.min(start + 6, count);
        int i = start;
        while (i < end) {
            ItemInfo item = shortcuts.get(i);
            switch ((int) item.container) {
                case -200:
                    dockItems.add(item);
                    break;
                default:
                    switch (item.itemType) {
                        case 0:
                        case 1:
                            workspace.addInScreen(createShortcut((ApplicationItemInfo) item), item.screen, item.cellX, item.cellY, 1, 1, desktopLocked ? false : LOGD);
                            break;
                        case 2:
                            workspace.addInScreen(FolderIcon.fromXml(R.layout.folder_icon, this, (ViewGroup) workspace.getChildAt(workspace.getCurrentScreen()), (UserFolderInfo) item), item.screen, item.cellX, item.cellY, 1, 1, desktopLocked ? false : LOGD);
                            break;
                        case 3:
                            workspace.addInScreen(LiveFolderIcon.fromXml(R.layout.live_folder_icon, this, (ViewGroup) workspace.getChildAt(workspace.getCurrentScreen()), (LiveFolderInfo) item), item.screen, item.cellX, item.cellY, 1, 1, desktopLocked ? false : LOGD);
                            break;
                        case 6:
                            workspace.addInScreen(createApplicationsGridItemView((ApplicationsGridItemInfo) item), item.screen, item.cellX, item.cellY, 1, 1, desktopLocked ? false : LOGD);
                            break;
                        case 1001:
                            View view = this.mInflater.inflate(R.layout.widget_search, (ViewGroup) workspace.getChildAt(workspace.getCurrentScreen()), false);
                            ((Search) view.findViewById(R.id.widget_search)).setLauncher(this);
                            Widget widget = (Widget) item;
                            view.setTag(widget);
                            workspace.addWidget(view, widget, desktopLocked ? false : LOGD);
                            break;
                    }
            }
            i++;
        }
        if (dockItems.size() > 0) {
            dock.addItemViews(dockItems);
        }
        if (this.mBootstrap) {
            if (dockItems.size() == 0) {
                bootstrapDock();
            } else {
                File file = new File(String.valueOf(Preferences.getApplicationDataPath(this)) + "dirty");
                if (file.exists()) {
                    dock.sendDrop(new ApplicationsGridItemInfo(this), 0);
                    file.delete();
                }
            }
        }
        workspace.requestLayout();
        if (end >= count) {
            finishBindDesktopItems();
            binder.startBindingDrawer();
            return;
        }
        binder.obtainMessage(1, i, count).sendToTarget();
    }

    private void finishBindDesktopItems() {
        if (this.mSavedState != null) {
            if (!this.mWorkspace.hasFocus()) {
                this.mWorkspace.getChildAt(this.mWorkspace.getCurrentScreen()).requestFocus();
            }
            long[] userFolders = this.mSavedState.getLongArray(RUNTIME_STATE_USER_FOLDERS);
            if (userFolders != null) {
                for (long folderId : userFolders) {
                    FolderInfo info = sLauncherModel.findFolderById(folderId);
                    if (info != null) {
                        openFolder(info);
                    }
                }
                Folder openFolder = this.mWorkspace.getOpenFolder();
                if (openFolder != null) {
                    openFolder.requestFocus();
                }
            }
            if (this.mSavedState.getBoolean(RUNTIME_STATE_ALL_APPS_FOLDER, false)) {
                openApplicationsGrid(false);
            }
            this.mSavedState = null;
        }
        if (this.mSavedInstanceState != null) {
            try {
                super.onRestoreInstanceState(this.mSavedInstanceState);
            } catch (Exception e) {
                Log.e(LOG_TAG, ":(", e);
            } finally {
                this.mSavedInstanceState = null;
            }
        }
        if (this.mApplicationsGridOpen && !this.mApplicationsView.getImplementingView().hasFocus()) {
            this.mApplicationsView.getImplementingView().requestFocus();
        }
        this.mDesktopLocked = false;
    }

    /* access modifiers changed from: private */
    public void bindDrawer(DesktopBinder binder, ApplicationsAdapter drawerAdapter) {
        binder.startBindingAppWidgetsWhenIdle();
    }

    /* access modifiers changed from: private */
    public void bindAppWidgets(DesktopBinder binder, LinkedList<LauncherAppWidgetInfo> appWidgets) {
        Workspace workspace = this.mWorkspace;
        boolean desktopLocked = this.mDesktopLocked;
        if (!appWidgets.isEmpty()) {
            LauncherAppWidgetInfo launcherAppWidgetInfo = appWidgets.removeFirst();
            int appWidgetId = launcherAppWidgetInfo.appWidgetId;
            AppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
            launcherAppWidgetInfo.hostView = this.mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
            Log.d(LOG_TAG, String.format("about to setAppWidget for id=%d, info=%s", new Object[]{Integer.valueOf(appWidgetId), appWidgetInfo}));
            launcherAppWidgetInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            launcherAppWidgetInfo.hostView.setTag(launcherAppWidgetInfo);
            workspace.addInScreen(launcherAppWidgetInfo.hostView, launcherAppWidgetInfo.screen, launcherAppWidgetInfo.cellX, launcherAppWidgetInfo.cellY, launcherAppWidgetInfo.spanX, launcherAppWidgetInfo.spanY, desktopLocked ? false : LOGD);
            workspace.requestLayout();
        }
        if (!appWidgets.isEmpty()) {
            binder.obtainMessage(2).sendToTarget();
        }
    }

    public void onClick(View view) {
        Object tag = view.getTag();
        if (tag instanceof ApplicationsGridItemInfo) {
            performAction(2);
        } else if (tag instanceof ApplicationItemInfo) {
            Intent intent = ((ApplicationItemInfo) tag).intent;
            int[] pos = new int[2];
            view.getLocationOnScreen(pos);
            try {
                intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight()));
            } catch (NoSuchMethodError e) {
            }
            startActivitySafely(intent);
        } else if (tag instanceof FolderInfo) {
            handleFolderClick((FolderInfo) tag);
        }
    }

    /* access modifiers changed from: package-private */
    public void startActivitySafely(Intent intent) {
        if (intent == null) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            return;
        }
        intent.addFlags(268435456);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
        } catch (SecurityException se) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e(LOG_TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity.", se);
        }
    }

    private void handleFolderClick(FolderInfo folderInfo) {
        if (!folderInfo.opened) {
            closeFolder();
            openFolder(folderInfo);
            return;
        }
        Folder openFolder = this.mWorkspace.getFolderForTag(folderInfo);
        if (openFolder != null) {
            int folderScreen = this.mWorkspace.getScreenForView(openFolder);
            closeFolder(openFolder);
            if (folderScreen != this.mWorkspace.getCurrentScreen()) {
                closeFolder();
                openFolder(folderInfo);
            }
        }
    }

    private void openFolder(FolderInfo folderInfo) {
        Folder openFolder;
        if (folderInfo instanceof UserFolderInfo) {
            openFolder = UserFolder.fromXml(this);
        } else if (folderInfo instanceof LiveFolderInfo) {
            openFolder = LiveFolder.fromXml(this, folderInfo);
        } else {
            return;
        }
        openFolder.setDragger(this.mDragLayer);
        openFolder.setLauncher(this);
        openFolder.bind(folderInfo);
        folderInfo.opened = LOGD;
        if (folderInfo.container == -200) {
            this.mWorkspace.addInScreen(openFolder, this.mWorkspace.getCurrentScreen(), 0, 0, this.mWorkspace.getCurrentDesktopColumns(), this.mWorkspace.getCurrentDesktopRows());
        } else {
            this.mWorkspace.addInScreen(openFolder, folderInfo.screen, 0, 0, this.mWorkspace.getCurrentDesktopColumns(), this.mWorkspace.getCurrentDesktopRows());
        }
        openFolder.onOpen();
        closeApplications(false);
    }

    /* access modifiers changed from: package-private */
    public boolean isWorkspaceLocked() {
        return this.mDesktopLocked;
    }

    public boolean onLongClick(View view) {
        if (this.mDesktopLocked) {
            return false;
        }
        if (!(view instanceof CellLayout)) {
            view = (View) view.getParent();
        }
        CellLayout.CellInfo cellInfo = (CellLayout.CellInfo) view.getTag();
        if (cellInfo == null) {
            return LOGD;
        }
        if (this.mWorkspace.allowLongPress()) {
            if (cellInfo.cell == null) {
                if (cellInfo.valid) {
                    this.mWorkspace.setAllowLongPress(false);
                    showLauncherDialog(cellInfo);
                }
            } else if (!(cellInfo.cell instanceof Folder)) {
                this.mWorkspace.startDrag(cellInfo);
            }
        }
        return LOGD;
    }

    static LauncherModel getModel() {
        return sLauncherModel;
    }

    /* access modifiers changed from: package-private */
    public void closeAllApplications() {
        closeApplicationsGrid(LOGD);
    }

    /* access modifiers changed from: package-private */
    public Workspace getWorkspace() {
        return this.mWorkspace;
    }

    /* access modifiers changed from: protected */
    public Dialog onCreateDialog(int id) {
        switch (id) {
            case 1:
                return new LauncherDialog(this, (LauncherDialog) null).createDialog();
            case 2:
                return new AddDialog(this, (AddDialog) null).createDialog();
            case 3:
                return new RenameFolderDialog(this, (RenameFolderDialog) null).createDialog();
            default:
                return super.onCreateDialog(id);
        }
    }

    /* access modifiers changed from: protected */
    public void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case 3:
                if (this.mFolderInfo != null) {
                    EditText input = (EditText) dialog.findViewById(R.id.folder_name);
                    CharSequence text = this.mFolderInfo.title;
                    input.setText(text);
                    input.setSelection(0, text.length());
                    return;
                }
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: package-private */
    public void showRenameDialog(FolderInfo info) {
        this.mFolderInfo = info;
        this.mWaitingForResult = LOGD;
        showDialog(3);
    }

    /* access modifiers changed from: private */
    public void showAddDialog(CellLayout.CellInfo cellInfo) {
        this.mAddItemCellInfo = cellInfo;
        this.mWaitingForResult = LOGD;
        showDialog(2);
    }

    private void showLauncherDialog(CellLayout.CellInfo cellInfo) {
        this.mAddItemCellInfo = cellInfo;
        this.mWaitingForResult = LOGD;
        showDialog(1);
    }

    /* access modifiers changed from: private */
    public void pickShortcut(int requestCode, int title) {
        Bundle bundle = new Bundle();
        ArrayList<String> shortcutNames = new ArrayList<>();
        shortcutNames.add(getString(R.string.group_applications));
        shortcutNames.add(getString(R.string.group_add_apps_grid));
        ArrayList<Intent.ShortcutIconResource> shortcutIcons = new ArrayList<>();
        shortcutIcons.add(Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_application));
        shortcutIcons.add(Intent.ShortcutIconResource.fromContext(this, R.drawable.applications_grid));
        bundle.putStringArrayList("android.intent.extra.shortcut.NAME", shortcutNames);
        bundle.putParcelableArrayList("android.intent.extra.shortcut.ICON_RESOURCE", shortcutIcons);
        Intent pickIntent = new Intent("android.intent.action.PICK_ACTIVITY");
        pickIntent.putExtra("android.intent.extra.INTENT", new Intent("android.intent.action.CREATE_SHORTCUT"));
        pickIntent.putExtra("android.intent.extra.TITLE", getText(title));
        pickIntent.putExtras(bundle);
        startActivityForResult(pickIntent, requestCode);
    }

    private class RenameFolderDialog {
        private EditText mInput;

        private RenameFolderDialog() {
        }

        /* synthetic */ RenameFolderDialog(Launcher launcher, RenameFolderDialog renameFolderDialog) {
            this();
        }

        /* access modifiers changed from: package-private */
        public Dialog createDialog() {
            Launcher.this.mWaitingForResult = Launcher.LOGD;
            View layout = View.inflate(Launcher.this, R.layout.rename_folder, (ViewGroup) null);
            this.mInput = (EditText) layout.findViewById(R.id.folder_name);
            AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
            builder.setTitle(Launcher.this.getString(R.string.rename_folder_title));
            builder.setCancelable(Launcher.LOGD);
            builder.setIcon(0);
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    RenameFolderDialog.this.cleanup();
                }
            });
            builder.setNegativeButton(Launcher.this.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    RenameFolderDialog.this.cleanup();
                }
            });
            builder.setPositiveButton(Launcher.this.getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    RenameFolderDialog.this.changeFolderName();
                }
            });
            builder.setView(layout);
            return builder.create();
        }

        /* access modifiers changed from: private */
        public void changeFolderName() {
            String name = this.mInput.getText().toString();
            if (!TextUtils.isEmpty(name)) {
                Launcher.this.mFolderInfo = Launcher.sLauncherModel.findFolderById(Launcher.this.mFolderInfo.f3id);
                Launcher.this.mFolderInfo.title = name;
                LauncherModel.updateItemInDatabase(Launcher.this, Launcher.this.mFolderInfo);
                if (Launcher.this.mDesktopLocked) {
                    Launcher.sLauncherModel.loadUserItems(false, Launcher.this, false);
                } else {
                    FolderIcon folderIcon = (FolderIcon) Launcher.this.mWorkspace.getViewForTag(Launcher.this.mFolderInfo);
                    if (folderIcon != null) {
                        folderIcon.setText(name);
                        Launcher.this.getWorkspace().requestLayout();
                    } else {
                        Launcher.this.mDesktopLocked = Launcher.LOGD;
                        Launcher.sLauncherModel.loadUserItems(false, Launcher.this, false);
                    }
                }
            }
            cleanup();
        }

        /* access modifiers changed from: private */
        public void cleanup() {
            Launcher.this.mWorkspace.unlock();
            Launcher.this.dismissDialog(3);
            Launcher.this.mWaitingForResult = false;
            Launcher.this.mFolderInfo = null;
        }
    }

    private class LauncherDialog implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {
        private LauncherDialogAdapter mHomeDialogAdapter;

        private LauncherDialog() {
        }

        /* synthetic */ LauncherDialog(Launcher launcher, LauncherDialog launcherDialog) {
            this();
        }

        /* access modifiers changed from: package-private */
        public Dialog createDialog() {
            Launcher.this.mWaitingForResult = Launcher.LOGD;
            this.mHomeDialogAdapter = new LauncherDialogAdapter(Launcher.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
            builder.setAdapter(this.mHomeDialogAdapter, this);
            builder.setInverseBackgroundForced(Launcher.LOGD);
            AlertDialog alertDialog = builder.create();
            alertDialog.setOnCancelListener(this);
            alertDialog.setOnDismissListener(this);
            return alertDialog;
        }

        public void onCancel(DialogInterface dialog) {
            Launcher.this.mWaitingForResult = false;
            cleanup();
        }

        public void onDismiss(DialogInterface dialog) {
            Launcher.this.mWorkspace.unlock();
        }

        private void cleanup() {
            Launcher.this.mWorkspace.unlock();
            Launcher.this.dismissDialog(1);
        }

        public void onClick(DialogInterface dialog, int which) {
            cleanup();
            switch (which) {
                case 0:
                    Launcher.this.showAddDialog(Launcher.this.mAddItemCellInfo);
                    return;
                case 1:
                    Launcher.this.startWallpaperChooser();
                    return;
                case 2:
                    Launcher.this.startPreferences();
                    return;
                default:
                    return;
            }
        }
    }

    private class AddDialog implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {
        private AddDialogAdapter mAdapter;

        private AddDialog() {
        }

        /* synthetic */ AddDialog(Launcher launcher, AddDialog addDialog) {
            this();
        }

        /* access modifiers changed from: package-private */
        public Dialog createDialog() {
            Launcher.this.mWaitingForResult = Launcher.LOGD;
            this.mAdapter = new AddDialogAdapter(Launcher.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
            builder.setTitle(Launcher.this.getString(R.string.add));
            builder.setInverseBackgroundForced(Launcher.LOGD);
            builder.setAdapter(this.mAdapter, this);
            AlertDialog dialog = builder.create();
            dialog.setOnCancelListener(this);
            dialog.setOnDismissListener(this);
            return dialog;
        }

        public void onCancel(DialogInterface dialog) {
            Launcher.this.mWaitingForResult = false;
            cleanup();
        }

        public void onDismiss(DialogInterface dialog) {
            Launcher.this.mWorkspace.unlock();
        }

        private void cleanup() {
            Launcher.this.mWorkspace.unlock();
            Launcher.this.dismissDialog(2);
        }

        public void onClick(DialogInterface dialog, int which) {
            Resources res = Launcher.this.getResources();
            cleanup();
            switch (which) {
                case 0:
                    int appWidgetId = Launcher.this.mAppWidgetHost.allocateAppWidgetId();
                    Intent pickIntent = new Intent("android.appwidget.action.APPWIDGET_PICK");
                    pickIntent.putExtra("appWidgetId", appWidgetId);
                    ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<>();
                    AppWidgetProviderInfo appWidgetProviderInfo = new AppWidgetProviderInfo();
                    appWidgetProviderInfo.provider = new ComponentName(Launcher.this.getPackageName(), "XXX.YYY");
                    appWidgetProviderInfo.label = Launcher.this.getString(R.string.group_search);
                    appWidgetProviderInfo.icon = R.drawable.ic_search_widget;
                    customInfo.add(appWidgetProviderInfo);
                    pickIntent.putParcelableArrayListExtra("customInfo", customInfo);
                    ArrayList<Bundle> customExtras = new ArrayList<>();
                    Bundle bundle = new Bundle();
                    bundle.putString(Launcher.EXTRA_CUSTOM_WIDGET, Launcher.SEARCH_WIDGET);
                    customExtras.add(bundle);
                    pickIntent.putParcelableArrayListExtra("customExtras", customExtras);
                    Launcher.this.startActivityForResult(pickIntent, 9);
                    return;
                case 1:
                    Launcher.this.pickShortcut(7, R.string.title_select_shortcut);
                    return;
                case 2:
                    Bundle bundle2 = new Bundle();
                    ArrayList<String> shortcutNames = new ArrayList<>();
                    shortcutNames.add(res.getString(R.string.group_folder));
                    bundle2.putStringArrayList("android.intent.extra.shortcut.NAME", shortcutNames);
                    ArrayList<Intent.ShortcutIconResource> shortcutIcons = new ArrayList<>();
                    shortcutIcons.add(Intent.ShortcutIconResource.fromContext(Launcher.this, R.drawable.ic_launcher_folder));
                    bundle2.putParcelableArrayList("android.intent.extra.shortcut.ICON_RESOURCE", shortcutIcons);
                    Intent pickIntent2 = new Intent("android.intent.action.PICK_ACTIVITY");
                    pickIntent2.putExtra("android.intent.extra.INTENT", new Intent("android.intent.action.CREATE_LIVE_FOLDER"));
                    pickIntent2.putExtra("android.intent.extra.TITLE", Launcher.this.getText(R.string.title_select_live_folder));
                    pickIntent2.putExtras(bundle2);
                    Launcher.this.startActivityForResult(pickIntent2, 8);
                    return;
                default:
                    return;
            }
        }
    }

    private class ApplicationsIntentReceiver extends BroadcastReceiver {
        private ApplicationsIntentReceiver() {
        }

        /* synthetic */ ApplicationsIntentReceiver(Launcher launcher, ApplicationsIntentReceiver applicationsIntentReceiver) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            String[] packages;
            String action = intent.getAction();
            if ("android.intent.action.PACKAGE_CHANGED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_ADDED".equals(action)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                boolean replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                if (!"android.intent.action.PACKAGE_CHANGED".equals(action)) {
                    if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                        if (!replacing) {
                            Launcher.this.removeShortcutsForPackage(packageName);
                            Launcher.sLauncherModel.removePackage(Launcher.this.mApplicationsView, packageName);
                        }
                    } else if (!replacing) {
                        Launcher.sLauncherModel.addPackage(Launcher.this.mApplicationsView, packageName);
                    } else {
                        Launcher.sLauncherModel.updatePackage(Launcher.this.mApplicationsView, packageName);
                        Launcher.this.updateShortcutsForPackage(packageName);
                    }
                    Launcher.this.removeDialog(2);
                }
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
                String[] packages2 = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                if (packages2 != null && packages2.length != 0) {
                    for (int i = 0; i < packages2.length; i++) {
                        Launcher.sLauncherModel.addPackage(Launcher.this.mApplicationsView, packages2[i]);
                        Launcher.this.updateShortcutsForPackage(packages2[i]);
                    }
                }
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action) && (packages = intent.getStringArrayExtra("android.intent.extra.changed_package_list")) != null && packages.length != 0) {
                for (int i2 = 0; i2 < packages.length; i2++) {
                    Launcher.sLauncherModel.removePackage(Launcher.this.mApplicationsView, packages[i2]);
                    Launcher.this.updateShortcutsForPackage(packages[i2]);
                }
            }
        }
    }

    private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        private CloseSystemDialogsIntentReceiver() {
        }

        /* synthetic */ CloseSystemDialogsIntentReceiver(Launcher launcher, CloseSystemDialogsIntentReceiver closeSystemDialogsIntentReceiver) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            Launcher.this.closeSystemDialogs();
        }
    }

    private class FavoritesChangeObserver extends ContentObserver {
        public FavoritesChangeObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            Launcher.this.onFavoritesChanged();
        }
    }

    private class AppWidgetResetObserver extends ContentObserver {
        public AppWidgetResetObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            Launcher.this.onAppWidgetReset();
        }
    }

    private static class DesktopBinder extends Handler implements MessageQueue.IdleHandler {
        static final int ITEMS_COUNT = 6;
        static final int MESSAGE_BIND_APPWIDGETS = 2;
        static final int MESSAGE_BIND_DRAWER = 3;
        static final int MESSAGE_BIND_ITEMS = 1;
        private final LinkedList<LauncherAppWidgetInfo> mAppWidgets;
        private final ArrayList<ItemInfo> mItemInfos;
        private final WeakReference<Launcher> mLauncher;
        public boolean mTerminate = false;

        DesktopBinder(Launcher launcher, ArrayList<ItemInfo> itemInfos, ArrayList<LauncherAppWidgetInfo> appWidgets) {
            this.mLauncher = new WeakReference<>(launcher);
            this.mItemInfos = itemInfos;
            Collections.sort(this.mItemInfos, ItemInfo.createCellXComparator());
            int currentScreen = launcher.mWorkspace.getCurrentScreen();
            int size = appWidgets.size();
            this.mAppWidgets = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                LauncherAppWidgetInfo launcherAppWidgetInfo = appWidgets.get(i);
                if (launcherAppWidgetInfo.screen == currentScreen) {
                    this.mAppWidgets.addFirst(launcherAppWidgetInfo);
                } else {
                    this.mAppWidgets.addLast(launcherAppWidgetInfo);
                }
            }
        }

        public void startBindingItems() {
            obtainMessage(1, 0, this.mItemInfos.size()).sendToTarget();
        }

        public void startBindingDrawer() {
            obtainMessage(3).sendToTarget();
        }

        public void startBindingAppWidgetsWhenIdle() {
            Looper.myQueue().addIdleHandler(this);
        }

        public boolean queueIdle() {
            startBindingAppWidgets();
            return false;
        }

        public void startBindingAppWidgets() {
            obtainMessage(2).sendToTarget();
        }

        public void handleMessage(Message msg) {
            Launcher launcher = (Launcher) this.mLauncher.get();
            if (launcher != null && !this.mTerminate) {
                switch (msg.what) {
                    case 1:
                        launcher.bindItems(this, this.mItemInfos, msg.arg1, msg.arg2);
                        return;
                    case 2:
                        launcher.bindAppWidgets(this, this.mAppWidgets);
                        return;
                    case 3:
                        launcher.bindDrawer(this, (ApplicationsAdapter) null);
                        return;
                    default:
                        return;
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void startPreferences() {
        startActivity(new Intent(this, Preferences.class));
    }

    /* access modifiers changed from: package-private */
    public View createSmallApplicationsGridItem(int layoutResId, ViewGroup parent, ApplicationsGridItemInfo applicationsGridItemInfo) {
        ImageView imageView = (ImageView) this.mInflater.inflate(layoutResId, parent, false);
        imageView.setImageDrawable(Utilities.createDockIconThumbnail(applicationsGridItemInfo.icon, this));
        imageView.setTag(applicationsGridItemInfo);
        imageView.setOnClickListener(this);
        return imageView;
    }

    /* access modifiers changed from: package-private */
    public View createSmallShortcut(int layoutResId, ViewGroup parent, ApplicationItemInfo info) {
        ImageView imageView = (ImageView) this.mInflater.inflate(layoutResId, parent, false);
        if (!info.filtered) {
            info.icon = Utilities.createIconThumbnail(info.icon, this);
            info.filtered = LOGD;
        }
        imageView.setImageDrawable(Utilities.createDockIconThumbnail(info.icon, this));
        imageView.setOnClickListener(this);
        imageView.setTag(info);
        return imageView;
    }

    /* access modifiers changed from: package-private */
    public View createSmallFolder(int layoutResId, ViewGroup parent, UserFolderInfo info) {
        ImageView imageView = (ImageView) this.mInflater.inflate(layoutResId, parent, false);
        imageView.setImageDrawable(Utilities.createDockIconThumbnail(getResources().getDrawable(R.drawable.ic_launcher_folder), this));
        imageView.setOnClickListener(this);
        imageView.setTag(info);
        return imageView;
    }

    /* access modifiers changed from: package-private */
    public View createSmallLiveFolder(int layoutResId, ViewGroup parent, LiveFolderInfo info) {
        ImageView imageView = (ImageView) this.mInflater.inflate(layoutResId, parent, false);
        Resources resources = getResources();
        Drawable drawable = info.icon;
        if (drawable == null) {
            drawable = resources.getDrawable(R.drawable.ic_launcher_folder);
        }
        info.filtered = LOGD;
        imageView.setImageDrawable(Utilities.createDockIconThumbnail(drawable, this));
        imageView.setOnClickListener(this);
        imageView.setTag(info);
        return imageView;
    }

    public void previousScreen(View v) {
        this.mWorkspace.scrollLeft();
    }

    public void nextScreen(View v) {
        this.mWorkspace.scrollRight();
    }

    /* access modifiers changed from: protected */
    public boolean isPreviewsShowing() {
        return this.mPreviewsShowing;
    }

    /* access modifiers changed from: protected */
    public boolean isFullScreenPreviewing() {
        if (!this.mPreviewsShowing || !this.mFullScreenPreviews) {
            return false;
        }
        return LOGD;
    }

    private void hideDesktop(boolean enable) {
        if (enable) {
            this.mDock.hide(LOGD);
        } else {
            this.mDock.show(LOGD);
        }
    }

    public void dismissPreviews() {
        if (this.mPreviewsShowing) {
            hideDesktop(false);
            this.mPreviewsShowing = false;
            this.mWorkspace.togglePreviews(false);
        }
    }

    public void showPreviews(int start, int end) {
        this.mPreviewsShowing = LOGD;
        hideDesktop(LOGD);
        this.mWorkspace.lock();
        this.mWorkspace.togglePreviews(LOGD);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setRequestedFullscreen();
        }
    }

    private void openApplicationsGrid(boolean animated) {
        if (!this.mApplicationsGridOpen) {
            this.mDock.hide(false);
            this.mWorkspace.enableChildrenCache();
            this.mWorkspace.lock();
            this.mDesktopLocked = LOGD;
            this.mWorkspace.invalidate();
            this.mApplicationsView.getImplementingView().bringToFront();
            this.mApplicationsView.open(animated && this.mAllowAppsGridAnimations);
            if (animated && this.mAllowAppsGridAnimations) {
                this.mHomeButton.setAnimation(AnimationUtils.loadAnimation(this, R.anim.home_button_fade_in));
            }
            this.mHomeButton.setVisibility(0);
            this.mHomeButton.bringToFront();
            if (this.mScreenIndicator != null) {
                this.mScreenIndicator.hide();
            }
            this.mApplicationsGridOpen = LOGD;
        }
    }

    private void closeApplicationsGrid(boolean animated) {
        boolean z;
        if (this.mApplicationsGridOpen) {
            ApplicationsView applicationsView = this.mApplicationsView;
            if (!animated || !this.mAllowAppsGridAnimations) {
                z = false;
            } else {
                z = LOGD;
            }
            if (applicationsView.close(z)) {
                this.mDock.show(false);
                this.mWorkspace.unlock();
                this.mDesktopLocked = false;
                this.mWorkspace.invalidate();
                if (this.mScreenIndicator != null) {
                    this.mScreenIndicator.show();
                }
                if (animated && this.mAllowAppsGridAnimations) {
                    this.mHomeButton.setAnimation(AnimationUtils.loadAnimation(this, R.anim.home_button_fade_out));
                }
                this.mHomeButton.setVisibility(4);
                this.mApplicationsGridOpen = false;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isApplicationsGridOpen() {
        if (this.mApplicationsView == null || this.mApplicationsView.getImplementingView().getVisibility() != 0) {
            return false;
        }
        return LOGD;
    }

    private static class WallpaperIntentReceiver extends BroadcastReceiver {
        private WeakReference<Launcher> mLauncher;

        WallpaperIntentReceiver(Application application, Launcher launcher) {
            setLauncher(launcher);
        }

        /* access modifiers changed from: package-private */
        public void setLauncher(Launcher launcher) {
            this.mLauncher = new WeakReference<>(launcher);
        }

        public void onReceive(Context context, Intent intent) {
            Launcher launcher;
            Workspace workspace;
            if (this.mLauncher != null && (launcher = (Launcher) this.mLauncher.get()) != null && (workspace = launcher.getWorkspace()) != null) {
                workspace.setWallpaper(Launcher.LOGD);
            }
        }
    }

    public void setWindowBackground(boolean liveWallpaper) {
        if (!liveWallpaper) {
            getWindow().setBackgroundDrawable(new ColorDrawable(-16777216));
        } else {
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
    }

    private void performAction(int actionId) {
        switch (actionId) {
            case 2:
                dismissPreviews();
                if (isApplicationsGridOpen()) {
                    closeApplications();
                    return;
                } else {
                    openApplicationsGrid(LOGD);
                    return;
                }
            case 3:
                dismissPreviews();
                if (!this.mWorkspace.isDefaultScreenShowing()) {
                    new AsyncTask() {
                        /* access modifiers changed from: protected */
                        public void onPostExecute(Object result) {
                            super.onPostExecute(result);
                            Launcher.this.mWorkspace.moveToDefaultScreen();
                        }

                        /* access modifiers changed from: protected */
                        public Object doInBackground(Object... objects) {
                            return null;
                        }
                    }.execute(new Object[0]);
                    return;
                }
                return;
            case 4:
                dismissPreviews();
                expandNotifications();
                return;
            case 5:
                if (!this.mPreviewsShowing) {
                    showPreviews(0, this.mWorkspace.mScreenCount);
                    return;
                } else {
                    dismissPreviews();
                    return;
                }
            case 6:
                toggleFullscreen();
                return;
            case 7:
                startActivity(new Intent("android.intent.action.DIAL"));
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: package-private */
    public void onDoubleTap(MotionEvent motionEvent) {
        performAction(PreferencesUtil.getActionBindingForDoubleTap(this));
    }

    public void fireSwipeDownAction() {
        performAction(PreferencesUtil.getActionBindingForSwipeDown(this));
    }

    public void fireSwipeUpAction() {
        performAction(PreferencesUtil.getActionBindingForSwipeUp(this));
    }

    /* access modifiers changed from: private */
    public void realAddWidget(AppWidgetProviderInfo appWidgetInfo, CellLayout.CellInfo cellInfo, int[] spans, int appWidgetId, boolean insertAtFirst) {
        int[] xy = this.mCellCoordinates;
        if (findSlot(cellInfo, xy, spans[0], spans[1])) {
            LauncherAppWidgetInfo launcherAppWidgetInfo = new LauncherAppWidgetInfo(appWidgetId);
            launcherAppWidgetInfo.spanX = spans[0];
            launcherAppWidgetInfo.spanY = spans[1];
            LauncherModel.addItemToDatabase(this, launcherAppWidgetInfo, -100, this.mWorkspace.getCurrentScreen(), xy[0], xy[1], false);
            if (!this.mRestoring) {
                sLauncherModel.addDesktopAppWidget(launcherAppWidgetInfo);
                launcherAppWidgetInfo.hostView = this.mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                launcherAppWidgetInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
                launcherAppWidgetInfo.hostView.setTag(launcherAppWidgetInfo);
                this.mWorkspace.addInCurrentScreen(launcherAppWidgetInfo.hostView, xy[0], xy[1], launcherAppWidgetInfo.spanX, launcherAppWidgetInfo.spanY, insertAtFirst);
            } else if (sLauncherModel.isDesktopLoaded()) {
                sLauncherModel.addDesktopAppWidget(launcherAppWidgetInfo);
            }
        } else if (appWidgetId != -1) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        }
    }

    public static int getScreenCount(Context context) {
        return PreferencesUtil.getNumberOfScreens(context);
    }

    public ScreenIndicator getScreenIndicator() {
        return this.mScreenIndicator;
    }
}
