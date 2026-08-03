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
    public static Grant ensure(final Instrumentation instrumentation,
            final Launcher launcher, final ComponentName provider) {
        validate(instrumentation, launcher, provider);
        UiAutomation automation = instrumentation.getUiAutomation();
        String userId = currentUserId(automation);
        Operations operations = operations(instrumentation, launcher, provider, automation,
                userId);
        return ensureGrant(operations, "Widget bind authority was not granted for " + provider);
    }

    static Grant ensure(Operations operations, String failureMessage) {
        return ensureGrant(operations, failureMessage);
    }

    static Grant ensureGrant(Operations operations, String failureMessage) {
        validateOperations(operations, failureMessage);
        if (operations.canBind()) {
            return new Grant(null);
        }
        try {
            operations.grant();
            verifyGranted(operations, failureMessage);
            return new Grant(operations);
        } catch (RuntimeException | Error failure) {
            rollback(operations, failure);
            throw failure;
        }
    }

    private static Operations operations(final Instrumentation instrumentation,
            final Launcher launcher, final ComponentName provider,
            final UiAutomation automation, final String userId) {
        return new Operations() {
            public boolean canBind() {
                return WidgetBindAuthority.canBind(instrumentation, launcher, provider);
            }
            public void grant() {
                runShell(automation, grantCommand(launcher, userId));
            }
            public void revoke() {
                runShell(automation, revokeCommand(launcher, userId));
            }
        };
    }

    private static void validateOperations(Operations operations, String failureMessage) {
        if (operations == null || failureMessage == null
                || failureMessage.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "Widget bind authority operations require failure context");
        }
    }

    private static void verifyGranted(Operations operations, String failureMessage) {
        if (!operations.canBind()) {
            throw new AssertionError(failureMessage);
        }
    }

    private static void rollback(Operations operations, Throwable failure) {
        try {
            operations.revoke();
        } catch (RuntimeException | Error rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    /** Represents authority provisioned by this utility. */
    public static final class Grant {
        private final Operations operations;
        private boolean revoked;

        private Grant(Operations operations) {
            this.operations = operations;
        }

        /** Revokes authority provisioned for this grant exactly once. */
        public void revoke() {
            if (operations == null || revoked) {
                return;
            }
            revoked = true;
            operations.revoke();
        }
    }

    private static String grantCommand(Launcher launcher, String userId) {
        return "appwidget grantbind --package " + launcher.getPackageName()
                + " --user " + userId;
    }

    private static String revokeCommand(Launcher launcher, String userId) {
        return "appwidget revokebind --package " + launcher.getPackageName()
                + " --user " + userId;
    }

    private static String currentUserId(UiAutomation automation) {
        String userId = runShell(automation, "am get-current-user").trim();
        if (!userId.matches("[0-9]+")) {
            throw new IllegalStateException("Current user ID is not decimal: " + userId);
        }
        try {
            return Integer.toString(Integer.parseInt(userId));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Current user ID exceeds integer range: " + userId,
                    exception);
        }
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

    private static String runShell(UiAutomation automation, String command) {
        if (automation == null || command == null || command.length() == 0) {
            throw new IllegalArgumentException("Widget bind shell command requires automation and command");
        }
        ParcelFileDescriptor descriptor = automation.executeShellCommand(command);
        if (descriptor == null) {
            throw new IllegalStateException("Widget bind shell command returned no output descriptor");
        }
        String output = readShellOutput(descriptor);
        rejectShellFailure(command, output);
        return output;
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
