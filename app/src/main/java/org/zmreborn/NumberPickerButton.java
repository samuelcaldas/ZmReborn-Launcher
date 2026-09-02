package org.zmreborn;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.ImageButton;

/**
 * Custom ImageButton dedicated to NumberPicker increment and decrement actions with long-press repeat support.
 */
public class NumberPickerButton extends ImageButton {
    private NumberPicker numberPicker;

    /**
     * Constructs a NumberPickerButton with the given context, attributes, and style.
     */
    public NumberPickerButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Constructs a NumberPickerButton with the given context and XML attributes.
     */
    public NumberPickerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructs a NumberPickerButton with the given context.
     */
    public NumberPickerButton(Context context) {
        super(context);
    }

    /**
     * Sets the associated parent NumberPicker to receive touch and repeat events.
     */
    public void setNumberPicker(NumberPicker picker) {
        this.numberPicker = picker;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        cancelLongpressIfRequired(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        cancelLongpressIfRequired(event);
        return super.onTrackballEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            cancelLongpress();
        }
        return super.onKeyUp(keyCode, event);
    }

    private void cancelLongpressIfRequired(MotionEvent event) {
        if (event == null) {
            return;
        }
        int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            cancelLongpress();
        }
    }

    private void cancelLongpress() {
        if (this.numberPicker == null) {
            return;
        }
        if (R.id.increment == getId()) {
            this.numberPicker.cancelIncrement();
            return;
        }
        if (R.id.decrement == getId()) {
            this.numberPicker.cancelDecrement();
        }
    }
}
