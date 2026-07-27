package org.zmreborn;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import org.zmreborn.ColourPickerView;

public class ColourPickerDialog extends Dialog implements ColourPickerView.OnColourChangedListener, View.OnClickListener {
    private ColourPickerView mColourPicker;
    private ColourPickerPanelView mNewColour;
    private ColourPickerPanelView mOldColour;
    private OnColourChangedListener mOnColourChangedListener;

    public interface OnColourChangedListener {
        void onColourChanged(int i);
    }

    public ColourPickerDialog(Context context, int initialColor) {
        super(context);
        init(initialColor);
    }

    private void init(int color) {
        getWindow().setFormat(1);
        setup(color);
    }

    private void setup(int color) {
        View layout = ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(R.layout.dialog_colour_picker, (ViewGroup) null);
        setContentView(layout);
        setTitle(R.string.dialog_colour_picker_title);
        this.mColourPicker = (ColourPickerView) layout.findViewById(R.id.colour_picker_view);
        this.mOldColour = (ColourPickerPanelView) layout.findViewById(R.id.old_colour_panel);
        this.mNewColour = (ColourPickerPanelView) layout.findViewById(R.id.new_colour_panel);
        ((LinearLayout) this.mOldColour.getParent()).setPadding(Math.round(this.mColourPicker.getDrawingOffset()), 0, Math.round(this.mColourPicker.getDrawingOffset()), 0);
        this.mOldColour.setOnClickListener(this);
        this.mNewColour.setOnClickListener(this);
        this.mColourPicker.setOnColorChangedListener(this);
        this.mOldColour.setColor(color);
        this.mColourPicker.setColor(color, true);
    }

    public void onColorChanged(int color) {
        this.mNewColour.setColor(color);
    }

    public void setAlphaSliderVisible(boolean visible) {
        this.mColourPicker.setAlphaSliderVisible(visible);
    }

    public void setOnColourChangedListener(OnColourChangedListener onColourChangedListener) {
        this.mOnColourChangedListener = onColourChangedListener;
    }

    public int getColor() {
        return this.mColourPicker.getColour();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.new_colour_panel) {
            if (this.mOnColourChangedListener != null) {
                this.mOnColourChangedListener.onColourChanged(this.mNewColour.getColor());
            }
        }
        dismiss();
    }
}
