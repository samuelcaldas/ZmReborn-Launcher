package org.zmreborn;

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

/**
 * Container view displaying contents of a live folder as an interactive grid or list.
 */
public class LiveFolder extends Folder {
    private AsyncTask<LiveFolderInfo, Void, Cursor> loadingTask;

    /**
     * Constructs a live folder view with context and XML attributes.
     */
    public LiveFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    static LiveFolder fromXml(Context context, FolderInfo folderInfo) {
        return (LiveFolder) LayoutInflater.from(context).inflate(
                isDisplayModeList(folderInfo) ? R.layout.live_folder_list : R.layout.live_folder_grid, null);
    }

    private static boolean isDisplayModeList(FolderInfo folderInfo) {
        return ((LiveFolderInfo) folderInfo).displayMode == 2;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LiveFolderAdapter.ViewHolder viewHolder = (LiveFolderAdapter.ViewHolder) view.getTag();
        if (viewHolder.useBaseIntent) {
            Intent baseIntent = ((LiveFolderInfo) this.folderInfo).baseIntent;
            if (baseIntent != null && baseIntent.getData() != null) {
                Intent intent = new Intent(baseIntent);
                intent.setData(baseIntent.getData().buildUpon().appendPath(Long.toString(viewHolder.id)).build());
                this.launcher.startActivitySafely(intent);
            }
        } else if (viewHolder.intent != null) {
            this.launcher.startActivitySafely(viewHolder.intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        return false;
    }

    @Override
    void bind(FolderInfo info) {
        super.bind(info);
        if (this.loadingTask != null && this.loadingTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.loadingTask.cancel(true);
        }
        this.loadingTask = new FolderLoadingTask(this).execute((LiveFolderInfo) info);
    }

    @Override
    void onOpen() {
        super.onOpen();
        requestFocus();
    }

    @Override
    void onClose() {
        super.onClose();
        if (this.loadingTask != null && this.loadingTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.loadingTask.cancel(true);
        }
        if (this.content != null && this.content.getAdapter() instanceof LiveFolderAdapter) {
            ((LiveFolderAdapter) this.content.getAdapter()).cleanup();
        }
    }

    static class FolderLoadingTask extends AsyncTask<LiveFolderInfo, Void, Cursor> {
        private final WeakReference<LiveFolder> folderRef;
        private LiveFolderInfo info;

        FolderLoadingTask(LiveFolder folder) {
            this.folderRef = new WeakReference<>(folder);
        }

        @Override
        protected Cursor doInBackground(LiveFolderInfo... params) {
            LiveFolder folder = this.folderRef.get();
            if (folder == null) {
                return null;
            }
            this.info = params[0];
            return LiveFolderAdapter.query(folder.launcher, this.info);
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            LiveFolder folder;
            if (!isCancelled()) {
                if (cursor != null && (folder = this.folderRef.get()) != null) {
                    folder.setContentAdapter(new LiveFolderAdapter(folder.launcher, this.info, cursor));
                }
            } else if (cursor != null) {
                cursor.close();
            }
        }
    }
}
