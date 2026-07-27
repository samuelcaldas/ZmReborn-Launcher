package org.zmreborn;

import java.util.ArrayList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrawerStateTransitionsTest {
    @Test
    public void initialStateIsReady() {
        TestApplicationsView view = new TestApplicationsView();
        assertEquals("READY", view.getCurrentState());
    }

    @Test
    public void loadingTransitionFromReady() {
        TestApplicationsView view = new TestApplicationsView();
        view.setLoading();
        assertEquals("LOADING", view.getCurrentState());
    }

    @Test
    public void readyTransitionFromLoading() {
        TestApplicationsView view = new TestApplicationsView();
        view.setLoading();
        view.setApplications(createApplicationList(1));
        assertEquals("READY", view.getCurrentState());
    }

    @Test
    public void emptyTransitionFromLoading() {
        TestApplicationsView view = new TestApplicationsView();
        view.setLoading();
        view.setEmpty();
        assertEquals("EMPTY", view.getCurrentState());
    }

    @Test
    public void errorTransitionFromLoading() {
        TestApplicationsView view = new TestApplicationsView();
        view.setLoading();
        view.setError();
        assertEquals("ERROR", view.getCurrentState());
    }

    @Test
    public void retryTransitionFromError() {
        TestApplicationsView view = new TestApplicationsView();
        view.setLoading();
        view.setError();
        view.setLoading();
        assertEquals("LOADING", view.getCurrentState());
    }

    @Test
    public void clearStateResetsToReady() {
        TestApplicationsView view = new TestApplicationsView();
        view.setError();
        view.clearState();
        assertEquals("READY", view.getCurrentState());
    }

    @Test
    public void multipleApplicationsTransitionToReady() {
        TestApplicationsView view = new TestApplicationsView();
        view.setLoading();
        ArrayList<ApplicationItemInfo> apps = createApplicationList(5);
        view.setApplications(apps);
        assertEquals("READY", view.getCurrentState());
        assertTrue("Must have applications", view.hasApplications());
    }

    @Test
    public void emptyApplicationListTransitionsToEmpty() {
        TestApplicationsView view = new TestApplicationsView();
        view.setLoading();
        view.setApplications(createApplicationList(0));
        assertEquals("EMPTY", view.getCurrentState());
        assertFalse("Must not have applications", view.hasApplications());
    }

    @Test
    public void errorStateCanTransitionToReadyWithNewApplications() {
        TestApplicationsView view = new TestApplicationsView();
        view.setLoading();
        view.setError();
        view.setApplications(createApplicationList(3));
        assertEquals("READY", view.getCurrentState());
    }

    private static ArrayList<ApplicationItemInfo> createApplicationList(int count) {
        ArrayList<ApplicationItemInfo> apps = new ArrayList<ApplicationItemInfo>();
        for (int i = 0; i < count; i++) {
            ApplicationItemInfo info = new ApplicationItemInfo();
            info.title = "App " + i;
            apps.add(info);
        }
        return apps;
    }

    private static class TestApplicationsView {
        private String currentState = "READY";
        private ArrayList<ApplicationItemInfo> applications = new ArrayList<ApplicationItemInfo>();

        void setLoading() {
            currentState = "LOADING";
        }

        void setApplications(ArrayList<ApplicationItemInfo> apps) {
            applications = new ArrayList<ApplicationItemInfo>(apps);
            if (apps.isEmpty()) {
                currentState = "EMPTY";
            } else {
                currentState = "READY";
            }
        }

        void setEmpty() {
            currentState = "EMPTY";
            applications.clear();
        }

        void setError() {
            currentState = "ERROR";
        }

        void clearState() {
            currentState = "READY";
            applications.clear();
        }

        String getCurrentState() {
            return currentState;
        }

        boolean hasApplications() {
            return !applications.isEmpty();
        }
    }
}
