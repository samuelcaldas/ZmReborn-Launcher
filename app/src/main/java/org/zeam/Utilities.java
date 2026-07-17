package org.zeam;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;

final class Utilities {
    private static final Rect sBounds = new Rect();
    private static Canvas sCanvas = new Canvas();
    private static int sIconHeight = -1;
    private static int sIconWidth = -1;
    private static final Rect sOldBounds = new Rect();
    private static final Paint sPaint = new Paint();

    Utilities() {
    }

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
    }

    static Drawable createIconThumbnail(Drawable icon, Context context) {
        if (sIconWidth == -1) {
            int dimension = (int) context.getResources().getDimension(17104896);
            sIconHeight = dimension;
            sIconWidth = dimension;
        }
        int width = sIconWidth;
        int height = sIconHeight;
        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        } else if (icon instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            if (bitmapDrawable.getBitmap().getDensity() == 0) {
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
            }
        }
        int iconWidth = icon.getIntrinsicWidth();
        int iconHeight = icon.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            return icon;
        }
        if (width < iconWidth || height < iconHeight) {
            float ratio = ((float) iconWidth) / ((float) iconHeight);
            if (iconWidth > iconHeight) {
                height = (int) (((float) width) / ratio);
            } else if (iconHeight > iconWidth) {
                width = (int) (((float) height) * ratio);
            }
            Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, icon.getOpacity() != -1 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
            Canvas canvas = sCanvas;
            canvas.setBitmap(thumb);
            sOldBounds.set(icon.getBounds());
            int x = (sIconWidth - width) / 2;
            int y = (sIconHeight - height) / 2;
            icon.setBounds(x, y, x + width, y + height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
            return new FastBitmapDrawable(thumb);
        } else if (iconWidth >= width || iconHeight >= height) {
            return icon;
        } else {
            Bitmap thumb2 = Bitmap.createBitmap(sIconWidth, sIconHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas2 = sCanvas;
            canvas2.setBitmap(thumb2);
            sOldBounds.set(icon.getBounds());
            int x2 = (width - iconWidth) / 2;
            int y2 = (height - iconHeight) / 2;
            icon.setBounds(x2, y2, x2 + iconWidth, y2 + iconHeight);
            icon.draw(canvas2);
            icon.setBounds(sOldBounds);
            return new FastBitmapDrawable(thumb2);
        }
    }

    static Bitmap createBitmapThumbnail(Bitmap bitmap, Context context) {
        if (sIconWidth == -1) {
            int dimension = (int) context.getResources().getDimension(17104896);
            sIconHeight = dimension;
            sIconWidth = dimension;
        }
        int width = sIconWidth;
        int height = sIconHeight;
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        if (width > 0 && height > 0) {
            if (width < bitmapWidth || height < bitmapHeight) {
                float ratio = ((float) bitmapWidth) / ((float) bitmapHeight);
                if (bitmapWidth > bitmapHeight) {
                    height = (int) (((float) width) / ratio);
                } else if (bitmapHeight > bitmapWidth) {
                    width = (int) (((float) height) * ratio);
                }
                Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, (width == sIconWidth && height == sIconHeight) ? bitmap.getConfig() : Bitmap.Config.ARGB_8888);
                Canvas canvas = sCanvas;
                Paint paint = sPaint;
                canvas.setBitmap(thumb);
                paint.setDither(false);
                paint.setFilterBitmap(true);
                sBounds.set((sIconWidth - width) / 2, (sIconHeight - height) / 2, width, height);
                sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
                canvas.drawBitmap(bitmap, sOldBounds, sBounds, paint);
                return thumb;
            } else if (bitmapWidth < width || bitmapHeight < height) {
                Bitmap thumb2 = Bitmap.createBitmap(sIconWidth, sIconHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas2 = sCanvas;
                Paint paint2 = sPaint;
                canvas2.setBitmap(thumb2);
                paint2.setDither(false);
                paint2.setFilterBitmap(true);
                canvas2.drawBitmap(bitmap, (float) ((sIconWidth - bitmapWidth) / 2), (float) ((sIconHeight - bitmapHeight) / 2), paint2);
                return thumb2;
            }
        }
        return bitmap;
    }

    static Drawable createDockIconThumbnail(Drawable icon, Context context) {
        int dimension = (int) context.getResources().getDimension(17104896);
        sIconHeight = dimension;
        sIconWidth = dimension;
        int width = sIconWidth;
        int height = sIconHeight;
        float ratio = ((float) sIconHeight) / (((float) sIconHeight) * 1.225f);
        Bitmap originalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas1 = new Canvas();
        canvas1.setBitmap(originalBitmap);
        icon.setBounds(0, 0, width, height);
        icon.draw(canvas1);
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        Bitmap reflectionImage = Bitmap.createBitmap(originalBitmap, 0, height / 2, width, height / 2, matrix, false);
        Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (int) (((float) height) * 1.225f), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapWithReflection);
        canvas.drawBitmap(reflectionImage, 0.0f, (float) (height - 6), (Paint) null);
        Paint paint = new Paint();
        paint.setShader(new LinearGradient(0.0f, (float) originalBitmap.getHeight(), 0.0f, (float) bitmapWithReflection.getHeight(), 1895825407, 16777215, Shader.TileMode.CLAMP));
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0.0f, (float) (height - 6), (float) width, (float) bitmapWithReflection.getHeight(), paint);
        canvas.drawBitmap(originalBitmap, 0.0f, 0.0f, (Paint) null);
        originalBitmap.recycle();
        reflectionImage.recycle();
        return new FastBitmapDrawable(Bitmap.createScaledBitmap(bitmapWithReflection, Math.round(((float) sIconWidth) * ratio), sIconHeight, true));
    }

    static boolean canUninstallApplication(Context context, ApplicationItemInfo applicationItemInfo) {
        ActivityInfo activityInfo;
        ApplicationInfo applicationInfo;
        String sourceDir;
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(applicationItemInfo.intent, 0);
        if (resolveInfo == null || (activityInfo = resolveInfo.activityInfo) == null || (applicationInfo = activityInfo.applicationInfo) == null || (sourceDir = applicationInfo.sourceDir) == null || sourceDir.startsWith("/system")) {
            return false;
        }
        return true;
    }

    static Drawable overlayUninstallIcon(Context context, Drawable iconDrawable) {
        if (iconDrawable == null) {
            return null;
        }
        Bitmap iconBitmap = getMutableBitmap(iconDrawable);
        return iconBitmap != null ? new FastBitmapDrawable(addOverlay(iconBitmap, ((BitmapDrawable) context.getResources().getDrawable(R.drawable.overlay_uninstall)).getBitmap())) : iconDrawable;
    }

    static Drawable adjustIconOpacity(Drawable iconDrawable) {
        return new FastBitmapDrawable(adjustOpacity(getMutableBitmap(iconDrawable), 95));
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
        Paint paint = new Paint(2);
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
