package org.zmreborn;

import android.view.View;

interface DropResultListener {
    void onDropCompleted(View target, boolean success, boolean targetFound);
}
