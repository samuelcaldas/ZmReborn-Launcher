package org.zmreborn.widget;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.ParcelFileDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.zmreborn.Launcher;

/** Provisions and revokes launcher widget-bind authority for external fixture tests. */
public final class WidgetBindAuthority {
    private WidgetBindAuthority() {
    }

    /** Grants bind authority only when launcher cannot already bind fixture provider. */
    public static boolean ensure(final Instrumentation instrumentation,
            final Launcher launcher, final ComponentName provider) {
        validate(instrumentation, launcher, provider);
        return ensure(new Operations() {
            public boolean canBind() {
                return WidgetBindAuthority.canBind(
                        instrumentation, launcher, provider);
            }

            public void grant() {
                runShell(instrumentation.getUiAutomation(),
                        grantCommand(launcher));
            }

            public void revoke() {
                WidgetBindAuthority.revoke(instrumentation, launcher);
            }
        }, "Widget bind authority was not granted for " + provider);
    }

    static boolean ensure(Operations operations, String failureMessage) {
        if (operations == null || failureMessage == null
                || failureMessage.length() == 0) {
            throw new IllegalArgumentException(
                    "Widget bind authority operations require failure context");
        }
        if (operations.canBind()) {
            return false;
        }
        try {
            operations.grant();
            if (!operations.canBind()) {
                throw new AssertionError(failureMessage);
            }
            return true;
        } catch (RuntimeException | Error failure) {
            rollback(operations, failure);
            throw failure;
        }
    }

    /** Revokes authority granted by {@link #ensure(Instrumentation, Launcher, ComponentName)}. */
    public static void revoke(Instrumentation instrumentation, Launcher launcher) {
        if (instrumentation == null || launcher == null) {
            throw new IllegalArgumentException("Widget bind revocation requires instrumentation and launcher");
        }
        runShell(instrumentation.getUiAutomation(), revokeCommand(launcher));
    }

    private static void rollback(Operations operations, Throwable failure) {
        try {
            operations.revoke();
        } catch (RuntimeException | Error rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private static String grantCommand(Launcher launcher) {
        return "appwidget grantbind --package "
                + launcher.getPackageName() + " --user current";
    }

    private static String revokeCommand(Launcher launcher) {
        return "appwidget revokebind --package "
                + launcher.getPackageName() + " --user current";
    }

    private static void validate(Instrumentation instrumentation, Launcher launcher,
            ComponentName provider) {
        if (instrumentation == null || launcher == null || provider == null) {
            throw new IllegalArgumentException("Widget bind authority requires instrumentation, launcher, and provider");
        }
    }

    private static boolean canBind(final Instrumentation instrumentation,
            final Launcher launcher, final ComponentName provider) {
        final boolean[] allowed = new boolean[1];
        instrumentation.runOnMainSync(new Runnable() {
            public void run() {
                int appWidgetId = launcher.getAppWidgetHost().allocateAppWidgetId();
                try {
                    allowed[0] = AppWidgetManager.getInstance(launcher)
                            .bindAppWidgetIdIfAllowed(appWidgetId, provider);
                } finally {
                    launcher.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
                }
            }
        });
        return allowed[0];
    }

    private static void runShell(UiAutomation automation, String command) {
        if (automation == null || command == null || command.length() == 0) {
            throw new IllegalArgumentException("Widget bind shell command requires automation and command");
        }
        ParcelFileDescriptor descriptor = automation.executeShellCommand(command);
        if (descriptor == null) {
            throw new IllegalStateException("Widget bind shell command returned no output descriptor");
        }
        String output = readShellOutput(descriptor);
        rejectShellFailure(command, output);
    }

    private static String readShellOutput(ParcelFileDescriptor descriptor) {
        try (InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[256];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read widget bind shell output", exception);
        }
    }

    private static void rejectShellFailure(String command, String output) {
        String normalized = output.toLowerCase(Locale.US);
        if (normalized.contains("error") || normalized.contains("exception")
                || normalized.contains("denied") || normalized.contains("usage:")) {
            throw new IllegalStateException("Widget bind command failed: " + command
                    + " output=" + output);
        }
    }

    interface Operations {
        boolean canBind();

        void grant();

        void revoke();
    }
}
