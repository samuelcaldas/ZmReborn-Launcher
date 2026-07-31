package org.zmreborn.widget;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.zmreborn.CellLayout;
import org.zmreborn.R;

/** Loads and displays selectable widget preview cards off the main thread. */
public final class WidgetPickerDialog extends Dialog {
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final Callback callback;
    private Future<?> load;

    /** Receives picker selection and lifecycle events. */
    public interface Callback {
        /** Handles selected Search or external provider entry. */
        void onWidgetSelected(WidgetPickerDialog dialog, WidgetPickerEntry entry);

        /** Handles picker dismissal after cancellation or selection. */
        void onWidgetPickerDismissed(WidgetPickerDialog dialog);
    }

    /** Creates picker owned by active launcher context. */
    public WidgetPickerDialog(Context context, Callback callback) {
        super(context);
        if (callback == null) {
            throw new IllegalArgumentException("Widget picker requires callback");
        }
        this.callback = callback;
        setTitle(R.string.widget_picker_title);
        setContentView(R.layout.widget_picker_dialog);
        setCanceledOnTouchOutside(true);
        bindSelection();
        bindDismissal();
    }

    /** Shows picker and begins cancellable provider-preview loading. */
    public void show(final AppWidgetManager manager, final CellLayout targetLayout) {
        if (manager == null) {
            throw new IllegalArgumentException("Widget picker requires manager");
        }
        if (this.load != null) {
            throw new IllegalStateException("Widget picker loading already started");
        }
        super.show();
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        final Context context = getContext();
        this.load = LOADER.submit(new Runnable() {
            public void run() {
                loadEntries(context, manager, targetLayout);
            }
        });
    }

    private void loadEntries(Context context, AppWidgetManager manager,
            CellLayout targetLayout) {
        try {
            publishEntries(WidgetPickerCatalog.load(context, manager), targetLayout);
        } catch (SecurityException exception) {
            publishFailure();
        }
    }

    private void publishEntries(final List<WidgetPickerEntry> entries,
            final CellLayout targetLayout) {
        MAIN_HANDLER.post(new Runnable() {
            public void run() {
                if (!canPublish()) {
                    return;
                }
                List<WidgetPickerEntry> adjusted = WidgetPickerCatalog.applyTargetSpans(
                        getContext(), entries, targetLayout);
                ListView list = (ListView) findViewById(R.id.widget_picker_list);
                list.setAdapter(new WidgetPickerAdapter(getContext(), adjusted));
                list.setVisibility(View.VISIBLE);
                findViewById(R.id.widget_picker_progress).setVisibility(View.GONE);
            }
        });
    }

    private void publishFailure() {
        MAIN_HANDLER.post(new Runnable() {
            public void run() {
                if (!canPublish()) {
                    return;
                }
                findViewById(R.id.widget_picker_loading).setVisibility(View.GONE);
                TextView status = (TextView) findViewById(R.id.widget_picker_status);
                status.setText(R.string.widget_picker_load_error);
                status.setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean canPublish() {
        if (!isShowing() || this.load == null || this.load.isCancelled()) {
            return false;
        }
        Context context = getContext();
        if (!(context instanceof Activity)) {
            return true;
        }
        Activity activity = (Activity) context;
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private void bindSelection() {
        ListView list = (ListView) findViewById(R.id.widget_picker_list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                WidgetPickerEntry entry = (WidgetPickerEntry) parent.getItemAtPosition(position);
                callback.onWidgetSelected(WidgetPickerDialog.this, entry);
                dismiss();
            }
        });
    }

    private void bindDismissal() {
        setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                cancelLoad();
                callback.onWidgetPickerDismissed(WidgetPickerDialog.this);
            }
        });
    }

    private void cancelLoad() {
        if (this.load != null) {
            this.load.cancel(true);
        }
    }
}
