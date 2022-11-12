package org.telegram.ui.DialogBuilder;

import android.content.DialogInterface;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DialogTemplate {
    public String title;
    public List<ViewTemplate> viewTemplates = new ArrayList<>();
    public DialogType type;
    public Consumer<List<View>> positiveListener;
    public DialogInterface.OnClickListener negativeListener;

    public void addEditTemplate(String text, String name, boolean singleLine) {
        EditTemplate editTemplate = new EditTemplate();
        editTemplate.text = text;
        editTemplate.name = name;
        editTemplate.singleLine = singleLine;
        viewTemplates.add(editTemplate);
    }

    public void addPhoneEditTemplate(String text, String name, boolean singleLine) {
        PhoneEditTemplate editTemplate = new PhoneEditTemplate();
        editTemplate.text = text;
        editTemplate.name = name;
        editTemplate.singleLine = singleLine;
        viewTemplates.add(editTemplate);
    }

    public void addNumberEditTemplate(String text, String name, boolean singleLine) {
        NumberEditTemplate editTemplate = new NumberEditTemplate();
        editTemplate.text = text;
        editTemplate.name = name;
        editTemplate.singleLine = singleLine;
        viewTemplates.add(editTemplate);
    }

    public void addCheckboxTemplate(boolean checked, String name) {
        addCheckboxTemplate(checked, name, null);
    }

    public void addCheckboxTemplate(boolean checked, String name, DialogCheckBox.OnCheckedChangeListener onCheckedChangeListener) {
        CheckBoxTemplate checkBoxTemplate = new CheckBoxTemplate();
        checkBoxTemplate.name = name;
        checkBoxTemplate.checked = checked;
        checkBoxTemplate.onCheckedChangeListener = onCheckedChangeListener;
        viewTemplates.add(checkBoxTemplate);
    }
}
