package org.zmreborn;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Dialog preference displaying an interactive seek bar with custom suffix formatting.
 */
public class DialogSeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String ATTRIBUTE_NAMESPACE = "http://schemas.android.com/apk/res-auto";

    private int max;
    private int min;
    private SeekBar seekBar;
    private String suffix;
    private int value;
    private TextView valueText;

    /**
     * Constructs a dialog seek bar preference with context and XML attributes.
     */
    public DialogSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
        this.suffix = attrs.getAttributeValue(ATTRIBUTE_NAMESPACE, "text");
        this.min = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "min", 0);
        this.max = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "max", 100);
        setDialogLayoutResource(R.layout.dialog_seekbar_preference);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ((TextView) view.findViewById(R.id.dialog_message)).setText(getDialogMessage());
        this.valueText = (TextView) view.findViewById(R.id.actual_value);
        this.seekBar = (SeekBar) view.findViewById(R.id.my_bar);
        this.seekBar.setOnSeekBarChangeListener(this);
        this.seekBar.setMax(Math.max(0, this.max - this.min));
        this.value = clampStoredValue(this.value);
        this.seekBar.setProgress(this.value - this.min);
        String text = String.valueOf(this.value);
        if (this.suffix != null) {
            text = text.concat(this.suffix);
        }
        this.valueText.setText(text);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        this.value = getPersistedInt(defaultValue == null ? 0 : ((Integer) defaultValue).intValue());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult && this.seekBar != null) {
            int progress = this.seekBar.getProgress();
            if (callChangeListener(progress)) {
                setValue(progress);
            }
        }
    }

    /**
     * Sets the preference value relative to the minimum bound.
     */
    public void setValue(int val) {
        setStoredValue(val + this.min);
    }

    /**
     * Sets the maximum value bound.
     */
    public void setMax(int maxVal) {
        this.max = Math.max(this.min, maxVal);
        if (this.value > this.max) {
            setStoredValue(this.max);
        }
    }

    /**
     * Sets the minimum value bound.
     */
    public void setMin(int minVal) {
        if (minVal < this.max) {
            this.min = minVal;
            if (this.value < this.min) {
                setStoredValue(this.min);
            }
        }
    }

    /**
     * Returns the minimum value bound.
     */
    public int getMin() {
        return this.min;
    }

    /**
     * Returns the maximum value bound.
     */
    public int getMax() {
        return this.max;
    }

    /**
     * Returns the currently stored value clamped between bounds.
     */
    public int getValue() {
        return clampStoredValue(this.value);
    }

    /**
     * Returns the formatted display string with suffix.
     */
    public String getDisplayValue() {
        String text = String.valueOf(getValue());
        if (this.suffix != null) {
            return text.concat(this.suffix);
        }
        return text;
    }

    private void setStoredValue(int val) {
        this.value = clampStoredValue(val);
        persistInt(this.value);
    }

    private int clampStoredValue(int val) {
        if (val < this.min) {
            return this.min;
        }
        if (val > this.max) {
            return this.max;
        }
        return val;
    }

    @Override
    public void onProgressChanged(SeekBar seek, int progress, boolean fromTouch) {
        String text = String.valueOf(this.min + progress);
        if (this.suffix != null) {
            text = text.concat(this.suffix);
        }
        if (this.valueText != null) {
            this.valueText.setText(text);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seek) {
    }
}
