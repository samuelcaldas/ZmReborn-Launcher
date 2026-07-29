package org.zmreborn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import org.zmreborn.theme.WallpaperColorExtractor;

/** Renders accessible, size-adaptive alphabetical navigation for drawer applications. */
public final class DrawerFastScrollView extends View {
    interface OnSectionSelectedListener {
        void onSectionSelected(int position);
    }

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AlphabetAccessibilityNodeProvider mNodeProvider =
            new AlphabetAccessibilityNodeProvider();
    private DrawerAlphabetIndex mSourceIndex = DrawerAlphabetIndex.from(null);
    private DrawerAlphabetIndex mDisplayedIndex = DrawerAlphabetIndex.from(null);
    private OnSectionSelectedListener mListener;
    private int mActiveColor;
    private int mInactiveColor;
    private static final int VIRTUAL_SECTION_ID_OFFSET = 1;

    private int mMinimumSectionHeight;
    private int mSelectedIndex = -1;

    /** Creates fast-scroll rail without XML attributes. */
    public DrawerFastScrollView(Context context) {
        this(context, null);
    }

    /** Creates fast-scroll rail from XML attributes. */
    public DrawerFastScrollView(Context context, AttributeSet attributes) {
        this(context, attributes, 0);
    }

    /** Creates fast-scroll rail from XML attributes and style. */
    public DrawerFastScrollView(Context context, AttributeSet attributes, int defStyleAttribute) {
        super(context, attributes, defStyleAttribute);
        this.mPaint.setTextAlign(Paint.Align.CENTER);
        this.mPaint.setTextSize(getResources().getDimension(
                R.dimen.drawer_fast_scroll_text_size));
        this.mMinimumSectionHeight = getResources().getDimensionPixelSize(
                R.dimen.drawer_fast_scroll_min_section_height);
        setFocusable(true);
        refreshPalette();
    }

    void setIndex(DrawerAlphabetIndex index) {
        this.mSourceIndex = index == null ? DrawerAlphabetIndex.from(null) : index;
        this.mSelectedIndex = -1;
        rebuildDisplayedIndex();
        updateContentDescription();
        announceIndexChange();
        invalidate();
    }

    void setOnSectionSelectedListener(OnSectionSelectedListener listener) {
        this.mListener = listener;
    }

    void clearSelection() {
        if (this.mSelectedIndex < 0) {
            return;
        }
        this.mSelectedIndex = -1;
        updateContentDescription();
        invalidate();
        announceIndexChange();
    }

    void refreshPalette() {
        this.mActiveColor = WallpaperColorExtractor.getPrimary(getContext());
        int onSurface = WallpaperColorExtractor.getOnSurface(getContext());
        this.mInactiveColor = Color.argb(191, Color.red(onSurface), Color.green(onSurface),
                Color.blue(onSurface));
        invalidate();
    }

