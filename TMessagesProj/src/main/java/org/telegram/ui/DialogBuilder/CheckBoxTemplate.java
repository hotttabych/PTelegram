package org.telegram.ui.DialogBuilder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class CheckBoxTemplate implements ViewTemplate {
    String name;
    boolean checked;
    boolean enabled;
    DialogCheckBox.OnCheckedChangeListener onCheckedChangeListener;

    @Override
    public View create(Context context) {
        DialogCheckBox checkBox = new DialogCheckBox(context);
        checkBox.setTextAndCheck(name, checked);
        checkBox.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        checkBox.setEnabled(enabled);
        checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
        return checkBox;
    }
}
