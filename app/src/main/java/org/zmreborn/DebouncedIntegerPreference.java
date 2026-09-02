package org.zmreborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Stores a bounded integer preference after a short trailing debounce window.
 */
public abstract class DebouncedIntegerPreference extends Preference {
    static final long DEBOUNCE_MILLIS = 250L;
    private static final String ATTRIBUTE_NAMESPACE = "http://schemas.android.com/apk/res-auto";

    interface AdditionalValueWriter {
        void append(SharedPreferences.Editor editor, int value);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable flushRunnable = new Runnable() {
        public void run() {
            flushPendingValue();
        }
    };
    private AdditionalValueWriter additionalValueWriter;
    private int max;
    private int min;
    private int persistedValue;
    private int value;
    private boolean needsDurableFlush;

    DebouncedIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.min = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "min", 0);
        this.max = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "max", 100);
        validateRange(this.min, this.max);
        setPersistent(true);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray values, int index) {
        return values.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        int fallback = defaultValue == null ? 0 : ((Integer) defaultValue).intValue();
        int storedValue = getPersistedInt(fallback);
        this.value = clamp(storedValue);
        this.persistedValue = storedValue;
        this.needsDurableFlush = false;
        schedulePendingValue();
    }

    final int getValue() {
        return this.value;
    }

    final int getMin() {
        return this.min;
    }

    final int getMax() {
        return this.max;
    }

    final void setMin(int minimum) {
        setRange(minimum, this.max);
    }

    final void setMax(int maximum) {
        setRange(this.min, maximum);
    }

    final void setRange(int minimum, int maximum) {
        validateRange(minimum, maximum);
        this.min = minimum;
        this.max = maximum;
        int boundedValue = clamp(this.value);
        if (boundedValue == this.value) {
            notifyChanged();
            return;
        }
        this.value = boundedValue;
        schedulePendingValue();
        notifyChanged();
    }

    final void setValueFromRuntime(int value) {
        this.handler.removeCallbacks(this.flushRunnable);
        this.value = clamp(value);
        this.persistedValue = this.value;
        notifyChanged();
    }

    final void setAdditionalValueWriter(AdditionalValueWriter writer) {
        this.additionalValueWriter = writer;
    }

    final boolean submitUserValue(int value) {
        int boundedValue = clamp(value);
        if (boundedValue == this.value) {
            return false;
        }
        if (!callChangeListener(boundedValue)) {
            return false;
        }
        this.value = boundedValue;
        schedulePendingValue();
        return true;
    }

    final void flushPendingValue() {
        this.handler.removeCallbacks(this.flushRunnable);
        if (this.value == this.persistedValue) {
            return;
        }
        SharedPreferences.Editor editor = createEditor();
        appendCurrentValue(editor);
        editor.apply();
        this.persistedValue = this.value;
        this.needsDurableFlush = true;
    }

    final void flushPendingValueDurably() {
        this.handler.removeCallbacks(this.flushRunnable);
        if (this.value == this.persistedValue && !this.needsDurableFlush) {
            return;
        }
        SharedPreferences.Editor editor = createEditor();
        appendCurrentValue(editor);
        if (!editor.commit()) {
            throw new IllegalStateException("Unable to persist numeric preference: " + getKey());
        }
        this.persistedValue = this.value;
        this.needsDurableFlush = false;
    }

    final void cancelPendingValue() {
        this.handler.removeCallbacks(this.flushRunnable);
        this.value = clamp(this.persistedValue);
        this.needsDurableFlush = false;
        notifyChanged();
    }

    private void schedulePendingValue() {
        this.handler.removeCallbacks(this.flushRunnable);
        if (this.value == this.persistedValue) {
            return;
        }
        this.handler.postDelayed(this.flushRunnable, DEBOUNCE_MILLIS);
    }

    private SharedPreferences.Editor createEditor() {
        SharedPreferences preferences = getSharedPreferences();
        if (preferences == null || getKey() == null) {
            throw new IllegalStateException("Numeric preference is not attached to storage");
        }
        return preferences.edit();
    }

    private void appendCurrentValue(SharedPreferences.Editor editor) {
        editor.putInt(getKey(), this.value);
        if (this.additionalValueWriter != null) {
            this.additionalValueWriter.append(editor, this.value);
        }
    }

    private int clamp(int value) {
        return Math.max(this.min, Math.min(this.max, value));
    }

    private static void validateRange(int minimum, int maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    "Numeric preference minimum exceeds maximum: " + minimum + " > " + maximum);
        }
    }
}
