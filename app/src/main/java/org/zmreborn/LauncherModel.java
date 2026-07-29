package org.zmreborn;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.zmreborn.ApplicationItemInfo;
import org.zmreborn.LauncherSettings;

public class LauncherModel {
    static final boolean DEBUG_LOADERS = false;
    static final String LOG_TAG = LauncherModel.class.getSimpleName();
    private static AsyncTask<Object, ArrayList<ApplicationItemInfo>, ArrayList<ApplicationItemInfo>> mLoadApplicationsAsyncTask;
    private final ApplicationsLoadGeneration mApplicationsLoadGeneration = new ApplicationsLoadGeneration();
    /* access modifiers changed from: private */
    public static ArrayList<ApplicationItemInfo> sCachedApplicationItemInfos;
    private static ArrayList<ApplicationItemInfo> sAllApplicationItemInfos;
    /* access modifiers changed from: private */
    public static final AtomicInteger sWorkspaceLoaderCount = new AtomicInteger(1);
    /* access modifiers changed from: private */
    public ArrayList<LauncherAppWidgetInfo> mDesktopAppWidgets;
    private int mDesktopColumns;
    /* access modifiers changed from: private */
    public ArrayList<ItemInfo> mDesktopItems;
    /* access modifiers changed from: private */
    public boolean mDesktopItemsLoaded;
    private DesktopItemsLoader mDesktopItemsLoader;
    private Thread mDesktopLoaderThread;
    private int mDesktopRows;
    /* access modifiers changed from: private */
    public HashMap<Long, FolderInfo> mFolders;

    /* access modifiers changed from: package-private */
    public synchronized void abortLoaders() {
        if (this.mDesktopItemsLoader != null && this.mDesktopItemsLoader.isRunning()) {
            this.mDesktopItemsLoader.stop();
            this.mDesktopItemsLoaded = false;
        }
    }

