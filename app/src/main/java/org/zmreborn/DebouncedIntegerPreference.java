package org.zmreborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.util.AttributeSet;

/** Stores a bounded integer after a short trailing debounce. */
public abstract class DebouncedIntegerPreference extends Preference {
    static final long DEBOUNCE_MILLIS = 250L;
    private static final String ATTRIBUTE_NAMESPACE = "http://schemas.android.com/apk/res-auto";

    interface AdditionalValueWriter {
        void append(SharedPreferences.Editor editor, int value);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mFlushRunnable = new Runnable() {
        public void run() {
            flushPendingValue();
        }
    };
    private AdditionalValueWriter mAdditionalValueWriter;
    private int mMax;
    private int mMin;
    private int mPersistedValue;
    private int mValue;
    private boolean mNeedsDurableFlush;

    DebouncedIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMin = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "min", 0);
        mMax = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "max", 100);
        validateRange(mMin, mMax);
        setPersistent(true);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray values, int index) {
        return Integer.valueOf(values.getInt(index, 0));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        int fallback = defaultValue == null ? 0 : ((Integer) defaultValue).intValue();
        int storedValue = getPersistedInt(fallback);
        mValue = clamp(storedValue);
        mPersistedValue = storedValue;
        mNeedsDurableFlush = false;
        schedulePendingValue();
    }

    final int getValue() {
        return mValue;
    }

    final int getMin() {
        return mMin;
    }

    final int getMax() {
        return mMax;
    }

    final void setMin(int minimum) {
        setRange(minimum, mMax);
    }

    final void setMax(int maximum) {
        setRange(mMin, maximum);
    }

    final void setRange(int minimum, int maximum) {
        validateRange(minimum, maximum);
        mMin = minimum;
        mMax = maximum;
        int boundedValue = clamp(mValue);
        if (boundedValue == mValue) {
            notifyChanged();
            return;
        }
        mValue = boundedValue;
        schedulePendingValue();
        notifyChanged();
    }

    final void setValueFromRuntime(int value) {
        mHandler.removeCallbacks(mFlushRunnable);
        mValue = clamp(value);
        mPersistedValue = mValue;
        notifyChanged();
    }

    final void setAdditionalValueWriter(AdditionalValueWriter writer) {
        mAdditionalValueWriter = writer;
    }

    final boolean submitUserValue(int value) {
        int boundedValue = clamp(value);
        if (boundedValue == mValue) {
            return false;
        }
        if (!callChangeListener(Integer.valueOf(boundedValue))) {
            return false;
        }
        mValue = boundedValue;
        schedulePendingValue();
        return true;
    }

    final void flushPendingValue() {
        mHandler.removeCallbacks(mFlushRunnable);
        if (mValue == mPersistedValue) {
            return;
        }
        SharedPreferences.Editor editor = createEditor();
        appendCurrentValue(editor);
        editor.apply();
        mPersistedValue = mValue;
        mNeedsDurableFlush = true;
    }

    final void flushPendingValueDurably() {
        mHandler.removeCallbacks(mFlushRunnable);
        if (mValue == mPersistedValue && !mNeedsDurableFlush) {
            return;
        }
        SharedPreferences.Editor editor = createEditor();
        appendCurrentValue(editor);
        if (!editor.commit()) {
            throw new IllegalStateException("Unable to persist numeric preference: " + getKey());
        }
        mPersistedValue = mValue;
        mNeedsDurableFlush = false;
    }

    final void cancelPendingValue() {
        mHandler.removeCallbacks(mFlushRunnable);
        mValue = clamp(mPersistedValue);
        mNeedsDurableFlush = false;
        notifyChanged();
    }

    private void schedulePendingValue() {
        mHandler.removeCallbacks(mFlushRunnable);
        if (mValue == mPersistedValue) {
            return;
        }
        mHandler.postDelayed(mFlushRunnable, DEBOUNCE_MILLIS);
    }

    private SharedPreferences.Editor createEditor() {
        SharedPreferences preferences = getSharedPreferences();
        if (preferences == null || getKey() == null) {
            throw new IllegalStateException("Numeric preference is not attached to storage");
        }
        return preferences.edit();
    }

    private void appendCurrentValue(SharedPreferences.Editor editor) {
        editor.putInt(getKey(), mValue);
        if (mAdditionalValueWriter != null) {
            mAdditionalValueWriter.append(editor, mValue);
        }
    }

    private int clamp(int value) {
        return Math.max(mMin, Math.min(mMax, value));
    }

    private static void validateRange(int minimum, int maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    "Numeric preference minimum exceeds maximum: " + minimum + " > " + maximum);
        }
    }
}
