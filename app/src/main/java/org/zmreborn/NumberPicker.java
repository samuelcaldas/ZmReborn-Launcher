package org.zmreborn;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Custom numeric stepper input control with increment/decrement buttons and keyboard entry.
 */
public class NumberPicker extends LinearLayout implements View.OnClickListener, View.OnFocusChangeListener, View.OnLongClickListener {
    private static final int DEFAULT_MAX = 200;
    private static final int DEFAULT_MIN = 0;
    private static final long DEFAULT_SPEED_MS = 300L;

    static final char[] DIGIT_CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    /**
     * Formatter providing two-digit zero-padded string representation (e.g. "01", "09").
     */
    public static final Formatter TWO_DIGIT_FORMATTER = new Formatter() {
        private final Object[] args = new Object[1];
        private final StringBuilder builder = new StringBuilder();
        private final java.util.Formatter formatter = new java.util.Formatter(this.builder);

        @Override
        public String toString(int value) {
            this.args[0] = value;
            this.builder.delete(0, this.builder.length());
            this.formatter.format("%02d", this.args);
            return this.formatter.toString();
        }
    };

    protected int current;
    protected int previous;
    protected int start;
    protected int end;

    private boolean isIncrementing;
    private boolean isDecrementing;
    private NumberPickerButton decrementButton;
    private NumberPickerButton incrementButton;
    private String[] displayedValues;
    private Formatter formatter;
    private final Handler handler;
    private OnChangedListener listener;
    private final InputFilter numberInputFilter;
    private final Runnable repeatRunnable;
    private long speedMs;
    private final EditText inputEditText;

    /**
     * Callback interface for listening to number picker value changes.
     */
    public interface Formatter {
        /**
         * Formats the given integer into a displayable string representation.
         */
        String toString(int value);
    }

    /**
     * Callback interface invoked when the current value of the NumberPicker changes.
     */
    public interface OnChangedListener {
        /**
         * Notifies when the picker's current value has changed.
         */
        void onChanged(NumberPicker numberPicker, int previousValue, int currentValue);
    }

    /**
     * Constructs a NumberPicker with the given context.
     */
    public NumberPicker(Context context) {
        this(context, null);
    }

