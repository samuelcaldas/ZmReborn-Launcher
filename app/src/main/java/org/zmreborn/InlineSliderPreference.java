package org.zmreborn;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/** Presents a bounded integer as an inline slider. */
public final class InlineSliderPreference extends DebouncedIntegerPreference
        implements SeekBar.OnSeekBarChangeListener {
    public InlineSliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectable(false);
        setLayoutResource(R.layout.settings_inline_slider_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        SeekBar slider = (SeekBar) view.findViewById(R.id.settings_inline_slider);
        TextView value = (TextView) view.findViewById(R.id.settings_numeric_value);
        slider.setOnSeekBarChangeListener(null);
        slider.setMax(getMax() - getMin());
        slider.setProgress(getValue() - getMin());
        slider.setEnabled(isEnabled());
        bindValue(slider, value);
        slider.setOnSeekBarChangeListener(this);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }
        if (!submitUserValue(getMin() + progress)) {
            seekBar.setProgress(getValue() - getMin());
        }
        bindValue(seekBar, findValueView(seekBar));
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private TextView findValueView(SeekBar seekBar) {
        View parent = (View) seekBar.getParent();
        TextView value = (TextView) parent.findViewById(R.id.settings_numeric_value);
        if (value == null) {
            throw new IllegalStateException("Inline slider row has no numeric value view");
        }
        return value;
    }

    private void bindValue(SeekBar slider, TextView value) {
        int currentValue = getValue();
        value.setText(String.valueOf(currentValue));
        value.setContentDescription(getContext().getString(
                R.string.preferences_accessibility_current_value, getTitle(), currentValue));
        slider.setContentDescription(getContext().getString(
                R.string.preferences_accessibility_slider_value, getTitle(), currentValue,
                getMax()));
    }
}
