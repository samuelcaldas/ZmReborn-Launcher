package org.zmreborn;

import android.graphics.drawable.Drawable;
import android.view.View;
import org.zmreborn.theme.FrostedGlassDrawable;
import org.zmreborn.theme.WallpaperColorExtractor;

/** Applies one cached wallpaper/frost treatment to workspace sibling surfaces. */
final class WorkspaceBlurController {
    private final WallpaperBlurGenerator generator;
    private final Workspace workspace;

    WorkspaceBlurController(Workspace workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("Workspace must not be null");
        }
        this.workspace = workspace;
        this.generator = new WallpaperBlurGenerator(workspace);
    }

    void apply(View dock, View drawer, int drawerAlpha) {
        validateTarget(dock, "Dock");
        validateTarget(drawer, "Drawer");
        Drawable dockBackdrop = this.generator.createBackdrop(dock);
        Drawable drawerBackdrop = this.generator.createBackdrop(drawer);
        int dockTint = WallpaperColorExtractor.getSurfaceVariant(this.workspace.getContext());
        int drawerTint = WallpaperColorExtractor.getSurface(this.workspace.getContext());
        dock.setBackground(FrostedGlassDrawable.forDock(dockBackdrop, dockTint));
        drawer.setBackground(FrostedGlassDrawable.forDrawer(
                drawerBackdrop, drawerTint, drawerAlpha));
        dock.invalidate();
        drawer.invalidate();
    }

    void refresh() {
        this.generator.refresh();
    }

    void destroy() {
        this.generator.destroy();
    }

    private void validateTarget(View target, String label) {
        if (target == null) {
            throw new IllegalArgumentException(label + " background target must not be null");
        }
        if (target.getParent() != this.workspace.getParent()) {
            throw new IllegalStateException(label + " and workspace must share a parent");
        }
    }
}
