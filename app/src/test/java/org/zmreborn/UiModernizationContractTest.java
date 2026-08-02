package org.zmreborn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UiModernizationContractTest {
    @Test
    public void homeStaysBlankAndDockUsesResolvedHandlers() throws Exception {
        String launcher = read("main/java/org/zmreborn/Launcher.java");
        String indicator = read("main/java/org/zmreborn/ScreenIndicator.java");
        String manifest = read("main/AndroidManifest.xml");
        assertTrue(launcher.contains("emptyTip.setVisibility(View.GONE)"));
        assertTrue(launcher.contains("resolveDefaultDockApplications()"));
        assertFalse(launcher.contains("dock.sendDrop(new ApplicationsGridItemInfo"));
        assertTrue(indicator.contains("return this.mItems > 1"));
        assertTrue(indicator.contains("params.gravity = Gravity.BOTTOM"));
        assertDefaultHandlerQueries(manifest);
    }

    @Test
    public void verticalDrawerIsDefaultResponsiveAndCacheFree() throws Exception {
        String defaults = read("main/res/values/defaults.xml");
        String launcher = read("main/java/org/zmreborn/Launcher.java");
        String manifest = read("main/AndroidManifest.xml");
        String grid = read("main/java/org/zmreborn/ApplicationsGridView.java");
        String paging = read("main/java/org/zmreborn/ApplicationsPagingView.java");
        assertTrue(defaults.contains(
                "name=\"preferences_default_apps_grid_type\" translatable=\"false\">1</string>"));
        assertTrue(grid.contains("super.setNumColumns(AUTO_FIT)"));
        assertTrue(grid.contains("R.dimen.drawer_cell_preferred_width"));
        assertFalse(grid.contains("smoothScrollBy"));
        assertTrue(grid.contains("!this.mActionsEnabled || this.mClosing"));
        assertFalse(paging.contains("System.gc()"));
        assertFalse(paging.contains("setDrawingCacheEnabled(true)"));
        assertTrue(paging.contains("!this.mActionsEnabled || this.mClosing"));
        assertTrue(launcher.contains(
                "!PreferencesUtil.rememberApplicationsPosition(Launcher.this)"));
        assertTrue(launcher.contains(
                "this.mApplicationsView.setSystemBarInsets(0, 0, 0, 0)"));
        assertTrue(manifest.contains("android:supportsRtl=\"true\""));
        assertDrawerLayoutsHaveNoFixedColumns();
    }

    @Test
    public void verticalDrawerUsesIntegratedSearchStableDataAndSharedSpacing() throws Exception {
        String drawer = read("main/java/org/zmreborn/ApplicationsDrawerView.java");
        String filter = read("main/java/org/zmreborn/DrawerSearchFilter.java");
        String adapter = read("main/java/org/zmreborn/ApplicationsAdapter.java");
        String portrait = read("main/res/layout-port/apps_grid_view.xml");
        String landscape = read("main/res/layout-land/apps_grid_view.xml");
        assertTrue(portrait.contains("org.zmreborn.ApplicationsDrawerView"));
        assertTrue(landscape.contains("org.zmreborn.ApplicationsDrawerView"));
        assertTrue(portrait.contains("@+id/drawer_search_input"));
        assertTrue(landscape.contains("@+id/drawer_search_input"));
        assertTrue(portrait.contains("android:verticalSpacing=\"@dimen/drawer_grid_spacing\""));
        assertTrue(landscape.contains("android:verticalSpacing=\"@dimen/drawer_grid_spacing\""));
        assertTrue(drawer.contains("captureScrollState()"));
        assertTrue(drawer.contains("setSelectionFromTop(position, top)"));
        assertTrue(filter.contains("prefixMatches.addAll(containsMatches)"));
        String fastScroll = read("main/java/org/zmreborn/DrawerFastScrollView.java");
        String alphabetIndex = read("main/java/org/zmreborn/DrawerAlphabetIndex.java");
        String grid = read("main/java/org/zmreborn/ApplicationsGridView.java");
        String indexReset = sourceSection(fastScroll,
                "    void setIndex(DrawerAlphabetIndex index)",
                "    void setOnSectionSelectedListener");
        assertTrue(adapter.contains("public boolean hasStableIds()"));
        assertTrue(adapter.contains("snapshot(applicationItemInfos)"));
        assertFalse(adapter.contains("application.icon ="));
        assertFalse(adapter.contains("application.filtered ="));
        assertTrue(portrait.contains("@+id/drawer_fast_scroll"));
        assertTrue(landscape.contains("@+id/drawer_fast_scroll"));
        assertTrue(drawer.contains("updateFastScroll()"));
        assertTrue(drawer.contains("setOnSectionSelectedListener"));
        assertTrue(drawer.contains("this.mGridView.setFastScrollVisible(this.mFastScrollEnabled)"));
        assertTrue(drawer.contains("this.mClosing || position < 0"));
        assertTrue(grid.contains("void setFastScrollVisible(boolean visible)"));
        assertTrue(grid.contains("onRtlPropertiesChanged(int layoutDirection)"));
        assertTrue(grid.contains("setNextFocusLeftId(railId)"));
        assertTrue(grid.contains("fastScrollInsetLeft"));
        assertTrue(drawer.contains("updateFastScrollFocus(this.mFastScrollEnabled)"));
        assertTrue(drawer.contains("updateSearchFocus()"));
        assertTrue(drawer.contains("setDrawerControlsEnabled(false)"));
        assertTrue(grid.contains("setEnabled(this.mActionsEnabled && !this.mClosing)"));
        assertFalse(fastScroll.contains("setSelectionFromTop"));
        assertTrue(fastScroll.contains("onTouchEvent(MotionEvent event)"));
        assertTrue(fastScroll.contains("AccessibilityNodeProvider"));
        assertTrue(fastScroll.contains("onKeyDown(int keyCode, KeyEvent event)"));
        assertTrue(indexReset.indexOf("this.mSelectedIndex = -1")
                < indexReset.indexOf("rebuildDisplayedIndex()"));
        assertTrue(fastScroll.contains("virtualViewIdForSection"));
        assertTrue(alphabetIndex.contains("DrawerSearchFilter.normalize"));
        assertTrue(alphabetIndex.contains("compact(int maximumSections)"));
        assertTrue(alphabetIndex.contains("compact(int maximumSections, String retainedSection)"));
        assertTrue(alphabetIndex.contains("TreeMap<String, Integer>"));
    }

    @Test
    public void applicationIconsUseSharedAdaptiveSafeNormalization() throws Exception {
        String utilities = read("main/java/org/zmreborn/Utilities.java");
        String compatibility = read("main/java/org/zmreborn/compat/AdaptiveIconCompat.java");
        String model = read("main/java/org/zmreborn/LauncherModel.java");
        assertTrue(utilities.contains("R.drawable.ic_launcher_application"));
        assertTrue(utilities.contains("AdaptiveIconCompat.isAdaptiveIcon"));
        assertTrue(utilities.contains("return resolvedIcon"));
        assertTrue(utilities.contains("return rasterizeDrawableCopy(normalizedIcon)"));
        assertTrue(utilities.contains("boundIcon.setBounds(0, 0, sIconWidth, sIconHeight)"));
        assertTrue(utilities.contains("view.setCompoundDrawables(null, boundIcon, null, null)"));
        assertTrue(utilities.contains("copyDrawable(resolvedIcon, context)"));
        assertFalse(utilities.contains("java.lang.reflect"));
        assertTrue(compatibility.contains("Build.VERSION.SDK_INT < 26"));
        assertTrue(model.contains("Utilities.normalizeApplicationIcon"));
    }

    @Test
    public void wallpaperPaletteRefreshesOffMainThreadAndReapplies() throws Exception {
        String launcher = read("main/java/org/zmreborn/Launcher.java");
        String applicationsView = read("main/java/org/zmreborn/ApplicationsView.java");
        String grid = read("main/java/org/zmreborn/ApplicationsGridView.java");
        String paging = read("main/java/org/zmreborn/ApplicationsPagingView.java");
        String folder = read("main/java/org/zmreborn/Folder.java");
        String userFolder = read("main/java/org/zmreborn/UserFolder.java");
        String refreshMethods = sourceSection(launcher,
                "    private void refreshWallpaperColorsAsync()",
                "    private void applyWallpaperPalette()");
        String pagingPaletteRefresh = sourceSection(paging,
                "    public void refreshPalette()",
                "    public void setLoading()");
        String userFolderPaletteRefresh = sourceSection(userFolder,
                "    @Override\n    void refreshPalette()",
                "    private void updateSignalRail()");
        String renameDialog = sourceSection(launcher,
                "    private class RenameFolderDialog {",
                "    private class LauncherDialog");
        String receiver = sourceSection(launcher,
                "    private static class WallpaperIntentReceiver extends BroadcastReceiver {",
                "    public void setWindowBackground(boolean liveWallpaper)");
        String testDispatch = sourceSection(launcher,
                "    static void dispatchWallpaperRefreshForTests(Context context)",
                "    private void startLoaders()");
        assertTrue(launcher.contains(
                "sWallpaperRefreshExecutor = Executors.newSingleThreadExecutor()"));
        assertFalse(refreshMethods.contains("AsyncTask"));
        assertFalse(receiver.contains("AsyncTask"));
        assertTrue(receiver.contains("PendingResult pendingResult = goAsync()"));
        assertFalse(receiver.contains("final PendingResult pendingResult = goAsync()"));
        assertTrue(receiver.contains("LocaleUtil.wrap(context.getApplicationContext())"));
        assertTrue(receiver.contains("dispatchWallpaperRefresh(context, pendingResult)"));
        assertTrue(testDispatch.contains(
                "wallpaperReceiver.dispatchWallpaperRefresh(context, null)"));
        assertTrue(launcher.contains("applyWallpaperPalette();"));
        assertTrue(refreshMethods.indexOf("Launcher.this.runOnUiThread") >= 0
                && refreshMethods.indexOf("Launcher.this.runOnUiThread")
                < refreshMethods.indexOf("applyWallpaperPalette();"));
        assertTrue(launcher.contains("WallpaperColorExtractor.getSurfaceVariant(this)"));
        assertTrue(launcher.contains("this.mApplicationsView.refreshPalette();"));
        assertTrue(launcher.contains("folder.refreshPalette();"));
        assertTrue(launcher.contains("this.mAppListFolderPaletteBinder.apply(this);"));
        assertTrue(applicationsView.contains("void refreshPalette();"));
        assertFalse(applicationsView.contains("default void refreshPalette()"));
        assertTrue(grid.contains("WallpaperColorExtractor.getSurface(getContext())"));
        assertTrue(grid.contains("applicationsAdapter.notifyDataSetChanged();"));
        assertTrue(paging.contains("((ApplicationsPageView) page).refreshPalette();"));
        assertTrue(pagingPaletteRefresh.contains("this.mScreenIndicator.refreshPalette();"));
        assertTrue(userFolderPaletteRefresh.contains("super.refreshPalette();"));
        assertTrue(userFolderPaletteRefresh.contains("this.mSignalRail.refreshPalette();"));
        assertTrue(userFolderPaletteRefresh.indexOf("super.refreshPalette();")
                < userFolderPaletteRefresh.indexOf("this.mSignalRail.refreshPalette();"));
        assertTrue(renameDialog.contains("WallpaperColorExtractor.getSurface(Launcher.this)"));
        assertTrue(renameDialog.contains("WallpaperColorExtractor.getSurfaceVariant(Launcher.this)"));
        assertTrue(renameDialog.contains("WallpaperColorExtractor.getOnSurface(Launcher.this)"));
        assertTrue(renameDialog.contains("WallpaperColorExtractor.getPrimary(Launcher.this)"));
        assertFalse(renameDialog.contains("getColor(R.color.zm_reborn_"));
        assertTrue(folder.contains("WallpaperColorExtractor.getOnSurface(context)"));
    }

    @Test
    public void widgetPlacementSurvivesDeferralAndUsesTargetGeometry() throws Exception {
        String launcher = read("main/java/org/zmreborn/Launcher.java");
        String stateSaving = sourceSection(launcher,
                "    public void onSaveInstanceState(Bundle outState)",
                "    public void onDestroy()");
        String stateRestoration = sourceSection(launcher,
                "    private void restoreState(Bundle savedState)",
                "    private void setupViews()");
        String widgetInfoCreation = sourceSection(launcher,
                "    private LauncherAppWidgetInfo createAppWidgetInfo(",
                "    private void bindNewAppWidget(");
        String widgetBinding = sourceSection(launcher,
                "    private void bindNewAppWidget(",
                "    private void updateAppWidgetSizeOptions(");
        String widgetSizeOptions = sourceSection(launcher,
                "    private void updateAppWidgetSizeOptions(",
                "    public static int getScreenCount(Context context)");
        assertTrue(stateSaving.contains(
                "outState.putBoolean(RUNTIME_STATE_PENDING_APPWIDGET_PLACEMENT,\n"
                        + "                this.mPendingAppWidgetPlacement);"));
        assertTrue(stateRestoration.contains(
                "RUNTIME_STATE_PENDING_APPWIDGET_PLACEMENT, false"));
        assertTrue(stateRestoration.contains("resumePendingAppWidgetPlacement();"));
        assertTrue(widgetInfoCreation.contains("cellInfo.screen, xy[0], xy[1], false"));
        assertTrue(widgetBinding.contains("this.mWorkspace.addInScreen(widgetInfo.hostView,"));
        assertTrue(widgetBinding.contains("widgetInfo.screen, xy[0], xy[1],"));
        assertTrue(widgetSizeOptions.contains(
                "if (targetLayout.isWidgetSizingGeometryReady())"));
        assertTrue(widgetSizeOptions.contains(
                "targetLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener()"));
        assertTrue(widgetSizeOptions.contains(
                "if (!targetLayout.isWidgetSizingGeometryReady())"));
        assertTrue(widgetSizeOptions.contains("targetLayout.removeOnLayoutChangeListener(this);"));
        assertTrue(widgetSizeOptions.contains(
                "this.mAppWidgetManager.updateAppWidgetOptions("));
    }

    @Test
    public void resizableWidgetsUseDirectCellSnappedOverlay() throws Exception {
        String launcher = read("main/java/org/zmreborn/Launcher.java");
        String cellLayout = read("main/java/org/zmreborn/CellLayout.java");
        String resizeFrame = read("main/java/org/zmreborn/WidgetResizeFrame.java");
        String strings = read("main/res/values/strings.xml");
        String portugueseStrings = read("main/res/values-pt-rBR/strings.xml");
        String commit = sourceSection(launcher,
                "    private void commitWidgetResize(",
                "    private void dismissWidgetResize(WidgetResizeSession session)");
        assertTrue(launcher.contains(
                "!showWidgetResize(pressedView, cellInfo)"));
        assertTrue(launcher.contains("this.launcher.startWidgetDrag(this)"));
        assertTrue(launcher.contains(
                "providerInfo.resizeMode == AppWidgetProviderInfo.RESIZE_NONE"));
        assertTrue(launcher.contains("new FrameLayout.LayoutParams(-1, -1)"));
        assertTrue(launcher.contains("if (dismissWidgetResize())"));
        assertTrue(launcher.contains("dismissWidgetResize();\n        super.onPause();"));
        assertTrue(commit.contains("applyResizeCandidate(session.widgetView, candidate)"));
        assertTrue(commit.contains("LauncherModel.updateItemInDatabase(this, session.widgetInfo)"));
        assertTrue(commit.contains("updateAppWidgetSizeOptions(session.widgetInfo)"));
        assertTrue(commit.indexOf("LauncherModel.updateItemInDatabase")
                < commit.indexOf("updateAppWidgetSizeOptions"));
        assertTrue(cellLayout.contains("findOccupiedCells(xCount, yCount, this.mOccupied, ignoredView)"));
        assertTrue(cellLayout.contains("static ResizeCandidate calculateResizeCandidate"));
        assertTrue(cellLayout.contains("throw new IllegalArgumentException(\"Unknown resize edge\")"));
        assertTrue(resizeFrame.contains("private static final int HANDLE_SIZE_DP = 48"));
        assertTrue(resizeFrame.contains("AppWidgetProviderInfo.RESIZE_HORIZONTAL"));
        assertTrue(resizeFrame.contains("AppWidgetProviderInfo.RESIZE_VERTICAL"));
        assertTrue(resizeFrame.contains("R.string.widget_resize_invalid_span"));
        assertTrue(resizeFrame.contains("setContentDescription"));
        assertTrue(strings.contains("name=\"widget_resize_handle_bottom_right\""));
        assertTrue(portugueseStrings.contains("name=\"widget_resize_handle_bottom_right\""));
        assertFalse(launcher.contains("R.layout.widget_span"));
    }

    @Test
    public void dockDrawerOpenButtonUsesOneUiStyleOblongSelector() throws Exception {
        String dock = read("main/java/org/zmreborn/Dock.java");
        String selector = read("main/java/org/zmreborn/SelectorDrawable.java");
        assertTrue("Dock must give the drawer-open button (itemType 6) an oblong pill selector "
                        + "instead of the shared rounded-rect dock selector",
                dock.contains("itemInfo.itemType == 6")
                        && dock.contains("SelectorDrawable.createOblongSelector(getContext())"));
        assertTrue("SelectorDrawable must expose a fully-rounded oblong/pill selector factory",
                selector.contains("static SelectorDrawable createOblongSelector(Context context)"));
    }

    @Test
    public void dialogListItemUsesThemeAwareTextColor() throws Exception {
        String dialogListItem = read("main/res/layout/dialog_list_item.xml");
        assertTrue("dialog_list_item.xml must set a theme-aware android:textColor "
                        + "so long-press menu text is readable in dark mode",
                dialogListItem.contains("android:textColor=\"@color/m3_on_surface\""));
    }

    private static void assertDefaultHandlerQueries(String manifest) {
        assertTrue(manifest.contains("android.intent.action.DIAL"));
        assertTrue(manifest.contains("android.intent.action.SENDTO"));
        assertTrue(manifest.contains("android.media.action.IMAGE_CAPTURE"));
        assertTrue(manifest.contains("android:scheme=\"https\""));
        assertTrue(manifest.contains("android:host=\"com.android.contacts\""));
    }

    @Test
    public void fastScrollClearsSelectionOnGridScroll() throws Exception {
        String drawer = read("main/java/org/zmreborn/ApplicationsDrawerView.java");
        assertTrue("ApplicationsDrawerView must call clearSelection() from a scroll listener",
                drawer.contains("clearSelection()"));
        assertTrue("ApplicationsDrawerView must register OnScrollListener on mGridView",
                drawer.contains("setOnScrollListener"));
        assertTrue("ApplicationsDrawerView must handle scroll-state changes",
                drawer.contains("onGridScrollStateChanged"));
    }

    @Test
    public void fastScrollNotShownDirectlyInUpdateMethod() throws Exception {
        String drawer = read("main/java/org/zmreborn/ApplicationsDrawerView.java");
        String updateFastScrollBody = sourceSection(drawer,
                "    private void updateFastScroll()",
                "    private void updateFastScrollFocus");
        assertFalse("updateFastScroll must not call setVisibility(VISIBLE) directly",
                updateFastScrollBody.contains("VISIBLE"));
        assertTrue("updateFastScroll must set mFastScrollEnabled",
                updateFastScrollBody.contains("mFastScrollEnabled"));
    }

    @Test
    public void searchBarCollapsedByDefaultRevealedOnPull() throws Exception {
        String drawer = read("main/java/org/zmreborn/ApplicationsDrawerView.java");
        assertTrue("ApplicationsDrawerView must have collapseSearchBar method",
                drawer.contains("collapseSearchBar()"));
        assertTrue("ApplicationsDrawerView must store max height of search bar",
                drawer.contains("mSearchBarMaxHeight"));
        assertTrue("ApplicationsDrawerView must override onInterceptTouchEvent for pull-to-reveal",
                drawer.contains("onInterceptTouchEvent"));
    }

    @Test
    public void pagingViewRestoresPositionByItemOrdinalOnRebuild() throws Exception {
        String paging = read("main/java/org/zmreborn/ApplicationsPagingView.java");
        assertTrue("ApplicationsPagingView must use pageIndexForItemOrdinal for position restoration",
                paging.contains("pageIndexForItemOrdinal"));
        assertTrue("ApplicationsPagingView must capture the first visible ordinal before rebuild",
                paging.contains("captureFirstVisibleOrdinal"));
        assertTrue("ApplicationsPagingView must react to viewport changes",
                paging.contains("onPagerViewportChanged"));
    }

    @Test
    public void pagingLayoutsUseDimensionForDockExclusionNotHardcodedDp() throws Exception {
        String portrait = read("main/res/layout-port/apps_paging_view.xml");
        String landscape = read("main/res/layout-land/apps_paging_view.xml");
        assertFalse("portrait paging layout must not contain 40dp literal",
                portrait.contains("40dp"));
        assertFalse("landscape paging layout must not contain 40dp literal",
                landscape.contains("40dp"));
        assertTrue("portrait paging layout must reference navigation_strip_size",
                portrait.contains("navigation_strip_size"));
        assertTrue("landscape paging layout must reference navigation_strip_size",
                landscape.contains("navigation_strip_size"));
    }

    @Test
    public void pagingPageAndCellLayoutsHaveEquivalentContentPaddingAcrossOrientations()
            throws Exception {
        String pagePort = read("main/res/layout-port/apps_page_view.xml");
        String pageLand = read("main/res/layout-land/apps_page_view.xml");
        String cellPort = read("main/res/layout-port/application_boxed_page.xml");
        String cellLand = read("main/res/layout-land/application_boxed_page.xml");
        assertFalse("landscape page view must not have asymmetric end padding 30dp",
                pageLand.contains("30dp"));
        assertTrue("landscape cell must define minHeight like portrait cell",
                cellLand.contains("label_min_height"));
    }

    private static void assertDrawerLayoutsHaveNoFixedColumns() throws Exception {
        String portrait = read("main/res/layout-port/apps_grid_view.xml");
        String landscape = read("main/res/layout-land/apps_grid_view.xml");
        assertFalse(portrait.contains("android:numColumns"));
        assertFalse(landscape.contains("android:numColumns"));
    }

    private static String sourceSection(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        assertTrue("Missing source section: " + startMarker, start >= 0);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue("Missing source section end: " + endMarker, end > start);
        return source.substring(start, end);
    }

    private static String read(String relativePath) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(sourceFile(relativePath)));
        StringBuilder content = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        return content.toString();
    }

    private static File sourceFile(String relativePath) {
        File workingDirectory = new File(System.getProperty("user.dir"));
        File moduleFile = new File(workingDirectory, "src/" + relativePath);
        if (moduleFile.isFile()) {
            return moduleFile;
        }
        return new File(workingDirectory, "app/src/" + relativePath);
    }
}
