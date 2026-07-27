package org.zmreborn;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParserException;
import org.zmreborn.LauncherSettings;

public class LauncherProvider extends ContentProvider {
    static final String AUTHORITY = LauncherSettings.AUTHORITY;
    static final Uri CONTENT_APPWIDGET_RESET_URI = Uri.parse("content://" + AUTHORITY + "/appWidgetReset");
    private static final String DATABASE_NAME = "launcher.db";
    private static final int DATABASE_VERSION = 6;
    static final String EXTRA_BIND_SOURCES = AUTHORITY + ".bindsources";
    static final String EXTRA_BIND_TARGETS = AUTHORITY + ".bindtargets";
    private static final boolean LOGD = true;
    /* access modifiers changed from: private */
    public static final String LOG_TAG = LauncherProvider.class.getSimpleName();
    static final String PARAMETER_NOTIFY = "notify";
    static final String TABLE_FAVORITES = "favorites";
    static final String TABLE_GESTURES = "gestures";
    static final String TABLE_APP_LIST_FOLDERS = "appListFolders";
    static final String TABLE_APP_LIST_FOLDER_ITEMS = "appListFolderItems";
    private SQLiteOpenHelper mOpenHelper;

    public boolean onCreate() {
        this.mOpenHelper = new DatabaseHelper(getContext());
        return LOGD;
    }

    public String getType(Uri uri) {
        SqlArguments sqlArguments = new SqlArguments(uri, (String) null, (String[]) null);
        if (TextUtils.isEmpty(sqlArguments.where)) {
            return "vnd.android.cursor.dir/" + sqlArguments.table;
        }
        return "vnd.android.cursor.item/" + sqlArguments.table;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);
        Cursor result = qb.query(this.mOpenHelper.getWritableDatabase(), projection, args.where, args.args, (String) null, (String) null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    public Uri insert(Uri uri, ContentValues initialValues) {
        long rowId = this.mOpenHelper.getWritableDatabase().insert(new SqlArguments(uri).table, (String) null, initialValues);
        if (rowId <= 0) {
            return null;
        }
        Uri uri2 = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri2);
        return uri2;
    }

