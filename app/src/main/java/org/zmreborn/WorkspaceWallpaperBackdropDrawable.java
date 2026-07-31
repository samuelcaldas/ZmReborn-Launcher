package org.zmreborn;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Draws a cached blurred wallpaper aligned with the workspace wallpaper position. */
final class WorkspaceWallpaperBackdropDrawable extends Drawable {
    private static final Paint FILTER_PAINT = createFilterPaint();
    private final WallpaperBackdrop backdrop;
    private final Workspace workspace;

    WorkspaceWallpaperBackdropDrawable(Workspace workspace, View target, Bitmap bitmap) {
        if (workspace == null || target == null || bitmap == null) {
            throw new IllegalArgumentException("Workspace backdrop inputs must not be null");
        }
        this.workspace = workspace;
        this.backdrop = new WallpaperBackdrop(target, bitmap);
    }

    @Override
    public void draw(Canvas canvas) {
        this.workspace.drawWallpaperBackdrop(canvas, getBounds(), this.backdrop.target,
                this.backdrop.bitmap, FILTER_PAINT);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private static Paint createFilterPaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setDither(true);
        return paint;
    }

    private static final class WallpaperBackdrop {
        private final Bitmap bitmap;
        private final View target;

        private WallpaperBackdrop(View target, Bitmap bitmap) {
            this.target = target;
            this.bitmap = bitmap;
        }
    }
}
