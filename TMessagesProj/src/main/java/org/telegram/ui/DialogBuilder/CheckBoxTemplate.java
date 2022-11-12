package org.telegram.ui.DialogBuilder;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

public class CheckBoxTemplate implements ViewTemplate {
    String name;
    boolean checked;
    DialogCheckBox.OnCheckedChangeListener onCheckedChangeListener;

    @Override
    public View create(Context context) {
        DialogCheckBox checkBox = new DialogCheckBox(context);
        checkBox.setTextAndCheck(name, checked);
        checkBox.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
        return checkBox;
    }
}