    /* JADX INFO: finally extract failed */
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ContentValues insert : values) {
                if (db.insert(args.table, (String) null, insert) < 0) {
                    db.endTransaction();
                    return 0;
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            sendNotify(uri);
            return values.length;
        } catch (Throwable th) {
            db.endTransaction();
            throw th;
        }
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        int count = this.mOpenHelper.getWritableDatabase().delete(args.table, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }
        return count;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        int count = this.mOpenHelper.getWritableDatabase().update(args.table, values, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }
        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, (ContentObserver) null);
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String TAG_APPWIDGET = "appwidget";
        private static final String TAG_CLOCK = "clock";
        private static final String TAG_FAVORITE = "favorite";
        private static final String TAG_FAVORITES = "favorites";
        private static final String TAG_SEARCH = "search";
        private static final String TAG_SHORTCUT = "shortcut";
        private final AppWidgetHost mAppWidgetHost;
        private final Context mContext;

        DatabaseHelper(Context context) {
            super(context, LauncherProvider.DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, LauncherProvider.DATABASE_VERSION);
            this.mContext = context;
            this.mAppWidgetHost = new AppWidgetHost(context, 1024);
        }

        private void sendAppWidgetResetNotify() {
            this.mContext.getContentResolver().notifyChange(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, (ContentObserver) null);
        }

        public void onCreate(SQLiteDatabase db) {
            Log.d(LauncherProvider.LOG_TAG, "creating new launcher database");
            db.execSQL("CREATE TABLE favorites (_id INTEGER PRIMARY KEY,title TEXT,intent TEXT,container INTEGER,screen INTEGER,cellX INTEGER,cellY INTEGER,spanX INTEGER,spanY INTEGER,itemType INTEGER,appWidgetId INTEGER NOT NULL DEFAULT -1,isShortcut INTEGER,iconType INTEGER,iconPackage TEXT,iconResource TEXT,icon BLOB,uri TEXT,action INTEGER,displayMode INTEGER);");
            db.execSQL("CREATE TABLE gestures (_id INTEGER PRIMARY KEY,title TEXT,intent TEXT,itemType INTEGER,iconType INTEGER,iconPackage TEXT,iconResource TEXT,icon BLOB);");
            createAppListFolderTables(db);
            if (this.mAppWidgetHost != null) {
                this.mAppWidgetHost.deleteHost();
                sendAppWidgetResetNotify();
            }
            if (!convertDatabase(db)) {
                loadFavorites(db);
            }
        }

        private boolean convertDatabase(SQLiteDatabase db) {
            Log.d(LauncherProvider.LOG_TAG, "converting database from an older format, but not onUpgrade");
            boolean converted = false;
            Uri uri = Uri.parse("content://settings/old_favorites?notify=true");
            ContentResolver resolver = this.mContext.getContentResolver();
            Cursor cursor = null;
            try {
                cursor = resolver.query(uri, (String[]) null, (String) null, (String[]) null, (String) null);
            } catch (Exception e) {
            }
            if (cursor != null && cursor.getCount() > 0) {
                try {
                    converted = copyFromCursor(db, cursor) > 0 ? LauncherProvider.LOGD : false;
                    if (converted) {
                        resolver.delete(uri, (String) null, (String[]) null);
                    }
                } finally {
                    cursor.close();
                }
            }
            if (converted) {
                Log.d(LauncherProvider.LOG_TAG, "converted and now triggering widget upgrade");
                convertWidgets(db);
            }
            return converted;
        }

        /* JADX INFO: finally extract failed */
        private int copyFromCursor(SQLiteDatabase db, Cursor cursor) {
            int idIndex = cursor.getColumnIndexOrThrow("_id");
            int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
            int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.TITLE);
            int iconTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_TYPE);
            int iconIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON);
            int iconPackageIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE);
            int iconResourceIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE);
            int containerIndex = cursor.getColumnIndexOrThrow("container");
            int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
            int screenIndex = cursor.getColumnIndexOrThrow("screen");
            int cellXIndex = cursor.getColumnIndexOrThrow("cellX");
            int cellYIndex = cursor.getColumnIndexOrThrow("cellY");
            int uriIndex = cursor.getColumnIndexOrThrow("uri");
            int displayModeIndex = cursor.getColumnIndexOrThrow("displayMode");
            ContentValues[] rows = new ContentValues[cursor.getCount()];
            int i = 0;
            while (cursor.moveToNext()) {
                ContentValues values = new ContentValues(cursor.getColumnCount());
                values.put("_id", Long.valueOf(cursor.getLong(idIndex)));
                values.put(LauncherSettings.BaseLauncherColumns.INTENT, cursor.getString(intentIndex));
                values.put(LauncherSettings.BaseLauncherColumns.TITLE, cursor.getString(titleIndex));
                values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE, Integer.valueOf(cursor.getInt(iconTypeIndex)));
                values.put(LauncherSettings.BaseLauncherColumns.ICON, cursor.getBlob(iconIndex));
                values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, cursor.getString(iconPackageIndex));
                values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE, cursor.getString(iconResourceIndex));
                values.put("container", Integer.valueOf(cursor.getInt(containerIndex)));
                values.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, Integer.valueOf(cursor.getInt(itemTypeIndex)));
                values.put("appWidgetId", -1);
                values.put("screen", Integer.valueOf(cursor.getInt(screenIndex)));
                values.put("cellX", Integer.valueOf(cursor.getInt(cellXIndex)));
                values.put("cellY", Integer.valueOf(cursor.getInt(cellYIndex)));
                values.put("uri", cursor.getString(uriIndex));
                values.put("displayMode", Integer.valueOf(cursor.getInt(displayModeIndex)));
                rows[i] = values;
                i++;
            }
            db.beginTransaction();
            int total = 0;
            try {
                for (ContentValues insert : rows) {
                    if (db.insert(TAG_FAVORITES, (String) null, insert) < 0) {
                        db.endTransaction();
                        return 0;
                    }
                    total++;
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                return total;
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(LauncherProvider.LOG_TAG, "onUpgrade triggered");
            int version = oldVersion;
            if (version < 3) {
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE favorites ADD COLUMN appWidgetId INTEGER NOT NULL DEFAULT -1;");
                    db.setTransactionSuccessful();
                    version = 3;
                } catch (SQLException ex) {
                    Log.e(LauncherProvider.LOG_TAG, ex.getMessage(), ex);
                } finally {
                    db.endTransaction();
                }
                if (version == 3) {
                    convertWidgets(db);
                }
            }
            if (version < 4) {
                db.beginTransaction();
                try {
                    db.execSQL("CREATE TABLE gestures (_id INTEGER PRIMARY KEY,title TEXT,intent TEXT,itemType INTEGER,iconType INTEGER,iconPackage TEXT,iconResource TEXT,icon BLOB);");
                    db.setTransactionSuccessful();
                    version = 4;
                } catch (SQLException ex2) {
                    Log.e(LauncherProvider.LOG_TAG, ex2.getMessage(), ex2);
                } finally {
                    db.endTransaction();
                }
            }
            if (version < 5) {
                db.beginTransaction();
                try {
                    db.execSQL("DELETE from favorites WHERE container = -200;");
                    db.execSQL("ALTER table favorites ADD COLUMN action INTEGER;");
                    db.setTransactionSuccessful();
                    version = 5;
                } catch (SQLException sqle) {
                    Log.e(LauncherProvider.LOG_TAG, sqle.getMessage(), sqle);
                } finally {
                    db.endTransaction();
                }
            }
            if (version < 6) {
                db.beginTransaction();
                try {
                    createAppListFolderTables(db);
                    db.setTransactionSuccessful();
                    version = 6;
                } catch (SQLException ex3) {
                    Log.e(LauncherProvider.LOG_TAG, ex3.getMessage(), ex3);
                } finally {
                    db.endTransaction();
                }
            }
            if (version != 6) {
                Log.w(LauncherProvider.LOG_TAG, "unable to upgrade database, recreating..");
                db.execSQL("DROP TABLE IF EXISTS favorites");
                db.execSQL("DROP TABLE IF EXISTS gestures");
                db.execSQL("DROP TABLE IF EXISTS appListFolderItems");
                db.execSQL("DROP TABLE IF EXISTS appListFolders");
                onCreate(db);
            }
        }

        private void createAppListFolderTables(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS appListFolders (_id INTEGER PRIMARY KEY,title TEXT NOT NULL,position INTEGER NOT NULL);");
            db.execSQL("CREATE TABLE IF NOT EXISTS appListFolderItems (_id INTEGER PRIMARY KEY,folderId INTEGER NOT NULL,componentName TEXT NOT NULL UNIQUE,position INTEGER NOT NULL);");
            db.execSQL("CREATE INDEX IF NOT EXISTS appListFolderItems_folderId_index ON appListFolderItems(folderId);");
            db.execSQL("CREATE INDEX IF NOT EXISTS appListFolderItems_componentName_index ON appListFolderItems(componentName);");
        }

        private void convertWidgets(SQLiteDatabase db) {
            int[] bindSources = {1000, 1002};
            ArrayList<ComponentName> bindTargets = new ArrayList<>();
            bindTargets.add(new ComponentName("com.android.alarmclock", "com.android.alarmclock.AnalogAppWidgetProvider"));
            bindTargets.add(new ComponentName("com.android.camera", "com.android.camera.PhotoAppWidgetProvider"));
            String selectWhere = LauncherProvider.buildOrWhereString(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, bindSources);
            Cursor cursor = null;
            boolean allocatedAppWidgets = false;
            db.beginTransaction();
            try {
                cursor = db.query(TAG_FAVORITES, new String[]{"_id"}, selectWhere, (String[]) null, (String) null, (String) null, (String) null);
                Log.d(LauncherProvider.LOG_TAG, "found upgrade cursor count=" + cursor.getCount());
                ContentValues values = new ContentValues();
                while (cursor != null && cursor.moveToNext()) {
                    long favoriteId = cursor.getLong(0);
                    try {
                        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                        Log.d(LauncherProvider.LOG_TAG, "allocated appWidgetId=" + appWidgetId + " for favoriteId=" + favoriteId);
                        values.clear();
                        values.put("appWidgetId", Integer.valueOf(appWidgetId));
                        values.put("spanX", 2);
                        values.put("spanY", 2);
                        db.update(TAG_FAVORITES, values, "_id=" + favoriteId, (String[]) null);
                        allocatedAppWidgets = LauncherProvider.LOGD;
                    } catch (RuntimeException ex) {
                        Log.e(LauncherProvider.LOG_TAG, "Problem allocating appWidgetId", ex);
                    }
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                if (cursor != null) {
                    cursor.close();
                }
            } catch (SQLException ex2) {
                Log.w(LauncherProvider.LOG_TAG, "Problem while allocating appWidgetIds for existing widgets", ex2);
                db.endTransaction();
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                db.endTransaction();
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
            if (allocatedAppWidgets) {
                launchAppWidgetBinder(bindSources, bindTargets);
            }
        }

        private void launchAppWidgetBinder(int[] bindSources, ArrayList<ComponentName> bindTargets) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.LauncherAppWidgetBinder"));
            intent.setFlags(268435456);
            Bundle extras = new Bundle();
            extras.putIntArray(LauncherProvider.EXTRA_BIND_SOURCES, bindSources);
            extras.putParcelableArrayList(LauncherProvider.EXTRA_BIND_TARGETS, bindTargets);
            intent.putExtras(extras);
            this.mContext.startActivity(intent);
        }

        private int loadFavorites(SQLiteDatabase db) {
            Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
            intent.addCategory("android.intent.category.LAUNCHER");
            ContentValues values = new ContentValues();
            PackageManager packageManager = this.mContext.getPackageManager();
            int i = 0;
            try {
                XmlResourceParser parser = this.mContext.getResources().getXml(R.xml.default_workspace);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                XmlUtils.beginDocument(parser, TAG_FAVORITES);
                int depth = parser.getDepth();
                while (true) {
                    int type = parser.next();
                    if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                        break;
                    } else if (type == 2) {
                        boolean added = false;
                        String name = parser.getName();
                        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.Favorite);
                        values.clear();
                        String container = a.getString(10);
                        if (container == null) {
                            values.put("container", -100);
                        } else {
                            values.put("container", container);
                        }
                        values.put("screen", a.getString(2));
                        values.put("cellX", a.getString(3));
                        values.put("cellY", a.getString(4));
                        if (TAG_FAVORITE.equals(name)) {
                            added = addAppShortcut(db, values, a, packageManager, intent);
                        } else if (TAG_SEARCH.equals(name)) {
                            added = addSearchWidget(db, values);
                        } else if (TAG_CLOCK.equals(name)) {
                            added = addClockWidget(db, values);
                        } else if (TAG_APPWIDGET.equals(name)) {
                            added = addAppWidget(db, values, a, packageManager);
                        } else if (TAG_SHORTCUT.equals(name)) {
                            added = addUriShortcut(db, values, a);
                        }
                        if (added) {
                            i++;
                        }
                        a.recycle();
                    }
                }
            } catch (XmlPullParserException e) {
                Log.w(LauncherProvider.LOG_TAG, "Got exception parsing favorites.", e);
            } catch (IOException e2) {
                Log.w(LauncherProvider.LOG_TAG, "Got exception parsing favorites.", e2);
            }
            return i;
        }

        private boolean addAppShortcut(SQLiteDatabase db, ContentValues contentValues, TypedArray typedArray, PackageManager packageManager, Intent intent) {
            ComponentName componentName = new ComponentName(typedArray.getString(1), typedArray.getString(0));
            try {
                ActivityInfo activityInfo = packageManager.getActivityInfo(componentName, 0);
                intent.setComponent(componentName);
                intent.setFlags(270532608);
                contentValues.put(LauncherSettings.BaseLauncherColumns.INTENT, intent.toUri(0));
                contentValues.put(LauncherSettings.BaseLauncherColumns.TITLE, activityInfo.loadLabel(packageManager).toString());
                contentValues.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, 0);
                contentValues.put("spanX", 1);
                contentValues.put("spanY", 1);
                db.insert(TAG_FAVORITES, (String) null, contentValues);
                return LauncherProvider.LOGD;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        private ComponentName getSearchWidgetProvider() {
            return null;
        }

        private boolean addSearchWidget(SQLiteDatabase db, ContentValues values) {
            return addAppWidget(db, values, getSearchWidgetProvider(), 4, 1);
        }

        private boolean addClockWidget(SQLiteDatabase db, ContentValues values) {
            return addAppWidget(db, values, new ComponentName("com.android.alarmclock", "com.android.alarmclock.AnalogAppWidgetProvider"), 2, 2);
        }

        private boolean addAppWidget(SQLiteDatabase db, ContentValues contentValues, TypedArray typedArray, PackageManager packageManager) {
            String packageName = typedArray.getString(1);
            String className = typedArray.getString(0);
            if (packageName == null || className == null) {
                return false;
            }
            ComponentName componentName = new ComponentName(packageName, className);
            try {
                packageManager.getReceiverInfo(componentName, 0);
            } catch (Exception e) {
            }
            if (1 == 0) {
                return false;
            }
            return addAppWidget(db, contentValues, componentName, typedArray.getInt(5, 0), typedArray.getInt(6, 0));
        }

        private boolean bindAppWidgetId(AppWidgetManager appWidgetManager, int appWidgetId,
                ComponentName componentName) {
            return appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, componentName);
        }

        private boolean addAppWidget(SQLiteDatabase db, ContentValues contentValues, ComponentName componentName, int spanX, int spanY) {
            boolean allocatedAppWidgets = false;
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
            try {
                int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                contentValues.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, 4);
                contentValues.put("spanX", Integer.valueOf(spanX));
                contentValues.put("spanY", Integer.valueOf(spanY));
                contentValues.put("appWidgetId", Integer.valueOf(appWidgetId));
                db.insert(TAG_FAVORITES, (String) null, contentValues);
                allocatedAppWidgets = LauncherProvider.LOGD;
                if (!bindAppWidgetId(appWidgetManager, appWidgetId, componentName)) {
                    Log.w(LauncherProvider.LOG_TAG, "App widget binding was not allowed for " + componentName);
                }
                return LauncherProvider.LOGD;
            } catch (RuntimeException ex) {
                Log.e(LauncherProvider.LOG_TAG, "Problem allocating appWidgetId", ex);
                return allocatedAppWidgets;
            }
        }

        private boolean addUriShortcut(SQLiteDatabase db, ContentValues values, TypedArray a) {
            Resources resources = this.mContext.getResources();
            int iconResId = a.getResourceId(7, 0);
            int titleResId = a.getResourceId(8, 0);
            String uri = null;
            try {
                uri = a.getString(9);
                Intent intent = Intent.parseUri(uri, 0);
                if (iconResId == 0 || titleResId == 0) {
                    Log.w(LauncherProvider.LOG_TAG, "Shortcut is missing title or icon resource ID");
                    return false;
                }
                intent.setFlags(268435456);
                values.put(LauncherSettings.BaseLauncherColumns.INTENT, intent.toUri(0));
                values.put(LauncherSettings.BaseLauncherColumns.TITLE, resources.getString(titleResId));
                values.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, 1);
                values.put("spanX", 1);
                values.put("spanY", 1);
                values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE, 0);
                values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, this.mContext.getPackageName());
                values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE, resources.getResourceName(iconResId));
                db.insert(TAG_FAVORITES, (String) null, values);
                return LauncherProvider.LOGD;
            } catch (URISyntaxException e) {
                Log.w(LauncherProvider.LOG_TAG, "Shortcut has malformed uri: " + uri);
                return false;
            }
        }
    }

    static String buildOrWhereString(String column, int[] values) {
        StringBuilder selectWhere = new StringBuilder();
        for (int i = values.length - 1; i >= 0; i--) {
            selectWhere.append(column).append("=").append(values[i]);
            if (i > 0) {
                selectWhere.append(" OR ");
            }
        }
        return selectWhere.toString();
    }

    static class SqlArguments {
        public final String[] args;
        public final String table;
        public final String where;

        SqlArguments(Uri url, String where2, String[] args2) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where2;
                this.args = args2;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where2)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = null;
                this.args = null;
                return;
            }
            throw new IllegalArgumentException("Invalid URI: " + url);
        }
    }
}
