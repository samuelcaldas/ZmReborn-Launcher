package org.zmreborn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;

// TODO(move): belongs in util/
/**
 * StateListDrawable that highlights pressed and focused states with Material You colors.
 */
public final class SelectorDrawable extends StateListDrawable {
    private static final float CORNER_RADIUS_ROUNDED = 6.0f;
    private static final float CORNER_RADIUS_OBLONG = 999.0f;

    private SelectorDrawable(int pressedColor, int focusedColor, float cornerRadius) {
        SelectorShapeDrawable pressedDrawable = new SelectorShapeDrawable(pressedColor, cornerRadius);
        SelectorShapeDrawable focusedDrawable = new SelectorShapeDrawable(focusedColor, cornerRadius);
        SelectorShapeDrawable transparentDrawable = new SelectorShapeDrawable(0, 0.0f);
        addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        addState(new int[]{-android.R.attr.state_focused}, transparentDrawable);
        addState(new int[]{android.R.attr.state_focused}, focusedDrawable);
        addState(new int[]{-android.R.attr.state_focused}, transparentDrawable);
    }

    /**
     * Creates a selector drawable using the current Material You palette.
     */
    static SelectorDrawable createSelector(Context context, boolean roundCorners) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        int pressed = context.getColor(R.color.m3_primary);
        int focused = context.getColor(R.color.m3_primary_container);
        return new SelectorDrawable(pressed, focused, roundCorners ? CORNER_RADIUS_ROUNDED : 0.0f);
    }

    /**
     * Creates a fully-rounded OneUI-style pill selector for the drawer-open dock button.
     */
    static SelectorDrawable createOblongSelector(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        int pressed = context.getColor(R.color.m3_primary);
        int focused = context.getColor(R.color.m3_primary_container);
        return new SelectorDrawable(pressed, focused, CORNER_RADIUS_OBLONG);
    }

    private static final class SelectorShapeDrawable extends ShapeDrawable {
        private final Paint strokePaint;

        SelectorShapeDrawable(int color, float cornerRadius) {
            if (cornerRadius > 0.0f) {
                float[] radius = {cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius, cornerRadius, cornerRadius};
                setShape(new RoundRectShape(radius, new RectF(0.0f, 0.0f, 0.0f, 0.0f), radius));
            } else {
                setShape(new RectShape());
            }
            this.strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.strokePaint.setColor(color);
        }

        @Override
        protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
            shape.draw(canvas, this.strokePaint);
            paint.setColor(0);
            shape.draw(canvas, paint);
        }
    }
}
