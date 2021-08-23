package org.telegram.ui.DialogBuilder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.core.widget.CompoundButtonCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class CheckBoxTemplate implements ViewTemplate {
    String name;
    boolean checked;
    boolean enabled;
    CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

    @Override
    public View create(Context context) {
        CheckBox checkBox = new CheckBox(context);
        checkBox.setText(name);
        checkBox.setChecked(checked);
        checkBox.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        checkBox.setGravity(Gravity.BOTTOM);
        checkBox.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        checkBox.setTextColor(Theme.getColor(Theme.key_chat_messagePanelText));
        CompoundButtonCompat.setButtonTintList(checkBox, getTintList());
        checkBox.setEnabled(enabled);
        checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
        return checkBox;
    }

    static private ColorStateList getTintList() {
        return new ColorStateList(new int[][]{
                {-android.R.attr.state_enabled},
                {android.R.attr.state_enabled, -android.R.attr.state_checked},
                {android.R.attr.state_enabled, android.R.attr.state_checked}
            }, new int[] {
                Theme.getColor(Theme.key_dialogCheckboxSquareDisabled),
                Theme.getColor(Theme.key_dialogCheckboxSquareUnchecked),
                Theme.getColor(Theme.key_dialogCheckboxSquareCheck)
            });
    }
}
