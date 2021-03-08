package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextCaption;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FakePasscodeDialogBuilder {
    public static class Template {
        public String title;
        public List<EditTemplate> editTemplates = new ArrayList<>();
        DialogType type;
        public Consumer<List<EditTextCaption>> positiveListener;
        public DialogInterface.OnClickListener negativeListener;

        public void addEditTemplate(String text, String name, boolean singleLine) {
            EditTemplate editTemplate = new EditTemplate();
            editTemplate.text = text;
            editTemplate.name = name;
            editTemplate.singleLine = singleLine;
            editTemplates.add(editTemplate);
        }
    }

    public static class EditTemplate {
        public String text;
        public String name;
        public boolean singleLine;
    }

    public enum DialogType {
        ADD,
        EDIT
    }

    public static AlertDialog build(Context context, Template template) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(template.title);

        List<EditTextCaption> edits = new ArrayList<>();
        for (EditTemplate editTemplate: template.editTemplates) {
            edits.add(createEditText(context, editTemplate));
        }
        LinearLayout layout = createLayout(dialogBuilder.getContext(), edits);
        dialogBuilder.setView(layout);

        if (template.type == DialogType.ADD) {
            dialogBuilder.setPositiveButton(LocaleController.getString("Add", R.string.Add), null);
            dialogBuilder.setNeutralButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        } else {
            dialogBuilder.setPositiveButton(LocaleController.getString("Change", R.string.Change), null);
            dialogBuilder.setNeutralButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            dialogBuilder.setNegativeButton(LocaleController.getString("Delete", R.string.Delete), template.negativeListener);
        }
        AlertDialog dialog = dialogBuilder.create();
        addPositiveButtonListener(dialog, template, edits);
        return dialog;
    }

    private static EditTextCaption createEditText(Context context, EditTemplate editTemplate) {
        EditTextCaption messageEditText = new EditTextCaption(context);
        messageEditText.setText(editTemplate.text);
        messageEditText.setHint(editTemplate.name);
        messageEditText.setSingleLine(editTemplate.singleLine);
        if (!editTemplate.singleLine) {
            messageEditText.setMaxLines(6);
        }
        messageEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        messageEditText.setGravity(Gravity.BOTTOM);
        messageEditText.setPadding(0, AndroidUtilities.dp(11), 0, AndroidUtilities.dp(12));
        messageEditText.setTextColor(Theme.getColor(Theme.key_chat_messagePanelText));
        messageEditText.setHintColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        messageEditText.setHintTextColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        messageEditText.setCursorColor(Theme.getColor(Theme.key_chat_messagePanelCursor));
        return messageEditText;
    }

    private static LinearLayout createLayout(Context context, List<EditTextCaption> edits) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(30, 0, 30, 0);
        for (EditTextCaption edit: edits) {
            layout.addView(edit, lp);
        }
        return layout;
    }

    private static void addPositiveButtonListener(AlertDialog dialog, Template template, List<EditTextCaption> edits) {
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                boolean error = false;
                for (int i = 0; i < edits.size(); i++) {
                    EditTextCaption edit = edits.get(i);
                    if (edit.getText().toString().isEmpty()) {
                        error = true;
                        edit.setError(template.editTemplates.get(i).name + " " + LocaleController.getString("CannotBeEmpty", R.string.CannotBeEmpty));
                    }
                }
                if (!error) {
                    template.positiveListener.accept(edits);
                    dialog.dismiss();
                }
            });
        });
    }
}
