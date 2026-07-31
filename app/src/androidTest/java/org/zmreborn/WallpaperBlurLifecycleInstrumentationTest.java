package org.zmreborn;

import android.graphics.Bitmap;
import android.test.InstrumentationTestCase;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WallpaperBlurLifecycleInstrumentationTest extends InstrumentationTestCase {
    public void testClearedCacheDoesNotRecycleInstalledBitmap() {
        Bitmap source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        Bitmap blurred = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        try {
            WallpaperBlurCache cache = new WallpaperBlurCache();
            cache.begin(source);
            cache.complete(source, blurred);

            cache.clear();

            assertFalse("Installed backdrop bitmap must remain drawable",
                    blurred.isRecycled());
        } finally {
            recycle(blurred);
            recycle(source);
        }
    }

    public void testCancelledQueuedWallpaperTaskDoesNotRun() throws Exception {
        final CountDownLatch blockerStarted = new CountDownLatch(1);
        final CountDownLatch releaseBlocker = new CountDownLatch(1);
        final CountDownLatch cancelledTaskRan = new CountDownLatch(1);
        final CountDownLatch executorIdle = new CountDownLatch(1);
        Future<?> blocker = Launcher.executeWallpaperRefresh(new Runnable() {
            public void run() {
                blockerStarted.countDown();
                awaitRelease(releaseBlocker);
            }
        });
        try {
            assertTrue("Executor blocker must start",
                    blockerStarted.await(30, TimeUnit.SECONDS));
            Future<?> cancelled = Launcher.executeWallpaperRefresh(new Runnable() {
                public void run() {
                    cancelledTaskRan.countDown();
                }
            });

            assertTrue("Queued wallpaper task must cancel", cancelled.cancel(false));
            releaseBlocker.countDown();
            Launcher.runAfterWallpaperRefreshesForTests(new Runnable() {
                public void run() {
                    executorIdle.countDown();
                }
            });

            assertTrue("Wallpaper executor must become idle",
                    executorIdle.await(30, TimeUnit.SECONDS));
            assertEquals("Cancelled task must never run",
                    1L, cancelledTaskRan.getCount());
        } finally {
            releaseBlocker.countDown();
            blocker.cancel(true);
        }
    }

    private static void awaitRelease(CountDownLatch release) {
        try {
            release.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
        }
    }

    private static void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
