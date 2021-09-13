package org.telegram.ui.DialogBuilder;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CheckBoxSquare;
import org.telegram.ui.Components.LayoutHelper;

public class DialogCheckBox extends FrameLayout {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(DialogCheckBox buttonView, boolean isChecked);
    }

    private final TextView textView;
    private final CheckBoxSquare checkBox;
    OnCheckedChangeListener onCheckedChangeListener;

    public DialogCheckBox(Context context) {
        super(context);

        checkBox = new CheckBoxSquare(context, true);
        checkBox.setDuplicateParentStateEnabled(false);
        checkBox.setFocusable(false);
        checkBox.setFocusableInTouchMode(false);
        checkBox.setClickable(false);
        addView(checkBox, LayoutHelper.createFrame(18, 18, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 15, 0, 15, 0));

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 15 : 40, 0, LocaleController.isRTL ? 40 : 15, 0));

        setClickable(true);
        setBackground(Theme.getSelectorDrawable(false));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        checkBox.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50), MeasureSpec.EXACTLY));
    }

    public void setTextAndCheck(String text, boolean checked) {
        textView.setText(text);
        checkBox.setChecked(checked, false);
    }

    public void setChecked(boolean checked) {
        checkBox.setChecked(checked, true);
        if (onCheckedChangeListener != null)
            onCheckedChangeListener.onCheckedChanged(this, checked);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        onCheckedChangeListener = listener;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.CheckBox");
        info.setCheckable(true);
        info.setChecked(isChecked());
    }

    @Override
    public boolean performClick() {
        boolean result = super.performClick();
        setChecked(!isChecked());
        return result;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        checkBox.setDisabled(!enabled);
    }

}