    /**
     * Constructs a NumberPicker with the given context and XML attributes.
     */
    public NumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructs a NumberPicker with the given context, attributes, and style.
     */
    public NumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.speedMs = DEFAULT_SPEED_MS;
        this.handler = new Handler(Looper.getMainLooper());
        this.repeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (NumberPicker.this.isIncrementing) {
                    NumberPicker.this.changeCurrent(NumberPicker.this.current + 1);
                    NumberPicker.this.handler.postDelayed(this, NumberPicker.this.speedMs);
                    return;
                }
                if (NumberPicker.this.isDecrementing) {
                    NumberPicker.this.changeCurrent(NumberPicker.this.current - 1);
                    NumberPicker.this.handler.postDelayed(this, NumberPicker.this.speedMs);
                }
            }
        };

        setOrientation(VERTICAL);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.number_picker, this, true);

        InputFilter inputFilter = new NumberPickerInputFilter();
        this.numberInputFilter = new NumberRangeKeyListener();

        this.incrementButton = findViewById(R.id.increment);
        this.incrementButton.setOnClickListener(this);
        this.incrementButton.setOnLongClickListener(this);
        this.incrementButton.setNumberPicker(this);

        this.decrementButton = findViewById(R.id.decrement);
        this.decrementButton.setOnClickListener(this);
        this.decrementButton.setOnLongClickListener(this);
        this.decrementButton.setNumberPicker(this);

        this.inputEditText = findViewById(R.id.timepicker_input);
        this.inputEditText.setOnFocusChangeListener(this);
        this.inputEditText.setFilters(new InputFilter[]{inputFilter});
        this.inputEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER);

        if (!isEnabled()) {
            setEnabled(false);
        }
        this.start = DEFAULT_MIN;
        this.end = DEFAULT_MAX;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.incrementButton.setEnabled(enabled);
        this.decrementButton.setEnabled(enabled);
        this.inputEditText.setEnabled(enabled);
    }

    /**
     * Sets the listener to be notified of value changes.
     */
    public void setOnChangeListener(OnChangedListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the optional string formatter for converting values to text.
     */
    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }

    /**
     * Sets the valid value range bounds.
     */
    public void setRange(int start, int end) {
        this.start = start;
        this.end = end;
        this.current = start;
        updateView();
    }

    /**
     * Sets the valid value range and an explicit array of displayed strings.
     */
    public void setRange(int start, int end, String[] displayedValues) {
        this.displayedValues = displayedValues;
        this.start = start;
        this.end = end;
        this.current = start;
        updateView();
    }

    /**
     * Sets the current selected value.
     */
    public void setCurrent(int current) {
        this.current = current;
        updateView();
    }

    /**
     * Sets the continuous stepping speed in milliseconds during long-press.
     */
    public void setSpeed(long speedMs) {
        this.speedMs = speedMs;
    }

    @Override
    public void onClick(View view) {
        validateInput(this.inputEditText);
        if (!this.inputEditText.hasFocus()) {
            this.inputEditText.requestFocus();
        }
        if (R.id.increment == view.getId()) {
            changeCurrent(this.current + 1);
            return;
        }
        if (R.id.decrement == view.getId()) {
            changeCurrent(this.current - 1);
        }
    }

    private String formatNumber(int value) {
        return this.formatter != null ? this.formatter.toString(value) : String.valueOf(value);
    }

    protected void changeCurrent(int newCurrent) {
        int boundedCurrent = newCurrent;
        if (boundedCurrent > this.end) {
            boundedCurrent = this.start;
        } else if (boundedCurrent < this.start) {
            boundedCurrent = this.end;
        }
        this.previous = this.current;
        this.current = boundedCurrent;
        notifyChange();
        updateView();
    }

    protected void notifyChange() {
        if (this.listener != null) {
            this.listener.onChanged(this, this.previous, this.current);
        }
    }

    protected void updateView() {
        if (this.displayedValues == null) {
            this.inputEditText.setText(formatNumber(this.current));
        } else {
            this.inputEditText.setText(this.displayedValues[this.current - this.start]);
        }
        this.inputEditText.setSelection(this.inputEditText.getText().length());
    }

    private void validateCurrentView(CharSequence text) {
        int selectedPosition = getSelectedPosition(text.toString());
        if (selectedPosition >= this.start && selectedPosition <= this.end && this.current != selectedPosition) {
            this.previous = this.current;
            this.current = selectedPosition;
            notifyChange();
        }
        updateView();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
            validateInput(view);
        }
    }

    private void validateInput(View view) {
        String str = String.valueOf(((TextView) view).getText());
        if ("".equals(str)) {
            updateView();
            return;
        }
        validateCurrentView(str);
    }

    @Override
    public boolean onLongClick(View view) {
        this.inputEditText.clearFocus();
        if (R.id.increment == view.getId()) {
            this.isIncrementing = true;
            this.handler.post(this.repeatRunnable);
            return true;
        }
        if (R.id.decrement == view.getId()) {
            this.isDecrementing = true;
            this.handler.post(this.repeatRunnable);
            return true;
        }
        return true;
    }

    /**
     * Cancels continuous increment repeat.
     */
    public void cancelIncrement() {
        this.isIncrementing = false;
    }

    /**
     * Cancels continuous decrement repeat.
     */
    public void cancelDecrement() {
        this.isDecrementing = false;
    }

    private class NumberPickerInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (NumberPicker.this.displayedValues == null) {
                return NumberPicker.this.numberInputFilter.filter(source, start, end, dest, dstart, dend);
            }
            CharSequence filtered = source.subSequence(start, end);
            String candidate = (dest.subSequence(0, dstart).toString() + filtered + dest.subSequence(dend, dest.length())).toLowerCase();
            for (String displayValue : NumberPicker.this.displayedValues) {
                if (displayValue.toLowerCase().startsWith(candidate)) {
                    return filtered;
                }
            }
            return "";
        }
    }

    private class NumberRangeKeyListener extends NumberKeyListener {
        @Override
        public int getInputType() {
            return InputType.TYPE_CLASS_NUMBER;
        }

        @Override
        protected char[] getAcceptedChars() {
            return NumberPicker.DIGIT_CHARACTERS;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
            if (filtered == null) {
                filtered = source.subSequence(start, end);
            }
            String candidate = dest.subSequence(0, dstart).toString() + filtered + dest.subSequence(dend, dest.length());
            if ("".equals(candidate)) {
                return candidate;
            }
            return NumberPicker.this.getSelectedPosition(candidate) > NumberPicker.this.end ? "" : filtered;
        }
    }

    int getSelectedPosition(String text) {
        if (this.displayedValues == null) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException exception) {
                return this.start;
            }
        }
        String lowerCaseText = text.toLowerCase();
        for (int index = 0; index < this.displayedValues.length; index++) {
            if (this.displayedValues[index].toLowerCase().startsWith(lowerCaseText)) {
                return this.start + index;
            }
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return this.start;
        }
    }

    /**
     * Returns the current integer value of this NumberPicker.
     */
    public int getCurrent() {
        return this.current;
    }
}
