package org.zmreborn;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.widget.TextView;
import org.zmreborn.compat.AdaptiveIconCompat;

/**
 * Common graphics, icon normalization, thumbnail rendering, and package utilities.
 */
final class Utilities {
    private static final Rect BOUNDS = new Rect();
    private static final Rect OLD_BOUNDS = new Rect();
    private static final Paint PAINT = new Paint();
    private static final Canvas CANVAS = new Canvas();
    private static int sIconHeight = -1;
    private static int sIconWidth = -1;

    private Utilities() {
    }

    static {
        CANVAS.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
    }

    private static synchronized void ensureIconSize(Context context) {
        if (sIconWidth > 0 && sIconHeight > 0) {
            return;
        }
        int dimension = Math.max(1, (int) context.getResources().getDimension(android.R.dimen.app_icon_size));
        sIconHeight = dimension;
        sIconWidth = dimension;
    }

    /**
     * Returns a normalized application icon drawable suitable for display.
     */
    static Drawable normalizeApplicationIcon(Drawable icon, Context context) {
        Drawable resolvedIcon = icon;
        if (resolvedIcon == null) {
            resolvedIcon = context.getDrawable(R.drawable.ic_launcher_application);
        }
        if (AdaptiveIconCompat.isAdaptiveIcon(resolvedIcon)) {
            return resolvedIcon;
        }
        return createIconThumbnail(resolvedIcon, context);
    }

    /**
     * Sets the application icon as the top compound drawable on the provided TextView and returns the normalized icon.
     */
    static Drawable setCompoundApplicationIcon(TextView view, Drawable icon, Context context) {
        Drawable resolvedIcon = normalizeApplicationIcon(icon, context);
        ensureIconSize(context);
        if (view != null) {
            Drawable boundIcon = copyDrawable(resolvedIcon, context);
            boundIcon.setBounds(0, 0, sIconWidth, sIconHeight);
            view.setCompoundDrawables(null, boundIcon, null, null);
        }
        return resolvedIcon;
    }

    /**
     * Sets the top compound drawable for an icon view, scaling to standard launcher icon dimensions.
     */
    public static void setCompoundDrawables(TextView view, Drawable icon, Context context) {
        if (view == null) {
            return;
        }
        Drawable resolvedIcon = icon != null ? icon : context.getDrawable(R.drawable.ic_launcher_application);
        ensureIconSize(context);
        Drawable boundIcon = copyDrawable(resolvedIcon, context);
        boundIcon.setBounds(0, 0, sIconWidth, sIconHeight);
        view.setCompoundDrawables(null, boundIcon, null, null);
    }

    /**
     * Generates a scaled thumbnail drawable matching launcher icon dimensions.
     */
    static Drawable createIconThumbnail(Drawable icon, Context context) {
        if (icon == null) {
            return normalizeApplicationIcon(null, context);
        }
        if (AdaptiveIconCompat.isAdaptiveIcon(icon)) {
            return icon;
        }
        ensureIconSize(context);
        int targetWidth = sIconWidth;
        int targetHeight = sIconHeight;
        prepareDrawableForThumbnail(icon, context, targetWidth, targetHeight);

        int intrinsicWidth = icon.getIntrinsicWidth();
        int intrinsicHeight = icon.getIntrinsicHeight();
        if (targetWidth <= 0 || targetHeight <= 0) {
            return icon;
        }
        if (targetWidth < intrinsicWidth || targetHeight < intrinsicHeight) {
            return scaleDownIconThumbnail(icon, intrinsicWidth, intrinsicHeight, targetWidth, targetHeight);
        }
        if (intrinsicWidth >= targetWidth || intrinsicHeight >= targetHeight) {
            return icon;
        }
        return centerIconThumbnail(icon, intrinsicWidth, intrinsicHeight, targetWidth, targetHeight);
    }

