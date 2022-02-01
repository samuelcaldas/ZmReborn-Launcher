package org.zeam;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
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
import org.zeam.DragController;

public class DeleteZone extends ImageView implements DropTarget, DragController.DragListener {
    private static final int ANIMATION_DURATION = 200;
    private static final int ORIENTATION_HORIZONTAL = 1;
    private static final int TRANSITION_DURATION = 250;
    private int mCustomPadding;
    private DragLayer mDragLayer;
    private View mHandle;
    private Animation mHandleInAnimation;
    private Animation mHandleOutAnimation;
    private Handler mHandler;
    private AnimationSet mInAnimation;
    private Launcher mLauncher;
    private final int[] mLocation;
    private int mOrientation;
    private AnimationSet mOutAnimation;
    private final RectF mRegion;
    private Runnable mShowUninstaller;
    private TransitionDrawable mTransition;
    private boolean mTrashMode;
    private boolean mTrickyLocation;
    private ApplicationItemInfo mUninstallApplicationItemInfo;
    /* access modifiers changed from: private */
    public boolean mUninstallTarget;
    /* access modifiers changed from: private */
    public boolean shouldUninstall;

    public DeleteZone(Context context) {
        super(context);
        this.mLocation = new int[2];
        this.mHandler = new Handler();
        this.mCustomPadding = 60;
        this.mRegion = new RectF();
        this.shouldUninstall = false;
        this.mUninstallTarget = false;
        this.mTrickyLocation = false;
        this.mShowUninstaller = new Runnable() {
            public void run() {
                DeleteZone.this.shouldUninstall = DeleteZone.this.mUninstallTarget;
                if (DeleteZone.this.shouldUninstall) {
                    Context context = DeleteZone.this.getContext();
                    ((Vibrator) context.getSystemService("vibrator")).vibrate(35);
                    Toast.makeText(context, C0041R.string.drop_to_uninstall, 500).show();
                }
            }
        };
    }

