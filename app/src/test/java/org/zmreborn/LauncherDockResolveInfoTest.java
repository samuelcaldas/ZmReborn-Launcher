package org.zmreborn;

import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class LauncherDockResolveInfoTest {
    @Test
    public void frameworkResolverFallsBackToStableNonFrameworkCandidate() {
        ResolveInfo frameworkResolver = resolveInfo("android", "ResolverActivity");
        ResolveInfo last = resolveInfo("com.example.zeta", "LastActivity");
        ResolveInfo later = resolveInfo("com.example.alpha", "ZuluActivity");
        ResolveInfo first = resolveInfo("com.example.alpha", "AlphaActivity");

        ResolveInfo selected = Launcher.selectDockResolveInfo(frameworkResolver,
                Arrays.asList(last, later, first));

        assertSame(first, selected);
    }

    @Test
    public void validDirectResolutionWinsOverCandidates() {
        ResolveInfo direct = resolveInfo("com.example.direct", "DirectActivity");
        ResolveInfo candidate = resolveInfo("com.example.alpha", "AlphaActivity");

        ResolveInfo selected = Launcher.selectDockResolveInfo(direct,
                Collections.singletonList(candidate));

        assertSame(direct, selected);
    }

    @Test
    public void noValidCandidatesReturnsNull() {
        ResolveInfo frameworkResolver = resolveInfo("android", "ResolverActivity");

        ResolveInfo selected = Launcher.selectDockResolveInfo(frameworkResolver,
                Collections.<ResolveInfo>emptyList());

        assertNull(selected);
    }

    private static ResolveInfo resolveInfo(String packageName, String activityName) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;
        resolveInfo.activityInfo.name = activityName;
        return resolveInfo;
    }
}
