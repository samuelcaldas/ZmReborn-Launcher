package org.zmreborn;

import android.content.Context;
import android.os.Handler;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NumberPicker extends LinearLayout implements View.OnClickListener, View.OnFocusChangeListener, View.OnLongClickListener {
    private static final int DEFAULT_MAX = 200;
    private static final int DEFAULT_MIN = 0;
    /* access modifiers changed from: private */
    public static final char[] DIGIT_CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    public static final Formatter TWO_DIGIT_FORMATTER = new Formatter() {
        final Object[] mArgs = new Object[1];
        final StringBuilder mBuilder = new StringBuilder();
        final java.util.Formatter mFmt = new java.util.Formatter(this.mBuilder);

        public String toString(int value) {
            this.mArgs[0] = Integer.valueOf(value);
            this.mBuilder.delete(0, this.mBuilder.length());
            this.mFmt.format("%02d", this.mArgs);
            return this.mFmt.toString();
        }
    };
    protected int mCurrent;
    /* access modifiers changed from: private */
    public boolean mDecrement;
    private NumberPickerButton mDecrementButton;
    /* access modifiers changed from: private */
    public String[] mDisplayedValues;
    protected int mEnd;
    private Formatter mFormatter;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mIncrement;
    private NumberPickerButton mIncrementButton;
    private OnChangedListener mListener;
    /* access modifiers changed from: private */
    public final InputFilter mNumberInputFilter;
    protected int mPrevious;
    private final Runnable mRunnable;
    /* access modifiers changed from: private */
    public long mSpeed;
    protected int mStart;
    private final EditText mText;

    public interface Formatter {
        String toString(int i);
    }

    public interface OnChangedListener {
        void onChanged(NumberPicker numberPicker, int i, int i2);
    }

    public NumberPicker(Context context) {
        this(context, (AttributeSet) null);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.mRunnable = new Runnable() {
            public void run() {
                if (NumberPicker.this.mIncrement) {
                    NumberPicker.this.changeCurrent(NumberPicker.this.mCurrent + 1);
                    NumberPicker.this.mHandler.postDelayed(this, NumberPicker.this.mSpeed);
                } else if (NumberPicker.this.mDecrement) {
                    NumberPicker.this.changeCurrent(NumberPicker.this.mCurrent - 1);
                    NumberPicker.this.mHandler.postDelayed(this, NumberPicker.this.mSpeed);
                }
            }
        };
        this.mSpeed = 300;
        setOrientation(1);
        ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.number_picker, this, true);
        this.mHandler = new Handler();
        InputFilter inputFilter = new NumberPickerInputFilter(this, (NumberPickerInputFilter) null);
        this.mNumberInputFilter = new NumberRangeKeyListener(this, (NumberRangeKeyListener) null);
        this.mIncrementButton = (NumberPickerButton) findViewById(R.id.increment);
        this.mIncrementButton.setOnClickListener(this);
        this.mIncrementButton.setOnLongClickListener(this);
        this.mIncrementButton.setNumberPicker(this);
        this.mDecrementButton = (NumberPickerButton) findViewById(R.id.decrement);
        this.mDecrementButton.setOnClickListener(this);
        this.mDecrementButton.setOnLongClickListener(this);
        this.mDecrementButton.setNumberPicker(this);
        this.mText = (EditText) findViewById(R.id.timepicker_input);
        this.mText.setOnFocusChangeListener(this);
        this.mText.setFilters(new InputFilter[]{inputFilter});
        this.mText.setRawInputType(2);
        if (!isEnabled()) {
            setEnabled(false);
        }
        this.mStart = 0;
        this.mEnd = DEFAULT_MAX;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mIncrementButton.setEnabled(enabled);
        this.mDecrementButton.setEnabled(enabled);
        this.mText.setEnabled(enabled);
    }

    public void setOnChangeListener(OnChangedListener listener) {
        this.mListener = listener;
    }

    public void setFormatter(Formatter formatter) {
        this.mFormatter = formatter;
    }

    public void setRange(int start, int end) {
        this.mStart = start;
        this.mEnd = end;
        this.mCurrent = start;
        updateView();
    }

    public void setRange(int start, int end, String[] displayedValues) {
        this.mDisplayedValues = displayedValues;
        this.mStart = start;
        this.mEnd = end;
        this.mCurrent = start;
        updateView();
    }

    public void setCurrent(int current) {
        this.mCurrent = current;
        updateView();
    }

    public void setSpeed(long speed) {
        this.mSpeed = speed;
    }

    public void onClick(View v) {
        validateInput(this.mText);
        if (!this.mText.hasFocus()) {
            this.mText.requestFocus();
        }
        if (R.id.increment == v.getId()) {
            changeCurrent(this.mCurrent + 1);
        } else if (R.id.decrement == v.getId()) {
            changeCurrent(this.mCurrent - 1);
        }
    }

    private String formatNumber(int value) {
        return this.mFormatter != null ? this.mFormatter.toString(value) : String.valueOf(value);
    }

    /* access modifiers changed from: protected */
    public void changeCurrent(int current) {
        if (current > this.mEnd) {
            current = this.mStart;
        } else if (current < this.mStart) {
            current = this.mEnd;
        }
        this.mPrevious = this.mCurrent;
        this.mCurrent = current;
        notifyChange();
        updateView();
    }

    /* access modifiers changed from: protected */
    public void notifyChange() {
        if (this.mListener != null) {
            this.mListener.onChanged(this, this.mPrevious, this.mCurrent);
        }
    }

    /* access modifiers changed from: protected */
    public void updateView() {
        if (this.mDisplayedValues == null) {
            this.mText.setText(formatNumber(this.mCurrent));
        } else {
            this.mText.setText(this.mDisplayedValues[this.mCurrent - this.mStart]);
        }
        this.mText.setSelection(this.mText.getText().length());
    }

    private void validateCurrentView(CharSequence str) {
        int val = getSelectedPos(str.toString());
        if (val >= this.mStart && val <= this.mEnd && this.mCurrent != val) {
            this.mPrevious = this.mCurrent;
            this.mCurrent = val;
            notifyChange();
        }
        updateView();
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            validateInput(v);
        }
    }

    private void validateInput(View v) {
        String str = String.valueOf(((TextView) v).getText());
        if ("".equals(str)) {
            updateView();
        } else {
            validateCurrentView(str);
        }
    }

    public boolean onLongClick(View v) {
        this.mText.clearFocus();
        if (R.id.increment == v.getId()) {
            this.mIncrement = true;
            this.mHandler.post(this.mRunnable);
        } else if (R.id.decrement == v.getId()) {
            this.mDecrement = true;
            this.mHandler.post(this.mRunnable);
        }
        return true;
    }

    public void cancelIncrement() {
        this.mIncrement = false;
    }

    public void cancelDecrement() {
        this.mDecrement = false;
    }

    private class NumberPickerInputFilter implements InputFilter {
        private NumberPickerInputFilter() {
        }

        /* synthetic */ NumberPickerInputFilter(NumberPicker numberPicker, NumberPickerInputFilter numberPickerInputFilter) {
            this();
        }

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (NumberPicker.this.mDisplayedValues == null) {
                return NumberPicker.this.mNumberInputFilter.filter(source, start, end, dest, dstart, dend);
            }
            CharSequence filtered = String.valueOf(source.subSequence(start, end));
            String str = String.valueOf(String.valueOf(String.valueOf(dest.subSequence(0, dstart))) + filtered + dest.subSequence(dend, dest.length())).toLowerCase();
            for (String val : NumberPicker.this.mDisplayedValues) {
                if (val.toLowerCase().startsWith(str)) {
                    return filtered;
                }
            }
            return "";
        }
    }

    private class NumberRangeKeyListener extends NumberKeyListener {
        private NumberRangeKeyListener() {
        }

        /* synthetic */ NumberRangeKeyListener(NumberPicker numberPicker, NumberRangeKeyListener numberRangeKeyListener) {
            this();
        }

        public int getInputType() {
            return 2;
        }

        /* access modifiers changed from: protected */
        public char[] getAcceptedChars() {
            return NumberPicker.DIGIT_CHARACTERS;
        }

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
            if (filtered == null) {
                filtered = source.subSequence(start, end);
            }
            String result = String.valueOf(String.valueOf(dest.subSequence(0, dstart))) + filtered + dest.subSequence(dend, dest.length());
            if ("".equals(result)) {
                return result;
            }
            return NumberPicker.this.getSelectedPos(result) > NumberPicker.this.mEnd ? "" : filtered;
        }
    }

    /* access modifiers changed from: private */
    public int getSelectedPos(String str) {
        if (this.mDisplayedValues == null) {
            return Integer.parseInt(str);
        }
        for (int i = 0; i < this.mDisplayedValues.length; i++) {
            str = str.toLowerCase();
            if (this.mDisplayedValues[i].toLowerCase().startsWith(str)) {
                return this.mStart + i;
            }
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return this.mStart;
        }
    }

    public int getCurrent() {
        return this.mCurrent;
    }
}
