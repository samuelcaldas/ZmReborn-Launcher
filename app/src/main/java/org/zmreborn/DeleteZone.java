package org.zmreborn;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Drop target view that deletes items dropped onto it or initiates uninstallation when held over the zone.
 */
public class DeleteZone extends ImageView implements DropTarget, DragController.DragListener {
    private static final int ANIMATION_DURATION = 200;
    private static final int ORIENTATION_HORIZONTAL = 1;
    private static final int TRANSITION_DURATION = 250;

    private int customPadding = 60;
    private DragLayer dragLayer;
    private View handle;
    private Animation handleInAnimation;
    private Animation handleOutAnimation;
    private final Handler handler;
    private AnimationSet inAnimation;
    private Launcher launcher;
    private final int[] location = new int[2];
    private int orientation = ORIENTATION_HORIZONTAL;
    private AnimationSet outAnimation;
    private final RectF region = new RectF();
    private final Runnable showUninstaller;
    private TransitionDrawable transition;
    private boolean trashMode;
    private boolean trickyLocation;
    private ApplicationItemInfo uninstallApplicationItemInfo;
    private boolean uninstallTarget;
    private boolean shouldUninstall;

    /**
     * Constructs a delete zone with context.
     */
    public DeleteZone(Context context) {
        this(context, null);
    }

