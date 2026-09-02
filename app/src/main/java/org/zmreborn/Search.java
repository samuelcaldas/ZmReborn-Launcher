package org.zmreborn;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Search bar widget with voice search support, expand animations, and query dispatching.
 */
public class Search extends LinearLayout implements View.OnClickListener, View.OnKeyListener, View.OnLongClickListener {
    private static final float ANIMATION_VELOCITY = 1.0f;
    private final String logTag = Search.class.getSimpleName();
    private Bundle appSearchData;
    private boolean globalSearch;
    private String initialQuery;
    private Launcher launcher;
    private Animation morphAnimation;
    private TextView searchText;
    private boolean selectInitialQuery;
    private Animation unmorphAnimation;
    private ImageButton voiceButton;
    private Intent voiceSearchIntent;

    /**
     * Constructs a search widget with context and XML attributes.
     */
    public Search(Context context, AttributeSet attrs) {
        super(context, attrs);
        Interpolator interpolator = new AccelerateDecelerateInterpolator();
        this.morphAnimation = new ToParentOriginAnimation();
        this.morphAnimation.setFillBefore(false);
        this.morphAnimation.setFillAfter(true);
        this.morphAnimation.setInterpolator(interpolator);
        this.morphAnimation.setAnimationListener(new Animation.AnimationListener() {
            private static final long TIME_BEFORE_ANIMATION_END = 80;
            private final Runnable showSearchDialogRunnable = new Runnable() {
                @Override
                public void run() {
                    Search.this.showSearchDialog();
                }
            };

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                Search.this.getHandler().postDelayed(this.showSearchDialogRunnable,
                        Math.max(Search.this.morphAnimation.getDuration() - TIME_BEFORE_ANIMATION_END, 0));
            }
        });
        this.unmorphAnimation = new FromParentOriginAnimation();
        this.unmorphAnimation.setFillBefore(true);
        this.unmorphAnimation.setFillAfter(false);
        this.unmorphAnimation.setInterpolator(interpolator);
        this.unmorphAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                Search.this.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });
        this.voiceSearchIntent = new Intent("android.speech.action.WEB_SEARCH");
        this.voiceSearchIntent.putExtra("android.speech.extra.LANGUAGE_MODEL", "web_search");
    }

    @Override
    public void onClick(View v) {
        if (v == this.voiceButton) {
            startVoiceSearch();
            return;
        }
        this.launcher.onSearchRequested();
    }

    private void startVoiceSearch() {
        try {
            getContext().startActivity(this.voiceSearchIntent);
        } catch (ActivityNotFoundException e) {
            Log.w(this.logTag, "Could not find voice search activity");
            Toast.makeText(getContext(), R.string.voice_search_unavailable, Toast.LENGTH_SHORT).show();
            this.voiceButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the query text displayed in the search bar.
     */
    public void setQuery(String query) {
        this.searchText.setText(query, TextView.BufferType.NORMAL);
    }

    /**
     * Initiates search with initial query and parameters.
     */
    public void startSearch(String initialQueryText, boolean selectInitial, Bundle searchData, boolean isGlobal) {
        this.initialQuery = initialQueryText;
        this.selectInitialQuery = selectInitial;
        this.appSearchData = searchData;
        this.globalSearch = isGlobal;
        showSearchDialog();
    }

    private void showSearchDialog() {
        this.launcher.showSearchDialog(this.initialQuery, this.selectInitialQuery, this.appSearchData, this.globalSearch);
    }

    /**
     * Stops the search and animates the search widget back into place.
     */
    public void stopSearch(boolean animate) {
        setQuery("");
        if (getAnimation() != this.morphAnimation) {
            return;
        }
        if (!animate || isAtTop()) {
            clearAnimation();
            return;
        }
        this.unmorphAnimation.setDuration((long) getAnimationDuration());
        startAnimation(this.unmorphAnimation);
    }

    private boolean isAtTop() {
        return getTop() == 0;
    }

    private int getAnimationDuration() {
        return (int) (((float) getTop()) / ANIMATION_VELOCITY);
    }

    @Override
    public void clearAnimation() {
        Animation animation = getAnimation();
        if (animation != null) {
            super.clearAnimation();
            if (!animation.hasEnded() || !animation.getFillAfter() || !animation.willChangeBounds()) {
                invalidate();
            } else {
                ((View) getParent()).invalidate();
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (!(event.isSystem() || keyCode == 19 || keyCode == 20 || keyCode == 21 || keyCode == 22 || keyCode == 23)) {
            switch (event.getAction()) {
                case 0:
                    return this.launcher.onKeyDown(keyCode, event);
                case 1:
                    return this.launcher.onKeyUp(keyCode, event);
                case 2:
                    return this.launcher.onKeyMultiple(keyCode, event.getRepeatCount(), event);
            }
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        return performLongClick();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.searchText = (TextView) findViewById(R.id.search_src_text);
        this.voiceButton = (ImageButton) findViewById(R.id.search_voice_btn);
        this.voiceButton.setContentDescription(getContext().getString(R.string.accessibility_voice_search));
        this.searchText.setOnKeyListener(this);
        this.searchText.setOnClickListener(this);
        this.voiceButton.setOnClickListener(this);
        setOnClickListener(this);
        this.searchText.setOnLongClickListener(this);
        this.voiceButton.setOnLongClickListener(this);
        this.searchText.setCompoundDrawablesWithIntrinsicBounds(getContext().getResources().getDrawable(R.drawable.placeholder_google), (Drawable) null, (Drawable) null, (Drawable) null);
        configureVoiceSearchButton();
    }

    private void configureVoiceSearchButton() {
        boolean voiceSearchVisible = getContext().getPackageManager().resolveActivity(this.voiceSearchIntent, 65536) != null;
        this.voiceButton.setVisibility(voiceSearchVisible ? View.VISIBLE : View.GONE);
    }

    /**
     * Associates the hosting Launcher activity instance.
     */
    public void setLauncher(Launcher home) {
        this.launcher = home;
    }

    private class ToParentOriginAnimation extends Animation {
        ToParentOriginAnimation() {
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            t.getMatrix().setTranslate(((float) (-Search.this.getLeft())) * interpolatedTime, ((float) (-Search.this.getTop())) * interpolatedTime);
        }
    }

    private class FromParentOriginAnimation extends Animation {
        FromParentOriginAnimation() {
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            t.getMatrix().setTranslate(((float) (-Search.this.getLeft())) * (Search.ANIMATION_VELOCITY - interpolatedTime), ((float) (-Search.this.getTop())) * (Search.ANIMATION_VELOCITY - interpolatedTime));
        }
    }
}