    static ArrayList<ApplicationItemInfo> getAllApplications() {
        if (sAllApplicationItemInfos == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(sAllApplicationItemInfos);
    }

    /* access modifiers changed from: package-private */
    public void loadApplications(final boolean isLaunching, final ApplicationsView applicationsView) {
        if (applicationsView == null) {
            return;
        }
        final int generation = this.mApplicationsLoadGeneration.start();
        applicationsView.setLoading();
        if (mLoadApplicationsAsyncTask != null) {
            mLoadApplicationsAsyncTask.cancel(true);
            mLoadApplicationsAsyncTask = null;
        }
        boolean load;
        if (!isLaunching) {
            load = true;
        } else {
            load = sCachedApplicationItemInfos == null;
        }
        if (!load) {
            ArrayList<ApplicationItemInfo> cachedApplications;
            synchronized (this) {
                cachedApplications = sCachedApplicationItemInfos;
            }
            if (this.mApplicationsLoadGeneration.isCurrent(generation)) {
                deliverApplications(applicationsView, cachedApplications);
            }
            return;
        }
        mLoadApplicationsAsyncTask = new AsyncTask<Object, ArrayList<ApplicationItemInfo>, ArrayList<ApplicationItemInfo>>() {
            private ArrayList<ApplicationItemInfo> mAllApplicationItemInfos;

            /* access modifiers changed from: protected */
            public ArrayList<ApplicationItemInfo> doInBackground(Object... arg0) {
                int priority = isLaunching ? 0 : 10;
                Process.setThreadPriority(priority);
                try {
                    if (isCancelled()) {
                        return null;
                    }
                    Launcher launcher = applicationsView.getLauncher();
                    Intent mainIntent = new Intent("android.intent.action.MAIN", (Uri) null);
                    mainIntent.addCategory("android.intent.category.LAUNCHER");
                    PackageManager packageManager = launcher.getPackageManager();
                    List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
                    for (int i = 0; i < apps.size(); i++) {
                        if (isCancelled()) {
                            return null;
                        }
                        if (apps.get(i).activityInfo.packageName.equals(launcher.getApplicationContext().getPackageName())) {
                            apps.remove(i);
                        }
                    }
                    ArrayList<ApplicationItemInfo> applicationItemInfos = new ArrayList<>();
                    if (apps != null) {
                        for (int i = 0; i < apps.size(); i++) {
                            if (isCancelled()) {
                                return null;
                            }
                            ResolveInfo resolveInfo = apps.get(i);
                            ComponentName componentName = new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name);
                            ApplicationItemInfo applicationItemInfo = new ApplicationItemInfo();
                            applicationItemInfo.container = -1;
                            applicationItemInfo.title = resolveInfo.loadLabel(packageManager);
                            if (applicationItemInfo.title == null) {
                                applicationItemInfo.title = resolveInfo.activityInfo.name;
                            }
                            applicationItemInfo.icon = Utilities.normalizeApplicationIcon(resolveInfo.activityInfo.loadIcon(packageManager), launcher);
                            applicationItemInfo.filtered = true;
                            applicationItemInfo.setActivity(componentName, 270532608);
                            applicationItemInfos.add(applicationItemInfo);
                        }
                    }
                    Collections.sort(applicationItemInfos, new ApplicationItemInfo.TitleComparator());
                    if (isCancelled()) {
                        return null;
                    }
                    ArrayList<AppListFolderRecord> folders = new AppListFolderStore(
                            launcher.getContentResolver()).loadFolders();
                    ArrayList<ApplicationItemInfo> projectedItems = AppListFolderProjection.project(
                            folders, applicationItemInfos);
                    if (isCancelled()) {
                        return null;
                    }
                    this.mAllApplicationItemInfos = new ArrayList<>(applicationItemInfos);
                    return projectedItems;
                } catch (RuntimeException exception) {
                    Log.e(LOG_TAG, "Applications loading failed", exception);
                    return null;
                } finally {
                    Process.setThreadPriority(0);
                }
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(ArrayList<ApplicationItemInfo> result) {
                super.onPostExecute(result);
                if (!LauncherModel.this.mApplicationsLoadGeneration.isCurrent(generation)) {
                    return;
                }
                synchronized (LauncherModel.this) {
                    if (!LauncherModel.this.mApplicationsLoadGeneration.isCurrent(generation)) {
                        return;
                    }
                    if (result == null || this.mAllApplicationItemInfos == null) {
                        applicationsView.setError();
                        return;
                    }
                    LauncherModel.sAllApplicationItemInfos = new ArrayList<>(this.mAllApplicationItemInfos);
                    applicationsView.setApplications(result);
                    LauncherModel.sCachedApplicationItemInfos = result;
                    if (result.isEmpty()) {
                        applicationsView.setEmpty();
                    } else {
                        applicationsView.clearState();
                    }
                }
            }

            /* access modifiers changed from: protected */
            public void onCancelled() {
                super.onCancelled();
                if (LauncherModel.this.mApplicationsLoadGeneration.isCurrent(generation)) {
                    applicationsView.setError();
                }
            }
        };
        mLoadApplicationsAsyncTask.execute(new Object[0]);
    }

    private void deliverApplications(ApplicationsView applicationsView,
            ArrayList<ApplicationItemInfo> applications) {
        if (applications == null) {
            applicationsView.setError();
            return;
        }
        applicationsView.setApplications(applications);
        if (applications.isEmpty()) {
            applicationsView.setEmpty();
            return;
        }
        applicationsView.clearState();
    }

    /* access modifiers changed from: package-private */
    public synchronized void addPackage(ApplicationsView applicationsView, String packageName) {
        loadApplications(false, applicationsView);
    }

    /* access modifiers changed from: package-private */
    public synchronized void removePackage(ApplicationsView applicationsView, String packageName) {
        if (applicationsView != null && applicationsView.getLauncher() != null) {
            new AppListFolderStore(applicationsView.getLauncher().getContentResolver()).removePackage(packageName);
        }
        loadApplications(false, applicationsView);
    }

    /* access modifiers changed from: package-private */
    public synchronized void updatePackage(ApplicationsView applicationsView, String packageName) {
        loadApplications(false, applicationsView);
    }

    /* access modifiers changed from: package-private */
    public Drawable getApplicationItemInfoIconOrNull(PackageManager packageManager, ApplicationItemInfo applicationItemInfo) {
        ResolveInfo resolveInfo = packageManager.resolveActivity(applicationItemInfo.intent, 0);
        if (resolveInfo == null) {
            return null;
        }
        return resolveInfo.activityInfo.loadIcon(packageManager);
    }

    /* access modifiers changed from: package-private */
    public boolean isDesktopLoaded() {
        return (this.mDesktopItems == null || this.mDesktopAppWidgets == null || !this.mDesktopItemsLoaded) ? false : true;
    }

    /* access modifiers changed from: package-private */
    public void loadUserItems(boolean isLaunching, Launcher launcher, boolean localeChanged) {
        this.mDesktopRows = PreferencesUtil.getContentGridRows(launcher);
        this.mDesktopColumns = PreferencesUtil.getContentGridColumns(launcher);
        if (!isLaunching || !isDesktopLoaded()) {
            if (this.mDesktopItemsLoader != null && this.mDesktopItemsLoader.isRunning()) {
                this.mDesktopItemsLoader.stop();
                try {
                    this.mDesktopLoaderThread.join();
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "mDesktopLoaderThread didn't exit in time");
                }
            }
            this.mDesktopItemsLoaded = false;
            this.mDesktopItemsLoader = new DesktopItemsLoader(launcher, localeChanged);
            this.mDesktopLoaderThread = new Thread(this.mDesktopItemsLoader, "Desktop Items Loader");
            this.mDesktopLoaderThread.start();
            return;
        }
        launcher.onDesktopItemsLoaded(this.mDesktopItems, this.mDesktopAppWidgets);
    }