    private static void prepareDrawableForThumbnail(Drawable icon, Context context, int targetWidth, int targetHeight) {
        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(targetWidth);
            painter.setIntrinsicHeight(targetHeight);
            return;
        }
        if (icon instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            if (bitmapDrawable.getBitmap().getDensity() == 0) {
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
            }
        }
    }

    private static synchronized Drawable scaleDownIconThumbnail(
            Drawable icon, int intrinsicWidth, int intrinsicHeight, int targetWidth, int targetHeight) {
        float ratio = ((float) intrinsicWidth) / ((float) intrinsicHeight);
        int scaledWidth = targetWidth;
        int scaledHeight = targetHeight;
        if (intrinsicWidth > intrinsicHeight) {
            scaledHeight = (int) (((float) targetWidth) / ratio);
        } else if (intrinsicHeight > intrinsicWidth) {
            scaledWidth = (int) (((float) targetHeight) * ratio);
        }
        Bitmap thumbnail = Bitmap.createBitmap(
                sIconWidth, sIconHeight,
                icon.getOpacity() != -1 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        CANVAS.setBitmap(thumbnail);
        OLD_BOUNDS.set(icon.getBounds());
        int left = (sIconWidth - scaledWidth) / 2;
        int top = (sIconHeight - scaledHeight) / 2;
        icon.setBounds(left, top, left + scaledWidth, top + scaledHeight);
        icon.draw(CANVAS);
        icon.setBounds(OLD_BOUNDS);
        return new FastBitmapDrawable(thumbnail);
    }

    private static synchronized Drawable centerIconThumbnail(
            Drawable icon, int intrinsicWidth, int intrinsicHeight, int targetWidth, int targetHeight) {
        Bitmap thumbnail = Bitmap.createBitmap(sIconWidth, sIconHeight, Bitmap.Config.ARGB_8888);
        CANVAS.setBitmap(thumbnail);
        OLD_BOUNDS.set(icon.getBounds());
        int left = (targetWidth - intrinsicWidth) / 2;
        int top = (targetHeight - intrinsicHeight) / 2;
        icon.setBounds(left, top, left + intrinsicWidth, top + intrinsicHeight);
        icon.draw(CANVAS);
        icon.setBounds(OLD_BOUNDS);
        return new FastBitmapDrawable(thumbnail);
    }

    /**
     * Creates a scaled bitmap thumbnail conforming to standard icon dimensions.
     */
    static synchronized Bitmap createBitmapThumbnail(Bitmap bitmap, Context context) {
        ensureIconSize(context);
        int targetWidth = sIconWidth;
        int targetHeight = sIconHeight;
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        if (targetWidth <= 0 || targetHeight <= 0) {
            return bitmap;
        }
        if (targetWidth < bitmapWidth || targetHeight < bitmapHeight) {
            float ratio = ((float) bitmapWidth) / ((float) bitmapHeight);
            int scaledWidth = targetWidth;
            int scaledHeight = targetHeight;
            if (bitmapWidth > bitmapHeight) {
                scaledHeight = (int) (((float) targetWidth) / ratio);
            } else if (bitmapHeight > bitmapWidth) {
                scaledWidth = (int) (((float) targetHeight) * ratio);
            }
            Bitmap thumbnail = Bitmap.createBitmap(
                    sIconWidth, sIconHeight,
                    (scaledWidth == sIconWidth && scaledHeight == sIconHeight) ? bitmap.getConfig() : Bitmap.Config.ARGB_8888);
            CANVAS.setBitmap(thumbnail);
            PAINT.setDither(false);
            PAINT.setFilterBitmap(true);
            BOUNDS.set((sIconWidth - scaledWidth) / 2, (sIconHeight - scaledHeight) / 2, scaledWidth, scaledHeight);
            OLD_BOUNDS.set(0, 0, bitmapWidth, bitmapHeight);
            CANVAS.drawBitmap(bitmap, OLD_BOUNDS, BOUNDS, PAINT);
            return thumbnail;
        }
        if (bitmapWidth < targetWidth || bitmapHeight < targetHeight) {
            Bitmap thumbnail = Bitmap.createBitmap(sIconWidth, sIconHeight, Bitmap.Config.ARGB_8888);
            CANVAS.setBitmap(thumbnail);
            PAINT.setDither(false);
            PAINT.setFilterBitmap(true);
            CANVAS.drawBitmap(bitmap, (float) ((sIconWidth - bitmapWidth) / 2), (float) ((sIconHeight - bitmapHeight) / 2), PAINT);
            return thumbnail;
        }
        return bitmap;
    }

    /**
     * Creates a dock thumbnail drawable for adaptive or legacy icons.
     */
    static Drawable createDockIconThumbnail(Drawable icon, Context context) {
        Drawable normalizedIcon = normalizeApplicationIcon(icon, context);
        if (!AdaptiveIconCompat.isAdaptiveIcon(normalizedIcon)) {
            return normalizedIcon;
        }
        ensureIconSize(context);
        return rasterizeDrawableCopy(normalizedIcon);
    }

    /**
     * Determines whether the given application can be uninstalled by the user.
     */
    static boolean canUninstallApplication(Context context, ApplicationItemInfo applicationItemInfo) {
        if (context == null || applicationItemInfo == null || applicationItemInfo.intent == null) {
            return false;
        }
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(applicationItemInfo.intent, 0);
        if (resolveInfo == null) {
            return false;
        }
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        if (activityInfo == null) {
            return false;
        }
        ApplicationInfo applicationInfo = activityInfo.applicationInfo;
        if (applicationInfo == null) {
            return false;
        }
        String sourceDir = applicationInfo.sourceDir;
        return sourceDir != null && !sourceDir.startsWith("/system");
    }

    /**
     * Overlays an uninstall badge on top of the given icon drawable.
     */
    static Drawable overlayUninstallIcon(Context context, Drawable iconDrawable) {
        Drawable normalizedIcon = normalizeApplicationIcon(iconDrawable, context);
        Bitmap iconBitmap = getMutableBitmap(normalizedIcon);
        Drawable overlay = context.getDrawable(R.drawable.overlay_uninstall);
        if (iconBitmap != null && overlay instanceof BitmapDrawable) {
            Bitmap overlayBitmap = ((BitmapDrawable) overlay).getBitmap();
            return new FastBitmapDrawable(addOverlay(iconBitmap, overlayBitmap));
        }
        return new LayerDrawable(new Drawable[]{
                copyDrawable(normalizedIcon, context), overlay});
    }

    /**
     * Adjusts the opacity of an icon drawable for disabled or hidden states.
     */
    static Drawable adjustIconOpacity(Drawable iconDrawable) {
        if (iconDrawable == null) {
            return null;
        }
        Bitmap iconBitmap = getMutableBitmap(iconDrawable);
        if (iconBitmap != null) {
            return new FastBitmapDrawable(adjustOpacity(iconBitmap, 95));
        }
        Drawable fadedIcon = copyDrawable(iconDrawable);
        fadedIcon.setAlpha(95);
        return fadedIcon;
    }

    private static Drawable copyDrawable(Drawable drawable, Context context) {
        Drawable.ConstantState constantState = drawable.getConstantState();
        if (constantState != null) {
            Drawable copy = constantState.newDrawable(context.getResources()).mutate();
            copyDrawableProperties(drawable, copy);
            return copy;
        }
        if (drawable instanceof FastBitmapDrawable) {
            return new FastBitmapDrawable(((FastBitmapDrawable) drawable).getBitmap());
        }
        return rasterizeDrawableCopy(drawable);
    }

    private static void copyDrawableProperties(Drawable source, Drawable destination) {
        destination.setAlpha(source.getAlpha());
        destination.setColorFilter(source.getColorFilter());
        destination.setLevel(source.getLevel());
        destination.setState(source.getState());
        destination.setVisible(source.isVisible(), false);
    }

    private static Drawable rasterizeDrawableCopy(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(sIconWidth, sIconHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Rect oldBounds = new Rect(drawable.getBounds());
        try {
            drawable.setBounds(0, 0, sIconWidth, sIconHeight);
            drawable.draw(canvas);
        } finally {
            drawable.setBounds(oldBounds);
        }
        return new FastBitmapDrawable(bitmap);
    }

    private static Drawable copyDrawable(Drawable drawable) {
        Drawable.ConstantState constantState = drawable.getConstantState();
        if (constantState == null) {
            return drawable.mutate();
        }
        return constantState.newDrawable().mutate();
    }

    private static Bitmap getMutableBitmap(Drawable iconDrawable) {
        Bitmap bitmap = null;
        if (iconDrawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) iconDrawable).getBitmap();
        } else if (iconDrawable instanceof FastBitmapDrawable) {
            bitmap = ((FastBitmapDrawable) iconDrawable).getBitmap();
        }
        if (bitmap != null) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        return null;
    }

    private static Bitmap addOverlay(Bitmap mutableBitmap, Bitmap overlay) {
        Bitmap bitmapOverlay = Bitmap.createBitmap(mutableBitmap.getWidth(), mutableBitmap.getHeight(), mutableBitmap.getConfig());
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        Canvas canvas = new Canvas(bitmapOverlay);
        canvas.drawBitmap(mutableBitmap, new Matrix(), paint);
        canvas.drawBitmap(overlay, new Matrix(), paint);
        return bitmapOverlay;
    }

    private static Bitmap adjustOpacity(Bitmap mutableBitmap, int opacity) {
        Canvas canvas = new Canvas();
        canvas.setBitmap(mutableBitmap);
        canvas.drawColor((opacity & 255) << 24, PorterDuff.Mode.DST_IN);
        return mutableBitmap;
    }
}
