package org.telegram.ui.DialogBuilder;

import android.content.Context;
import android.view.View;

public interface ViewTemplate {
    View create(Context context);
    default boolean validate(View view) { return true; }
}