    /* access modifiers changed from: private */
    public static String getLabel(PackageManager manager, ActivityInfo activityInfo) {
        String label = activityInfo.loadLabel(manager).toString();
        if (label != null) {
            return label;
        }
        String label2 = manager.getApplicationLabel(activityInfo.applicationInfo).toString();
        if (label2 == null) {
            return activityInfo.name;
        }
        return label2;
    }

    private class DesktopItemsLoader implements Runnable {
        final /* synthetic */ boolean $assertionsDisabled = (!LauncherModel.class.desiredAssertionStatus());
        private volatile boolean mFinished;
        private final int mId = LauncherModel.sWorkspaceLoaderCount.getAndIncrement();
        private final WeakReference<Launcher> mLauncher;
        private final boolean mLocaleChanged;
        private volatile boolean mStopped;

        DesktopItemsLoader(Launcher launcher, boolean localeChanged) {
            this.mLauncher = new WeakReference<>(launcher);
            this.mLocaleChanged = localeChanged;
            this.mFinished = false;
        }

        /* access modifiers changed from: package-private */
        public void stop() {
            Log.d(LauncherModel.LOG_TAG, "  ----> workspace loader " + this.mId + " stopped from " + Thread.currentThread().toString());
            this.mStopped = true;
        }

        /* access modifiers changed from: package-private */
        public boolean isRunning() {
            return !this.mFinished;
        }

        public void run() {
            if ($assertionsDisabled || !this.mFinished) {
                loadWorkspace();
                this.mFinished = true;
                return;
            }
            throw new AssertionError();
        }

