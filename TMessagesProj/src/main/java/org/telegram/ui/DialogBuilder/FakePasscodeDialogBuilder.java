package org.telegram.ui.DialogBuilder;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.EditTextCaption;

import java.util.ArrayList;
import java.util.List;

public class FakePasscodeDialogBuilder {
    public static List<View> views;

    public static AlertDialog build(Context context, DialogTemplate template) {
        return buildAndGetViews(context, template, new ArrayList<>());
    }

    public static AlertDialog buildAndGetViews(Context context, DialogTemplate template, List<View> viewsOutput) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(template.title);

        for (ViewTemplate viewTemplate: template.viewTemplates) {
            viewsOutput.add(viewTemplate.create(context));
        }
        LinearLayout layout = createLayout(dialogBuilder.getContext(), viewsOutput);
        dialogBuilder.setView(layout);

        if (template.type == DialogType.ADD) {
            dialogBuilder.setPositiveButton(LocaleController.getString("Add", R.string.Add), null);
            dialogBuilder.setNeutralButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        } else if (template.type == DialogType.DELETE) {
            dialogBuilder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), null);
            dialogBuilder.setNeutralButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        } else if (template.type == DialogType.EDIT) {
            dialogBuilder.setPositiveButton(LocaleController.getString("Save", R.string.Save), null);
            dialogBuilder.setNeutralButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            dialogBuilder.setNegativeButton(LocaleController.getString("Delete", R.string.Delete), template.negativeListener);
        }
        FakePasscodeDialogBuilder.views = viewsOutput;
        AlertDialog dialog = dialogBuilder.create();
        addPositiveButtonListener(dialog, template, viewsOutput);
        return dialog;
    }

    private static LinearLayout createLayout(Context context, List<View> views) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(30, 0, 30, 0);
        for (View view: views) {
            layout.addView(view, lp);
        }
        return layout;
    }

    private static void addPositiveButtonListener(AlertDialog dialog, DialogTemplate template, List<View> views) {
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                boolean error = false;
                if (((DialogCheckBox)views.get(3)).isChecked() == false) {
                    for (int i = 0; i < views.size(); i++) {
                        if (!template.viewTemplates.get(i).validate(views.get(i))) {
                            error = true;
                        }
                    }
                }
                if (!error) {
                    template.positiveListener.accept(views);
                    dialog.dismiss();
                }
            });
        });
    }

    public static DialogCheckBox.OnCheckedChangeListener getDeleteAllMessageCheckboxListener(Context context) {
        return (view, checked) -> {
            if(checked ){
                view.requestFocus();
                ((EditTextCaption)FakePasscodeDialogBuilder.views.get(0)).clearFocus();
                ((EditTextCaption)FakePasscodeDialogBuilder.views.get(0)).setText("");
                ((EditTextCaption)FakePasscodeDialogBuilder.views.get(0)).setError("",null);
                ((DialogCheckBox)FakePasscodeDialogBuilder.views.get(1)).setChecked(false);
                ((DialogCheckBox)FakePasscodeDialogBuilder.views.get(2)).setChecked(false);

                InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        };
    }
    public static DialogCheckBox.OnCheckedChangeListener getDeleteRegexMessageCheckboxListener() {
        return (view, checked) -> {
            if(checked ){
                ((DialogCheckBox)FakePasscodeDialogBuilder.views.get(3)).setChecked(false);
            }
        };
    }

    public static DialogCheckBox.OnCheckedChangeListener getDeleteCaseSensMessageCheckboxListener() {
        return (view, checked) -> {
           if(checked ){
               ((DialogCheckBox)FakePasscodeDialogBuilder.views.get(3)).setChecked(false);           }
        };
    }

    public static View.OnFocusChangeListener getTextClickListener() {
        return (view, checked) -> {
            if(checked) {
                ((DialogCheckBox) FakePasscodeDialogBuilder.views.get(3)).setChecked(false);
            }
        };
    }

}





