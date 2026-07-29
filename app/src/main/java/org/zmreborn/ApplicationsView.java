package org.zmreborn;

import android.graphics.Rect;
import android.view.View;
import java.util.ArrayList;

public interface ApplicationsView {
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_UNINSTALL = 1;

    boolean close(boolean z);

    View getImplementingView();

    Launcher getLauncher();

    int getMode();

    void onDestroy();

    void open(boolean z);

    void setLoading();

    void setApplications(ArrayList<ApplicationItemInfo> arrayList);

    void setEmpty();

    void setError();

    void clearState();

    void setBackgroundAlpha(int i);

    void refreshPalette();

    void setDragController(DragController dragController);

    void setLauncher(Launcher launcher);

    void setMode(int i);

    void setNumColumns(int i);

    void setSystemBarInsets(int left, int top, int right, int bottom);

    void setSystemGestureInsets(Rect insets);
}
