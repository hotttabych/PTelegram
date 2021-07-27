package org.telegram.ui.DialogBuilder;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextCaption;

public class
EditTemplate implements ViewTemplate {
    String text;
    String name;
    boolean singleLine;

    @Override
    public View create(Context context) {
        EditTextCaption editText = new EditTextCaption(context);
        editText.setText(text);
        editText.setHint(name);
        editText.setSingleLine(singleLine);
        if (!singleLine) {
            editText.setMaxLines(6);
        }
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editText.setGravity(Gravity.BOTTOM);
        editText.setPadding(0, AndroidUtilities.dp(11), 0, AndroidUtilities.dp(12));
        editText.setTextColor(Theme.getColor(Theme.key_chat_messagePanelText));
        editText.setHintColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        editText.setHintTextColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        editText.setCursorColor(Theme.getColor(Theme.key_chat_messagePanelCursor));
        return editText;
    }

    @Override
    public boolean validate(View view) {
        if (view instanceof EditTextCaption) {
            EditTextCaption edit = (EditTextCaption)view;
            if (edit.getText().toString().isEmpty()) {
                edit.setError(name + " " + LocaleController.getString("CannotBeEmpty", R.string.CannotBeEmpty));
                return false;
            }
        }
        return true;
    }
}
