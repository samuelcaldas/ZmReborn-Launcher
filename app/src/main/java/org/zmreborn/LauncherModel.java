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

/**
 * Coordinates in-memory launcher models, background workspace and application loading, and database persistence.
 */
public class LauncherModel {
    static final boolean DEBUG_LOADERS = false;
    static final String LOG_TAG = LauncherModel.class.getSimpleName();
    private static AsyncTask<Object, ArrayList<ApplicationItemInfo>, ArrayList<ApplicationItemInfo>> loadApplicationsAsyncTask;
    private final ApplicationsLoadGeneration applicationsLoadGeneration = new ApplicationsLoadGeneration();
    private static ArrayList<ApplicationItemInfo> cachedApplicationItemInfos;
    private static ArrayList<ApplicationItemInfo> allApplicationItemInfos;
    public static final AtomicInteger sWorkspaceLoaderCount = new AtomicInteger(1);
    private ArrayList<LauncherAppWidgetInfo> desktopAppWidgets;
    private int desktopColumns;
    private ArrayList<ItemInfo> desktopItems;
    private boolean desktopItemsLoaded;
    private DesktopItemsLoader desktopItemsLoader;
    private Thread desktopLoaderThread;
    private int desktopRows;
    private HashMap<Long, FolderInfo> folders;

    /**
     * Aborts active background loaders.
     */
    public synchronized void abortLoaders() {
        if (this.desktopItemsLoader != null && this.desktopItemsLoader.isRunning()) {
            this.desktopItemsLoader.stop();
            this.desktopItemsLoaded = false;
        }
    }

