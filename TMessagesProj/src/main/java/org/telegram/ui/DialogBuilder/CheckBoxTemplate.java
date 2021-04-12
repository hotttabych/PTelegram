package org.telegram.ui.DialogBuilder;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class CheckBoxTemplate implements ViewTemplate {
    String name;
    boolean checked;

    @Override
    public View create(Context context) {
        CheckBox checkBox = new CheckBox(context);
        checkBox.setText(name);
        checkBox.setChecked(checked);
        checkBox.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        checkBox.setGravity(Gravity.BOTTOM);
        checkBox.setPadding(0, AndroidUtilities.dp(11), 0, AndroidUtilities.dp(12));
        checkBox.setTextColor(Theme.getColor(Theme.key_chat_messagePanelText));
        return checkBox;
    }
}
