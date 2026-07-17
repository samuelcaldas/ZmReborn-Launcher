package org.zeam;

import android.view.View;
import java.util.ArrayList;

public interface ApplicationsView {
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_UNINSTALL = 1;

    boolean close(boolean z);

    View getImplementingView();

    Launcher getLauncher();

    void onDestroy();

    void open(boolean z);

    void setApplications(ArrayList<ApplicationItemInfo> arrayList);

    void setBackgroundAlpha(int i);

    void setDragController(DragController dragController);

    void setLauncher(Launcher launcher);

    void setMode(int i);

    void setNumColumns(int i);
}
