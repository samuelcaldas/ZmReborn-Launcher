package org.zmreborn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Preference that displays a color preview swatch and opens a color picker dialog when clicked.
 */
public class ColourPickerPreference extends Preference implements Preference.OnPreferenceClickListener, ColourPickerDialog.OnColourChangedListener {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String TAG = "ColourPickerPreference";

    private boolean alphaSliderEnabled;
    private int defaultValue = 0xFF000000;
    private float density;
    private int value = 0xFF000000;
    private View boundView;

    /**
     * Constructs a color picker preference with context.
     */
    public ColourPickerPreference(Context context) {
        super(context);
        init(context, null);
    }

    /**
     * Constructs a color picker preference with context and XML attributes.
     */
    public ColourPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * Constructs a color picker preference with context, XML attributes, and default style.
     */
    public ColourPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.density = getContext().getResources().getDisplayMetrics().density;
        setOnPreferenceClickListener(this);
        if (attrs != null) {
            String defaultAttr = attrs.getAttributeValue(ANDROID_NS, "defaultValue");
            if (defaultAttr != null && defaultAttr.startsWith("#")) {
                try {
                    this.defaultValue = convertToColorInt(defaultAttr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Wrong color: " + defaultAttr);
                    this.defaultValue = convertToColorInt("#FF000000");
                }
            } else {
                int resourceId = attrs.getAttributeResourceValue(ANDROID_NS, "defaultValue", 0);
                if (resourceId != 0) {
                    this.defaultValue = context.getColor(resourceId);
                }
            }
            this.alphaSliderEnabled = attrs.getAttributeBooleanValue((String) null, "alphaSlider", false);
        }
        this.value = this.defaultValue;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        this.boundView = view;
        setPreviewColor();
    }

    private void setPreviewColor() {
        if (this.boundView == null) {
            return;
        }
        ImageView previewView = new ImageView(getContext());
        LinearLayout widgetFrameView = (LinearLayout) this.boundView.findViewById(android.R.id.widget_frame);
        if (widgetFrameView != null) {
            widgetFrameView.setPadding(widgetFrameView.getPaddingLeft(), widgetFrameView.getPaddingTop(),
                    (int) (this.density * 8.0f), widgetFrameView.getPaddingBottom());
            int count = widgetFrameView.getChildCount();
            if (count > 0) {
                widgetFrameView.removeViews(0, count);
            }
            widgetFrameView.addView(previewView);
            previewView.setContentDescription(convertToARGB(getValue()));
            previewView.setBackgroundDrawable(new AlphaPatternDrawable((int) (5.0f * this.density)));
            previewView.setImageBitmap(getPreviewBitmap());
        }
    }

    private Bitmap getPreviewBitmap() {
        int dimension = (int) (this.density * 31.0f);
        int currentColor = getValue();
        Bitmap bitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = x; y < height; y++) {
                int pixelColor = (x <= 1 || y <= 1 || x >= width - 2 || y >= height - 2)
                        ? 0xFF888888 : currentColor;
                bitmap.setPixel(x, y, pixelColor);
                if (x != y) {
                    bitmap.setPixel(y, x, pixelColor);
                }
            }
        }
        return bitmap;
    }

    /**
     * Returns the persisted or current color value.
     */
    public int getValue() {
        try {
            if (isPersistent()) {
                this.value = getPersistedInt(this.defaultValue);
            }
        } catch (ClassCastException e) {
            this.value = this.defaultValue;
        }
        return this.value;
    }

    @Override
    public void onColourChanged(int color) {
        if (isPersistent()) {
            persistInt(color);
        }
        this.value = color;
        setPreviewColor();
        Preference.OnPreferenceChangeListener listener = getOnPreferenceChangeListener();
        if (listener != null) {
            listener.onPreferenceChange(this, color);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        ColourPickerDialog colourPickerDialog = new ColourPickerDialog(getContext(), getValue());
        colourPickerDialog.setOnColourChangedListener(this);
        if (this.alphaSliderEnabled) {
            colourPickerDialog.setAlphaSliderVisible(true);
        }
        colourPickerDialog.show();
        return false;
    }

    /**
     * Enables or disables the alpha channel slider in the picker dialog.
     */
    public void setAlphaSliderEnabled(boolean enable) {
        this.alphaSliderEnabled = enable;
    }

    /**
     * Converts a color integer into an #AARRGGBB hex string.
     */
    public static String convertToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));
        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }
        if (red.length() == 1) {
            red = "0" + red;
        }
        if (green.length() == 1) {
            green = "0" + green;
        }
        if (blue.length() == 1) {
            blue = "0" + blue;
        }
        return "#" + alpha + red + green + blue;
    }

    /**
     * Parses an #AARRGGBB or #RRGGBB hex color string into a color integer.
     */
    public static int convertToColorInt(String argb) throws NumberFormatException {
        if (argb == null) {
            throw new NumberFormatException("Null color string");
        }
        String cleanArgb = argb.startsWith("#") ? argb.substring(1) : argb;
        int alpha = 255;
        int red = 0;
        int green = 0;
        int blue = 0;
        if (cleanArgb.length() == 8) {
            alpha = Integer.parseInt(cleanArgb.substring(0, 2), 16);
            red = Integer.parseInt(cleanArgb.substring(2, 4), 16);
            green = Integer.parseInt(cleanArgb.substring(4, 6), 16);
            blue = Integer.parseInt(cleanArgb.substring(6, 8), 16);
        } else if (cleanArgb.length() == 6) {
            red = Integer.parseInt(cleanArgb.substring(0, 2), 16);
            green = Integer.parseInt(cleanArgb.substring(2, 4), 16);
            blue = Integer.parseInt(cleanArgb.substring(4, 6), 16);
        }
        return Color.argb(alpha, red, green, blue);
    }
}
