package org.zeam;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class DialogSeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    /* renamed from: ns */
    private static final String f2ns = "http://schemas.android.com/apk/res/org.zeam";
    private int mMax;
    private int mMin;
    private SeekBar mSeekBar;
    private String mSuffix;
    private int mValue = 0;
    private TextView mValueText;

    public DialogSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
        this.mSuffix = attrs.getAttributeValue(f2ns, "text");
        this.mMin = attrs.getAttributeIntValue(f2ns, "min", 0);
        this.mMax = attrs.getAttributeIntValue(f2ns, "max", 100);
        setDialogLayoutResource(R.layout.dialog_seekbar_preference);
    }

    /* access modifiers changed from: protected */
    public void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ((TextView) view.findViewById(R.id.dialog_message)).setText(getDialogMessage());
        this.mValueText = (TextView) view.findViewById(R.id.actual_value);
        this.mSeekBar = (SeekBar) view.findViewById(R.id.my_bar);
        this.mSeekBar.setOnSeekBarChangeListener(this);
        this.mSeekBar.setMax(this.mMax - this.mMin);
        this.mSeekBar.setProgress(this.mValue - this.mMin);
        String t = String.valueOf(this.mValue);
        TextView textView = this.mValueText;
        if (this.mSuffix != null) {
            t = t.concat(this.mSuffix);
        }
        textView.setText(t);
    }

    /* access modifiers changed from: protected */
    public Object onGetDefaultValue(TypedArray a, int index) {
        return Integer.valueOf(a.getInt(index, 0));
    }

    /* access modifiers changed from: protected */
    public void onSetInitialValue(boolean restore, Object defaultValue) {
        this.mValue = getPersistedInt(defaultValue == null ? 0 : ((Integer) defaultValue).intValue());
    }

    /* access modifiers changed from: protected */
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            int value = this.mSeekBar.getProgress();
            if (callChangeListener(Integer.valueOf(value))) {
                setValue(value);
            }
        }
    }

    public void setValue(int value) {
        int value2 = value + this.mMin;
        if (value2 > this.mMax) {
            value2 = this.mMax;
        } else if (value2 < 0) {
            value2 = 0;
        }
        this.mValue = value2;
        persistInt(value2);
    }

    public void setMax(int max) {
        this.mMax = max;
        if (this.mValue > this.mMax) {
            setValue(this.mMax);
        }
    }

    public void setMin(int min) {
        if (min < this.mMax) {
            this.mMin = min;
        }
    }

    public int getMin() {
        return this.mMin;
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        String t = String.valueOf(this.mMin + value);
        TextView textView = this.mValueText;
        if (this.mSuffix != null) {
            t = t.concat(this.mSuffix);
        }
        textView.setText(t);
    }

    public void onStartTrackingTouch(SeekBar seek) {
    }

    public void onStopTrackingTouch(SeekBar seek) {
    }
}
