package org.zeam;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import java.lang.ref.WeakReference;
import org.zeam.LiveFolderAdapter;

public class LiveFolder extends Folder {
    private AsyncTask<LiveFolderInfo, Void, Cursor> mLoadingTask;

    public LiveFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    static LiveFolder fromXml(Context context, FolderInfo folderInfo) {
        return (LiveFolder) LayoutInflater.from(context).inflate(isDisplayModeList(folderInfo) ? R.layout.live_folder_list : R.layout.live_folder_grid, (ViewGroup) null);
    }

    private static boolean isDisplayModeList(FolderInfo folderInfo) {
        return ((LiveFolderInfo) folderInfo).displayMode == 2;
    }

    public void onItemClick(AdapterView parent, View view, int position, long id) {
        LiveFolderAdapter.ViewHolder viewHolder = (LiveFolderAdapter.ViewHolder) view.getTag();
        if (viewHolder.useBaseIntent) {
            Intent baseIntent = ((LiveFolderInfo) this.mFolderInfo).baseIntent;
            if (baseIntent != null) {
                Intent intent = new Intent(baseIntent);
                intent.setData(baseIntent.getData().buildUpon().appendPath(Long.toString(viewHolder.f4id)).build());
                this.mLauncher.startActivitySafely(intent);
            }
        } else if (viewHolder.intent != null) {
            this.mLauncher.startActivitySafely(viewHolder.intent);
        }
    }

    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        return false;
    }

    /* access modifiers changed from: package-private */
    public void bind(FolderInfo info) {
        super.bind(info);
        if (this.mLoadingTask != null && this.mLoadingTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.mLoadingTask.cancel(true);
        }
        this.mLoadingTask = new FolderLoadingTask(this).execute(new LiveFolderInfo[]{(LiveFolderInfo) info});
    }

    /* access modifiers changed from: package-private */
    public void onOpen() {
        super.onOpen();
        requestFocus();
    }

    /* access modifiers changed from: package-private */
    public void onClose() {
        super.onClose();
        if (this.mLoadingTask != null && this.mLoadingTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.mLoadingTask.cancel(true);
        }
        LiveFolderAdapter adapter = (LiveFolderAdapter) this.mContent.getAdapter();
        if (adapter != null) {
            adapter.cleanup();
        }
    }

    static class FolderLoadingTask extends AsyncTask<LiveFolderInfo, Void, Cursor> {
        private final WeakReference<LiveFolder> mFolder;
        private LiveFolderInfo mInfo;

        FolderLoadingTask(LiveFolder folder) {
            this.mFolder = new WeakReference<>(folder);
        }

        /* access modifiers changed from: protected */
        public Cursor doInBackground(LiveFolderInfo... params) {
            LiveFolder folder = (LiveFolder) this.mFolder.get();
            if (folder == null) {
                return null;
            }
            this.mInfo = params[0];
            return LiveFolderAdapter.query(folder.mLauncher, this.mInfo);
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Cursor cursor) {
            LiveFolder folder;
            if (!isCancelled()) {
                if (cursor != null && (folder = (LiveFolder) this.mFolder.get()) != null) {
                    folder.setContentAdapter(new LiveFolderAdapter(folder.mLauncher, this.mInfo, cursor));
                }
            } else if (cursor != null) {
                cursor.close();
            }
        }
    }
}