    /**
     * Returns a copy of all loaded application items.
     */
    static ArrayList<ApplicationItemInfo> getAllApplications() {
        if (allApplicationItemInfos == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(allApplicationItemInfos);
    }

    /**
     * Asynchronously loads installed applications and delivers them to the applications view.
     */
    public void loadApplications(final boolean isLaunching, final ApplicationsView applicationsView) {
        if (applicationsView == null) {
            return;
        }
        final int generation = this.applicationsLoadGeneration.start();
        applicationsView.setLoading();
        if (loadApplicationsAsyncTask != null) {
            loadApplicationsAsyncTask.cancel(true);
            loadApplicationsAsyncTask = null;
        }
        boolean shouldLoad = !isLaunching || cachedApplicationItemInfos == null;
        if (!shouldLoad) {
            ArrayList<ApplicationItemInfo> cachedApplications;
            synchronized (this) {
                cachedApplications = cachedApplicationItemInfos;
            }
            if (this.applicationsLoadGeneration.isCurrent(generation)) {
                deliverApplications(applicationsView, cachedApplications);
            }
            return;
        }
        loadApplicationsAsyncTask = new AsyncTask<Object, ArrayList<ApplicationItemInfo>, ArrayList<ApplicationItemInfo>>() {
            private ArrayList<ApplicationItemInfo> localAllApplicationItemInfos;

            @Override
            protected ArrayList<ApplicationItemInfo> doInBackground(Object... arg0) {
                int priority = isLaunching ? 0 : 10;
                Process.setThreadPriority(priority);
                try {
                    if (isCancelled()) {
                        return null;
                    }
                    Launcher launcher = applicationsView.getLauncher();
                    Intent mainIntent = new Intent("android.intent.action.MAIN", null);
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
                    Collections.sort(applicationItemInfos, new ApplicationItemInfo.TitleComparator());
                    if (isCancelled()) {
                        return null;
                    }
                    ArrayList<AppListFolderRecord> folderRecords = new AppListFolderStore(
                            launcher.getContentResolver()).loadFolders();
                    ArrayList<ApplicationItemInfo> projectedItems = AppListFolderProjection.project(
                            folderRecords, applicationItemInfos);
                    if (isCancelled()) {
                        return null;
                    }
                    this.localAllApplicationItemInfos = new ArrayList<>(applicationItemInfos);
                    return projectedItems;
                } catch (RuntimeException exception) {
                    Log.e(LOG_TAG, "Applications loading failed", exception);
                    return null;
                } finally {
                    Process.setThreadPriority(0);
                }
            }

            @Override
            protected void onPostExecute(ArrayList<ApplicationItemInfo> result) {
                super.onPostExecute(result);
                if (!LauncherModel.this.applicationsLoadGeneration.isCurrent(generation)) {
                    return;
                }
                synchronized (LauncherModel.this) {
                    if (!LauncherModel.this.applicationsLoadGeneration.isCurrent(generation)) {
                        return;
                    }
                    if (result == null || this.localAllApplicationItemInfos == null) {
                        applicationsView.setError();
                        return;
                    }
                    LauncherModel.allApplicationItemInfos = new ArrayList<>(this.localAllApplicationItemInfos);
                    applicationsView.setApplications(result);
                    LauncherModel.cachedApplicationItemInfos = result;
                    if (result.isEmpty()) {
                        applicationsView.setEmpty();
                    } else {
                        applicationsView.clearState();
                    }
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                if (LauncherModel.this.applicationsLoadGeneration.isCurrent(generation)) {
                    applicationsView.setError();
                }
            }
        };
        loadApplicationsAsyncTask.execute(new Object[0]);
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

    synchronized void addPackage(ApplicationsView applicationsView, String packageName) {
        loadApplications(false, applicationsView);
    }

    synchronized void removePackage(ApplicationsView applicationsView, String packageName) {
        if (applicationsView != null && applicationsView.getLauncher() != null) {
            new AppListFolderStore(applicationsView.getLauncher().getContentResolver()).removePackage(packageName);
        }
        loadApplications(false, applicationsView);
    }

    synchronized void updatePackage(ApplicationsView applicationsView, String packageName) {
        loadApplications(false, applicationsView);
    }

    Drawable getApplicationItemInfoIconOrNull(PackageManager packageManager, ApplicationItemInfo applicationItemInfo) {
        ResolveInfo resolveInfo = packageManager.resolveActivity(applicationItemInfo.intent, 0);
        if (resolveInfo == null) {
            return null;
        }
        return resolveInfo.activityInfo.loadIcon(packageManager);
    }

    boolean isDesktopLoaded() {
        return this.desktopItems != null && this.desktopAppWidgets != null && this.desktopItemsLoaded;
    }

    void loadUserItems(boolean isLaunching, Launcher launcher, boolean localeChanged) {
        this.desktopRows = PreferencesUtil.getContentGridRows(launcher);
        this.desktopColumns = PreferencesUtil.getContentGridColumns(launcher);
        if (!isLaunching || !isDesktopLoaded()) {
            if (this.desktopItemsLoader != null && this.desktopItemsLoader.isRunning()) {
                this.desktopItemsLoader.stop();
                try {
                    this.desktopLoaderThread.join();
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "desktopLoaderThread didn't exit in time");
                }
            }
            this.desktopItemsLoaded = false;
            this.desktopItemsLoader = new DesktopItemsLoader(launcher, localeChanged);
            this.desktopLoaderThread = new Thread(this.desktopItemsLoader, "Desktop Items Loader");
            this.desktopLoaderThread.start();
            return;
        }
        launcher.onDesktopItemsLoaded(this.desktopItems, this.desktopAppWidgets);
    }

    private static String getLabel(PackageManager manager, ActivityInfo activityInfo) {
        String label = activityInfo.loadLabel(manager).toString();
        if (label != null) {
            return label;
        }
        String appLabel = manager.getApplicationLabel(activityInfo.applicationInfo).toString();
        return appLabel != null ? appLabel : activityInfo.name;
    }

    private class DesktopItemsLoader implements Runnable {
        private volatile boolean finished;
        private final int id = LauncherModel.sWorkspaceLoaderCount.getAndIncrement();
        private final WeakReference<Launcher> launcherRef;
        private final boolean localeChanged;
        private volatile boolean stopped;

        DesktopItemsLoader(Launcher launcher, boolean localeChanged) {
            this.launcherRef = new WeakReference<>(launcher);
            this.localeChanged = localeChanged;
            this.finished = false;
        }

        void stop() {
            Log.d(LauncherModel.LOG_TAG, "  ----> workspace loader " + this.id + " stopped from " + Thread.currentThread());
            this.stopped = true;
        }

        boolean isRunning() {
            return !this.finished;
        }

        @Override
        public void run() {
            loadWorkspace();
            this.finished = true;
        }

        private void loadWorkspace() {
            Log.d(LauncherModel.LOG_TAG, "  ----> running workspace loader (" + this.id + ")");
            Process.setThreadPriority(0);

            Launcher launcher = this.launcherRef.get();
            if (launcher == null) {
                return;
            }
            ContentResolver resolver = launcher.getContentResolver();
            PackageManager packageManager = launcher.getPackageManager();

            if (this.localeChanged) {
                updateShortcutLabels(resolver, packageManager);
            }

            ArrayList<ItemInfo> items = new ArrayList<>();
            ArrayList<LauncherAppWidgetInfo> appWidgets = new ArrayList<>();
            HashMap<Long, FolderInfo> folderMap = new HashMap<>();
            Cursor cursor = resolver.query(
                    LauncherSettings.Favorites.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );
            if (cursor == null) {
                return;
            }

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

                while (!this.stopped && cursor.moveToNext()) {
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
                                applicationItemInfo.id = cursor.getLong(idIndex);
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

                                LauncherModel.this.findOrMakeUserFolder(folderMap, applicationContainer)
                                        .add(applicationItemInfo);
                                continue;

                            case LauncherSettings.Favorites.ITEM_TYPE_APPS_GRID:
                                ApplicationsGridItemInfo applicationsGridItemInfo = new ApplicationsGridItemInfo(launcher);
                                applicationsGridItemInfo.id = cursor.getLong(idIndex);
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
                                UserFolderInfo userFolderInfo = LauncherModel.this.findOrMakeUserFolder(folderMap, userFolderId);
                                userFolderInfo.title = cursor.getString(titleIndex);
                                userFolderInfo.id = userFolderId;
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
                                LiveFolderInfo liveFolderInfo = LauncherModel.this.findOrMakeLiveFolder(folderMap, liveFolderId);
                                String liveFolderIntentUri = cursor.getString(intentIndex);
                                Intent baseIntent = null;

                                if (liveFolderIntentUri != null) {
                                    try {
                                        baseIntent = Intent.parseUri(liveFolderIntentUri, 0);
                                    } catch (URISyntaxException ignored) {
                                    }
                                }

                                liveFolderInfo.title = cursor.getString(titleIndex);
                                liveFolderInfo.id = liveFolderId;
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

                                searchWidget.id = cursor.getLong(idIndex);
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
                                appWidgetInfo.id = cursor.getLong(idIndex);
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
                if (this.stopped) {
                    return;
                }

                final ArrayList<ItemInfo> itemsToBind = new ArrayList<>(items);
                final ArrayList<LauncherAppWidgetInfo> appWidgetsToBind = new ArrayList<>(appWidgets);

                if (!this.stopped) {
                    Log.d(LauncherModel.LOG_TAG, "  ----> items cloned, ready to refresh UI");
                    final Launcher launcherToBind = launcher;
                    launcherToBind.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            launcherToBind.onDesktopItemsLoaded(itemsToBind, appWidgetsToBind);
                        }
                    });
                }

                LauncherModel.this.desktopItems = items;
                LauncherModel.this.desktopAppWidgets = appWidgets;
                LauncherModel.this.folders = folderMap;
                LauncherModel.this.desktopItemsLoaded = true;
            }
        }

