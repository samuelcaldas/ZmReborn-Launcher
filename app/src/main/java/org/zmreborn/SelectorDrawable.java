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

public final class SelectorDrawable extends StateListDrawable {
    private SelectorDrawable(int pressedColor, int focusedColor, boolean roundCorners) {
        SelectorShapeDrawable pressedSelectorShapeDrawable = new SelectorShapeDrawable(this, pressedColor, roundCorners, (SelectorShapeDrawable) null);
        SelectorShapeDrawable focusedSelectorShapeDrawable = new SelectorShapeDrawable(this, focusedColor, roundCorners, (SelectorShapeDrawable) null);
        SelectorShapeDrawable transparentSelectorShapeDrawable = new SelectorShapeDrawable(this, 0, (SelectorShapeDrawable) null);
        addState(new int[]{16842919}, pressedSelectorShapeDrawable);
        addState(new int[]{-16842908}, transparentSelectorShapeDrawable);
        addState(new int[]{16842909}, focusedSelectorShapeDrawable);
        addState(new int[]{-16842909}, transparentSelectorShapeDrawable);
    }

    static SelectorDrawable createSelector(Context context, boolean roundCorners) {
        return new SelectorDrawable(PreferencesUtil.getSelectorPressedColorHex(context), PreferencesUtil.getSelectorFocusedColorHex(context), roundCorners);
    }

    private class SelectorShapeDrawable extends ShapeDrawable {
        private Paint mStrokePaint;

        /* synthetic */ SelectorShapeDrawable(SelectorDrawable selectorDrawable, int i, SelectorShapeDrawable selectorShapeDrawable) {
            this(selectorDrawable, i);
        }

        private SelectorShapeDrawable(SelectorDrawable selectorDrawable, int color) {
            this(color, false);
        }

        private SelectorShapeDrawable(int color, boolean roundCorners) {
            if (roundCorners) {
                float[] radius = {6.0f, 6.0f, 6.0f, 6.0f, 6.0f, 6.0f, 6.0f, 6.0f};
                setShape(new RoundRectShape(radius, new RectF(0.0f, 0.0f, 0.0f, 0.0f), radius));
            } else {
                setShape(new RectShape());
            }
            this.mStrokePaint = new Paint(1);
            this.mStrokePaint.setColor(color);
        }

        /* synthetic */ SelectorShapeDrawable(SelectorDrawable selectorDrawable, int i, boolean z, SelectorShapeDrawable selectorShapeDrawable) {
            this(i, z);
        }

        /* access modifiers changed from: protected */
        public void onDraw(Shape shape, Canvas canvas, Paint paint) {
            shape.draw(canvas, this.mStrokePaint);
            paint.setColor(0);
            shape.draw(canvas, paint);
        }
    }
}
