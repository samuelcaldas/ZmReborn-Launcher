package org.zeam;

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
import org.zeam.ApplicationItemInfo;
import org.zeam.LauncherSettings;

public class LauncherModel {
    static final boolean DEBUG_LOADERS = false;
    static final String LOG_TAG = LauncherModel.class.getSimpleName();
    private static AsyncTask<Object, ArrayList<ApplicationItemInfo>, ArrayList<ApplicationItemInfo>> mLoadApplicationsAsyncTask;
    /* access modifiers changed from: private */
    public static ArrayList<ApplicationItemInfo> sCachedApplicationItemInfos;
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

    /* access modifiers changed from: package-private */
    public void loadApplications(final boolean isLaunching, final ApplicationsView applicationsView) {
        boolean load;
        if (applicationsView != null) {
            if (!isLaunching) {
                load = true;
            } else {
                load = sCachedApplicationItemInfos == null;
            }
            if (!load) {
                synchronized (this) {
                    applicationsView.setApplications(sCachedApplicationItemInfos);
                }
                return;
            }
            mLoadApplicationsAsyncTask = null;
            mLoadApplicationsAsyncTask = new AsyncTask<Object, ArrayList<ApplicationItemInfo>, ArrayList<ApplicationItemInfo>>() {
                /* access modifiers changed from: protected */
                public ArrayList<ApplicationItemInfo> doInBackground(Object... arg0) {
                    int i;
                    if (isLaunching) {
                        i = 0;
                    } else {
                        i = 10;
                    }
                    Process.setThreadPriority(i);
                    Launcher launcher = applicationsView.getLauncher();
                    Intent mainIntent = new Intent("android.intent.action.MAIN", (Uri) null);
                    mainIntent.addCategory("android.intent.category.LAUNCHER");
                    PackageManager packageManager = launcher.getPackageManager();
                    List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
                    for (int i2 = 0; i2 < apps.size(); i2++) {
                        if (apps.get(i2).activityInfo.packageName.equals(launcher.getApplicationContext().getPackageName())) {
                            apps.remove(i2);
                        }
                    }
                    ArrayList<ApplicationItemInfo> applicationItemInfos = new ArrayList<>();
                    if (apps != null) {
                        for (int i3 = 0; i3 < apps.size(); i3++) {
                            ResolveInfo resolveInfo = apps.get(i3);
                            ComponentName componentName = new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name);
                            ApplicationItemInfo applicationItemInfo = new ApplicationItemInfo();
                            applicationItemInfo.container = -1;
                            applicationItemInfo.title = resolveInfo.loadLabel(packageManager);
                            if (applicationItemInfo.title == null) {
                                applicationItemInfo.title = resolveInfo.activityInfo.name;
                            }
                            applicationItemInfo.icon = Utilities.createIconThumbnail(resolveInfo.activityInfo.loadIcon(packageManager), launcher);
                            applicationItemInfo.filtered = true;
                            applicationItemInfo.setActivity(componentName, 270532608);
                            applicationItemInfos.add(applicationItemInfo);
                        }
                    }
                    Collections.sort(applicationItemInfos, new ApplicationItemInfo.TitleComparator());
                    Process.setThreadPriority(0);
                    return applicationItemInfos;
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(ArrayList<ApplicationItemInfo> result) {
                    super.onPostExecute(result);
                    synchronized (this) {
                        applicationsView.setApplications(result);
                        LauncherModel.sCachedApplicationItemInfos = result;
                    }
                }
            };
            mLoadApplicationsAsyncTask.execute(new Object[0]);
        }
    }

    /* access modifiers changed from: package-private */
    public synchronized void addPackage(ApplicationsView applicationsView, String packageName) {
        loadApplications(false, applicationsView);
    }

