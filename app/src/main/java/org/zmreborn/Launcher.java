package org.zmreborn;

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
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import android.view.WindowInsets;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.zmreborn.CellLayout;
import org.zmreborn.LauncherSettings;
import org.zmreborn.compat.BackGestureCompat;
import org.zmreborn.compat.WindowInsetsCompat;
import org.zmreborn.theme.WallpaperColorExtractor;
import org.zmreborn.widget.WidgetPickerDialog;
import org.zmreborn.widget.WidgetPickerEntry;

/** Coordinates launcher activity interactions and workspace state. */
public final class Launcher extends Activity implements View.OnClickListener, View.OnLongClickListener, DragController.DragListener {
    static final int APPWIDGET_HOST_ID = 1024;
    static final int DEFAULT_SCREEN = 2;
    private static final int DIALOG_ADD = 2;
    private static final int DIALOG_LAUNCHER = 1;
    static final int DIALOG_RENAME_FOLDER = 3;
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
    private static final int MENU_NEW_APP_LIST_FOLDER = 10;
    private static final int MENU_WALLPAPER = 3;
    private static final int APPLICATIONS_STATE_READY = 0;
    private static final int APPLICATIONS_STATE_LOADING = 1;
    private static final int APPLICATIONS_STATE_EMPTY = 2;
    private static final int APPLICATIONS_STATE_ERROR = 3;
    private static final float BACK_PREVIEW_ALPHA_DISTANCE = 0.35f;
    private static final float BACK_PREVIEW_SCALE_DISTANCE = 0.08f;
    private static final String PREFERENCES = "launcher.preferences";
    private static final boolean PROFILE_ROTATE = false;
    private static final boolean PROFILE_STARTUP = false;
    private static final int REQUEST_BIND_APPWIDGET = 9;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_CREATE_LIVE_FOLDER = 4;
    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_PICK_APPLICATION = 6;
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
    private static final String RUNTIME_STATE_PENDING_APPWIDGET_ID =
            "launcher.pending_appwidget_id";
    private static final String RUNTIME_STATE_PENDING_APPWIDGET_PLACEMENT =
            "launcher.pending_appwidget_placement";
    private static final String RUNTIME_STATE_PENDING_APPWIDGET_INSERT_AT_FIRST =
            "launcher.pending_appwidget_insert_at_first";
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";
    private static final String RUNTIME_STATE_WIDGET_PICKER_OPEN =
            "launcher.widget_picker_open";
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";
    private static final String RUNTIME_STATE_USER_FOLDERS = "launcher.user_folder";
    static final int SCREEN_COUNT = 5;
    static final int WALLPAPER_SCREEN_SPAN = 2;
    /* access modifiers changed from: private */
    public static final LauncherModel sLauncherModel = new LauncherModel();
    private static final Object sLock = new Object();
    static boolean sRestart = false;
    static boolean sRestartLoaders = false;
    private static int sScreen = 2;
    static volatile boolean sSuppressWallpaperRefreshForTests;
    private static final ExecutorService sWallpaperRefreshExecutor = Executors.newSingleThreadExecutor();
    private static WallpaperIntentReceiver sWallpaperReceiver;
    /* access modifiers changed from: private */
    public CellLayout.CellInfo mAddItemCellInfo;
    private boolean mAllowAppsGridAnimations = LOGD;
    /* access modifiers changed from: private */
    public LauncherAppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private boolean mApplicationsGridOpen = false;
    private String mAppearanceFingerprint;
    private int mApplicationsState = APPLICATIONS_STATE_READY;
    private FrameLayout mApplicationsStateOverlay;
    private Object mBackGestureRegistration;
    private View mBackPreviewTarget;
    private final BackGestureCompat.BackHandler mBackHandler = new BackGestureCompat.BackHandler() {
        public void onBackInvoked() {
            Launcher.this.handleBackInvoked();
        }

        public void onBackProgressed(float progress) {
            Launcher.this.onBackProgressed(progress);
        }

        public void onBackCancelled() {
            Launcher.this.onBackCancelled();
        }
    };
    private TextView mApplicationsStateMessage;
    private Button mApplicationsStateRetry;
    private Button mApplicationsStateClose;
    private AlertDialog mAppListFolderDialog;
    private AppListFolderPaletteBinder mAppListFolderPaletteBinder;
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
    private WidgetPickerDialog mWidgetPickerDialog;
    private WidgetResizeSession mWidgetResizeSession;
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
    private boolean mRestoreWidgetPickerWhenReady;
    private int mRotation;
    private int mPendingAppWidgetId = -1;
    private boolean mPendingAppWidgetPlacement;
    private boolean mPendingAppWidgetInsertAtFirst;
    private View.OnLayoutChangeListener mPendingAppWidgetPlacementListener;
    private CellLayout mPendingAppWidgetPlacementLayout;
    private Bundle mSavedInstanceState;
    private Bundle mSavedState;
    private ScreenIndicator mScreenIndicator;
    /* access modifiers changed from: private */
    public boolean mWaitingForResult;
    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();
    /* access modifiers changed from: private */
    public Workspace mWorkspace;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAppearanceFingerprint = Appearance.fingerprint(this);
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
        refreshWallpaperColorsAsync();
        this.mBackGestureRegistration = BackGestureCompat.registerBackHandler(
                this, this.mBackHandler);
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
        String locale = LocaleUtil.currentLocaleFingerprint(this);
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