    public DeleteZone(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteZone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLocation = new int[2];
        this.mHandler = new Handler();
        this.mCustomPadding = 60;
        this.mRegion = new RectF();
        this.shouldUninstall = false;
        this.mUninstallTarget = false;
        this.mTrickyLocation = false;
        this.mShowUninstaller = new Runnable() {
            public void run() {
                DeleteZone.this.shouldUninstall = DeleteZone.this.mUninstallTarget;
                if (DeleteZone.this.shouldUninstall) {
                    Context context = DeleteZone.this.getContext();
                    ((Vibrator) context.getSystemService("vibrator")).vibrate(35);
                    Toast.makeText(context, C0041R.string.drop_to_uninstall, 500).show();
                }
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, C0041R.styleable.DeleteZone, defStyle, 0);
        this.mOrientation = a.getInt(0, 1);
        a.recycle();
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        return true;
    }

    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo, Rect recycle) {
        return null;
    }

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
                LauncherModel.deleteUserFolderContentsFromDatabase(this.mLauncher, userFolderInfo);
                model.removeUserFolder(userFolderInfo);
            } else if (item instanceof LauncherAppWidgetInfo) {
                LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
                LauncherAppWidgetHost appWidgetHost = this.mLauncher.getAppWidgetHost();
                if (appWidgetHost != null) {
                    appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                }
            }
            LauncherModel.deleteItemFromDatabase(this.mLauncher, item);
        }
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        if (((ItemInfo) dragInfo) instanceof ApplicationItemInfo) {
            this.mTransition.reverseTransition(TRANSITION_DURATION);
            this.mUninstallTarget = true;
            this.mHandler.removeCallbacks(this.mShowUninstaller);
            this.mHandler.postDelayed(this.mShowUninstaller, 1000);
        }
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        this.mTransition.reverseTransition(TRANSITION_DURATION);
        this.mHandler.removeCallbacks(this.mShowUninstaller);
        if (this.shouldUninstall) {
            this.mUninstallTarget = false;
            this.mHandler.postDelayed(this.mShowUninstaller, 100);
        }
    }

    public void onDragStart(View view, DragSource source, Object info, int dragAction) {
        ItemInfo item = (ItemInfo) info;
        this.mCustomPadding = 1;
        if (item != null) {
            this.mTrashMode = true;
            createAnimations();
            int[] location = this.mLocation;
            getLocationOnScreen(location);
            if (this.mTrickyLocation) {
                if (this.mOrientation == 1) {
                    this.mRegion.set((float) location[0], (float) (location[1] + this.mCustomPadding), (float) ((location[0] + getRight()) - getLeft()), (float) (((location[1] + getBottom()) - getTop()) + this.mCustomPadding));
                } else {
                    this.mRegion.set((float) (location[0] + this.mCustomPadding), (float) location[1], (float) (((location[0] + getRight()) - getLeft()) + this.mCustomPadding), (float) ((location[1] + getBottom()) - getTop()));
                }
                this.mTrickyLocation = false;
            } else {
                this.mRegion.set((float) location[0], (float) location[1], (float) ((location[0] + getRight()) - getLeft()), (float) ((location[1] + getBottom()) - getTop()));
            }
            this.mDragLayer.setDeleteRegion(this.mRegion);
            this.mTransition.resetTransition();
            startAnimation(this.mInAnimation);
            this.mHandle.startAnimation(this.mHandleOutAnimation);
            setVisibility(0);
            if (item instanceof ApplicationItemInfo) {
                this.mUninstallApplicationItemInfo = (ApplicationItemInfo) item;
            }
        }
    }

    public void onDragEnd() {
        if (this.mTrashMode) {
            this.mTrashMode = false;
            this.mDragLayer.setDeleteRegion((RectF) null);
            startAnimation(this.mOutAnimation);
            this.mHandle.startAnimation(this.mHandleInAnimation);
            setVisibility(8);
        }
        if (this.shouldUninstall && this.mUninstallApplicationItemInfo != null) {
            this.mLauncher.uninstallApplication(this.mUninstallApplicationItemInfo);
            this.mUninstallApplicationItemInfo = null;
        }
    }

    private void createAnimations() {
        if (this.mInAnimation == null) {
            this.mInAnimation = new FastAnimationSet();
            AnimationSet animationSet = this.mInAnimation;
            animationSet.setInterpolator(new AccelerateInterpolator());
            animationSet.addAnimation(new AlphaAnimation(0.0f, 1.0f));
            if (this.mOrientation == 1) {
                animationSet.addAnimation(new TranslateAnimation(0, 0.0f, 0, 0.0f, 1, 1.0f, 1, 0.0f));
            } else {
                animationSet.addAnimation(new TranslateAnimation(1, 1.0f, 1, 0.0f, 0, 0.0f, 0, 0.0f));
            }
            animationSet.setDuration(200);
        }
        if (this.mHandleInAnimation == null) {
            if (this.mOrientation == 1) {
                this.mHandleInAnimation = new TranslateAnimation(0, 0.0f, 0, 0.0f, 1, 1.0f, 1, 0.0f);
            } else {
                this.mHandleInAnimation = new TranslateAnimation(1, 1.0f, 1, 0.0f, 0, 0.0f, 0, 0.0f);
            }
            this.mHandleInAnimation.setDuration(200);
        }
        if (this.mOutAnimation == null) {
            this.mOutAnimation = new FastAnimationSet();
            AnimationSet animationSet2 = this.mOutAnimation;
            animationSet2.setInterpolator(new AccelerateInterpolator());
            animationSet2.addAnimation(new AlphaAnimation(1.0f, 0.0f));
            if (this.mOrientation == 1) {
                animationSet2.addAnimation(new FastTranslateAnimation(0, 0.0f, 0, 0.0f, 1, 0.0f, 1, 1.0f));
            } else {
                animationSet2.addAnimation(new FastTranslateAnimation(1, 0.0f, 1, 1.0f, 0, 0.0f, 0, 0.0f));
            }
            animationSet2.setDuration(200);
        }
        if (this.mHandleOutAnimation == null) {
            if (this.mOrientation == 1) {
                this.mHandleOutAnimation = new FastTranslateAnimation(0, 0.0f, 0, 0.0f, 1, 0.0f, 1, 1.0f);
            } else {
                this.mHandleOutAnimation = new FastTranslateAnimation(1, 0.0f, 1, 1.0f, 0, 0.0f, 0, 0.0f);
            }
            this.mHandleOutAnimation.setFillAfter(true);
            this.mHandleOutAnimation.setDuration(200);
        }
    }

    /* access modifiers changed from: package-private */
    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    /* access modifiers changed from: package-private */
    public void setDragController(DragLayer dragLayer) {
        this.mDragLayer = dragLayer;
    }

    /* access modifiers changed from: package-private */
    public void setHandle(View view) {
        this.mHandle = view;
    }

    private static class FastTranslateAnimation extends TranslateAnimation {
        public FastTranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue, int fromYType, float fromYValue, int toYType, float toYValue) {
            super(fromXType, fromXValue, toXType, toXValue, fromYType, fromYValue, toYType, toYValue);
        }

        public boolean willChangeTransformationMatrix() {
            return true;
        }

        public boolean willChangeBounds() {
            return false;
        }
    }

    private static class FastAnimationSet extends AnimationSet {
        FastAnimationSet() {
            super(false);
        }

        public boolean willChangeTransformationMatrix() {
            return true;
        }

        public boolean willChangeBounds() {
            return false;
        }
    }

    public void setBackgroundDrawable(Drawable d) {
        super.setBackgroundDrawable(d);
        this.mTransition = (TransitionDrawable) d;
    }
}
