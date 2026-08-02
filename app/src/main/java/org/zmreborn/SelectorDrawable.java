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
/** StateListDrawable that highlights pressed and focused states with Material You colors. */
public final class SelectorDrawable extends StateListDrawable {
    private static final float CORNER_RADIUS_ROUNDED = 6.0f;
    private static final float CORNER_RADIUS_OBLONG = 999.0f;

    private SelectorDrawable(int pressedColor, int focusedColor, float cornerRadius) {
        SelectorShapeDrawable pressedSelectorShapeDrawable = new SelectorShapeDrawable(pressedColor, cornerRadius);
        SelectorShapeDrawable focusedSelectorShapeDrawable = new SelectorShapeDrawable(focusedColor, cornerRadius);
        SelectorShapeDrawable transparentSelectorShapeDrawable = new SelectorShapeDrawable(this, 0, (SelectorShapeDrawable) null);
        addState(new int[]{16842919}, pressedSelectorShapeDrawable);
        addState(new int[]{-16842908}, transparentSelectorShapeDrawable);
        addState(new int[]{16842909}, focusedSelectorShapeDrawable);
        addState(new int[]{-16842909}, transparentSelectorShapeDrawable);
    }

    /** Creates a selector drawable using the current Material You palette. */
    static SelectorDrawable createSelector(Context context, boolean roundCorners) {
        int pressed = context.getResources().getColor(R.color.m3_primary);
        int focused = context.getResources().getColor(R.color.m3_primary_container);
        return new SelectorDrawable(pressed, focused, roundCorners ? CORNER_RADIUS_ROUNDED : 0.0f);
    }

    /** Creates a fully-rounded OneUI-style pill selector for the drawer-open dock button. */
    static SelectorDrawable createOblongSelector(Context context) {
        int pressed = context.getResources().getColor(R.color.m3_primary);
        int focused = context.getResources().getColor(R.color.m3_primary_container);
        return new SelectorDrawable(pressed, focused, CORNER_RADIUS_OBLONG);
    }

    private class SelectorShapeDrawable extends ShapeDrawable {
        private Paint mStrokePaint;

        /* synthetic */ SelectorShapeDrawable(SelectorDrawable selectorDrawable, int i, SelectorShapeDrawable selectorShapeDrawable) {
            this(selectorDrawable, i);
        }

        private SelectorShapeDrawable(SelectorDrawable selectorDrawable, int color) {
            this(color, 0.0f);
        }

        private SelectorShapeDrawable(int color, float cornerRadius) {
            if (cornerRadius > 0.0f) {
                float[] radius = {cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius, cornerRadius, cornerRadius};
                setShape(new RoundRectShape(radius, new RectF(0.0f, 0.0f, 0.0f, 0.0f), radius));
            } else {
                setShape(new RectShape());
            }
            this.mStrokePaint = new Paint(1);
            this.mStrokePaint.setColor(color);
        }

        /* access modifiers changed from: protected */
        public void onDraw(Shape shape, Canvas canvas, Paint paint) {
            shape.draw(canvas, this.mStrokePaint);
            paint.setColor(0);
            shape.draw(canvas, paint);
        }
    }
}
