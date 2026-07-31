package org.zmreborn;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import org.zmreborn.theme.WallpaperBackdropRenderer;

/** Generates, caches, and dispatches best-effort wallpaper blur off the main thread. */
final class WallpaperBlurGenerator {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private final WallpaperBlurState state = new WallpaperBlurState();
    private final Workspace workspace;
    private Future<?> pendingTask;

    WallpaperBlurGenerator(Workspace workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("Workspace must not be null");
        }
        this.workspace = workspace;
    }

    void refresh() {
        Bitmap wallpaper = this.workspace.getBlurWallpaperSource();
        if (wallpaper == null) {
            cancelPendingTask();
            clearAndApply();
            return;
        }
        if (this.state.matches(wallpaper)) {
            this.workspace.applyBackgroundEffectsFromBlur();
            return;
        }
        cancelPendingTask();
        int generation = this.state.begin(wallpaper);
        this.workspace.applyBackgroundEffectsFromBlur();
        renderAsync(wallpaper, generation);
    }

    void destroy() {
        cancelPendingTask();
        this.state.clear();
    }

    Drawable createBackdrop(View target) {
        Bitmap blurred = this.state.getBlurred();
        if (blurred == null || blurred.isRecycled() || this.workspace.usesLiveWallpaper()) {
            return null;
        }
        return new WorkspaceWallpaperBackdropDrawable(this.workspace, target, blurred);
    }

    private void clearAndApply() {
        this.state.clear();
        this.workspace.applyBackgroundEffectsFromBlur();
    }

    private void renderAsync(final Bitmap wallpaper, final int generation) {
        this.pendingTask = Launcher.executeWallpaperRefresh(new Runnable() {
            public void run() {
                if (!state.isCurrent(generation, wallpaper)) {
                    return;
                }
                Bitmap rendered = render(wallpaper);
                dispatchResult(wallpaper, rendered, generation);
            }
        });
    }

    private void cancelPendingTask() {
        Future<?> task = this.pendingTask;
        this.pendingTask = null;
        if (task != null) {
            task.cancel(true);
        }
    }

    private Bitmap render(Bitmap wallpaper) {
        try {
            return WallpaperBackdropRenderer.render(wallpaper);
        } catch (CancellationException cancelled) {
            return null;
        } catch (IllegalArgumentException | IllegalStateException failure) {
            Log.w(Launcher.LOG_TAG, "Wallpaper blur unavailable", failure);
            return null;
        } catch (OutOfMemoryError failure) {
            Log.w(Launcher.LOG_TAG, "Wallpaper blur allocation failed", failure);
            return null;
        }
    }

    private void dispatchResult(Bitmap wallpaper, Bitmap rendered, int generation) {
        if (!this.state.isCurrent(generation, wallpaper)) {
            recycle(rendered);
            return;
        }
        if (rendered == null) {
            postFailure(wallpaper, generation);
            return;
        }
        postSuccess(wallpaper, rendered, generation);
    }

    private void postSuccess(final Bitmap wallpaper, final Bitmap rendered,
            final int generation) {
        boolean posted = MAIN_HANDLER.post(new Runnable() {
            public void run() {
                applySuccess(wallpaper, rendered, generation);
            }
        });
        if (!posted) {
            recycle(rendered);
        }
    }

    private void postFailure(final Bitmap wallpaper, final int generation) {
        MAIN_HANDLER.post(new Runnable() {
            public void run() {
                if (state.isCurrent(generation, wallpaper)) {
                    state.clear();
                }
            }
        });
    }

    private void applySuccess(Bitmap wallpaper, Bitmap rendered, int generation) {
        if (!this.state.isCurrent(generation, wallpaper)) {
            recycle(rendered);
            return;
        }
        this.state.complete(wallpaper, rendered);
        this.workspace.applyBackgroundEffectsFromBlur();
    }

    private static void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