    private static void readConfiguration(Context context, LocaleConfiguration configuration) {
        if (context == null) {
            return;
        }
        DataInputStream stream = null;
        try {
            stream = new DataInputStream(context.openFileInput(PREFERENCES));
            configuration.locale = stream.readUTF();
            configuration.mcc = stream.readInt();
            configuration.mnc = stream.readInt();
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e3) {
                }
            }
        }
    }

    private static void writeConfiguration(Context context, LocaleConfiguration configuration) {
        if (context == null) {
            return;
        }
        DataOutputStream stream = null;
        try {
            stream = new DataOutputStream(context.openFileOutput(PREFERENCES, 0));
            stream.writeUTF(configuration.locale);
            stream.writeInt(configuration.mcc);
            stream.writeInt(configuration.mnc);
            stream.flush();
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            context.getFileStreamPath(PREFERENCES).delete();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e3) {
                }
            }
        }
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

    static void setWallpaperRefreshSuppressedForTests(boolean suppressed) {
        sSuppressWallpaperRefreshForTests = suppressed;
    }

    static void dispatchWallpaperRefreshForTests(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        WallpaperIntentReceiver wallpaperReceiver = sWallpaperReceiver;
        if (wallpaperReceiver == null) {
            throw new IllegalStateException("Wallpaper receiver is unavailable");
        }
        wallpaperReceiver.dispatchWallpaperRefresh(context, null);
    }

    static void runAfterWallpaperRefreshesForTests(Runnable runnable) {
        executeWallpaperRefresh(runnable);
    }

    static Future<?> executeWallpaperRefresh(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("Wallpaper refresh task must not be null");
        }
        return sWallpaperRefreshExecutor.submit(runnable);
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

    void onApplicationsLoading() {
        setApplicationsState(APPLICATIONS_STATE_LOADING);
    }

    void onApplicationsEmpty() {
        setApplicationsState(APPLICATIONS_STATE_EMPTY);
    }

    void onApplicationsError() {
        setApplicationsState(APPLICATIONS_STATE_ERROR);
    }

    void onApplicationsReady() {
        setApplicationsState(APPLICATIONS_STATE_READY);
        focusFirstApplicationsItem();
    }

    private void setApplicationsState(int state) {
        if (this.mDestroyed) {
            return;
        }
        this.mApplicationsState = state;
        if (this.mApplicationsStateOverlay == null) {
            return;
        }
        if (!this.mApplicationsGridOpen || state == APPLICATIONS_STATE_READY) {
            this.mApplicationsStateOverlay.setVisibility(View.GONE);
            return;
        }
        switch (state) {
            case APPLICATIONS_STATE_LOADING:
                this.mApplicationsStateMessage.setText(R.string.apps_loading);
                this.mApplicationsStateRetry.setVisibility(View.GONE);
                this.mApplicationsStateClose.setVisibility(View.VISIBLE);
                break;
            case APPLICATIONS_STATE_EMPTY:
                this.mApplicationsStateMessage.setText(R.string.apps_empty);
                this.mApplicationsStateRetry.setVisibility(View.GONE);
                this.mApplicationsStateClose.setVisibility(View.VISIBLE);
                break;
            case APPLICATIONS_STATE_ERROR:
                this.mApplicationsStateMessage.setText(R.string.apps_load_error);
                this.mApplicationsStateRetry.setVisibility(View.VISIBLE);
                this.mApplicationsStateClose.setVisibility(View.VISIBLE);
                break;
            default:
                this.mApplicationsStateOverlay.setVisibility(View.GONE);
                return;
        }
        this.mApplicationsStateOverlay.setVisibility(View.VISIBLE);
        this.mApplicationsStateOverlay.bringToFront();
    }

    private void renderApplicationsState() {
        setApplicationsState(this.mApplicationsState);
        if (this.mApplicationsState == APPLICATIONS_STATE_READY) {
            focusFirstApplicationsItem();
        }
    }

    private void focusFirstApplicationsItem() {
        if (!this.mApplicationsGridOpen || this.mApplicationsView == null) {
            return;
        }
        final View applicationsView = this.mApplicationsView.getImplementingView();
        applicationsView.post(new Runnable() {
            public void run() {
                if (!Launcher.this.mApplicationsGridOpen
                        || Launcher.this.mApplicationsState != APPLICATIONS_STATE_READY) {
                    return;
                }
                ApplicationsGridView gridView = Launcher.this.getVerticalApplicationsGrid();
                if (gridView != null) {
                    if (!PreferencesUtil.rememberApplicationsPosition(Launcher.this)) {
                        gridView.setSelection(0);
                    }
                    gridView.setFocusableInTouchMode(true);
                    gridView.requestFocus();
                    return;
                }
                View firstActionableView = findFirstActionableView(applicationsView);
                if (firstActionableView != null) {
                    firstActionableView.requestFocus();
                    return;
                }
                applicationsView.setFocusableInTouchMode(true);
                applicationsView.requestFocus();
            }
        });
    }

    private ApplicationsGridView getVerticalApplicationsGrid() {
        if (this.mApplicationsView instanceof ApplicationsGridView) {
            return (ApplicationsGridView) this.mApplicationsView;
        }
        if (this.mApplicationsView instanceof ApplicationsDrawerView) {
            return ((ApplicationsDrawerView) this.mApplicationsView).getGridView();
        }
        return null;
    }

    private View findFirstActionableView(View root) {
        if (root != this.mApplicationsStateOverlay && root.isClickable() && root.isEnabled()) {
            return root;
        }
        if (!(root instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) root;
        for (int index = 0; index < group.getChildCount(); index++) {
            View child = group.getChildAt(index);
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            View actionableView = findFirstActionableView(child);
            if (actionableView != null) {
                return actionableView;
            }
        }
        return null;
    }

    private void setWallpaperDimension() {
        WallpaperManager wallpaperManager = (WallpaperManager) getSystemService("wallpaper");
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        boolean isPortrait = defaultDisplay.getWidth() < defaultDisplay.getHeight() ? LOGD : false;
        wallpaperManager.suggestDesiredDimensions((isPortrait ? defaultDisplay.getWidth() : defaultDisplay.getHeight()) * 2, isPortrait ? defaultDisplay.getHeight() : defaultDisplay.getWidth());
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.mWaitingForResult = false;
        if (isAppWidgetRequest(requestCode)) {
            data = normalizeAppWidgetResult(resultCode, data);
            if (data == null) {
                return;
            }
        }
        if (resultCode != RESULT_OK || this.mAddItemCellInfo == null) {
            return;
        }
        boolean insertAtFirst = !this.mDesktopLocked;
        handleSuccessfulActivityResult(requestCode, data, insertAtFirst);
    }

    private boolean isAppWidgetRequest(int requestCode) {
        return requestCode == REQUEST_BIND_APPWIDGET
                || requestCode == REQUEST_CREATE_APPWIDGET;
    }

    private Intent normalizeAppWidgetResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || this.mAddItemCellInfo == null) {
            releasePendingAppWidgetId(data);
            return null;
        }
        int pendingAppWidgetId = this.mPendingAppWidgetId;
        if (pendingAppWidgetId == -1) {
            return null;
        }
        if (data != null && data.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, -1) != -1
                && data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                != pendingAppWidgetId) {
            releasePendingAppWidgetId(null);
            return null;
        }
        Intent normalizedData = data == null ? new Intent() : data;
        normalizedData.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                pendingAppWidgetId);
        return normalizedData;
    }

    private void handleSuccessfulActivityResult(
            int requestCode, Intent data, boolean insertAtFirst) {
        switch (requestCode) {
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(data, this.mAddItemCellInfo, insertAtFirst);
                return;
            case REQUEST_CREATE_LIVE_FOLDER:
                completeAddLiveFolder(data, this.mAddItemCellInfo, insertAtFirst);
                return;
            case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(data, this.mAddItemCellInfo, insertAtFirst);
                return;
            case REQUEST_PICK_APPLICATION:
                completeAddApplication(this, data, this.mAddItemCellInfo, insertAtFirst);
                return;
            case REQUEST_PICK_SHORTCUT:
                processShortcut(data, REQUEST_PICK_APPLICATION,
                        REQUEST_CREATE_SHORTCUT);
                return;
            case REQUEST_PICK_LIVE_FOLDER:
                addLiveFolder(data);
                return;
            case REQUEST_BIND_APPWIDGET:
                addAppWidget(data);
                return;
            default:
        }
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        if (!Appearance.fingerprint(this).equals(this.mAppearanceFingerprint)) {
            sRestart = false;
            recreate();
            return;
        }
        if (sRestart) {
            sRestart = false;
            recreate();
            return;
        }
        loadPreferences();
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        this.mRotation = landscape ? 1 : 0;
        int rotation = this.mRotation;
        if (this.mApplicationsView instanceof ApplicationsPagingView) {
            ApplicationsPagingView applicationsPagingView = (ApplicationsPagingView) this.mApplicationsView;
            if (rotation == 0) {
                applicationsPagingView.setNumRows(PreferencesUtil.getAppsGridHorizontalPagingContentRowsPortrait(this));
                applicationsPagingView.setNumColumns(PreferencesUtil.getAppsGridHorizontalPagingContentColumnsPortrait(this));
            } else {
                applicationsPagingView.setNumRows(PreferencesUtil.getAppsGridHorizontalPagingContentRowsLandscape(this));
                applicationsPagingView.setNumColumns(PreferencesUtil.getAppsGridHorizontalPagingContentColumnsLandscape(this));
            }
        }
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
        dismissWidgetResize();
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
        if (keyCode == 19 || keyCode == 20 || keyCode == 21 || keyCode == 22) {
            return handleDpadNavigation(keyCode, event);
        }
        boolean handled = super.onKeyDown(keyCode, event);
        if (handled || !acceptFilter() || keyCode == 66 || !TextKeyListener.getInstance().onKeyDown(this.mWorkspace, this.mDefaultKeySsb, keyCode, event) || this.mDefaultKeySsb == null || this.mDefaultKeySsb.length() <= 0) {
            return handled;
        }
        return onSearchRequested();
    }

    private boolean handleDpadNavigation(int keyCode, KeyEvent event) {
        Folder openFolder = this.mWorkspace.getOpenFolder();
        if (openFolder != null) {
            openFolder.handleFolderKeyEvent(keyCode);
            return true;
        }
        if (this.mApplicationsGridOpen) {
            View appView = this.mApplicationsView.getImplementingView();
            if (appView != null && appView.hasFocus()) {
                if (keyCode == 4) {
                    closeApplications();
                    return true;
                }
                return false;
            }
        }
        return false;
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
            this.mPendingAppWidgetId = savedState.getInt(
                    RUNTIME_STATE_PENDING_APPWIDGET_ID, -1);
            this.mPendingAppWidgetPlacement = savedState.getBoolean(
                    RUNTIME_STATE_PENDING_APPWIDGET_PLACEMENT, false);
            this.mPendingAppWidgetInsertAtFirst = savedState.getBoolean(
                    RUNTIME_STATE_PENDING_APPWIDGET_INSERT_AT_FIRST, false);
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
                boolean[] occupiedCells = savedState.getBooleanArray(
                        RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS);
                int countX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_X);
                int countY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y);
                if (occupiedCells != null && countX > 0 && countY > 0) {
                    addItemCellInfo.findVacantCellsFromOccupied(occupiedCells, countX, countY);
                }
                this.mRestoring = LOGD;
            }
            if (this.mPendingAppWidgetPlacement) {
                resumePendingAppWidgetPlacement();
            }
            restoreWidgetPicker(savedState);
            if (savedState.getBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, false)) {
                this.mFolderInfo = sLauncherModel.getFolderById(this, savedState.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID));
                this.mRestoring = LOGD;
            }
        }
    }

    private void restoreWidgetPicker(Bundle savedState) {
        this.mRestoreWidgetPickerWhenReady = savedState.getBoolean(
                RUNTIME_STATE_WIDGET_PICKER_OPEN, false);
        restoreWidgetPickerWhenReady();
    }

    private void restoreWidgetPickerWhenReady() {
        if (!this.mRestoreWidgetPickerWhenReady || this.mDesktopLocked) {
            return;
        }
        this.mRestoreWidgetPickerWhenReady = false;
        if (this.mPendingAppWidgetId != -1 || this.mAddItemCellInfo == null) {
            return;
        }
        startAddWidgets();
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
                appsGridStub.setLayoutResource(R.layout.apps_grid_view);
                break;
        }
        this.mApplicationsView = (ApplicationsView) appsGridStub.inflate();
        ApplicationsView applicationsView = this.mApplicationsView;
        this.mApplicationsStateOverlay = (FrameLayout) dragLayer.findViewById(R.id.apps_state_overlay);
        this.mApplicationsStateMessage = (TextView) dragLayer.findViewById(R.id.apps_state_message);
        this.mApplicationsStateRetry = (Button) dragLayer.findViewById(R.id.apps_state_retry);
        this.mApplicationsStateClose = (Button) dragLayer.findViewById(R.id.apps_state_close);
        this.mApplicationsStateRetry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Launcher.sLauncherModel.loadApplications(false, Launcher.this.mApplicationsView);
            }
        });
        this.mApplicationsStateClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Launcher.this.closeApplicationsFromStateOverlay();
            }
        });
        ViewStub dockbarStub = (ViewStub) dragLayer.findViewById(R.id.dock_stub);
        dockbarStub.setLayoutResource(R.layout.dockbar);
        dockbarStub.inflate();
        this.mHomeButton = (ImageButton) findViewById(R.id.home_button);
        this.mHomeButton.setContentDescription(getString(R.string.accessibility_open_drawer));
        this.mHomeButton.setFocusable(true);
        this.mHomeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Launcher.this.isApplicationsGridOpen()) {
                    Launcher.this.closeAllApplications();
                }
            }
        });
        DeleteZone deleteZone = (DeleteZone) dragLayer.findViewById(R.id.delete_zone);
        deleteZone.setContentDescription(getString(R.string.accessibility_delete_zone));
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
        dragLayer.addDragListener(this);
        deleteZone.setHandle(new View(this));
        this.mScreenIndicator = (ScreenIndicator) findViewById(R.id.workspace_screen_indicator);
        getWindow().getDecorView().setOnApplyWindowInsetsListener(
                new View.OnApplyWindowInsetsListener() {
                    public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                        return view.onApplyWindowInsets(Launcher.this.applySystemInsets(insets));
                    }
                });
    }

    private WindowInsets applySystemInsets(WindowInsets insets) {
        Rect systemBarInsets = WindowInsetsCompat.getSystemBarInsets(insets);
        Rect gestureInsets = WindowInsetsCompat.getSystemGestureInsets(insets);
        // Decor fitting already keeps drawer outside system bars while edge-to-edge is opted out.
        this.mApplicationsView.setSystemBarInsets(0, 0, 0, 0);
        this.mApplicationsView.setSystemGestureInsets(gestureInsets);
        this.mWorkspace.setSystemGestureInsets(gestureInsets);
        this.mDragLayer.setSystemGestureInsets(gestureInsets);
        this.mDock.setSystemBarInsets(systemBarInsets);
        return insets;
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
        applyBackgroundEffects();
        this.mWorkspace.updateSystemGestureExclusionRects();
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

    void applyBackgroundEffects() {
        if (this.mDestroyed || this.mWorkspace == null || this.mDock == null
                || this.mApplicationsView == null) {
            return;
        }
        int drawerAlpha = PreferencesUtil.getAppsGridBackgroundAlpha(this);
        this.mApplicationsView.setBackgroundAlpha(drawerAlpha);
        if (!PreferencesUtil.isBlurBackgroundsEnabled(this)) {
            loadDockBackground();
            return;
        }
        View drawer = this.mApplicationsView.getImplementingView();
        this.mWorkspace.applyFrostedBackgrounds(this.mDock, drawer, drawerAlpha);
    }

    void invalidateBackgroundEffects() {
        if (this.mDock != null) {
            this.mDock.invalidate();
        }
        if (this.mApplicationsView != null) {
            this.mApplicationsView.getImplementingView().invalidate();
        }
    }

    private void loadDockBackground() {
        String selected = PreferencesUtil.getDockBackgroundType(this);
        String[] backgrounds = getResources().getStringArray(
                R.array.preferences_values_dock_backgrounds);
        applyDockBackground(this.mDock, 0);
        for (int index = 0; index < backgrounds.length; index++) {
            if (backgrounds[index].equals(selected)) {
                applyDockBackground(this.mDock, index);
                break;
            }
        }
        this.mDock.invalidate();
    }

    private void applyDockBackground(Dock dock, int backgroundIndex) {
        int surface = WallpaperColorExtractor.getSurface(this);
        int surfaceVariant = WallpaperColorExtractor.getSurfaceVariant(this);
        switch (backgroundIndex) {
            case 2:
                dock.setBackgroundDrawable(null);
                return;
            case 3:
                dock.setBackgroundColor(surface);
                return;
            case 4:
                dock.setBackgroundColor(surfaceVariant);
                return;
            case 5:
                dock.setBackgroundResource(R.drawable.dock_bg_bar_grey);
                return;
            case 6:
                dock.setBackgroundColor(withAlpha(surfaceVariant, 217));
                return;
            default:
                dock.setBackgroundColor(withAlpha(surface, 217));
        }
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color),
                Color.blue(color));
    }

    private static class AppListFolderPaletteBinder {
        private final FrameLayout mContent;
        private final TextView mTitle;

        private AppListFolderPaletteBinder(FrameLayout content, TextView title) {
            this.mContent = content;
            this.mTitle = title;
        }

        void apply(Context context) {
            int surface = WallpaperColorExtractor.getSurface(context);
            this.mContent.setBackgroundColor(withAlpha(surface, 217));
            this.mTitle.setTextColor(WallpaperColorExtractor.getPrimary(context));
            applyGridView(surface);
            applyEmptyView(context);
        }

        private void applyGridView(int surface) {
            GridView gridView = getGridView();
            if (gridView == null) {
                return;
            }
            gridView.setBackgroundColor(surface);
            if (gridView.getAdapter() instanceof ApplicationsAdapter) {
                ((ApplicationsAdapter) gridView.getAdapter()).notifyDataSetChanged();
            }
        }

        private void applyEmptyView(Context context) {
            TextView emptyView = getEmptyView();
            if (emptyView != null) {
                emptyView.setTextColor(WallpaperColorExtractor.getOnSurface(context));
            }
        }

        private GridView getGridView() {
            for (int index = 0; index < this.mContent.getChildCount(); index++) {
                View child = this.mContent.getChildAt(index);
                if (child instanceof GridView) {
                    return (GridView) child;
                }
            }
            return null;
        }

        private TextView getEmptyView() {
            for (int index = 0; index < this.mContent.getChildCount(); index++) {
                View child = this.mContent.getChildAt(index);
                if (child instanceof TextView) {
                    return (TextView) child;
                }
            }
            return null;
        }
    }

    private void refreshWallpaperColorsAsync() {
        refreshWallpaperColorsAsync(null);
    }

    private void refreshWallpaperColorsAsync(
            final BroadcastReceiver.PendingResult pendingResult) {
        final Context applicationContext = LocaleUtil.wrap(getApplicationContext());
        sWallpaperRefreshExecutor.execute(new Runnable() {
            public void run() {
                try {
                    WallpaperColorExtractor.refresh(applicationContext);
                    Launcher.this.runOnUiThread(new Runnable() {
                        public void run() {
                            applyWallpaperPalette();
                        }
                    });
                } finally {
                    if (pendingResult != null) {
                        pendingResult.finish();
                    }
                }
            }
        });
    }

    private void applyWallpaperPalette() {
        if (this.mDestroyed || this.mApplicationsView == null || this.mDock == null) {
            return;
        }
        this.mApplicationsView.refreshPalette();
        applyBackgroundEffects();
        if (this.mScreenIndicator != null) {
            this.mScreenIndicator.refreshPalette();
        }
        if (this.mWorkspace != null) {
            Folder folder = this.mWorkspace.getOpenFolder();
            if (folder != null) {
                folder.refreshPalette();
            }
        }
        if (this.mAppListFolderPaletteBinder != null) {
            this.mAppListFolderPaletteBinder.apply(this);
        }
    }

    private void loadIndicator() {
        ScreenIndicator indicatorView = (ScreenIndicator) findViewById(R.id.workspace_screen_indicator);
        Resources resources = getResources();
        String screenIndicator = PreferencesUtil.getScreenIndicator(this);
        String[] screenIndicatorTypes = resources.getStringArray(R.array.preferences_values_workspace_screen_indicator_types);
        try {
            for (int i = 0; i < screenIndicatorTypes.length; i++) {
                if (screenIndicatorTypes[i].equals(screenIndicator)) {
                    switch (i) {
                        case 0:
                            this.mScreenIndicator = null;
                            break;
                        case 1:
                            this.mScreenIndicator = indicatorView;
                            this.mScreenIndicator.setType(ScreenIndicator.TYPE_SLIDER_BOTTOM);
                            break;
                        case 2:
                            this.mScreenIndicator = indicatorView;
                            this.mScreenIndicator.setType(ScreenIndicator.TYPE_DOTS);
                            break;
                        default:
                            this.mScreenIndicator = indicatorView;
                            this.mScreenIndicator.setType(ScreenIndicator.TYPE_SLIDER_BOTTOM);
                            break;
                    }
                }
            }
        } catch (ClassCastException e) {
            this.mScreenIndicator = null;
        }
        if (this.mScreenIndicator != null) {
            this.mScreenIndicator.setAutoHide(true);
            if (this.mWorkspace != null) {
                this.mScreenIndicator.setItems(this.mWorkspace.getChildCount());
                this.mWorkspace.indicateCurrent();
            }
        }
        if (this.mApplicationsView instanceof ApplicationsPagingView && indicatorView != null) {
            int drawerType = resolveDrawerIndicatorType();
            ((ApplicationsPagingView) this.mApplicationsView).configureIndicator(
                    indicatorView,
                    drawerType != -1,
                    drawerType == -1 ? ScreenIndicator.TYPE_DOTS : drawerType);
        }
    }

    /** Returns the ScreenIndicator type matching the workspace indicator preference, or -1 for None. */
    private int resolveDrawerIndicatorType() {
        String pref = PreferencesUtil.getScreenIndicator(this);
        String[] types = getResources().getStringArray(
                R.array.preferences_values_workspace_screen_indicator_types);
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(pref)) {
                switch (i) {
                    case 0: return -1;
                    case 1: return ScreenIndicator.TYPE_SLIDER_BOTTOM;
                    case 2: return ScreenIndicator.TYPE_DOTS;
                    default: break;
                }
            }
        }
        return ScreenIndicator.TYPE_SLIDER_BOTTOM;
    }

    /* access modifiers changed from: package-private */
    public View createShortcut(ApplicationItemInfo info) {
        return createShortcut(R.layout.application, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentScreen()), info);
    }

    /* access modifiers changed from: package-private */
    public View createShortcut(int layoutResId, ViewGroup parent, ApplicationItemInfo info) {
        TextView favorite = (TextView) this.mInflater.inflate(layoutResId, parent, false);
        if (!info.filtered) {
            info.icon = Utilities.normalizeApplicationIcon(info.icon, this);
            info.filtered = LOGD;
        }
        info.icon = Utilities.setCompoundApplicationIcon(favorite, info.icon, this);
        if (PreferencesUtil.isShowShortcutTitlesEnabled(this)) {
            favorite.setText(info.title);
        }
        favorite.setContentDescription(info.title);
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
        textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, Utilities.createIconThumbnail(applicationsGridItemInfo.resolveIcon(this), this), (Drawable) null, (Drawable) null);
        if (PreferencesUtil.isShowShortcutTitlesEnabled(this)) {
            textView.setText(applicationsGridItemInfo.title);
        }
        textView.setContentDescription(applicationsGridItemInfo.title);
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

    private void completeAddAppWidget(
            Intent data, CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
        int appWidgetId = data.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId != this.mPendingAppWidgetId) {
            return;
        }
        AppWidgetProviderInfo appWidgetInfo =
                this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetId == -1 || appWidgetInfo == null) {
            releasePendingAppWidgetId(data);
            return;
        }
        this.mPendingAppWidgetId = appWidgetId;
        schedulePendingAppWidgetPlacement(appWidgetInfo, cellInfo, insertAtFirst);
    }

    private void resumePendingAppWidgetPlacement() {
        if (this.mPendingAppWidgetId == -1 || this.mAddItemCellInfo == null) {
            releasePendingAppWidgetId(null);
            return;
        }
        AppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(
                this.mPendingAppWidgetId);
        if (appWidgetInfo == null) {
            releasePendingAppWidgetId(null);
            return;
        }
        schedulePendingAppWidgetPlacement(appWidgetInfo, this.mAddItemCellInfo,
                this.mPendingAppWidgetInsertAtFirst);
    }

    private void schedulePendingAppWidgetPlacement(
            final AppWidgetProviderInfo appWidgetInfo,
            final CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
        removePendingAppWidgetPlacementListener();
        this.mPendingAppWidgetPlacement = LOGD;
        this.mPendingAppWidgetInsertAtFirst = insertAtFirst;
        final int appWidgetId = this.mPendingAppWidgetId;
        final CellLayout targetLayout = getCellLayoutForScreen(cellInfo.screen);
        if (appWidgetId == -1 || targetLayout == null) {
            releasePendingAppWidgetId(null);
            return;
        }
        if (targetLayout.isWidgetSizingGeometryReady()) {
            placePendingAppWidget(appWidgetInfo, cellInfo, targetLayout, appWidgetId);
            return;
        }
        this.mPendingAppWidgetPlacementLayout = targetLayout;
        this.mPendingAppWidgetPlacementListener = new View.OnLayoutChangeListener() {
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (!Launcher.this.isPendingAppWidgetPlacementCurrent(
                        appWidgetId, cellInfo, targetLayout)) {
                    Launcher.this.abandonPendingAppWidgetPlacement(appWidgetId);
                    return;
                }
                if (!targetLayout.isWidgetSizingGeometryReady()) {
                    return;
                }
                Launcher.this.placePendingAppWidget(
                        appWidgetInfo, cellInfo, targetLayout, appWidgetId);
            }
        };
        targetLayout.addOnLayoutChangeListener(this.mPendingAppWidgetPlacementListener);
        targetLayout.requestLayout();
    }

    private boolean isPendingAppWidgetPlacementCurrent(int appWidgetId,
            CellLayout.CellInfo cellInfo, CellLayout targetLayout) {
        return !this.mDestroyed
                && this.mPendingAppWidgetPlacement
                && this.mPendingAppWidgetId == appWidgetId
                && this.mAddItemCellInfo == cellInfo
                && getCellLayoutForScreen(cellInfo.screen) == targetLayout;
    }

    private void placePendingAppWidget(AppWidgetProviderInfo appWidgetInfo,
            CellLayout.CellInfo cellInfo, CellLayout targetLayout, int appWidgetId) {
        if (!isPendingAppWidgetPlacementCurrent(appWidgetId, cellInfo, targetLayout)) {
            abandonPendingAppWidgetPlacement(appWidgetId);
            return;
        }
        removePendingAppWidgetPlacementListener();
        int[] spans = targetLayout.rectToCellFromDp(
                appWidgetInfo.minWidth, appWidgetInfo.minHeight);
        realAddWidget(appWidgetInfo, cellInfo, spans, appWidgetId,
                this.mPendingAppWidgetInsertAtFirst);
    }

    private void abandonPendingAppWidgetPlacement(int appWidgetId) {
        removePendingAppWidgetPlacementListener();
        if (this.mDestroyed && getChangingConfigurations() != 0) {
            return;
        }
        if (this.mPendingAppWidgetId == appWidgetId) {
            releasePendingAppWidgetId(null);
            return;
        }
        if (appWidgetId != -1) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        }
    }

    private void removePendingAppWidgetPlacementListener() {
        if (this.mPendingAppWidgetPlacementLayout != null
                && this.mPendingAppWidgetPlacementListener != null) {
            this.mPendingAppWidgetPlacementLayout.removeOnLayoutChangeListener(
                    this.mPendingAppWidgetPlacementListener);
        }
        this.mPendingAppWidgetPlacementLayout = null;
        this.mPendingAppWidgetPlacementListener = null;
    }

    private CellLayout getCellLayoutForScreen(int screen) {
        if (this.mWorkspace == null || screen < 0
                || screen >= this.mWorkspace.getChildCount()) {
            return null;
        }
        View child = this.mWorkspace.getChildAt(screen);
        if (!(child instanceof CellLayout)) {
            return null;
        }
        return (CellLayout) child;
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
            dismissWidgetResize();
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
        boolean savePendingAdd = this.mAddItemCellInfo != null
                && (this.mWaitingForResult || this.mPendingAppWidgetPlacement);
        if (savePendingAdd) {
            CellLayout.CellInfo addItemCellInfo = this.mAddItemCellInfo;
            CellLayout layout = getCellLayoutForScreen(addItemCellInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN, addItemCellInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, addItemCellInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, addItemCellInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, addItemCellInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, addItemCellInfo.spanY);
            if (layout != null) {
                outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_X, layout.getCountX());
                outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y, layout.getCountY());
                outState.putBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS,
                        layout.getOccupiedCells());
            }
        }
        outState.putBoolean(RUNTIME_STATE_WIDGET_PICKER_OPEN,
                this.mRestoreWidgetPickerWhenReady
                        || (this.mWidgetPickerDialog != null
                        && this.mWidgetPickerDialog.isShowing()));
        if (this.mPendingAppWidgetId != -1) {
            outState.putInt(RUNTIME_STATE_PENDING_APPWIDGET_ID,
                    this.mPendingAppWidgetId);
        }
        outState.putBoolean(RUNTIME_STATE_PENDING_APPWIDGET_PLACEMENT,
                this.mPendingAppWidgetPlacement);
        outState.putBoolean(RUNTIME_STATE_PENDING_APPWIDGET_INSERT_AT_FIRST,
                this.mPendingAppWidgetInsertAtFirst);
        if (this.mFolderInfo != null && this.mWaitingForResult) {
            outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, LOGD);
            outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID, this.mFolderInfo.f3id);
        }
    }

    public void onDestroy() {
        this.mDestroyed = LOGD;
        dismissWidgetPicker();
        dismissWidgetResize();
        removePendingAppWidgetPlacementListener();
        if (getChangingConfigurations() == 0) {
            releasePendingAppWidgetId(null);
        }
        onBackCancelled();
        BackGestureCompat.unregisterBackHandler(this, this.mBackGestureRegistration);
        this.mBackGestureRegistration = null;
        if (this.mWorkspace != null) {
            this.mWorkspace.destroyBackgroundEffects();
        }
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
        dismissWidgetResize();
        closeApplications(false);
        if (requestCode >= 0) {
            this.mWaitingForResult = LOGD;
        }
        super.startActivityForResult(intent, requestCode);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(1, 2, 0, R.string.menu_add).setIcon(17301555).setAlphabeticShortcut('A');
        menu.add(2, 6, 0, R.string.menu_preferences).setIcon(17301570).setAlphabeticShortcut('P');
        menu.add(2, 3, 0, R.string.menu_wallpaper).setIcon(17301567).setAlphabeticShortcut('W');
        menu.add(2, 4, 0, R.string.menu_search).setIcon(17301600).setAlphabeticShortcut('s');
        menu.add(2, 5, 0, R.string.menu_applications).setIcon(17301591).setAlphabeticShortcut('M');
        menu.add(2, 7, 0, R.string.menu_settings).setIntent(new Intent("android.settings.SETTINGS")).setIcon(17301577).setAlphabeticShortcut('S');
        menu.add(3, 8, 0, R.string.menu_manage_apps).setIcon(17301570).setAlphabeticShortcut('M');
        menu.add(3, 9, 0, R.string.menu_uninstall_apps).setIcon(17301564).setAlphabeticShortcut('U');
        menu.add(3, MENU_NEW_APP_LIST_FOLDER, 1, R.string.menu_new_app_list_folder).setIcon(R.drawable.ic_menu_edit);
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
            case MENU_NEW_APP_LIST_FOLDER:
                showNewAppListFolderDialog();
                return LOGD;
            default:
                if (PreferencesUtil.isFullscreenEnabled(this) && !isFullscreen()) {
                    setFullscreen(LOGD, false);
                }
                return super.onOptionsItemSelected(item);
        }
    }

    /* access modifiers changed from: package-private */
    public void openAppListFolder(final AppListFolderInfo folderInfo) {
        if (this.mAppListFolderDialog != null) {
            this.mAppListFolderDialog.dismiss();
        }
        GridView gridView = new GridView(this);
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int requestedColumns = landscape ? 5 : 4;
        gridView.setNumColumns(requestedColumns);
        gridView.setVerticalSpacing(7);
        gridView.setPadding(4, 4, 4, 4);
        gridView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView parent, View view, int position, long id) {
                ApplicationItemInfo application = (ApplicationItemInfo) parent.getItemAtPosition(position);
                startActivitySafely(application.intent);
            }
        });
        gridView.setAdapter(new ApplicationsAdapter(this, folderInfo.getContents()));
        TextView emptyView = new TextView(this);
        emptyView.setText(R.string.app_list_folder_empty);
        emptyView.setTextAppearance(R.style.TextAppearance_ZmReborn_Body);
        emptyView.setGravity(17);
        FrameLayout content = new FrameLayout(this);
        content.addView(gridView, new FrameLayout.LayoutParams(-1, -1));
        content.addView(emptyView, new FrameLayout.LayoutParams(-1, -1));
        gridView.setEmptyView(emptyView);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(content).setNegativeButton(R.string.button_close, null);
        TextView titleView = new TextView(this);
        titleView.setText(folderInfo.title);
        titleView.setTextAppearance(R.style.TextAppearance_ZmReborn_Title);
        titleView.setPadding(12, 12, 12, 12);
        builder.setCustomTitle(titleView);
        this.mAppListFolderDialog = builder.create();
        this.mAppListFolderPaletteBinder = new AppListFolderPaletteBinder(content, titleView);
        this.mAppListFolderPaletteBinder.apply(this);
        this.mAppListFolderDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                if (dialog == Launcher.this.mAppListFolderDialog) {
                    Launcher.this.mAppListFolderDialog = null;
                    Launcher.this.mAppListFolderPaletteBinder = null;
                }
            }
        });
        this.mAppListFolderDialog.show();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int margin = Math.max(1, (int) (12.0f * displayMetrics.density));
        FolderLayoutMetrics metrics = FolderLayoutMetrics.calculate(displayMetrics.widthPixels,
                displayMetrics.heightPixels, margin, displayMetrics.widthPixels - margin * 2,
                (int) (displayMetrics.heightPixels * 0.72f), (int) (58.0f * displayMetrics.density),
                requestedColumns, folderInfo.getContents().size(), (int) (48.0f * displayMetrics.density));
        if (this.mAppListFolderDialog.getWindow() != null) {
            this.mAppListFolderDialog.getWindow().setLayout(metrics.getPanelWidth(), metrics.getPanelHeight());
        }
    }

    /* access modifiers changed from: package-private */
    public void showNewAppListFolderDialog() {
        final EditText titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setHint(R.string.folder_name);
        titleInput.setBackgroundColor(
                WallpaperColorExtractor.getSurfaceVariant(this));
        titleInput.setTextColor(WallpaperColorExtractor.getOnSurface(this));
        titleInput.setHintTextColor(WallpaperColorExtractor.getOutline(this));
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.menu_new_app_list_folder)
                .setView(titleInput)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_done, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String title = titleInput.getText().toString().trim();
                        if (title.length() == 0) {
                            Toast.makeText(Launcher.this, R.string.folder_name, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Launcher.this.showAppListFolderSelection(title);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    void showAppListFolderActions(final AppListFolderInfo folderInfo) {
        final String[] actions = {getString(R.string.app_list_folder_rename),
                getString(R.string.app_list_folder_delete)};
        new AlertDialog.Builder(this).setTitle(folderInfo.title).setItems(actions,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showAppListFolderRenameDialog(folderInfo);
                            return;
                        }
                        confirmDeleteAppListFolder(folderInfo);
                    }
                }).show();
    }

    private void showAppListFolderRenameDialog(final AppListFolderInfo folderInfo) {
        final EditText titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setText(folderInfo.title);
        titleInput.setBackgroundColor(
                WallpaperColorExtractor.getSurfaceVariant(this));
        titleInput.setTextColor(WallpaperColorExtractor.getOnSurface(this));
        new AlertDialog.Builder(this).setTitle(R.string.rename_folder_title).setView(titleInput)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_done, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String title = titleInput.getText().toString().trim();
                        if (title.length() == 0) {
                            return;
                        }
                        new AppListFolderStore(getContentResolver()).renameFolder(
                                folderInfo.getFolderId(), title);
                        sLauncherModel.loadApplications(false, mApplicationsView);
                    }
                }).show();
    }

    private void confirmDeleteAppListFolder(final AppListFolderInfo folderInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(folderInfo.title);
        TextView messageView = new TextView(this);
        messageView.setText(R.string.app_list_folder_delete);
        messageView.setTextColor(getColor(R.color.zm_reborn_ember));
        messageView.setPadding(12, 12, 12, 12);
        builder.setView(messageView)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new AppListFolderStore(getContentResolver()).deleteFolder(
                                folderInfo.getFolderId());
                        sLauncherModel.loadApplications(false, mApplicationsView);
                    }
                }).show();
    }

    private void showAppListFolderSelection(final String title) {
        ArrayList<ApplicationItemInfo> allApplications = LauncherModel.getAllApplications();
        Set<String> assignedComponents = new AppListFolderStore(getContentResolver()).loadAssignedComponents();
        final ArrayList<ApplicationItemInfo> applications = new ArrayList<>();
        for (ApplicationItemInfo application : allApplications) {
            if (!assignedComponents.contains(AppListFolderProjection.componentNameOf(application))) {
                applications.add(application);
            }
        }
        final boolean[] checked = new boolean[applications.size()];
        String[] labels = new String[applications.size()];
        for (int index = 0; index < applications.size(); index++) {
            labels[index] = applications.get(index).title.toString();
        }
        new AlertDialog.Builder(this).setTitle(title).setMultiChoiceItems(labels, checked,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                }).setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_done, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        AppListFolderStore store = new AppListFolderStore(getContentResolver());
                        long folderId = store.createFolder(title, 0);
                        ArrayList<String> components = new ArrayList<>();
                        for (int index = 0; index < applications.size(); index++) {
                            if (checked[index]) {
                                components.add(AppListFolderProjection.componentNameOf(applications.get(index)));
                            }
                        }
                        store.replaceContents(folderId, components);
                        sLauncherModel.loadApplications(false, mApplicationsView);
                    }
                }).show();
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
        Toast.makeText(this, getString(R.string.toast_activity_info_not_found, applicationItemInfo.title), 1).show();
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
        int appWidgetId = data.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        this.mPendingAppWidgetId = appWidgetId;
        AppWidgetProviderInfo appWidget =
                this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidget == null) {
            releasePendingAppWidgetId(data);
            return;
        }
        continueAddAppWidget(appWidget, appWidgetId, data);
    }

    private void continueAddAppWidget(AppWidgetProviderInfo appWidget,
            int appWidgetId, Intent resultData) {
        if (appWidget.configure != null) {
            startAppWidgetConfiguration(appWidget, appWidgetId, resultData);
            return;
        }
        onActivityResult(REQUEST_CREATE_APPWIDGET, RESULT_OK, resultData);
    }

    private void startAppWidgetConfiguration(
            AppWidgetProviderInfo appWidget, int appWidgetId, Intent resultData) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setComponent(appWidget.configure);
        this.mWaitingForResult = LOGD;
        try {
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } catch (ActivityNotFoundException exception) {
            Log.e(LOG_TAG, "Unable to configure app widget " + appWidget.provider,
                    exception);
            releasePendingAppWidgetId(resultData);
            this.mWaitingForResult = false;
        }
    }

    private void addSelectedAppWidget(AppWidgetProviderInfo provider) {
        validateSelectedAppWidget(provider);
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        this.mPendingAppWidgetId = appWidgetId;
        Intent resultData = createAppWidgetResult(appWidgetId);
        try {
            if (this.mAppWidgetManager.bindAppWidgetIdIfAllowed(
                    appWidgetId, provider.provider)) {
                addAppWidget(resultData);
                return;
            }
            startAppWidgetBinding(provider, resultData);
        } catch (IllegalArgumentException exception) {
            failSelectedAppWidget(provider, resultData, exception);
        } catch (SecurityException exception) {
            failSelectedAppWidget(provider, resultData, exception);
        }
    }

    private void validateSelectedAppWidget(AppWidgetProviderInfo provider) {
        if (provider == null || provider.provider == null) {
            throw new IllegalArgumentException("Selected widget requires provider component");
        }
        if (provider.minWidth < 0 || provider.minHeight < 0) {
            throw new IllegalArgumentException(
                    "Selected widget has invalid minimum dimensions: " + provider.provider);
        }
    }

    private Intent createAppWidgetResult(int appWidgetId) {
        Intent resultData = new Intent();
        resultData.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return resultData;
    }

    private void startAppWidgetBinding(AppWidgetProviderInfo provider,
            Intent resultData) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                this.mPendingAppWidgetId);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,
                provider.provider);
        try {
            startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
        } catch (ActivityNotFoundException exception) {
            failSelectedAppWidget(provider, resultData, exception);
        }
    }

    private void failSelectedAppWidget(AppWidgetProviderInfo provider,
            Intent resultData, RuntimeException exception) {
        Log.e(LOG_TAG, "Unable to bind app widget " + provider.provider,
                exception);
        releasePendingAppWidgetId(resultData);
        this.mWaitingForResult = false;
    }

    private void releasePendingAppWidgetId(Intent resultData) {
        removePendingAppWidgetPlacementListener();
        int appWidgetId = this.mPendingAppWidgetId;
        if (appWidgetId == -1 && resultData != null) {
            appWidgetId = resultData.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        }
        if (appWidgetId != -1) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        }
        clearPendingAppWidgetId();
    }

    private void clearPendingAppWidgetId() {
        removePendingAppWidgetPlacementListener();
        this.mPendingAppWidgetId = -1;
        this.mPendingAppWidgetPlacement = false;
        this.mPendingAppWidgetInsertAtFirst = false;
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
            CellLayout targetLayout = getCellLayoutForScreen(cellInfo.screen);
            boolean[] occupiedCells = this.mSavedState == null ? null
                    : this.mSavedState.getBooleanArray(
                            RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS);
            CellLayout.CellInfo vacantCells = targetLayout == null ? null
                    : targetLayout.findAllVacantCells(occupiedCells, null);
            if (vacantCells == null || !vacantCells.findCellForSpan(xy, spanX, spanY)) {
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

    private static void registerReceiverCompat(Context context, BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT < 33) {
            context.registerReceiver(receiver, filter);
            return;
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void registerIntentReceivers() {
        if (sWallpaperReceiver == null) {
            Application application = getApplication();
            sWallpaperReceiver = new WallpaperIntentReceiver(application, this);
            registerReceiverCompat(application, sWallpaperReceiver, new IntentFilter("android.intent.action.WALLPAPER_CHANGED"));
        } else {
            sWallpaperReceiver.setLauncher(this);
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme("package");
        registerReceiverCompat(this, this.mApplicationsReceiver, intentFilter);
        registerReceiverCompat(this, this.mCloseSystemDialogsReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        registerReceiverCompat(this, this.mApplicationsReceiver, intentFilter2);
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
                    handleBackInvoked();
                    return LOGD;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void handleBackInvoked() {
        resetBackPreview();
        if (dismissWidgetResize()) {
            return;
        }
        if (this.mApplicationsGridOpen) {
            closeApplications();
        } else {
            closeFolder();
        }
        if (isPreviewsShowing()) {
            dismissPreviews();
        }
    }

    private void onBackProgressed(float progress) {
        View target = getBackPreviewTarget();
        if (target == null) {
            return;
        }
        this.mBackPreviewTarget = target;
        float boundedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        target.setAlpha(1.0f - (BACK_PREVIEW_ALPHA_DISTANCE * boundedProgress));
        float scale = 1.0f - (BACK_PREVIEW_SCALE_DISTANCE * boundedProgress);
        target.setScaleX(scale);
        target.setScaleY(scale);
    }

    private void onBackCancelled() {
        resetBackPreview();
    }

    private View getBackPreviewTarget() {
        if (this.mWidgetResizeSession != null) {
            return this.mWidgetResizeSession.frame;
        }
        if (this.mApplicationsGridOpen) {
            return this.mApplicationsView.getImplementingView();
        }
        return this.mWorkspace.getOpenFolder();
    }

    private void resetBackPreview() {
        if (this.mBackPreviewTarget == null) {
            return;
        }
        this.mBackPreviewTarget.setAlpha(1.0f);
        this.mBackPreviewTarget.setScaleX(1.0f);
        this.mBackPreviewTarget.setScaleY(1.0f);
        this.mBackPreviewTarget = null;
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
        updateWorkspaceEmptyTip();
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
        } catch (IOException exception) {
            Log.w(LOG_TAG, "Unable to persist dock bootstrap marker", exception);
        }
        return LOGD;
    }

    private static File getBootstrapFile(Context context) {
        return new File(String.valueOf(Preferences.getApplicationDataPath(context)) + "x");
    }

    /* access modifiers changed from: package-private */
    public void bootstrapDock() {
        Log.d(LOG_TAG, "-- bootstrapDock()");
        ArrayList<ApplicationItemInfo> applications = resolveDefaultDockApplications();
        for (int index = 0; index < applications.size(); index++) {
            this.mDock.sendDrop(applications.get(index));
        }
    }

    private ArrayList<ApplicationItemInfo> resolveDefaultDockApplications() {
        ArrayList<ApplicationItemInfo> applications = new ArrayList<>();
        ArrayList<Intent> intents = createDefaultDockIntents();
        for (int index = 0; index < intents.size(); index++) {
            addResolvedDockApplication(applications, intents.get(index));
        }
        return applications;
    }

    private ArrayList<Intent> createDefaultDockIntents() {
        ArrayList<Intent> intents = new ArrayList<>();
        intents.add(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:")));
        intents.add(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")));
        intents.add(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com")));
        intents.add(new Intent("android.media.action.IMAGE_CAPTURE"));
        intents.add(new Intent(Intent.ACTION_VIEW,
                Uri.parse("content://com.android.contacts/contacts")));
        return intents;
    }

    private void addResolvedDockApplication(ArrayList<ApplicationItemInfo> applications,
            Intent intent) {
        PackageManager packageManager = getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (!isValidDockResolveInfo(resolveInfo)) {
            resolveInfo = selectDockResolveInfo(resolveInfo,
                    packageManager.queryIntentActivities(intent,
                            PackageManager.MATCH_DEFAULT_ONLY));
        }
        if (resolveInfo == null) {
            return;
        }
        ComponentName componentName = new ComponentName(
                resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        if (containsComponent(applications, componentName)) {
            return;
        }
        ApplicationItemInfo application = getApplicationItemInfoOrNull(componentName);
        if (application != null) {
            applications.add(application);
        }
    }

    static ResolveInfo selectDockResolveInfo(ResolveInfo resolvedActivity,
            List<ResolveInfo> candidates) {
        if (isValidDockResolveInfo(resolvedActivity)) {
            return resolvedActivity;
        }
        if (candidates == null) {
            return null;
        }
        ResolveInfo selected = null;
        for (int index = 0; index < candidates.size(); index++) {
            ResolveInfo candidate = candidates.get(index);
            if (!isValidDockResolveInfo(candidate)) {
                continue;
            }
            if (selected == null) {
                selected = candidate;
                continue;
            }
            ActivityInfo candidateActivity = candidate.activityInfo;
            ActivityInfo selectedActivity = selected.activityInfo;
            int packageComparison = candidateActivity.packageName.compareTo(
                    selectedActivity.packageName);
            if (packageComparison < 0 || (packageComparison == 0
                    && candidateActivity.name.compareTo(selectedActivity.name) < 0)) {
                selected = candidate;
            }
        }
        return selected;
    }

    private static boolean isValidDockResolveInfo(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return false;
        }
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        return activityInfo.packageName != null && activityInfo.packageName.length() > 0
                && activityInfo.name != null && activityInfo.name.length() > 0
                && !"android".equals(activityInfo.packageName);
    }

    private boolean containsComponent(ArrayList<ApplicationItemInfo> applications,
            ComponentName componentName) {
        for (int index = 0; index < applications.size(); index++) {
            if (componentName.equals(applications.get(index).intent.getComponent())) {
                return true;
            }
        }
        return false;
    }

    private ApplicationItemInfo getApplicationItemInfoOrNull(ComponentName componentName) {
        PackageManager packageManager = getPackageManager();
        try {
            ActivityInfo activityInfo = packageManager.getActivityInfo(componentName, 0);
            ApplicationItemInfo applicationItemInfo = new ApplicationItemInfo();
            Drawable icon = activityInfo.loadIcon(packageManager);
            applicationItemInfo.icon = Utilities.normalizeApplicationIcon(icon, this);
            applicationItemInfo.container = -1;
            applicationItemInfo.filtered = false;
            applicationItemInfo.title = activityInfo.loadLabel(packageManager);
            applicationItemInfo.setActivity(componentName, 270532608);
            return applicationItemInfo;
        } catch (PackageManager.NameNotFoundException exception) {
            return null;
        }
    }

    private void bindDesktopItems(ArrayList<ItemInfo> shortcuts, ArrayList<LauncherAppWidgetInfo> appWidgets) {
        dismissWidgetResize();
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
        if (this.mBootstrap && dockItems.size() == 0) {
            bootstrapDock();
        }
        deleteLegacyDrawerBootstrapMarker();
        workspace.requestLayout();
        if (end >= count) {
            finishBindDesktopItems();
            binder.startBindingDrawer();
            return;
        }
        binder.obtainMessage(1, i, count).sendToTarget();
    }

    private void deleteLegacyDrawerBootstrapMarker() {
        File marker = new File(
                String.valueOf(Preferences.getApplicationDataPath(this)) + "dirty");
        if (marker.exists() && !marker.delete()) {
            Log.w(LOG_TAG, "Unable to delete legacy drawer bootstrap marker");
        }
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
        restoreWidgetPickerWhenReady();
        updateWorkspaceEmptyTip();
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
            updateAppWidgetSizeOptions(launcherAppWidgetInfo);
            launcherAppWidgetInfo.hostView = this.mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
            Log.d(LOG_TAG, String.format("about to setAppWidget for id=%d, info=%s", new Object[]{Integer.valueOf(appWidgetId), appWidgetInfo}));
            launcherAppWidgetInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            launcherAppWidgetInfo.hostView.setTag(launcherAppWidgetInfo);
            workspace.addInScreen(launcherAppWidgetInfo.hostView, launcherAppWidgetInfo.screen, launcherAppWidgetInfo.cellX, launcherAppWidgetInfo.cellY, launcherAppWidgetInfo.spanX, launcherAppWidgetInfo.spanY, desktopLocked ? false : LOGD);
            workspace.requestLayout();
        }
        if (!appWidgets.isEmpty()) {
            binder.obtainMessage(2).sendToTarget();
        } else {
            updateWorkspaceEmptyTip();
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
        updateWorkspaceEmptyTip();
    }

    /* access modifiers changed from: package-private */
    public boolean isWorkspaceLocked() {
        return this.mDesktopLocked;
    }

    /** Starts a workspace action for the long-pressed view. */
    public boolean onLongClick(View view) {
        if (this.mDesktopLocked) {
            return false;
        }
        View pressedView = view;
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
            } else if (!(cellInfo.cell instanceof Folder)
                    && !showWidgetResize(pressedView, cellInfo)) {
                this.mWorkspace.startDrag(cellInfo);
            }
        }
        return LOGD;
    }

    private boolean showWidgetResize(View view, CellLayout.CellInfo cellInfo) {
        if (!(view instanceof LauncherAppWidgetHostView)
                || !(view.getTag() instanceof LauncherAppWidgetInfo)
                || !(view.getParent() instanceof CellLayout)) {
            return false;
        }
        LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) view.getTag();
        AppWidgetProviderInfo providerInfo = this.mAppWidgetManager.getAppWidgetInfo(
                widgetInfo.appWidgetId);
        if (providerInfo == null
                || providerInfo.resizeMode == AppWidgetProviderInfo.RESIZE_NONE) {
            return false;
        }
        CellLayout cellLayout = (CellLayout) view.getParent();
        if (!cellLayout.isWidgetSizingGeometryReady()) {
            return false;
        }
        dismissWidgetResize();
        WidgetResizeSession session = new WidgetResizeSession(this, cellLayout,
                (LauncherAppWidgetHostView) view, cellInfo, widgetInfo, providerInfo);
        if (!session.frame.supportsResize()) {
            return false;
        }
        this.mWidgetResizeSession = session;
        this.mWorkspace.setAllowLongPress(false);
        this.mDragLayer.addView(session.frame,
                new FrameLayout.LayoutParams(-1, -1));
        session.frame.bringToFront();
        session.frame.requestFocus();
        return true;
    }

    private void commitWidgetResize(WidgetResizeSession session,
            CellLayout.ResizeCandidate candidate) {
        if (session != this.mWidgetResizeSession
                || !session.cellLayout.applyResizeCandidate(session.widgetView, candidate)) {
            dismissWidgetResize(session);
            return;
        }
        CellLayout.LayoutParams params = (CellLayout.LayoutParams)
                session.widgetView.getLayoutParams();
        session.widgetInfo.cellX = params.cellX;
        session.widgetInfo.cellY = params.cellY;
        session.widgetInfo.spanX = params.cellHSpan;
        session.widgetInfo.spanY = params.cellVSpan;
        LauncherModel.updateItemInDatabase(this, session.widgetInfo);
        updateAppWidgetSizeOptions(session.widgetInfo);
        dismissWidgetResize(session);
    }

    private void startWidgetDrag(WidgetResizeSession session) {
        if (!isWidgetDragSessionValid(session)) {
            dismissWidgetResize(session);
            return;
        }
        dismissWidgetResize(session);
        this.mWorkspace.startDrag(session.cellInfo);
    }

    private boolean isWidgetDragSessionValid(WidgetResizeSession session) {
        if (session != this.mWidgetResizeSession || this.mWorkspace == null
                || isWorkspaceLocked()) {
            return false;
        }
        if (session.cellInfo == null || session.cellInfo.cell != session.widgetView) {
            return false;
        }
        if (session.widgetView.getTag() != session.widgetInfo
                || session.widgetView.getParent() != session.cellLayout) {
            return false;
        }
        return session.widgetView.getMeasuredWidth() > 0
                && session.widgetView.getMeasuredHeight() > 0;
    }

    private void dismissWidgetResize(WidgetResizeSession session) {
        if (session == this.mWidgetResizeSession) {
            dismissWidgetResize();
        }
    }

    private boolean dismissWidgetResize() {
        WidgetResizeSession session = this.mWidgetResizeSession;
        if (session == null) {
            return false;
        }
        this.mWidgetResizeSession = null;
        if (session.frame.getParent() == this.mDragLayer) {
            this.mDragLayer.removeView(session.frame);
        }
        return true;
    }

    boolean isWidgetResizeShowing() {
        return this.mWidgetResizeSession != null;
    }

    private static final class WidgetResizeSession implements WidgetResizeFrame.Callback {
        final CellLayout cellLayout;
        final CellLayout.CellInfo cellInfo;
        final WidgetResizeFrame frame;
        final LauncherAppWidgetInfo widgetInfo;
        final LauncherAppWidgetHostView widgetView;
        private final Launcher launcher;

        WidgetResizeSession(Launcher launcher, CellLayout cellLayout,
                LauncherAppWidgetHostView widgetView, CellLayout.CellInfo cellInfo,
                LauncherAppWidgetInfo widgetInfo, AppWidgetProviderInfo providerInfo) {
            this.launcher = launcher;
            this.cellLayout = cellLayout;
            this.cellInfo = cellInfo;
            this.widgetView = widgetView;
            this.widgetInfo = widgetInfo;
            this.frame = new WidgetResizeFrame(launcher, cellLayout, widgetView,
                    providerInfo, this);
        }

        /** Requests a workspace drag after a widget-body gesture. */
        public void onWidgetDragRequested() {
            this.launcher.startWidgetDrag(this);
        }

        public void onWidgetResizeCancelled() {
            this.launcher.dismissWidgetResize(this);
        }

        public void onWidgetResizeCommitted(CellLayout.ResizeCandidate candidate) {
            this.launcher.commitWidgetResize(this, candidate);
        }
    }

    static LauncherModel getModel() {
        return sLauncherModel;
    }

    private void closeApplicationsFromStateOverlay() {
        if (this.mApplicationsView.getMode() != ApplicationsView.MODE_DEFAULT) {
            this.mApplicationsView.setMode(ApplicationsView.MODE_DEFAULT);
        }
        closeAllApplications();
    }

    /* access modifiers changed from: package-private */
    public void closeAllApplications() {
        closeApplicationsGrid(LOGD);
    }

    /* access modifiers changed from: package-private */
    public Workspace getWorkspace() {
        return this.mWorkspace;
    }

    /* access modifiers changed from: package-private */
    public Dock getDock() {
        return this.mDock;
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

    private void startAddWidgets() {
        CellLayout targetLayout = requireWidgetPickerTarget();
        dismissWidgetPicker();
        this.mWaitingForResult = LOGD;
        this.mWidgetPickerDialog = createWidgetPickerDialog();
        this.mWidgetPickerDialog.show(this.mAppWidgetManager, targetLayout);
    }

    private CellLayout requireWidgetPickerTarget() {
        if (this.mAddItemCellInfo == null) {
            throw new IllegalStateException("Widget picker requires pending target cell");
        }
        CellLayout targetLayout = getCellLayoutForScreen(this.mAddItemCellInfo.screen);
        if (targetLayout == null) {
            throw new IllegalStateException("Widget picker target screen is unavailable");
        }
        return targetLayout;
    }

    private WidgetPickerDialog createWidgetPickerDialog() {
        return new WidgetPickerDialog(this, new WidgetPickerDialog.Callback() {
            public void onWidgetSelected(WidgetPickerDialog dialog,
                    WidgetPickerEntry entry) {
                Launcher.this.onWidgetPickerSelection(dialog, entry);
            }

            public void onWidgetPickerDismissed(WidgetPickerDialog dialog) {
                Launcher.this.onWidgetPickerDismissed(dialog);
            }
        });
    }

    private void onWidgetPickerSelection(WidgetPickerDialog dialog,
            WidgetPickerEntry entry) {
        if (!canSelectWidget(dialog) || entry == null) {
            return;
        }
        if (entry.isSearch()) {
            this.mWaitingForResult = false;
            addSearch();
            return;
        }
        addSelectedAppWidget(entry.getProvider());
    }

    private void onWidgetPickerDismissed(WidgetPickerDialog dialog) {
        if (!isActiveWidgetPicker(dialog)) {
            return;
        }
        this.mWidgetPickerDialog = null;
        if (this.mPendingAppWidgetId == -1 && !this.mPendingAppWidgetPlacement) {
            this.mWaitingForResult = false;
        }
    }

    private boolean canSelectWidget(WidgetPickerDialog dialog) {
        return isActiveWidgetPicker(dialog) && this.mAddItemCellInfo != null
                && !isWorkspaceLocked();
    }

    private boolean isActiveWidgetPicker(WidgetPickerDialog dialog) {
        return dialog != null && dialog == this.mWidgetPickerDialog
                && !this.mDestroyed;
    }

    private void dismissWidgetPicker() {
        WidgetPickerDialog dialog = this.mWidgetPickerDialog;
        this.mWidgetPickerDialog = null;
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void startAddShortcuts() {
        pickShortcut(REQUEST_PICK_SHORTCUT, R.string.title_select_shortcut);
    }

    private void startAddFolders() {
        Resources resources = getResources();
        Bundle bundle = new Bundle();
        ArrayList<String> shortcutNames = new ArrayList<>();
        shortcutNames.add(resources.getString(R.string.group_folder));
        bundle.putStringArrayList("android.intent.extra.shortcut.NAME", shortcutNames);
        ArrayList<Intent.ShortcutIconResource> shortcutIcons = new ArrayList<>();
        shortcutIcons.add(Intent.ShortcutIconResource.fromContext(this,
                R.drawable.ic_launcher_folder));
        bundle.putParcelableArrayList("android.intent.extra.shortcut.ICON_RESOURCE", shortcutIcons);
        Intent pickIntent = new Intent("android.intent.action.PICK_ACTIVITY");
        pickIntent.putExtra("android.intent.extra.INTENT",
                new Intent("android.intent.action.CREATE_LIVE_FOLDER"));
        pickIntent.putExtra("android.intent.extra.TITLE",
                getText(R.string.title_select_live_folder));
        pickIntent.putExtras(bundle);
        startActivityForResult(pickIntent, REQUEST_PICK_LIVE_FOLDER);
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
            layout.setBackgroundColor(withAlpha(
                    WallpaperColorExtractor.getSurface(Launcher.this), 217));
            this.mInput = (EditText) layout.findViewById(R.id.folder_name);
            this.mInput.setBackgroundColor(
                    WallpaperColorExtractor.getSurfaceVariant(Launcher.this));
            this.mInput.setTextColor(WallpaperColorExtractor.getOnSurface(Launcher.this));
            TextView label = (TextView) layout.findViewById(R.id.label);
            if (label != null) {
                label.setTextColor(WallpaperColorExtractor.getPrimary(Launcher.this));
            }
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

        /** Dispatches the selected direct launcher action. */
        public void onClick(DialogInterface dialog, int which) {
            if (which < 0 || which >= this.mHomeDialogAdapter.getCount()) {
                throw new IllegalArgumentException("Invalid launcher dialog selection: " + which);
            }
            LauncherDialogAdapter.ListItem selectedItem =
                    (LauncherDialogAdapter.ListItem) this.mHomeDialogAdapter.getItem(which);
            cleanup();
            switch (selectedItem.mActionTag) {
                case AddDialogAdapter.ITEM_WIDGETS:
                    Launcher.this.startAddWidgets();
                    return;
                case AddDialogAdapter.ITEM_SHORTCUTS:
                    Launcher.this.startAddShortcuts();
                    return;
                case AddDialogAdapter.ITEM_FOLDERS:
                    Launcher.this.startAddFolders();
                    return;
                case LauncherDialogAdapter.ITEM_WALLPAPER:
                    Launcher.this.startWallpaperChooser();
                    return;
                case LauncherDialogAdapter.ITEM_PREFERENCES:
                    Launcher.this.startPreferences();
                    return;
                default:
                    throw new IllegalArgumentException(
                            "Unknown launcher dialog action tag: " + selectedItem.mActionTag);
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

        /** Dispatches the selected add action. */
        public void onClick(DialogInterface dialog, int which) {
            if (which < 0 || which >= this.mAdapter.getCount()) {
                throw new IllegalArgumentException("Invalid add dialog selection: " + which);
            }
            AddDialogAdapter.ListItem selectedItem =
                    (AddDialogAdapter.ListItem) this.mAdapter.getItem(which);
            cleanup();
            switch (selectedItem.mActionTag) {
                case AddDialogAdapter.ITEM_WIDGETS:
                    Launcher.this.startAddWidgets();
                    return;
                case AddDialogAdapter.ITEM_SHORTCUTS:
                    Launcher.this.startAddShortcuts();
                    return;
                case AddDialogAdapter.ITEM_FOLDERS:
                    Launcher.this.startAddFolders();
                    return;
                default:
                    throw new IllegalArgumentException(
                            "Unknown add dialog action tag: " + selectedItem.mActionTag);
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
        imageView.setImageDrawable(Utilities.createDockIconThumbnail(applicationsGridItemInfo.resolveIcon(this), this));
        imageView.setContentDescription(applicationsGridItemInfo.title);
        imageView.setTag(applicationsGridItemInfo);
        imageView.setOnClickListener(this);
        return imageView;
    }

    /* access modifiers changed from: package-private */
    public View createSmallShortcut(int layoutResId, ViewGroup parent, ApplicationItemInfo info) {
        ImageView imageView = (ImageView) this.mInflater.inflate(layoutResId, parent, false);
        if (!info.filtered) {
            info.icon = Utilities.normalizeApplicationIcon(info.icon, this);
            info.filtered = LOGD;
        }
        imageView.setImageDrawable(Utilities.createDockIconThumbnail(info.icon, this));
        imageView.setContentDescription(info.title);
        imageView.setOnClickListener(this);
        imageView.setTag(info);
        return imageView;
    }

    /* access modifiers changed from: package-private */
    public View createSmallFolder(int layoutResId, ViewGroup parent, UserFolderInfo info) {
        ImageView imageView = (ImageView) this.mInflater.inflate(layoutResId, parent, false);
        imageView.setImageDrawable(Utilities.createDockIconThumbnail(getResources().getDrawable(R.drawable.ic_launcher_folder), this));
        imageView.setContentDescription(info.title);
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
        imageView.setContentDescription(info.title);
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
        dismissWidgetResize();
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
        dismissWidgetResize();
        if (!this.mApplicationsGridOpen) {
            this.mDock.hide(false);
            this.mWorkspace.enableChildrenCache();
            this.mWorkspace.lock();
            this.mDesktopLocked = LOGD;
            this.mWorkspace.invalidate();
            this.mApplicationsView.getImplementingView().bringToFront();
            this.mApplicationsView.open(animated && this.mAllowAppsGridAnimations);
            if (!(this.mApplicationsView instanceof ApplicationsPagingView)) {
                if (animated && this.mAllowAppsGridAnimations) {
                    this.mHomeButton.setAnimation(
                            AnimationUtils.loadAnimation(this, R.anim.home_button_fade_in));
                }
                this.mHomeButton.setContentDescription(
                        getString(R.string.accessibility_close_drawer));
                this.mHomeButton.setVisibility(View.VISIBLE);
                this.mHomeButton.bringToFront();
            }
            if (this.mApplicationsView instanceof ApplicationsPagingView) {
                if (this.mScreenIndicator != null) {
                    this.mScreenIndicator.bringToFront();
                }
            } else if (this.mScreenIndicator != null) {
                this.mScreenIndicator.hide();
            }
            this.mApplicationsGridOpen = LOGD;
            renderApplicationsState();
            updateWorkspaceEmptyTip();
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
                    this.mScreenIndicator.setAutoHide(true);
                    this.mScreenIndicator.setItems(this.mWorkspace.getChildCount());
                    this.mWorkspace.indicateCurrent();
                }
                if (!(this.mApplicationsView instanceof ApplicationsPagingView)) {
                    if (animated && this.mAllowAppsGridAnimations) {
                        this.mHomeButton.setAnimation(
                                AnimationUtils.loadAnimation(this, R.anim.home_button_fade_out));
                    }
                    this.mHomeButton.setContentDescription(
                            getString(R.string.accessibility_open_drawer));
                    this.mHomeButton.setVisibility(View.INVISIBLE);
                }
                this.mApplicationsGridOpen = false;
                if (this.mApplicationsStateOverlay != null) {
                    this.mApplicationsStateOverlay.setVisibility(View.GONE);
                }
                updateWorkspaceEmptyTip();
            }
        }
    }

    /* access modifiers changed from: package-private */
    boolean isApplicationsGridLogicallyOpen() {
        return this.mApplicationsGridOpen;
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
            PendingResult pendingResult = goAsync();
            dispatchWallpaperRefresh(context, pendingResult);
        }

        void dispatchWallpaperRefresh(final Context context,
                final PendingResult pendingResult) {
            if (Launcher.sSuppressWallpaperRefreshForTests) {
                finishPendingResult(pendingResult);
                return;
            }
            Launcher launcher = this.mLauncher == null ? null : this.mLauncher.get();
            if (launcher != null) {
                launcher.refreshWallpaperColorsAsync(pendingResult);
                Workspace workspace = launcher.getWorkspace();
                if (workspace != null) {
                    workspace.setWallpaper(Launcher.LOGD);
                }
                return;
            }
            final Context applicationContext = LocaleUtil.wrap(context.getApplicationContext());
            sWallpaperRefreshExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        WallpaperColorExtractor.refresh(applicationContext);
                    } finally {
                        finishPendingResult(pendingResult);
                    }
                }
            });
        }

        private void finishPendingResult(PendingResult pendingResult) {
            if (pendingResult != null) {
                pendingResult.finish();
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
    public void realAddWidget(AppWidgetProviderInfo appWidgetInfo,
            CellLayout.CellInfo cellInfo, int[] spans, int appWidgetId,
            boolean insertAtFirst) {
        int[] xy = this.mCellCoordinates;
        if (!findSlot(cellInfo, xy, spans[0], spans[1])) {
            releasePendingAppWidgetId(null);
            return;
        }
        LauncherAppWidgetInfo widgetInfo = createAppWidgetInfo(
                appWidgetId, cellInfo, spans, xy);
        if (!this.mRestoring) {
            bindNewAppWidget(widgetInfo, appWidgetInfo, xy, insertAtFirst);
        } else if (sLauncherModel.isDesktopLoaded()) {
            sLauncherModel.addDesktopAppWidget(widgetInfo);
        }
        clearPendingAppWidgetId();
    }

    private LauncherAppWidgetInfo createAppWidgetInfo(int appWidgetId,
            CellLayout.CellInfo cellInfo, int[] spans, int[] xy) {
        LauncherAppWidgetInfo widgetInfo = new LauncherAppWidgetInfo(appWidgetId);
        widgetInfo.spanX = spans[0];
        widgetInfo.spanY = spans[1];
        LauncherModel.addItemToDatabase(this, widgetInfo, -100,
                cellInfo.screen, xy[0], xy[1], false);
        return widgetInfo;
    }

    private void bindNewAppWidget(LauncherAppWidgetInfo widgetInfo,
            AppWidgetProviderInfo providerInfo, int[] xy, boolean insertAtFirst) {
        sLauncherModel.addDesktopAppWidget(widgetInfo);
        updateAppWidgetSizeOptions(widgetInfo);
        widgetInfo.hostView = this.mAppWidgetHost.createView(
                this, widgetInfo.appWidgetId, providerInfo);
        widgetInfo.hostView.setAppWidget(widgetInfo.appWidgetId, providerInfo);
        widgetInfo.hostView.setTag(widgetInfo);
        this.mWorkspace.addInScreen(widgetInfo.hostView, widgetInfo.screen, xy[0], xy[1],
                widgetInfo.spanX, widgetInfo.spanY, insertAtFirst);
    }

    private void updateAppWidgetSizeOptions(final LauncherAppWidgetInfo widgetInfo) {
        final CellLayout targetLayout = getCellLayoutForScreen(widgetInfo.screen);
        if (targetLayout == null) {
            return;
        }
        if (targetLayout.isWidgetSizingGeometryReady()) {
            updateAppWidgetSizeOptionsWhenReady(widgetInfo, targetLayout);
            return;
        }
        targetLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (Launcher.this.mDestroyed
                        || Launcher.this.getCellLayoutForScreen(widgetInfo.screen)
                        != targetLayout) {
                    targetLayout.removeOnLayoutChangeListener(this);
                    return;
                }
                if (!targetLayout.isWidgetSizingGeometryReady()) {
                    return;
                }
                targetLayout.removeOnLayoutChangeListener(this);
                Launcher.this.updateAppWidgetSizeOptionsWhenReady(widgetInfo, targetLayout);
            }
        });
        targetLayout.requestLayout();
    }

    private void updateAppWidgetSizeOptionsWhenReady(LauncherAppWidgetInfo widgetInfo,
            CellLayout targetLayout) {
        int[] size = targetLayout.spanToPixels(widgetInfo.spanX, widgetInfo.spanY);
        float density = getResources().getDisplayMetrics().density;
        int width = Math.max(1, Math.round(size[0] / density));
        int height = Math.max(1, Math.round(size[1] / density));
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height);
        this.mAppWidgetManager.updateAppWidgetOptions(
                widgetInfo.appWidgetId, options);
    }

    public static int getScreenCount(Context context) {
        return PreferencesUtil.getNumberOfScreens(context);
    }

    public ScreenIndicator getScreenIndicator() {
        return this.mScreenIndicator;
    }

    public void updateWorkspaceEmptyTip() {
        View emptyTip = findViewById(R.id.workspace_empty_tip);
        if (emptyTip != null) {
            emptyTip.setVisibility(View.GONE);
        }
    }

    public void onDragStart(View view, DragSource source, Object info, int dragAction) {
        dismissWidgetResize();
        TextView emptyTip = (TextView) findViewById(R.id.workspace_empty_tip);
        if (emptyTip != null) {
            emptyTip.setVisibility(View.GONE);
        }
    }

    public void onDragEnd() {
        updateWorkspaceEmptyTip();
    }
}
