package org.zmreborn;

import android.graphics.Rect;
import android.view.View;
import java.util.ArrayList;

/**
 * Common contract for all drawer representations (vertical grid and horizontal paging views).
 */
public interface ApplicationsView {
    int MODE_DEFAULT = 0;
    int MODE_UNINSTALL = 1;

    /**
     * Closes the applications view.
     *
     * @param animated whether to animate the closing transition
     * @return true if close sequence was accepted, false if intercepted (e.g. exiting uninstall mode first)
     */
    boolean close(boolean animated);

    /**
     * Returns the underlying Android View instance for layout and hierarchy manipulation.
     */
    View getImplementingView();

    /**
     * Returns the Launcher activity associated with this view.
     */
    Launcher getLauncher();

    /**
     * Returns the current drawer interaction mode (MODE_DEFAULT or MODE_UNINSTALL).
     */
    int getMode();

    /**
     * Performs view cleanup when the parent launcher activity is destroyed.
     */
    void onDestroy();

    /**
     * Opens and displays the applications view.
     *
     * @param animated whether to animate the opening transition
     */
    void open(boolean animated);

    /**
     * Puts the view in a loading state while items are being loaded from the database or package manager.
     */
    void setLoading();

    /**
     * Sets the complete list of launchable application items.
     *
     * @param applicationItemInfos list of installed and filtered applications
     */
    void setApplications(ArrayList<ApplicationItemInfo> applicationItemInfos);

    /**
     * Puts the view in an empty state when no applications are available.
     */
    void setEmpty();

    /**
     * Puts the view in an error state if application loading failed.
     */
    void setError();

    /**
     * Clears loading/error states and marks the view ready for interaction.
     */
    void clearState();

    /**
     * Sets the background surface alpha opacity [0..255].
     */
    void setBackgroundAlpha(int alpha);

    /**
     * Refreshes the dynamic theme palette and colors for this view and its children.
     */
    void refreshPalette();

    /**
     * Sets the drag controller for initiating workspace drag-and-drop operations.
     */
    void setDragController(DragController dragController);

    /**
     * Binds the owning Launcher activity.
     */
    void setLauncher(Launcher launcher);

    /**
     * Sets the drawer mode (MODE_DEFAULT or MODE_UNINSTALL).
     */
    void setMode(int mode);

    /**
     * Sets the column count for grid rendering.
     */
    void setNumColumns(int columns);

    /**
     * Insets content for edge-to-edge window system bars.
     */
    void setSystemBarInsets(int left, int top, int right, int bottom);

    /**
     * Sets system gesture exclusion/insets rect.
     */
    void setSystemGestureInsets(Rect insets);
}