    /* access modifiers changed from: package-private */
    public synchronized void removePackage(ApplicationsView applicationsView, String packageName) {
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

        /* JADX WARNING: Can't fix incorrect switch cases order */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void loadWorkspace() {
            /*
                r49 = this;
                java.lang.String r4 = org.zeam.LauncherModel.LOG_TAG
                java.lang.StringBuilder r6 = new java.lang.StringBuilder
                java.lang.String r12 = "  ----> running workspace loader ("
                r6.<init>(r12)
                r0 = r49
                int r12 = r0.mId
                java.lang.StringBuilder r6 = r6.append(r12)
                java.lang.String r12 = ")"
                java.lang.StringBuilder r6 = r6.append(r12)
                java.lang.String r6 = r6.toString()
                android.util.Log.d(r4, r6)
                r4 = 0
                android.os.Process.setThreadPriority(r4)
                r0 = r49
                java.lang.ref.WeakReference<org.zeam.Launcher> r4 = r0.mLauncher
                java.lang.Object r11 = r4.get()
                org.zeam.Launcher r11 = (org.zeam.Launcher) r11
                android.content.ContentResolver r3 = r11.getContentResolver()
                android.content.pm.PackageManager r40 = r11.getPackageManager()
                r0 = r49
                boolean r4 = r0.mLocaleChanged
                if (r4 == 0) goto L_0x0041
                r0 = r49
                r1 = r40
                r0.updateShortcutLabels(r3, r1)
            L_0x0041:
                java.util.ArrayList r27 = new java.util.ArrayList
                r27.<init>()
                java.util.ArrayList r26 = new java.util.ArrayList
                r26.<init>()
                java.util.HashMap r31 = new java.util.HashMap
                r31.<init>()
                android.net.Uri r4 = org.zeam.LauncherSettings.Favorites.CONTENT_URI
                r5 = 0
                r6 = 0
                r7 = 0
                r8 = 0
                android.database.Cursor r5 = r3.query(r4, r5, r6, r7, r8)
                java.lang.String r4 = "_id"
                int r34 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "intent"
                int r37 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "title"
                int r44 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "iconType"
                int r7 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "icon"
                int r10 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "iconPackage"
                int r8 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "iconResource"
                int r9 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "container"
                int r25 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "itemType"
                int r39 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "appWidgetId"
                int r18 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "screen"
                int r41 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "cellX"
                int r22 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "cellY"
                int r23 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "spanX"
                int r42 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "spanY"
                int r43 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "uri"
                int r47 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
                java.lang.String r4 = "displayMode"
                int r28 = r5.getColumnIndexOrThrow(r4)     // Catch:{ all -> 0x01c8 }
            L_0x00c0:
                r0 = r49
                boolean r4 = r0.mStopped     // Catch:{ all -> 0x01c8 }
                if (r4 != 0) goto L_0x00cc
                boolean r4 = r5.moveToNext()     // Catch:{ all -> 0x01c8 }
                if (r4 != 0) goto L_0x012c
            L_0x00cc:
                r5.close()
                r0 = r49
                org.zeam.LauncherModel r6 = org.zeam.LauncherModel.this
                monitor-enter(r6)
                r0 = r49
                boolean r4 = r0.mStopped     // Catch:{ all -> 0x0400 }
                if (r4 != 0) goto L_0x012a
                java.util.ArrayList r45 = new java.util.ArrayList     // Catch:{ all -> 0x0400 }
                r0 = r45
                r1 = r27
                r0.<init>(r1)     // Catch:{ all -> 0x0400 }
                java.util.ArrayList r46 = new java.util.ArrayList     // Catch:{ all -> 0x0400 }
                r0 = r46
                r1 = r26
                r0.<init>(r1)     // Catch:{ all -> 0x0400 }
                r0 = r49
                boolean r4 = r0.mStopped     // Catch:{ all -> 0x0400 }
                if (r4 != 0) goto L_0x0107
                java.lang.String r4 = org.zeam.LauncherModel.LOG_TAG     // Catch:{ all -> 0x0400 }
                java.lang.String r12 = "  ----> items cloned, ready to refresh UI"
                android.util.Log.d(r4, r12)     // Catch:{ all -> 0x0400 }
                org.zeam.LauncherModel$DesktopItemsLoader$1 r4 = new org.zeam.LauncherModel$DesktopItemsLoader$1     // Catch:{ all -> 0x0400 }
                r0 = r49
                r1 = r45
                r2 = r46
                r4.<init>(r11, r1, r2)     // Catch:{ all -> 0x0400 }
                r11.runOnUiThread(r4)     // Catch:{ all -> 0x0400 }
            L_0x0107:
                r0 = r49
                org.zeam.LauncherModel r4 = org.zeam.LauncherModel.this     // Catch:{ all -> 0x0400 }
                r0 = r27
                r4.mDesktopItems = r0     // Catch:{ all -> 0x0400 }
                r0 = r49
                org.zeam.LauncherModel r4 = org.zeam.LauncherModel.this     // Catch:{ all -> 0x0400 }
                r0 = r26
                r4.mDesktopAppWidgets = r0     // Catch:{ all -> 0x0400 }
                r0 = r49
                org.zeam.LauncherModel r4 = org.zeam.LauncherModel.this     // Catch:{ all -> 0x0400 }
                r0 = r31
                r4.mFolders = r0     // Catch:{ all -> 0x0400 }
                r0 = r49
                org.zeam.LauncherModel r4 = org.zeam.LauncherModel.this     // Catch:{ all -> 0x0400 }
                r12 = 1
                r4.mDesktopItemsLoaded = r12     // Catch:{ all -> 0x0400 }
            L_0x012a:
                monitor-exit(r6)     // Catch:{ all -> 0x0400 }
                return
            L_0x012c:
                r0 = r39
                int r38 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                switch(r38) {
                    case 0: goto L_0x0136;
                    case 1: goto L_0x0136;
                    case 2: goto L_0x023b;
                    case 3: goto L_0x0296;
                    case 4: goto L_0x038c;
                    case 6: goto L_0x01cd;
                    case 1001: goto L_0x0327;
                    default: goto L_0x0135;
                }     // Catch:{ Exception -> 0x01bc }
            L_0x0135:
                goto L_0x00c0
            L_0x0136:
                r0 = r37
                java.lang.String r36 = r5.getString(r0)     // Catch:{ Exception -> 0x01bc }
                r4 = 0
                r0 = r36
                android.content.Intent r35 = android.content.Intent.parseUri(r0, r4)     // Catch:{ URISyntaxException -> 0x0224 }
                if (r38 != 0) goto L_0x0227
                r0 = r40
                r1 = r35
                org.zeam.ApplicationItemInfo r20 = org.zeam.LauncherModel.getApplicationInfo(r0, r1, r11)     // Catch:{ Exception -> 0x01bc }
            L_0x014d:
                if (r20 != 0) goto L_0x015c
                org.zeam.ApplicationItemInfo r20 = new org.zeam.ApplicationItemInfo     // Catch:{ Exception -> 0x01bc }
                r20.<init>()     // Catch:{ Exception -> 0x01bc }
                android.graphics.drawable.Drawable r4 = r40.getDefaultActivityIcon()     // Catch:{ Exception -> 0x01bc }
                r0 = r20
                r0.icon = r4     // Catch:{ Exception -> 0x01bc }
            L_0x015c:
                if (r20 == 0) goto L_0x00c0
                r0 = r44
                java.lang.String r4 = r5.getString(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r20
                r0.title = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r35
                r1 = r20
                r1.intent = r0     // Catch:{ Exception -> 0x01bc }
                r0 = r34
                long r12 = r5.getLong(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r20
                r0.f3id = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r25
                int r24 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r24
                long r12 = (long) r0     // Catch:{ Exception -> 0x01bc }
                r0 = r20
                r0.container = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r41
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r20
                r0.screen = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r22
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r20
                r0.cellX = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r23
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r20
                r0.cellY = r4     // Catch:{ Exception -> 0x01bc }
                switch(r24) {
                    case -200: goto L_0x0232;
                    case -100: goto L_0x0232;
                    default: goto L_0x01a6;
                }     // Catch:{ Exception -> 0x01bc }
            L_0x01a6:
                r0 = r49
                org.zeam.LauncherModel r4 = org.zeam.LauncherModel.this     // Catch:{ Exception -> 0x01bc }
                r0 = r24
                long r12 = (long) r0     // Catch:{ Exception -> 0x01bc }
                r0 = r31
                org.zeam.UserFolderInfo r30 = r4.findOrMakeUserFolder(r0, r12)     // Catch:{ Exception -> 0x01bc }
                r0 = r30
                r1 = r20
                r0.add(r1)     // Catch:{ Exception -> 0x01bc }
                goto L_0x00c0
            L_0x01bc:
                r29 = move-exception
                java.lang.String r4 = org.zeam.Launcher.LOG_TAG     // Catch:{ all -> 0x01c8 }
                java.lang.String r6 = "Desktop items loading interrupted:"
                r0 = r29
                android.util.Log.w(r4, r6, r0)     // Catch:{ all -> 0x01c8 }
                goto L_0x00c0
            L_0x01c8:
                r4 = move-exception
                r5.close()
                throw r4
            L_0x01cd:
                org.zeam.ApplicationsGridItemInfo r21 = new org.zeam.ApplicationsGridItemInfo     // Catch:{ Exception -> 0x01bc }
                r0 = r21
                r0.<init>(r11)     // Catch:{ Exception -> 0x01bc }
                r0 = r34
                long r12 = r5.getLong(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r21
                r0.f3id = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r25
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                long r12 = (long) r4     // Catch:{ Exception -> 0x01bc }
                r0 = r21
                r0.container = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r41
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r21
                r0.screen = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r22
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r21
                r0.cellX = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r23
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r21
                r0.cellY = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r42
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r21
                r0.spanX = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r43
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r21
                r0.spanY = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r27
                r1 = r21
                r0.add(r1)     // Catch:{ Exception -> 0x01bc }
                goto L_0x00c0
            L_0x0224:
                r29 = move-exception
                goto L_0x00c0
            L_0x0227:
                r0 = r49
                org.zeam.LauncherModel r4 = org.zeam.LauncherModel.this     // Catch:{ Exception -> 0x01bc }
                r6 = r11
                org.zeam.ApplicationItemInfo r20 = r4.getApplicationInfoShortcut(r5, r6, r7, r8, r9, r10)     // Catch:{ Exception -> 0x01bc }
                goto L_0x014d
            L_0x0232:
                r0 = r27
                r1 = r20
                r0.add(r1)     // Catch:{ Exception -> 0x01bc }
                goto L_0x00c0
            L_0x023b:
                r0 = r34
                long r32 = r5.getLong(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r49
                org.zeam.LauncherModel r4 = org.zeam.LauncherModel.this     // Catch:{ Exception -> 0x01bc }
                r0 = r31
                r1 = r32
                org.zeam.UserFolderInfo r30 = r4.findOrMakeUserFolder(r0, r1)     // Catch:{ Exception -> 0x01bc }
                r0 = r44
                java.lang.String r4 = r5.getString(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r30
                r0.title = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r32
                r2 = r30
                r2.f3id = r0     // Catch:{ Exception -> 0x01bc }
                r0 = r25
                int r24 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r24
                long r12 = (long) r0     // Catch:{ Exception -> 0x01bc }
                r0 = r30
                r0.container = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r41
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r30
                r0.screen = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r22
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r30
                r0.cellX = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r23
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r30
                r0.cellY = r4     // Catch:{ Exception -> 0x01bc }
                switch(r24) {
                    case -200: goto L_0x028d;
                    case -100: goto L_0x028d;
                    default: goto L_0x028b;
                }     // Catch:{ Exception -> 0x01bc }
            L_0x028b:
                goto L_0x00c0
            L_0x028d:
                r0 = r27
                r1 = r30
                r0.add(r1)     // Catch:{ Exception -> 0x01bc }
                goto L_0x00c0
            L_0x0296:
                r0 = r34
                long r32 = r5.getLong(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r49
                org.zeam.LauncherModel r4 = org.zeam.LauncherModel.this     // Catch:{ Exception -> 0x01bc }
                r0 = r31
                r1 = r32
                org.zeam.LiveFolderInfo r16 = r4.findOrMakeLiveFolder(r0, r1)     // Catch:{ Exception -> 0x01bc }
                r0 = r37
                java.lang.String r36 = r5.getString(r0)     // Catch:{ Exception -> 0x01bc }
                r35 = 0
                if (r36 == 0) goto L_0x02b9
                r4 = 0
                r0 = r36
                android.content.Intent r35 = android.content.Intent.parseUri(r0, r4)     // Catch:{ URISyntaxException -> 0x0403 }
            L_0x02b9:
                r0 = r44
                java.lang.String r4 = r5.getString(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r16
                r0.title = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r32
                r2 = r16
                r2.f3id = r0     // Catch:{ Exception -> 0x01bc }
                r0 = r25
                int r24 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r24
                long r12 = (long) r0     // Catch:{ Exception -> 0x01bc }
                r0 = r16
                r0.container = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r41
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r16
                r0.screen = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r22
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r16
                r0.cellX = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r23
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r16
                r0.cellY = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r47
                java.lang.String r4 = r5.getString(r0)     // Catch:{ Exception -> 0x01bc }
                android.net.Uri r4 = android.net.Uri.parse(r4)     // Catch:{ Exception -> 0x01bc }
                r0 = r16
                r0.uri = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r35
                r1 = r16
                r1.baseIntent = r0     // Catch:{ Exception -> 0x01bc }
                r0 = r28
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r16
                r0.displayMode = r4     // Catch:{ Exception -> 0x01bc }
                r12 = r5
                r13 = r7
                r14 = r8
                r15 = r9
                org.zeam.LauncherModel.loadLiveFolderIcon(r11, r12, r13, r14, r15, r16)     // Catch:{ Exception -> 0x01bc }
                switch(r24) {
                    case -200: goto L_0x031e;
                    case -100: goto L_0x031e;
                    default: goto L_0x031c;
                }     // Catch:{ Exception -> 0x01bc }
            L_0x031c:
                goto L_0x00c0
            L_0x031e:
                r0 = r27
                r1 = r16
                r0.add(r1)     // Catch:{ Exception -> 0x01bc }
                goto L_0x00c0
            L_0x0327:
                org.zeam.Widget r48 = org.zeam.Widget.makeSearch()     // Catch:{ Exception -> 0x01bc }
                r0 = r25
                int r24 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r4 = -100
                r0 = r24
                if (r0 == r4) goto L_0x0340
                java.lang.String r4 = org.zeam.Launcher.LOG_TAG     // Catch:{ Exception -> 0x01bc }
                java.lang.String r6 = "Widget found where container != CONTAINER_DESKTOP  ignoring!"
                android.util.Log.e(r4, r6)     // Catch:{ Exception -> 0x01bc }
                goto L_0x00c0
            L_0x0340:
                r0 = r34
                long r12 = r5.getLong(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r48
                r0.f3id = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r41
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r48
                r0.screen = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r24
                long r12 = (long) r0     // Catch:{ Exception -> 0x01bc }
                r0 = r48
                r0.container = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r22
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r48
                r0.cellX = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r23
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r48
                r0.cellY = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r42
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r48
                r0.spanX = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r43
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r48
                r0.spanY = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r27
                r1 = r48
                r0.add(r1)     // Catch:{ Exception -> 0x01bc }
                goto L_0x00c0
            L_0x038c:
                r0 = r18
                int r17 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                org.zeam.LauncherAppWidgetInfo r19 = new org.zeam.LauncherAppWidgetInfo     // Catch:{ Exception -> 0x01bc }
                r0 = r19
                r1 = r17
                r0.<init>(r1)     // Catch:{ Exception -> 0x01bc }
                r0 = r34
                long r12 = r5.getLong(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r19
                r0.f3id = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r41
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r19
                r0.screen = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r22
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r19
                r0.cellX = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r23
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r19
                r0.cellY = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r42
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r19
                r0.spanX = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r43
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r0 = r19
                r0.spanY = r4     // Catch:{ Exception -> 0x01bc }
                r0 = r25
                int r24 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                r4 = -100
                r0 = r24
                if (r0 == r4) goto L_0x03ec
                java.lang.String r4 = org.zeam.Launcher.LOG_TAG     // Catch:{ Exception -> 0x01bc }
                java.lang.String r6 = "Widget found where container != CONTAINER_DESKTOP -- ignoring!"
                android.util.Log.e(r4, r6)     // Catch:{ Exception -> 0x01bc }
                goto L_0x00c0
            L_0x03ec:
                r0 = r25
                int r4 = r5.getInt(r0)     // Catch:{ Exception -> 0x01bc }
                long r12 = (long) r4     // Catch:{ Exception -> 0x01bc }
                r0 = r19
                r0.container = r12     // Catch:{ Exception -> 0x01bc }
                r0 = r26
                r1 = r19
                r0.add(r1)     // Catch:{ Exception -> 0x01bc }
                goto L_0x00c0
            L_0x0400:
                r4 = move-exception
                monitor-exit(r6)     // Catch:{ all -> 0x0400 }
                throw r4
            L_0x0403:
                r4 = move-exception
                goto L_0x02b9
            */
            throw new UnsupportedOperationException("Method not decompiled: org.zeam.LauncherModel.DesktopItemsLoader.loadWorkspace():void");
        }

        private void updateShortcutLabels(ContentResolver resolver, PackageManager manager) {
            String intentUri;
            ComponentName name;
            Cursor cursor = resolver.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"_id", LauncherSettings.BaseLauncherColumns.TITLE, LauncherSettings.BaseLauncherColumns.INTENT, LauncherSettings.BaseLauncherColumns.ITEM_TYPE}, (String) null, (String[]) null, (String) null);
            int idIndex = cursor.getColumnIndexOrThrow("_id");
            int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
            int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
            int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.TITLE);
            while (!this.mStopped && cursor.moveToNext()) {
                try {
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
                } finally {
                    cursor.close();
                }
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
        applicationItemInfo.icon = Utilities.createIconThumbnail(activityInfo.loadIcon(packageManager), context);
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
                    applicationItemInfo.icon = Utilities.createIconThumbnail(resources.getDrawable(resources.getIdentifier(resourceName, (String) null, (String) null)), context);
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
