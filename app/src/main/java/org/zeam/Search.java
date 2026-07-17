package org.zeam;

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

public class Search extends LinearLayout implements View.OnClickListener, View.OnKeyListener, View.OnLongClickListener {
    private static final float ANIMATION_VELOCITY = 1.0f;
    private final String TAG = Search.class.getSimpleName();
    private Bundle mAppSearchData;
    private boolean mGlobalSearch;
    private String mInitialQuery;
    private Launcher mLauncher;
    /* access modifiers changed from: private */
    public Animation mMorphAnimation;
    private TextView mSearchText;
    private boolean mSelectInitialQuery;
    private Animation mUnmorphAnimation;
    private ImageButton mVoiceButton;
    private Intent mVoiceSearchIntent;

    public Search(Context context, AttributeSet attrs) {
        super(context, attrs);
        Interpolator interpolator = new AccelerateDecelerateInterpolator();
        this.mMorphAnimation = new ToParentOriginAnimation(this, (ToParentOriginAnimation) null);
        this.mMorphAnimation.setFillBefore(false);
        this.mMorphAnimation.setFillAfter(true);
        this.mMorphAnimation.setInterpolator(interpolator);
        this.mMorphAnimation.setAnimationListener(new Animation.AnimationListener() {
            private static final long TIME_BEFORE_ANIMATION_END = 80;
            private final Runnable mShowSearchDialogRunnable = new Runnable() {
                public void run() {
                    Search.this.showSearchDialog();
                }
            };

            public void onAnimationEnd(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
                Search.this.getHandler().postDelayed(this.mShowSearchDialogRunnable, Math.max(Search.this.mMorphAnimation.getDuration() - TIME_BEFORE_ANIMATION_END, 0));
            }
        });
        this.mUnmorphAnimation = new FromParentOriginAnimation(this, (FromParentOriginAnimation) null);
        this.mUnmorphAnimation.setFillBefore(true);
        this.mUnmorphAnimation.setFillAfter(false);
        this.mUnmorphAnimation.setInterpolator(interpolator);
        this.mUnmorphAnimation.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                Search.this.clearAnimation();
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });
        this.mVoiceSearchIntent = new Intent("android.speech.action.WEB_SEARCH");
        this.mVoiceSearchIntent.putExtra("android.speech.extra.LANGUAGE_MODEL", "web_search");
    }

    public void onClick(View v) {
        if (v == this.mVoiceButton) {
            startVoiceSearch();
        } else {
            this.mLauncher.onSearchRequested();
        }
    }

    private void startVoiceSearch() {
        try {
            getContext().startActivity(this.mVoiceSearchIntent);
        } catch (ActivityNotFoundException e) {
            Log.w(this.TAG, "Could not find voice search activity");
        }
    }

    public void setQuery(String query) {
        this.mSearchText.setText(query, TextView.BufferType.NORMAL);
    }

    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
        this.mInitialQuery = initialQuery;
        this.mSelectInitialQuery = selectInitialQuery;
        this.mAppSearchData = appSearchData;
        this.mGlobalSearch = globalSearch;
        showSearchDialog();
    }

    /* access modifiers changed from: private */
    public void showSearchDialog() {
        this.mLauncher.showSearchDialog(this.mInitialQuery, this.mSelectInitialQuery, this.mAppSearchData, this.mGlobalSearch);
    }

    public void stopSearch(boolean animate) {
        setQuery("");
        if (getAnimation() != this.mMorphAnimation) {
            return;
        }
        if (!animate || isAtTop()) {
            clearAnimation();
            return;
        }
        this.mUnmorphAnimation.setDuration((long) getAnimationDuration());
        startAnimation(this.mUnmorphAnimation);
    }

    private boolean isAtTop() {
        return getTop() == 0;
    }

    private int getAnimationDuration() {
        return (int) (((float) getTop()) / ANIMATION_VELOCITY);
    }

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

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (!(event.isSystem() || keyCode == 19 || keyCode == 20 || keyCode == 21 || keyCode == 22 || keyCode == 23)) {
            switch (event.getAction()) {
                case 0:
                    return this.mLauncher.onKeyDown(keyCode, event);
                case 1:
                    return this.mLauncher.onKeyUp(keyCode, event);
                case 2:
                    return this.mLauncher.onKeyMultiple(keyCode, event.getRepeatCount(), event);
            }
        }
        return false;
    }

    public boolean onLongClick(View v) {
        return performLongClick();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSearchText = (TextView) findViewById(R.id.search_src_text);
        this.mVoiceButton = (ImageButton) findViewById(R.id.search_voice_btn);
        this.mVoiceButton.setContentDescription(getContext().getString(R.string.accessibility_voice_search));
        this.mSearchText.setOnKeyListener(this);
        this.mSearchText.setOnClickListener(this);
        this.mVoiceButton.setOnClickListener(this);
        setOnClickListener(this);
        this.mSearchText.setOnLongClickListener(this);
        this.mVoiceButton.setOnLongClickListener(this);
        this.mSearchText.setCompoundDrawablesWithIntrinsicBounds(getContext().getResources().getDrawable(R.drawable.placeholder_google), (Drawable) null, (Drawable) null, (Drawable) null);
        configureVoiceSearchButton();
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void configureVoiceSearchButton() {
        boolean voiceSearchVisible;
        int i = 0;
        if (getContext().getPackageManager().resolveActivity(this.mVoiceSearchIntent, 65536) != null) {
            voiceSearchVisible = true;
        } else {
            voiceSearchVisible = false;
        }
        ImageButton imageButton = this.mVoiceButton;
        if (!voiceSearchVisible) {
            i = 8;
        }
        imageButton.setVisibility(i);
    }

    public void setLauncher(Launcher home) {
        this.mLauncher = home;
    }

    private class ToParentOriginAnimation extends Animation {
        private ToParentOriginAnimation() {
        }

        /* synthetic */ ToParentOriginAnimation(Search search, ToParentOriginAnimation toParentOriginAnimation) {
            this();
        }

        /* access modifiers changed from: protected */
        public void applyTransformation(float interpolatedTime, Transformation t) {
            t.getMatrix().setTranslate(((float) (-Search.this.getLeft())) * interpolatedTime, ((float) (-Search.this.getTop())) * interpolatedTime);
        }
    }

    private class FromParentOriginAnimation extends Animation {
        private FromParentOriginAnimation() {
        }

        /* synthetic */ FromParentOriginAnimation(Search search, FromParentOriginAnimation fromParentOriginAnimation) {
            this();
        }

        /* access modifiers changed from: protected */
        public void applyTransformation(float interpolatedTime, Transformation t) {
            t.getMatrix().setTranslate(((float) (-Search.this.getLeft())) * (Search.ANIMATION_VELOCITY - interpolatedTime), ((float) (-Search.this.getTop())) * (Search.ANIMATION_VELOCITY - interpolatedTime));
        }
    }
}