        private void loadWorkspace() {
            Log.d(LauncherModel.LOG_TAG, "  ----> running workspace loader (" + this.mId + ")");
            Process.setThreadPriority(0);

            Launcher launcher = this.mLauncher.get();
            ContentResolver resolver = launcher.getContentResolver();
            PackageManager packageManager = launcher.getPackageManager();

            if (this.mLocaleChanged) {
                updateShortcutLabels(resolver, packageManager);
            }

            ArrayList<ItemInfo> items = new ArrayList<>();
            ArrayList<LauncherAppWidgetInfo> appWidgets = new ArrayList<>();
            HashMap<Long, FolderInfo> folders = new HashMap<>();
            Cursor cursor = resolver.query(
                    LauncherSettings.Favorites.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );

            try {
                int idIndex = cursor.getColumnIndexOrThrow("_id");
                int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
                int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.TITLE);
                int iconTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_TYPE);
                int iconIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON);
                int iconPackageIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE);
                int iconResourceIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE);
                int containerIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
                int appWidgetIdIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_ID);
                int screenIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                int cellXIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                int cellYIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                int spanXIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
                int spanYIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
                int uriIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.URI);
                int displayModeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.DISPLAY_MODE);

                while (!this.mStopped && cursor.moveToNext()) {
                    try {
                        int itemType = cursor.getInt(itemTypeIndex);

                        switch (itemType) {
                            case LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION:
                            case LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT:
                                String intentUri = cursor.getString(intentIndex);
                                Intent intent;

                                try {
                                    intent = Intent.parseUri(intentUri, 0);
                                } catch (URISyntaxException e) {
                                    continue;
                                }

                                ApplicationItemInfo applicationItemInfo;
                                if (itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION) {
                                    applicationItemInfo = LauncherModel.getApplicationInfo(packageManager, intent, launcher);
                                } else {
                                    applicationItemInfo = LauncherModel.this.getApplicationInfoShortcut(
                                            cursor,
                                            launcher,
                                            iconTypeIndex,
                                            iconPackageIndex,
                                            iconResourceIndex,
                                            iconIndex
                                    );
                                }

                                if (applicationItemInfo == null) {
                                    applicationItemInfo = new ApplicationItemInfo();
                                    applicationItemInfo.icon = packageManager.getDefaultActivityIcon();
                                }

                                applicationItemInfo.title = cursor.getString(titleIndex);
                                applicationItemInfo.intent = intent;
                                applicationItemInfo.f3id = cursor.getLong(idIndex);
                                int applicationContainer = cursor.getInt(containerIndex);
                                applicationItemInfo.container = applicationContainer;
                                applicationItemInfo.screen = cursor.getInt(screenIndex);
                                applicationItemInfo.cellX = cursor.getInt(cellXIndex);
                                applicationItemInfo.cellY = cursor.getInt(cellYIndex);

                                if (applicationContainer == LauncherSettings.Favorites.CONTAINER_DESKTOP
                                        || applicationContainer == LauncherSettings.Favorites.CONTAINER_DOCKBAR) {
                                    items.add(applicationItemInfo);
                                    continue;
                                }

                                LauncherModel.this.findOrMakeUserFolder(folders, applicationContainer)
                                        .add(applicationItemInfo);
                                continue;

                            case LauncherSettings.Favorites.ITEM_TYPE_APPS_GRID:
                                ApplicationsGridItemInfo applicationsGridItemInfo = new ApplicationsGridItemInfo(launcher);
                                applicationsGridItemInfo.f3id = cursor.getLong(idIndex);
                                applicationsGridItemInfo.container = cursor.getInt(containerIndex);
                                applicationsGridItemInfo.screen = cursor.getInt(screenIndex);
                                applicationsGridItemInfo.cellX = cursor.getInt(cellXIndex);
                                applicationsGridItemInfo.cellY = cursor.getInt(cellYIndex);
                                applicationsGridItemInfo.spanX = cursor.getInt(spanXIndex);
                                applicationsGridItemInfo.spanY = cursor.getInt(spanYIndex);
                                items.add(applicationsGridItemInfo);
                                continue;

                            case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
                                long userFolderId = cursor.getLong(idIndex);
                                UserFolderInfo userFolderInfo = LauncherModel.this.findOrMakeUserFolder(folders, userFolderId);
                                userFolderInfo.title = cursor.getString(titleIndex);
                                userFolderInfo.f3id = userFolderId;
                                int userFolderContainer = cursor.getInt(containerIndex);
                                userFolderInfo.container = userFolderContainer;
                                userFolderInfo.screen = cursor.getInt(screenIndex);
                                userFolderInfo.cellX = cursor.getInt(cellXIndex);
                                userFolderInfo.cellY = cursor.getInt(cellYIndex);

                                if (userFolderContainer == LauncherSettings.Favorites.CONTAINER_DESKTOP
                                        || userFolderContainer == LauncherSettings.Favorites.CONTAINER_DOCKBAR) {
                                    items.add(userFolderInfo);
                                }
                                continue;

                            case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
                                long liveFolderId = cursor.getLong(idIndex);
                                LiveFolderInfo liveFolderInfo = LauncherModel.this.findOrMakeLiveFolder(folders, liveFolderId);
                                String liveFolderIntentUri = cursor.getString(intentIndex);
                                Intent baseIntent = null;

                                if (liveFolderIntentUri != null) {
                                    try {
                                        baseIntent = Intent.parseUri(liveFolderIntentUri, 0);
                                    } catch (URISyntaxException e) {
                                    }
                                }

                                liveFolderInfo.title = cursor.getString(titleIndex);
                                liveFolderInfo.f3id = liveFolderId;
                                int liveFolderContainer = cursor.getInt(containerIndex);
                                liveFolderInfo.container = liveFolderContainer;
                                liveFolderInfo.screen = cursor.getInt(screenIndex);
                                liveFolderInfo.cellX = cursor.getInt(cellXIndex);
                                liveFolderInfo.cellY = cursor.getInt(cellYIndex);
                                liveFolderInfo.uri = Uri.parse(cursor.getString(uriIndex));
                                liveFolderInfo.baseIntent = baseIntent;
                                liveFolderInfo.displayMode = cursor.getInt(displayModeIndex);
                                LauncherModel.loadLiveFolderIcon(
                                        launcher,
                                        cursor,
                                        iconTypeIndex,
                                        iconPackageIndex,
                                        iconResourceIndex,
                                        liveFolderInfo
                                );

                                if (liveFolderContainer == LauncherSettings.Favorites.CONTAINER_DESKTOP
                                        || liveFolderContainer == LauncherSettings.Favorites.CONTAINER_DOCKBAR) {
                                    items.add(liveFolderInfo);
                                }
                                continue;

                            case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_SEARCH:
                                Widget searchWidget = Widget.makeSearch();
                                int searchWidgetContainer = cursor.getInt(containerIndex);

                                if (searchWidgetContainer != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                                    Log.e(Launcher.LOG_TAG, "Widget found where container != CONTAINER_DESKTOP  ignoring!");
                                    continue;
                                }

                                searchWidget.f3id = cursor.getLong(idIndex);
                                searchWidget.screen = cursor.getInt(screenIndex);
                                searchWidget.container = searchWidgetContainer;
                                searchWidget.cellX = cursor.getInt(cellXIndex);
                                searchWidget.cellY = cursor.getInt(cellYIndex);
                                searchWidget.spanX = cursor.getInt(spanXIndex);
                                searchWidget.spanY = cursor.getInt(spanYIndex);
                                items.add(searchWidget);
                                continue;

                            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                                LauncherAppWidgetInfo appWidgetInfo = new LauncherAppWidgetInfo(cursor.getInt(appWidgetIdIndex));
                                appWidgetInfo.f3id = cursor.getLong(idIndex);
                                appWidgetInfo.screen = cursor.getInt(screenIndex);
                                appWidgetInfo.cellX = cursor.getInt(cellXIndex);
                                appWidgetInfo.cellY = cursor.getInt(cellYIndex);
                                appWidgetInfo.spanX = cursor.getInt(spanXIndex);
                                appWidgetInfo.spanY = cursor.getInt(spanYIndex);
                                int appWidgetContainer = cursor.getInt(containerIndex);

                                if (appWidgetContainer != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                                    Log.e(Launcher.LOG_TAG, "Widget found where container != CONTAINER_DESKTOP -- ignoring!");
                                    continue;
                                }

                                appWidgetInfo.container = appWidgetContainer;
                                appWidgets.add(appWidgetInfo);
                                continue;

                            default:
                                continue;
                        }
                    } catch (Exception e) {
                        Log.w(Launcher.LOG_TAG, "Desktop items loading interrupted:", e);
                    }
                }
            } finally {
                cursor.close();
            }

            synchronized (LauncherModel.this) {
                if (this.mStopped) {
                    return;
                }

                final ArrayList<ItemInfo> itemsToBind = new ArrayList<>(items);
                final ArrayList<LauncherAppWidgetInfo> appWidgetsToBind = new ArrayList<>(appWidgets);

                if (!this.mStopped) {
                    Log.d(LauncherModel.LOG_TAG, "  ----> items cloned, ready to refresh UI");
                    final Launcher launcherToBind = launcher;
                    launcherToBind.runOnUiThread(new Runnable() {
                        public void run() {
                            launcherToBind.onDesktopItemsLoaded(itemsToBind, appWidgetsToBind);
                        }
                    });
                }

                LauncherModel.this.mDesktopItems = items;
                LauncherModel.this.mDesktopAppWidgets = appWidgets;
                LauncherModel.this.mFolders = folders;
                LauncherModel.this.mDesktopItemsLoaded = true;
            }
        }

        private void updateShortcutLabels(ContentResolver resolver, PackageManager manager) {
            String intentUri;
            ComponentName name;
            Cursor cursor = resolver.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"_id", LauncherSettings.BaseLauncherColumns.TITLE, LauncherSettings.BaseLauncherColumns.INTENT, LauncherSettings.BaseLauncherColumns.ITEM_TYPE}, (String) null, (String[]) null, (String) null);
            try {
                int idIndex = cursor.getColumnIndexOrThrow("_id");
                int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
                int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
                int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.TITLE);
                while (!this.mStopped && cursor.moveToNext()) {
                    try {
                        if (cursor.getInt(itemTypeIndex) == 0 && (intentUri = cursor.getString(intentIndex)) != null) {
                            Intent shortcut = Intent.parseUri(intentUri, 0);
                            if ("android.intent.action.MAIN".equals(shortcut.getAction()) && (name = shortcut.getComponent()) != null) {
                                ActivityInfo activityInfo = manager.getActivityInfo(name, 0);
                                String title = cursor.getString(titleIndex);
                                String label = LauncherModel.getLabel(manager, activityInfo);
                                if (title == null || !title.equals(label)) {
                                    ContentValues values = new ContentValues();
                                    values.put(LauncherSettings.BaseLauncherColumns.TITLE, label);
                                    resolver.update(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values, "_id=?", new String[]{String.valueOf(cursor.getLong(idIndex))});
                                }
                            }
                        }
                    } catch (PackageManager.NameNotFoundException | URISyntaxException e) {
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    /* access modifiers changed from: private */
    public static void loadLiveFolderIcon(Launcher launcher, Cursor cursor, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, LiveFolderInfo liveFolderInfo) {
        switch (cursor.getInt(iconTypeIndex)) {
            case 0:
                String packageName = cursor.getString(iconPackageIndex);
                String resourceName = cursor.getString(iconResourceIndex);
                try {
                    Resources resources = launcher.getPackageManager().getResourcesForApplication(packageName);
                    liveFolderInfo.icon = resources.getDrawable(resources.getIdentifier(resourceName, (String) null, (String) null));
                } catch (Exception e) {
                    liveFolderInfo.icon = launcher.getResources().getDrawable(R.drawable.ic_launcher_folder);
                }
                liveFolderInfo.iconResource = new Intent.ShortcutIconResource();
                liveFolderInfo.iconResource.packageName = packageName;
                liveFolderInfo.iconResource.resourceName = resourceName;
                return;
            default:
                liveFolderInfo.icon = launcher.getResources().getDrawable(R.drawable.ic_launcher_folder);
                return;
        }
    }

    /* access modifiers changed from: package-private */
    public FolderInfo findFolderById(long id) {
        if (this.mFolders != null) {
            return this.mFolders.get(Long.valueOf(id));
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public void addFolder(FolderInfo info) {
        this.mFolders.put(Long.valueOf(info.f3id), info);
    }

    /* access modifiers changed from: private */
    public UserFolderInfo findOrMakeUserFolder(HashMap<Long, FolderInfo> folders, long id) {
        FolderInfo folderInfo = folders.get(Long.valueOf(id));
        if (folderInfo == null || !(folderInfo instanceof UserFolderInfo)) {
            folderInfo = new UserFolderInfo();
            folders.put(Long.valueOf(id), folderInfo);
        }
        return (UserFolderInfo) folderInfo;
    }

    /* access modifiers changed from: private */
    public LiveFolderInfo findOrMakeLiveFolder(HashMap<Long, FolderInfo> folders, long id) {
        FolderInfo folderInfo = folders.get(Long.valueOf(id));
        if (folderInfo == null || !(folderInfo instanceof LiveFolderInfo)) {
            folderInfo = new LiveFolderInfo();
            folders.put(Long.valueOf(id), folderInfo);
        }
        return (LiveFolderInfo) folderInfo;
    }

    /* access modifiers changed from: package-private */
    public void unbind() {
        unbindDrawables(this.mDesktopItems);
        unbindAppWidgetHostViews(this.mDesktopAppWidgets);
    }

    private void unbindDrawables(ArrayList<ItemInfo> desktopItems) {
        if (desktopItems != null) {
            int count = desktopItems.size();
            for (int i = 0; i < count; i++) {
                ItemInfo item = desktopItems.get(i);
                switch (item.itemType) {
                    case 0:
                    case 1:
                        ((ApplicationItemInfo) item).icon.setCallback((Drawable.Callback) null);
                        break;
                }
            }
        }
    }

    private void unbindAppWidgetHostViews(ArrayList<LauncherAppWidgetInfo> appWidgets) {
        if (appWidgets != null) {
            int count = appWidgets.size();
            for (int i = 0; i < count; i++) {
                appWidgets.get(i).hostView = null;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void findAllOccupiedCells(boolean[][] occupied, int countX, int countY, int screen) {
        ArrayList<ItemInfo> desktopItems = this.mDesktopItems;
        if (desktopItems != null) {
            int count = desktopItems.size();
            for (int i = 0; i < count; i++) {
                if ((desktopItems.get(i).spanX - 1) + desktopItems.get(i).cellX < this.mDesktopColumns) {
                    if ((desktopItems.get(i).spanY - 1) + desktopItems.get(i).cellY < this.mDesktopRows) {
                        addOccupiedCells(occupied, screen, desktopItems.get(i));
                    }
                }
            }
        }
        ArrayList<LauncherAppWidgetInfo> desktopAppWidgets = this.mDesktopAppWidgets;
        if (desktopAppWidgets != null) {
            int count2 = desktopAppWidgets.size();
            for (int i2 = 0; i2 < count2; i2++) {
                addOccupiedCells(occupied, screen, desktopAppWidgets.get(i2));
            }
        }
    }

    private void addOccupiedCells(boolean[][] occupied, int screen, ItemInfo item) {
        if (item.screen == screen) {
            for (int xx = item.cellX; xx < item.cellX + item.spanX; xx++) {
                for (int yy = item.cellY; yy < item.cellY + item.spanY; yy++) {
                    if (xx < this.mDesktopColumns && yy < this.mDesktopRows) {
                        occupied[xx][yy] = true;
                    }
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void addDesktopItem(ItemInfo info) {
        if (isDesktopLoaded()) {
            this.mDesktopItems.add(info);
        }
    }

    /* access modifiers changed from: package-private */
    public void removeDesktopItem(ItemInfo info) {
        this.mDesktopItems.remove(info);
    }

    /* access modifiers changed from: package-private */
    public void addDesktopAppWidget(LauncherAppWidgetInfo info) {
        if (isDesktopLoaded()) {
            this.mDesktopAppWidgets.add(info);
        }
    }

    /* access modifiers changed from: package-private */
    public void removeDesktopAppWidget(LauncherAppWidgetInfo info) {
        this.mDesktopAppWidgets.remove(info);
    }

    /* access modifiers changed from: private */
    public static ApplicationItemInfo getApplicationInfo(PackageManager packageManager, Intent intent, Context context) {
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return null;
        }
        ApplicationItemInfo applicationItemInfo = new ApplicationItemInfo();
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        applicationItemInfo.icon = Utilities.normalizeApplicationIcon(activityInfo.loadIcon(packageManager), context);
        if (applicationItemInfo.title == null || applicationItemInfo.title.length() == 0) {
            applicationItemInfo.title = activityInfo.loadLabel(packageManager);
        }
        if (applicationItemInfo.title == null) {
            applicationItemInfo.title = "";
        }
        applicationItemInfo.itemType = 0;
        return applicationItemInfo;
    }

    /* access modifiers changed from: private */
    public ApplicationItemInfo getApplicationInfoShortcut(Cursor cursor, Context context, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex) {
        ApplicationItemInfo applicationItemInfo = new ApplicationItemInfo();
        applicationItemInfo.itemType = 1;
        switch (cursor.getInt(iconTypeIndex)) {
            case 0:
                String packageName = cursor.getString(iconPackageIndex);
                String resourceName = cursor.getString(iconResourceIndex);
                PackageManager packageManager = context.getPackageManager();
                try {
                    Resources resources = packageManager.getResourcesForApplication(packageName);
                    applicationItemInfo.icon = Utilities.normalizeApplicationIcon(resources.getDrawable(resources.getIdentifier(resourceName, (String) null, (String) null)), context);
                } catch (Exception e) {
                    applicationItemInfo.icon = packageManager.getDefaultActivityIcon();
                }
                applicationItemInfo.iconResource = new Intent.ShortcutIconResource();
                applicationItemInfo.iconResource.packageName = packageName;
                applicationItemInfo.iconResource.resourceName = resourceName;
                applicationItemInfo.customIcon = false;
                break;
            case 1:
                byte[] data = cursor.getBlob(iconIndex);
                try {
                    applicationItemInfo.icon = new FastBitmapDrawable(Utilities.createBitmapThumbnail(BitmapFactory.decodeByteArray(data, 0, data.length), context));
                } catch (Exception e2) {
                    applicationItemInfo.icon = context.getPackageManager().getDefaultActivityIcon();
                }
                applicationItemInfo.filtered = true;
                applicationItemInfo.customIcon = true;
                break;
            default:
                applicationItemInfo.icon = context.getPackageManager().getDefaultActivityIcon();
                applicationItemInfo.customIcon = false;
                break;
        }
        return applicationItemInfo;
    }

    /* access modifiers changed from: package-private */
    public void removeUserFolderItem(UserFolderInfo folder, ItemInfo info) {
        folder.contents.remove(info);
    }

    /* access modifiers changed from: package-private */
    public void removeUserFolder(UserFolderInfo userFolderInfo) {
        this.mFolders.remove(Long.valueOf(userFolderInfo.f3id));
    }

    static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY) {
        if (item.container == -1) {
            addItemToDatabase(context, item, container, screen, cellX, cellY, false);
        } else {
            moveItemInDatabase(context, item, container, screen, cellX, cellY);
        }
    }

    static void moveItemInDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = context.getContentResolver();
        values.put("container", Long.valueOf(item.container));
        values.put("cellX", Integer.valueOf(item.cellX));
        values.put("cellY", Integer.valueOf(item.cellY));
        values.put("screen", Integer.valueOf(item.screen));
        contentResolver.update(LauncherSettings.Favorites.getContentUri(item.f3id, false), values, (String) null, (String[]) null);
    }

    static boolean shortcutExists(Context context, String title, Intent intent) {
        Cursor cursor = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, new String[]{LauncherSettings.BaseLauncherColumns.TITLE, LauncherSettings.BaseLauncherColumns.INTENT}, "title=? and intent=?", new String[]{title, intent.toUri(0)}, (String) null);
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    /* access modifiers changed from: package-private */
    public FolderInfo getFolderById(Context context, long id) {
        Cursor cursor = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, (String[]) null, "_id=? and (itemType=? or itemType=?)", new String[]{String.valueOf(id), String.valueOf(2), String.valueOf(3)}, (String) null);
        try {
            if (cursor.moveToFirst()) {
                int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
                int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.TITLE);
                int containerIndex = cursor.getColumnIndexOrThrow("container");
                int screenIndex = cursor.getColumnIndexOrThrow("screen");
                int cellXIndex = cursor.getColumnIndexOrThrow("cellX");
                int cellYIndex = cursor.getColumnIndexOrThrow("cellY");
                FolderInfo folderInfo = null;
                switch (cursor.getInt(itemTypeIndex)) {
                    case 2:
                        folderInfo = findOrMakeUserFolder(this.mFolders, id);
                        break;
                    case 3:
                        folderInfo = findOrMakeLiveFolder(this.mFolders, id);
                        break;
                }
                folderInfo.title = cursor.getString(titleIndex);
                folderInfo.f3id = id;
                folderInfo.container = (long) cursor.getInt(containerIndex);
                folderInfo.screen = cursor.getInt(screenIndex);
                folderInfo.cellX = cursor.getInt(cellXIndex);
                folderInfo.cellY = cursor.getInt(cellYIndex);
                return folderInfo;
            }
            cursor.close();
            return null;
        } finally {
            cursor.close();
        }
    }

    static void addItemToDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY, boolean notify) {
        Uri uri;
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = context.getContentResolver();
        item.onAddToDatabase(values);
        if (notify) {
            uri = LauncherSettings.Favorites.CONTENT_URI;
        } else {
            uri = LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION;
        }
        Uri result = contentResolver.insert(uri, values);
        if (result != null) {
            item.f3id = (long) Integer.parseInt(result.getPathSegments().get(1));
        }
    }

    static synchronized void updateItemInDatabase(Context context, ItemInfo item) {
        synchronized (LauncherModel.class) {
            ContentValues values = new ContentValues();
            ContentResolver contentResolver = context.getContentResolver();
            item.onAddToDatabase(values);
            contentResolver.update(LauncherSettings.Favorites.getContentUri(item.f3id, false), values, (String) null, (String[]) null);
        }
    }

    static void deleteItemFromDatabase(Context context, ItemInfo item) {
        context.getContentResolver().delete(LauncherSettings.Favorites.getContentUri(item.f3id, false), (String) null, (String[]) null);
    }

    static void deleteUserFolderContentsFromDatabase(Context context, UserFolderInfo info) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(LauncherSettings.Favorites.getContentUri(info.f3id, false), (String) null, (String[]) null);
        contentResolver.delete(LauncherSettings.Favorites.CONTENT_URI, "container=" + info.f3id, (String[]) null);
    }
}