    /**
     * Constructs a delete zone with context and XML attributes.
     */
    public DeleteZone(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructs a delete zone with context, XML attributes, and default style.
     */
    public DeleteZone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.handler = new Handler(Looper.getMainLooper());
        this.showUninstaller = new Runnable() {
            public void run() {
                DeleteZone.this.shouldUninstall = DeleteZone.this.uninstallTarget;
                if (DeleteZone.this.shouldUninstall) {
                    Context ctx = DeleteZone.this.getContext();
                    Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        vibrator.vibrate(35);
                    }
                    Toast.makeText(ctx, R.string.drop_to_uninstall, Toast.LENGTH_SHORT).show();
                    DeleteZone.this.setColorFilter(ctx.getColor(R.color.zm_reborn_ember), PorterDuff.Mode.SRC_ATOP);
                    DeleteZone.this.setContentDescription(ctx.getString(R.string.drop_to_uninstall));
                }
            }
        };
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DeleteZone, defStyle, 0);
            this.orientation = a.getInt(0, ORIENTATION_HORIZONTAL);
            a.recycle();
        }
    }

    @Override
    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        return true;
    }

    @Override
    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        ItemInfo item = (ItemInfo) dragInfo;
        if (item.container != -1) {
            LauncherModel model = Launcher.getModel();
            if (item.container == -100) {
                if (item instanceof LauncherAppWidgetInfo) {
                    model.removeDesktopAppWidget((LauncherAppWidgetInfo) item);
                } else {
                    model.removeDesktopItem(item);
                }
            } else if (source instanceof UserFolder) {
                model.removeUserFolderItem((UserFolderInfo) ((UserFolder) source).getInfo(), item);
            }
            if (item instanceof UserFolderInfo) {
                UserFolderInfo userFolderInfo = (UserFolderInfo) item;
                LauncherModel.deleteUserFolderContentsFromDatabase(this.launcher, userFolderInfo);
                model.removeUserFolder(userFolderInfo);
            } else if (item instanceof LauncherAppWidgetInfo) {
                LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
                LauncherAppWidgetHost appWidgetHost = this.launcher.getAppWidgetHost();
                if (appWidgetHost != null) {
                    appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                }
            }
            LauncherModel.deleteItemFromDatabase(this.launcher, item);
        }
    }

    @Override
    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        if (((ItemInfo) dragInfo) instanceof ApplicationItemInfo && this.transition != null) {
            this.transition.reverseTransition(TRANSITION_DURATION);
            this.uninstallTarget = true;
            this.handler.removeCallbacks(this.showUninstaller);
            this.handler.postDelayed(this.showUninstaller, 1000);
        }
    }

    @Override
    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    @Override
    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        if (this.transition != null) {
            this.transition.reverseTransition(TRANSITION_DURATION);
        }
        this.handler.removeCallbacks(this.showUninstaller);
        clearColorFilter();
        setContentDescription(getContext().getString(R.string.accessibility_delete_zone));
        if (this.shouldUninstall) {
            this.uninstallTarget = false;
            this.handler.postDelayed(this.showUninstaller, 100);
        }
    }

    @Override
    public void onDragStart(View view, DragSource source, Object info, int dragAction) {
        ItemInfo item = (ItemInfo) info;
        this.customPadding = 1;
        if (item == null) {
            return;
        }
        this.trashMode = true;
        this.shouldUninstall = false;
        this.uninstallTarget = false;
        clearColorFilter();
        setContentDescription(getContext().getString(R.string.accessibility_delete_zone));
        createAnimations();
        getLocationOnScreen(this.location);
        if (this.trickyLocation) {
            if (this.orientation == ORIENTATION_HORIZONTAL) {
                this.region.set(this.location[0], this.location[1] + this.customPadding,
                        (this.location[0] + getRight()) - getLeft(),
                        ((this.location[1] + getBottom()) - getTop()) + this.customPadding);
            } else {
                this.region.set(this.location[0] + this.customPadding, this.location[1],
                        ((this.location[0] + getRight()) - getLeft()) + this.customPadding,
                        (this.location[1] + getBottom()) - getTop());
            }
            this.trickyLocation = false;
        } else {
            this.region.set(this.location[0], this.location[1],
                    (this.location[0] + getRight()) - getLeft(),
                    (this.location[1] + getBottom()) - getTop());
        }
        if (this.dragLayer != null) {
            this.dragLayer.setDeleteRegion(this.region);
        }
        if (this.transition != null) {
            this.transition.resetTransition();
        }
        startAnimation(this.inAnimation);
        if (this.handle != null) {
            this.handle.startAnimation(this.handleOutAnimation);
        }
        setVisibility(View.VISIBLE);
        if (item instanceof ApplicationItemInfo) {
            this.uninstallApplicationItemInfo = (ApplicationItemInfo) item;
        }
    }

    @Override
    public void onDragEnd() {
        if (this.trashMode) {
            this.trashMode = false;
            if (this.dragLayer != null) {
                this.dragLayer.setDeleteRegion(null);
            }
            startAnimation(this.outAnimation);
            if (this.handle != null) {
                this.handle.startAnimation(this.handleInAnimation);
            }
            setVisibility(View.GONE);
        }
        clearColorFilter();
        setContentDescription(getContext().getString(R.string.accessibility_delete_zone));
        if (this.shouldUninstall && this.uninstallApplicationItemInfo != null && this.launcher != null) {
            this.launcher.uninstallApplication(this.uninstallApplicationItemInfo);
            this.uninstallApplicationItemInfo = null;
        }
        this.shouldUninstall = false;
        this.uninstallTarget = false;
    }

    private void createAnimations() {
        if (this.inAnimation == null) {
            this.inAnimation = new FastAnimationSet();
            this.inAnimation.setInterpolator(new AccelerateInterpolator());
            this.inAnimation.addAnimation(new AlphaAnimation(0.0f, 1.0f));
            if (this.orientation == ORIENTATION_HORIZONTAL) {
                this.inAnimation.addAnimation(new TranslateAnimation(0, 0.0f, 0, 0.0f, 1, -1.0f, 1, 0.0f));
            } else {
                this.inAnimation.addAnimation(new TranslateAnimation(1, 1.0f, 1, 0.0f, 0, 0.0f, 0, 0.0f));
            }
            this.inAnimation.setDuration(ANIMATION_DURATION);
        }
        if (this.handleInAnimation == null) {
            if (this.orientation == ORIENTATION_HORIZONTAL) {
                this.handleInAnimation = new TranslateAnimation(0, 0.0f, 0, 0.0f, 1, -1.0f, 1, 0.0f);
            } else {
                this.handleInAnimation = new TranslateAnimation(1, 1.0f, 1, 0.0f, 0, 0.0f, 0, 0.0f);
            }
            this.handleInAnimation.setDuration(ANIMATION_DURATION);
        }
        if (this.outAnimation == null) {
            this.outAnimation = new FastAnimationSet();
            this.outAnimation.setInterpolator(new AccelerateInterpolator());
            this.outAnimation.addAnimation(new AlphaAnimation(1.0f, 0.0f));
            if (this.orientation == ORIENTATION_HORIZONTAL) {
                this.outAnimation.addAnimation(new FastTranslateAnimation(0, 0.0f, 0, 0.0f, 1, 0.0f, 1, -1.0f));
            } else {
                this.outAnimation.addAnimation(new FastTranslateAnimation(1, 0.0f, 1, 1.0f, 0, 0.0f, 0, 0.0f));
            }
            this.outAnimation.setDuration(ANIMATION_DURATION);
        }
        if (this.handleOutAnimation == null) {
            if (this.orientation == ORIENTATION_HORIZONTAL) {
                this.handleOutAnimation = new FastTranslateAnimation(0, 0.0f, 0, 0.0f, 1, 0.0f, 1, -1.0f);
            } else {
                this.handleOutAnimation = new FastTranslateAnimation(1, 0.0f, 1, 1.0f, 0, 0.0f, 0, 0.0f);
            }
            this.handleOutAnimation.setFillAfter(true);
            this.handleOutAnimation.setDuration(ANIMATION_DURATION);
        }
    }

    void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    void setDragController(DragLayer dragLayer) {
        this.dragLayer = dragLayer;
    }

    void setHandle(View view) {
        this.handle = view;
    }

    private static class FastTranslateAnimation extends TranslateAnimation {
        FastTranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue, int fromYType, float fromYValue, int toYType, float toYValue) {
            super(fromXType, fromXValue, toXType, toXValue, fromYType, fromYValue, toYType, toYValue);
        }

        @Override
        public boolean willChangeTransformationMatrix() {
            return true;
        }

        @Override
        public boolean willChangeBounds() {
            return false;
        }
    }

    private static class FastAnimationSet extends AnimationSet {
        FastAnimationSet() {
            super(false);
        }

        @Override
        public boolean willChangeTransformationMatrix() {
            return true;
        }

        @Override
        public boolean willChangeBounds() {
            return false;
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        super.setBackgroundDrawable(d);
        if (d instanceof TransitionDrawable) {
            this.transition = (TransitionDrawable) d;
        }
    }
}
