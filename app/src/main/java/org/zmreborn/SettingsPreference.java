package org.zmreborn;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/** Applies destructive styling to reset without changing preference behavior. */
public class SettingsPreference extends Preference {
    public SettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (!isResetPreference()) {
            return;
        }
        TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setTextColor(getContext().getResources().getColor(R.color.zm_reborn_ember));
        }
    }

    private boolean isResetPreference() {
        return getKey() != null
                && getKey().equals(getContext().getString(R.string.preferences_key_reset));
    }
}
