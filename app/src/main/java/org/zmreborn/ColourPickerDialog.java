package org.zmreborn;

import android.app.Dialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Dialog presenting HSV and alpha color pickers for visual theme customization.
 */
public class ColourPickerDialog extends Dialog implements ColourPickerView.OnColourChangedListener, View.OnClickListener {
    private ColourPickerView colourPicker;
    private ColourPickerPanelView newColour;
    private ColourPickerPanelView oldColour;
    private OnColourChangedListener onColourChangedListener;

    /**
     * Listener interface for colour selection events.
     */
    public interface OnColourChangedListener {
        void onColourChanged(int color);
    }

    /**
     * Constructs a color picker dialog with an initial color value.
     */
    public ColourPickerDialog(Context context, int initialColor) {
        super(context);
        init(initialColor);
    }

    private void init(int color) {
        if (getWindow() != null) {
            getWindow().setFormat(PixelFormat.RGBA_8888);
        }
        setup(color);
    }

    private void setup(int color) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View layout = inflater.inflate(R.layout.dialog_colour_picker, (ViewGroup) null);
        setContentView(layout);
        setTitle(R.string.dialog_colour_picker_title);
        this.colourPicker = (ColourPickerView) layout.findViewById(R.id.colour_picker_view);
        this.oldColour = (ColourPickerPanelView) layout.findViewById(R.id.old_colour_panel);
        this.newColour = (ColourPickerPanelView) layout.findViewById(R.id.new_colour_panel);
        ((LinearLayout) this.oldColour.getParent()).setPadding(
                Math.round(this.colourPicker.getDrawingOffset()), 0,
                Math.round(this.colourPicker.getDrawingOffset()), 0);
        this.oldColour.setOnClickListener(this);
        this.newColour.setOnClickListener(this);
        this.colourPicker.setOnColorChangedListener(this);
        this.oldColour.setColor(color);
        this.colourPicker.setColor(color, true);
    }

    @Override
    public void onColorChanged(int color) {
        this.newColour.setColor(color);
    }

    /**
     * Sets whether the alpha slider is visible in the dialog.
     */
    public void setAlphaSliderVisible(boolean visible) {
        this.colourPicker.setAlphaSliderVisible(visible);
    }

    /**
     * Sets the listener to be notified when color selection changes.
     */
    public void setOnColourChangedListener(OnColourChangedListener onColourChangedListener) {
        this.onColourChangedListener = onColourChangedListener;
    }

    /**
     * Returns the currently selected color value.
     */
    public int getColor() {
        return this.colourPicker.getColour();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.new_colour_panel) {
            if (this.onColourChangedListener != null) {
                this.onColourChangedListener.onColourChanged(this.newColour.getColor());
            }
        }
        dismiss();
    }
}
