package org.telegram.ui.DialogBuilder;

import android.content.Context;
import android.text.InputType;
import android.view.View;

import org.telegram.ui.Components.EditTextCaption;

public class PhoneEditTemplate extends EditTemplate {
    @Override
    public View create(Context context) {
        EditTextCaption editText = (EditTextCaption)super.create(context);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        return editText;
    }
}
