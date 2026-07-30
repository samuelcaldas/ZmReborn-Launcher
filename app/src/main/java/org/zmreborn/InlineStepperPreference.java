package org.zmreborn;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

/** Presents a bounded integer as inline decrement and increment actions. */
public final class InlineStepperPreference extends DebouncedIntegerPreference {
    public InlineStepperPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectable(false);
        setWidgetLayoutResource(R.layout.settings_stepper_widget);
    }

    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);
        final ImageButton decrement = (ImageButton) view.findViewById(R.id.decrement);
        final ImageButton increment = (ImageButton) view.findViewById(R.id.increment);
        bindState(view, decrement, increment);
        decrement.setOnClickListener(new View.OnClickListener() {
            public void onClick(View clickedView) {
                submitUserValue(getValue() - 1);
                bindState(view, decrement, increment);
            }
        });
        increment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View clickedView) {
                submitUserValue(getValue() + 1);
                bindState(view, decrement, increment);
            }
        });
        View.OnLongClickListener consumeLongClick = new View.OnLongClickListener() {
            public boolean onLongClick(View clickedView) {
                return true;
            }
        };
        decrement.setOnLongClickListener(consumeLongClick);
        increment.setOnLongClickListener(consumeLongClick);
    }

    private void bindState(View view, ImageButton decrement, ImageButton increment) {
        TextView value = (TextView) view.findViewById(R.id.settings_numeric_value);
        int currentValue = getValue();
        boolean enabled = isEnabled();
        value.setText(String.valueOf(currentValue));
        value.setContentDescription(getContext().getString(
                R.string.preferences_accessibility_current_value, getTitle(), currentValue));
        decrement.setEnabled(enabled && currentValue > getMin());
        increment.setEnabled(enabled && currentValue < getMax());
        decrement.setContentDescription(getContext().getString(
                R.string.preferences_accessibility_decrease_value, getTitle(), currentValue,
                getMin()));
        increment.setContentDescription(getContext().getString(
                R.string.preferences_accessibility_increase_value, getTitle(), currentValue,
                getMax()));
    }
}