    boolean selectSection(String section) {
        if (!isEnabled()) {
            return false;
        }
        int index = this.mDisplayedIndex.indexOf(section);
        if (index < 0) {
            return false;
        }
        selectIndex(index);
        return true;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        rebuildDisplayedIndex();
        updateContentDescription();
        announceIndexChange();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int index = 0; index < this.mDisplayedIndex.size(); index++) {
            drawSection(canvas, index);
        }
    }

    private void drawSection(Canvas canvas, int index) {
        this.mPaint.setColor(index == this.mSelectedIndex
                ? this.mActiveColor : this.mInactiveColor);
        float baseline = sectionCenter(index) - ((this.mPaint.descent() + this.mPaint.ascent()) / 2.0f);
        canvas.drawText(this.mDisplayedIndex.getSectionAt(index), getWidth() / 2.0f,
                baseline, this.mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || this.mDisplayedIndex.size() == 0) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                requestParentInterception(true);
                setPressed(true);
                selectIndex(sectionAt(event.getY()));
                return true;
            case MotionEvent.ACTION_MOVE:
                selectIndex(sectionAt(event.getY()));
                return true;
            case MotionEvent.ACTION_UP:
                selectIndex(sectionAt(event.getY()));
                setPressed(false);
                requestParentInterception(false);
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                requestParentInterception(false);
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isEnabled()) {
            return super.onKeyDown(keyCode, event);
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            return moveSelection(1);
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            return moveSelection(-1);
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (this.mSelectedIndex < 0 && this.mDisplayedIndex.size() > 0) {
                selectIndex(0);
            }
            return this.mSelectedIndex >= 0;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return this.mNodeProvider;
    }

    private boolean moveSelection(int offset) {
        if (this.mDisplayedIndex.size() == 0) {
            return false;
        }
        int selected = this.mSelectedIndex;
        if (selected < 0) {
            selected = offset < 0 ? this.mDisplayedIndex.size() - 1 : 0;
        } else {
            selected = Math.max(0, Math.min(selected + offset,
                    this.mDisplayedIndex.size() - 1));
        }
        selectIndex(selected);
        return true;
    }

    private int sectionAt(float y) {
        int height = Math.max(1, getHeight());
        float boundedY = Math.max(0.0f, Math.min(y, height - 1.0f));
        int index = (int) ((boundedY * this.mDisplayedIndex.size()) / height);
        return Math.min(index, this.mDisplayedIndex.size() - 1);
    }

    private float sectionCenter(int index) {
        return ((index + 0.5f) * getHeight()) / this.mDisplayedIndex.size();
    }

    private void selectIndex(int index) {
        if (index == this.mSelectedIndex) {
            return;
        }
        this.mSelectedIndex = index;
        updateContentDescription();
        invalidate();
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if (this.mListener != null) {
            this.mListener.onSectionSelected(this.mDisplayedIndex.getPositionAt(index));
        }
    }

    private void rebuildDisplayedIndex() {
        String selectedSection = selectedSection();
        int textHeight = Math.round(this.mPaint.descent() - this.mPaint.ascent());
        int minimumHeight = Math.max(1, Math.max(this.mMinimumSectionHeight, textHeight));
        int maximumSections = getHeight() / minimumHeight;
        this.mDisplayedIndex = this.mSourceIndex.compact(
                maximumSections, selectedSection);
        this.mSelectedIndex = this.mDisplayedIndex.indexOf(selectedSection);
    }

    private String selectedSection() {
        if (this.mSelectedIndex < 0 || this.mSelectedIndex >= this.mDisplayedIndex.size()) {
            return "";
        }
        return this.mDisplayedIndex.getSectionAt(this.mSelectedIndex);
    }

    private void updateContentDescription() {
        if (this.mSelectedIndex < 0) {
            setContentDescription(getResources().getString(
                    R.string.accessibility_fast_scroll));
            return;
        }
        setContentDescription(getResources().getString(
                R.string.accessibility_fast_scroll_section,
                this.mDisplayedIndex.getSectionAt(this.mSelectedIndex)));
    }

    private void announceIndexChange() {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    private void requestParentInterception(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    private int virtualViewIdForSection(int index) {
        return this.mDisplayedIndex.getSectionAt(index).charAt(0)
                + VIRTUAL_SECTION_ID_OFFSET;
    }

    private int sectionIndexForVirtualViewId(int virtualViewId) {
        if (virtualViewId <= 0) {
            return -1;
        }
        char sectionCharacter = (char) (virtualViewId - VIRTUAL_SECTION_ID_OFFSET);
        return this.mDisplayedIndex.indexOf(String.valueOf(sectionCharacter));
    }

    private final class AlphabetAccessibilityNodeProvider extends AccessibilityNodeProvider {
        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            if (virtualViewId == HOST_VIEW_ID) {
                return createHostNode();
            }
            return createSectionNode(virtualViewId);
        }

        @Override
        public boolean performAction(int virtualViewId, int action, android.os.Bundle arguments) {
            if (virtualViewId == HOST_VIEW_ID) {
                return performHostAction(action);
            }
            if (action != AccessibilityNodeInfo.ACTION_CLICK || !isEnabled()) {
                return false;
            }
            int index = sectionIndexForVirtualViewId(virtualViewId);
            if (index < 0) {
                return false;
            }
            selectIndex(index);
            return true;
        }

        private AccessibilityNodeInfo createHostNode() {
            AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain(DrawerFastScrollView.this);
            node.setClassName(DrawerFastScrollView.class.getName());
            node.setContentDescription(getContentDescription());
            node.setEnabled(isEnabled());
            node.setScrollable(isEnabled() && mDisplayedIndex.size() > 1);
            if (isEnabled()) {
                node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            }
            for (int index = 0; index < mDisplayedIndex.size(); index++) {
                node.addChild(DrawerFastScrollView.this,
                        virtualViewIdForSection(index));
            }
            return node;
        }

        private AccessibilityNodeInfo createSectionNode(int virtualViewId) {
            int index = sectionIndexForVirtualViewId(virtualViewId);
            if (index < 0) {
                return null;
            }
            AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
            node.setSource(DrawerFastScrollView.this, virtualViewId);
            node.setParent(DrawerFastScrollView.this);
            node.setClassName(android.widget.Button.class.getName());
            node.setText(getResources().getString(
                    R.string.accessibility_fast_scroll_section,
                    mDisplayedIndex.getSectionAt(index)));
            boolean enabled = isEnabled();
            node.setClickable(enabled);
            node.setEnabled(enabled);
            node.setBoundsInParent(sectionBounds(index));
            if (enabled) {
                node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            return node;
        }

        private boolean performHostAction(int action) {
            if (!isEnabled()) {
                return false;
            }
            if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                return moveSelection(1);
            }
            if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
                return moveSelection(-1);
            }
            return false;
        }
    }

    private Rect sectionBounds(int index) {
        int top = Math.round((index * getHeight()) / (float) this.mDisplayedIndex.size());
        int bottom = Math.round(((index + 1) * getHeight())
                / (float) this.mDisplayedIndex.size());
        return new Rect(0, top, getWidth(), bottom);
    }
}