        private void updateShortcutLabels(ContentResolver resolver, PackageManager manager) {
            String intentUri;
            ComponentName name;
            Cursor cursor = resolver.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"_id", LauncherSettings.BaseLauncherColumns.TITLE, LauncherSettings.BaseLauncherColumns.INTENT, LauncherSettings.BaseLauncherColumns.ITEM_TYPE}, null, null, null);
            if (cursor == null) {
                return;
            }
            try {
                int idIndex = cursor.getColumnIndexOrThrow("_id");
                int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
                int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
                int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.TITLE);
                while (!this.stopped && cursor.moveToNext()) {
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
                    } catch (PackageManager.NameNotFoundException | URISyntaxException ignored) {
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    private static void loadLiveFolderIcon(Launcher launcher, Cursor cursor, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, LiveFolderInfo liveFolderInfo) {
        if (cursor.getInt(iconTypeIndex) == 0) {
            String packageName = cursor.getString(iconPackageIndex);
            String resourceName = cursor.getString(iconResourceIndex);
            try {
                Resources resources = launcher.getPackageManager().getResourcesForApplication(packageName);
                liveFolderInfo.icon = resources.getDrawable(resources.getIdentifier(resourceName, null, null));
            } catch (Exception e) {
                liveFolderInfo.icon = launcher.getResources().getDrawable(R.drawable.ic_launcher_folder);
            }
            liveFolderInfo.iconResource = new Intent.ShortcutIconResource();
            liveFolderInfo.iconResource.packageName = packageName;
            liveFolderInfo.iconResource.resourceName = resourceName;
            return;
        }
        liveFolderInfo.icon = launcher.getResources().getDrawable(R.drawable.ic_launcher_folder);
    }

    FolderInfo findFolderById(long id) {
        if (this.folders != null) {
            return this.folders.get(Long.valueOf(id));
        }
        return null;
    }

    void addFolder(FolderInfo info) {
        this.folders.put(Long.valueOf(info.id), info);
    }

    private UserFolderInfo findOrMakeUserFolder(HashMap<Long, FolderInfo> folderMap, long id) {
        FolderInfo folderInfo = folderMap.get(Long.valueOf(id));
        if (folderInfo == null || !(folderInfo instanceof UserFolderInfo)) {
            folderInfo = new UserFolderInfo();
            folderMap.put(Long.valueOf(id), folderInfo);
        }
        return (UserFolderInfo) folderInfo;
    }

    private LiveFolderInfo findOrMakeLiveFolder(HashMap<Long, FolderInfo> folderMap, long id) {
        FolderInfo folderInfo = folderMap.get(Long.valueOf(id));
        if (folderInfo == null || !(folderInfo instanceof LiveFolderInfo)) {
            folderInfo = new LiveFolderInfo();
            folderMap.put(Long.valueOf(id), folderInfo);
        }
        return (LiveFolderInfo) folderInfo;
    }

    void unbind() {
        unbindDrawables(this.desktopItems);
        unbindAppWidgetHostViews(this.desktopAppWidgets);
    }

    private void unbindDrawables(ArrayList<ItemInfo> items) {
        if (items != null) {
            int count = items.size();
            for (int i = 0; i < count; i++) {
                ItemInfo item = items.get(i);
                if (item.itemType == 0 || item.itemType == 1) {
                    ((ApplicationItemInfo) item).icon.setCallback(null);
                }
            }
        }
    }

    private void unbindAppWidgetHostViews(ArrayList<LauncherAppWidgetInfo> widgets) {
        if (widgets != null) {
            int count = widgets.size();
            for (int i = 0; i < count; i++) {
                widgets.get(i).hostView = null;
            }
        }
    }

    void findAllOccupiedCells(boolean[][] occupied, int countX, int countY, int screen) {
        ArrayList<ItemInfo> items = this.desktopItems;
        if (items != null) {
            int count = items.size();
            for (int i = 0; i < count; i++) {
                if ((items.get(i).spanX - 1) + items.get(i).cellX < this.desktopColumns
                        && (items.get(i).spanY - 1) + items.get(i).cellY < this.desktopRows) {
                    addOccupiedCells(occupied, screen, items.get(i));
                }
            }
        }
        ArrayList<LauncherAppWidgetInfo> widgets = this.desktopAppWidgets;
        if (widgets != null) {
            int count2 = widgets.size();
            for (int i2 = 0; i2 < count2; i2++) {
                addOccupiedCells(occupied, screen, widgets.get(i2));
            }
        }
    }

    private void addOccupiedCells(boolean[][] occupied, int screen, ItemInfo item) {
        if (item.screen == screen) {
            for (int xx = item.cellX; xx < item.cellX + item.spanX; xx++) {
                for (int yy = item.cellY; yy < item.cellY + item.spanY; yy++) {
                    if (xx < this.desktopColumns && yy < this.desktopRows) {
                        occupied[xx][yy] = true;
                    }
                }
            }
        }
    }

    void addDesktopItem(ItemInfo info) {
        if (isDesktopLoaded()) {
            this.desktopItems.add(info);
        }
    }

    void removeDesktopItem(ItemInfo info) {
        this.desktopItems.remove(info);
    }

    void addDesktopAppWidget(LauncherAppWidgetInfo info) {
        if (isDesktopLoaded()) {
            this.desktopAppWidgets.add(info);
        }
    }

    void removeDesktopAppWidget(LauncherAppWidgetInfo info) {
        this.desktopAppWidgets.remove(info);
    }

    private static ApplicationItemInfo getApplicationInfo(PackageManager packageManager, Intent intent, Context context) {
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

    private ApplicationItemInfo getApplicationInfoShortcut(Cursor cursor, Context context, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex) {
        ApplicationItemInfo applicationItemInfo = new ApplicationItemInfo();
        applicationItemInfo.itemType = 1;
        switch (cursor.getInt(iconTypeIndex)) {
            case 0:
                String packageName = cursor.getString(iconPackageIndex);
                String resourceName = cursor.getString(iconResourceIndex);
                PackageManager packageManager = context.getPackageManager();
                try {
                    Resources resources = packageManager.getResourcesForApplication(packageName);
                    applicationItemInfo.icon = Utilities.normalizeApplicationIcon(resources.getDrawable(resources.getIdentifier(resourceName, null, null)), context);
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

    void removeUserFolderItem(UserFolderInfo folder, ItemInfo info) {
        folder.contents.remove(info);
    }

    void removeUserFolder(UserFolderInfo userFolderInfo) {
        this.folders.remove(Long.valueOf(userFolderInfo.id));
    }

    static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY) {
        if (item.container == -1) {
            addItemToDatabase(context, item, container, screen, cellX, cellY, false);
            return;
        }
        moveItemInDatabase(context, item, container, screen, cellX, cellY);
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
        contentResolver.update(LauncherSettings.Favorites.getContentUri(item.id, false), values, null, null);
    }

    static boolean shortcutExists(Context context, String title, Intent intent) {
        Cursor cursor = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, new String[]{LauncherSettings.BaseLauncherColumns.TITLE, LauncherSettings.BaseLauncherColumns.INTENT}, "title=? and intent=?", new String[]{title, intent.toUri(0)}, null);
        if (cursor == null) {
            return false;
        }
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    FolderInfo getFolderById(Context context, long id) {
        Cursor cursor = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, "_id=? and (itemType=? or itemType=?)", new String[]{String.valueOf(id), String.valueOf(2), String.valueOf(3)}, null);
        if (cursor == null) {
            return null;
        }
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
                        folderInfo = findOrMakeUserFolder(this.folders, id);
                        break;
                    case 3:
                        folderInfo = findOrMakeLiveFolder(this.folders, id);
                        break;
                }
                if (folderInfo != null) {
                    folderInfo.title = cursor.getString(titleIndex);
                    folderInfo.id = id;
                    folderInfo.container = (long) cursor.getInt(containerIndex);
                    folderInfo.screen = cursor.getInt(screenIndex);
                    folderInfo.cellX = cursor.getInt(cellXIndex);
                    folderInfo.cellY = cursor.getInt(cellYIndex);
                }
                return folderInfo;
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    static void addItemToDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY, boolean notify) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = context.getContentResolver();
        item.onAddToDatabase(values);
        Uri uri = notify ? LauncherSettings.Favorites.CONTENT_URI : LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION;
        Uri result = contentResolver.insert(uri, values);
        if (result != null) {
            item.id = (long) Integer.parseInt(result.getPathSegments().get(1));
        }
    }

    static synchronized void updateItemInDatabase(Context context, ItemInfo item) {
        synchronized (LauncherModel.class) {
            ContentValues values = new ContentValues();
            ContentResolver contentResolver = context.getContentResolver();
            item.onAddToDatabase(values);
            contentResolver.update(LauncherSettings.Favorites.getContentUri(item.id, false), values, null, null);
        }
    }

    static void deleteItemFromDatabase(Context context, ItemInfo item) {
        context.getContentResolver().delete(LauncherSettings.Favorites.getContentUri(item.id, false), null, null);
    }

    static void deleteUserFolderContentsFromDatabase(Context context, UserFolderInfo info) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(LauncherSettings.Favorites.getContentUri(info.id, false), null, null);
        contentResolver.delete(LauncherSettings.Favorites.CONTENT_URI, "container=" + info.id, null);
    }
}
