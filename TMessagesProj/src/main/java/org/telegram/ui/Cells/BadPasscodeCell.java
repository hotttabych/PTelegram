package org.telegram.ui.Cells;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BadPasscodeAttempt;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class BadPasscodeCell extends FrameLayout {

    private TextView typeView;
    private TextView fakePasscodeView;
    private TextView dateView;
    private LinearLayout layout;

    public BadPasscodeCell(Context context) {
        super(context);

        layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);

        typeView = addTextView(context);
        typeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        layout.addView(typeView, lp);

        fakePasscodeView = addTextView(context);
        fakePasscodeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        layout.addView(fakePasscodeView, lp);

        dateView = addTextView(context);
        dateView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
        layout.addView(dateView, lp);

        addView(layout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 21, 2, 21, 2));
    }

    private TextView addTextView(Context context) {
        TextView textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        return textView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(50));
        int availableWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - AndroidUtilities.dp(34);
        layout.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec( getMeasuredHeight(), MeasureSpec.UNSPECIFIED));
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), layout.getMeasuredHeight() + AndroidUtilities.dp(10));
    }

    public void setBadPasscodeAttempt(BadPasscodeAttempt badPasscodeAttempt) {
        typeView.setVisibility(VISIBLE);
        typeView.setText(badPasscodeAttempt.getTypeString());
        if (badPasscodeAttempt.isFakePasscode) {
            fakePasscodeView.setVisibility(VISIBLE);
            fakePasscodeView.setText(LocaleController.getString("FakePasscode", R.string.FakePasscode));
        } else {
            fakePasscodeView.setVisibility(GONE);
        }
        dateView.setVisibility(VISIBLE);
        dateView.setText(badPasscodeAttempt.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(20), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(20) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setEnabled(isEnabled());
    }
}
