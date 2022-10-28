package org.telegram.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Intro;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SimpleThemeDescription;
import org.telegram.ui.Components.voip.CellFlickerDrawable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OldTelegramWarningActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private final int currentAccount = UserConfig.selectedAccount;

    TextView headerTextView;
    TextView messageTextView;
    private TextView startMessagingButton;
    private TextView backToOldTelegramButton;
    private FrameLayout frameLayout2;
    private FrameLayout frameContainerView;

    private boolean startPressed = false;

    private final boolean oldTelegramDataReceiver;

    private AlertDialog progressDialog;

    public OldTelegramWarningActivity(boolean oldTelegramDataReceiver) {
        this.oldTelegramDataReceiver = oldTelegramDataReceiver;
    }

    @Override
    public View createView(Context context) {
        actionBar.setAddToContainer(false);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        frameContainerView = new FrameLayout(context) {

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);

                int oneFourth = (bottom - top) / 4;

                int y = (oneFourth * 3 - AndroidUtilities.dp(275)) / 2;
                frameLayout2.layout(0, y, frameLayout2.getMeasuredWidth(), y + frameLayout2.getMeasuredHeight());

                y = oneFourth * 3 + (oneFourth - startMessagingButton.getMeasuredHeight()) / 2;
                int x = (getMeasuredWidth() - startMessagingButton.getMeasuredWidth()) / 2;
                startMessagingButton.layout(x, y, x + startMessagingButton.getMeasuredWidth(), y + startMessagingButton.getMeasuredHeight());
                x = (getMeasuredWidth() - backToOldTelegramButton.getMeasuredWidth()) / 2;
                backToOldTelegramButton.layout(x, y - backToOldTelegramButton.getMeasuredHeight(), x + backToOldTelegramButton.getMeasuredWidth(), y);
            }
        };
        scrollView.addView(frameContainerView, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP));

        frameLayout2 = new FrameLayout(context);
        frameContainerView.addView(frameLayout2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 78, 0, 0));

        startMessagingButton = new TextView(context);
        startMessagingButton.setText(LocaleController.getString(R.string.StartMessagingAnyway));
        startMessagingButton.setGravity(Gravity.CENTER);
        startMessagingButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        frameContainerView.addView(startMessagingButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 16, 0, 16, 76));
        startMessagingButton.setOnClickListener(view -> {
            DialogDismissedInfo dialogInfo = new DialogDismissedInfo();
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString(R.string.UpdateNotCompletedTitle));
            builder.setMessage(LocaleController.getString(R.string.UpdateNotCompletedMessage));
            builder.setNegativeButton(LocaleController.getString(R.string.Continue) + " (5)", (dialog, which) -> {
                if (dialogInfo.timeout == 0) {
                    if (startPressed) {
                        return;
                    }
                    startPressed = true;
                    presentFragment(new LoginActivity().setIntroView(frameContainerView, startMessagingButton), true);
                }
            });
            builder.setPositiveButton(LocaleController.getString(R.string.Cancel), null);
            builder.setOnDismissListener(d -> dialogInfo.isDismissed = true);
            TextView button;
            AlertDialog dialog = builder.create();
            showDialog(dialog);
            button = (TextView)dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            button.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
            button.setEnabled(false);
            TimeoutRunnable timeoutRunnable = new TimeoutRunnable(button, dialogInfo);
            Utilities.globalQueue.postRunnable(timeoutRunnable, 1000);
        });

        backToOldTelegramButton = new InternalButton(context);
        backToOldTelegramButton.setText(LocaleController.getString(R.string.BackToOldTelegram));
        backToOldTelegramButton.setGravity(Gravity.CENTER);
        backToOldTelegramButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        backToOldTelegramButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        backToOldTelegramButton.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
        frameContainerView.addView(backToOldTelegramButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 16, 0, 16, 76));
        backToOldTelegramButton.setOnClickListener(view -> {
            Intent searchIntent = new Intent(Intent.ACTION_MAIN);
            searchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infoList = getParentActivity().getPackageManager().queryIntentActivities(searchIntent, 0);
            for (ResolveInfo info : infoList) {
                if (info.activityInfo.packageName.equals("org.telegram.messenger")
                        || info.activityInfo.packageName.equals("org.telegram.messenger.beta")) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    try {
                        intent.setClassName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        getParentActivity().startActivity(intent);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 21) {
                getParentActivity().finishAndRemoveTask();
            } else {
                getParentActivity().finishAffinity();
            }
        });

        headerTextView = new TextView(frameContainerView.getContext());
        messageTextView = new TextView(frameContainerView.getContext());

        FrameLayout frameLayout = new FrameLayout(frameContainerView.getContext()) {
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                int oneFourth = (bottom - top) / 4;
                int y = (oneFourth * 3 - AndroidUtilities.dp(275)) / 2;
                y += AndroidUtilities.dp(150);
                y += AndroidUtilities.dp(16);
                int x = AndroidUtilities.dp(18);
                headerTextView.layout(x, y, x + headerTextView.getMeasuredWidth(), y + headerTextView.getMeasuredHeight());

                y += headerTextView.getTextSize();
                y += AndroidUtilities.dp(16);
                x = AndroidUtilities.dp(16);
                messageTextView.layout(x, y, x + messageTextView.getMeasuredWidth(), y + messageTextView.getMeasuredHeight());
            }
        };

        headerTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        headerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        headerTextView.setGravity(Gravity.CENTER);
        frameLayout.addView(headerTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 18, 244, 18, 0));

        messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        messageTextView.setGravity(Gravity.CENTER);
        frameLayout.addView(messageTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 16, 286, 16, 0));

        frameContainerView.addView(frameLayout, 0);

        headerTextView.setText(LocaleController.getString(R.string.UpdateTitle));
        messageTextView.setText(LocaleController.getString(R.string.UpdateMessage));

        fragmentView = scrollView;

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.telegramDataReceived);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.telegramDataReceivingError);
        ConnectionsManager.getInstance(currentAccount).updateDcSettings();
        LocaleController.getInstance().loadRemoteLanguages(currentAccount);
        checkContinueText();

        updateColors();

        if (oldTelegramDataReceiver) {
            AndroidUtilities.runOnUIThread(() -> {
                progressDialog = new AlertDialog(getParentActivity(), 3);
                progressDialog.setCanCancel(false);
                progressDialog.show();
            });
        }

        return fragmentView;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onResume() {
        super.onResume();
        if (!AndroidUtilities.isTablet()) {
            Activity activity = getParentActivity();
            if (activity != null) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (!AndroidUtilities.isTablet()) {
            Activity activity = getParentActivity();
            if (activity != null) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    }

    @Override
    public boolean hasForceLightStatusBar() {
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.telegramDataReceived);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.telegramDataReceivingError);
        MessagesController.getGlobalMainSettings().edit().putLong("intro_crashed_time", 0).apply();
    }

    private void checkContinueText() {
        LocaleController.LocaleInfo englishInfo = null;
        LocaleController.LocaleInfo systemInfo = null;
        String systemLang = MessagesController.getInstance(currentAccount).suggestedLangCode;
        if (systemLang == null || systemLang.equals("en") && LocaleController.getInstance().getSystemDefaultLocale().getLanguage() != null && !LocaleController.getInstance().getSystemDefaultLocale().getLanguage().equals("en")) {
            systemLang = LocaleController.getInstance().getSystemDefaultLocale().getLanguage();
            if (systemLang == null) {
                systemLang = "en";
            }
        }

        String arg = systemLang.contains("-") ? systemLang.split("-")[0] : systemLang;
        String alias = LocaleController.getLocaleAlias(arg);
        for (int a = 0; a < LocaleController.getInstance().languages.size(); a++) {
            LocaleController.LocaleInfo info = LocaleController.getInstance().languages.get(a);
            if (info.shortName.equals("en")) {
                englishInfo = info;
            }
            if (info.shortName.replace("_", "-").equals(systemLang) || info.shortName.equals(arg) || info.shortName.equals(alias)) {
                systemInfo = info;
            }
            if (englishInfo != null && systemInfo != null) {
                break;
            }
        }
        if (englishInfo == null || systemInfo == null || englishInfo == systemInfo) {
            return;
        }
        LocaleController.getInstance().applyLanguage(systemInfo, true, false, currentAccount);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.telegramDataReceived) {
            AndroidUtilities.runOnUIThread(() -> {
                progressDialog = new AlertDialog(getParentActivity(), 3);
                progressDialog.setCanCancel(false);
                progressDialog.show();
            });
        } else if (id == NotificationCenter.telegramDataReceivingError) {
            AndroidUtilities.runOnUIThread(() -> {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
            });
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return SimpleThemeDescription.createThemeDescriptions(this::updateColors, Theme.key_windowBackgroundWhite,
                Theme.key_windowBackgroundWhiteBlueText4, Theme.key_chats_actionBackground, Theme.key_chats_actionPressedBackground,
                Theme.key_featuredStickers_buttonText, Theme.key_windowBackgroundWhiteBlackText, Theme.key_windowBackgroundWhiteGrayText3
        );
    }

    private void updateColors() {
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        startMessagingButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        startMessagingButton.setBackground(null);
        backToOldTelegramButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        backToOldTelegramButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_changephoneinfo_image2), Theme.getColor(Theme.key_chats_actionPressedBackground)));
        Intro.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
    }

    @Override
    public boolean isLightStatusBar() {
        int color = Theme.getColor(Theme.key_windowBackgroundWhite, null, true);
        return ColorUtils.calculateLuminance(color) > 0.7f;
    }

    private static class InternalButton extends TextView {
        public InternalButton(Context context) {
            super(context);
        }

        CellFlickerDrawable cellFlickerDrawable;

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (cellFlickerDrawable == null) {
                cellFlickerDrawable = new CellFlickerDrawable();
                cellFlickerDrawable.drawFrame = false;
                cellFlickerDrawable.repeatProgress = 2f;
            }
            cellFlickerDrawable.setParentWidth(getMeasuredWidth());
            AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            cellFlickerDrawable.draw(canvas, AndroidUtilities.rectTmp, AndroidUtilities.dp(4), null);
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int size = MeasureSpec.getSize(widthMeasureSpec);
            if (size > AndroidUtilities.dp(260)) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(320), MeasureSpec.EXACTLY), heightMeasureSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    private static class DialogDismissedInfo {
        public boolean isDismissed = false;
        public int timeout = 5;
    }

    private static class TimeoutRunnable implements Runnable {
        TextView cancelButton;
        public DialogDismissedInfo dialogInfo;

        public TimeoutRunnable(TextView cancelButton, DialogDismissedInfo dialogInfo) {
            this.cancelButton = cancelButton;
            this.dialogInfo = dialogInfo;
        }

        @Override
        public void run() {
            if (!dialogInfo.isDismissed) {
                dialogInfo.timeout--;
                AndroidUtilities.runOnUIThread(() -> {
                    if (dialogInfo.timeout > 0) {
                        cancelButton.setText((LocaleController.getString(R.string.Continue) + " (" + dialogInfo.timeout + ")").toUpperCase(Locale.ROOT));
                    } else {
                        cancelButton.setText(LocaleController.getString(R.string.Continue).toUpperCase(Locale.ROOT));
                        cancelButton.setTextColor(Theme.getColor(Theme.key_dialogTextGray));
                        cancelButton.setEnabled(true);
                    }
                });
                if (dialogInfo.timeout > 0) {
                    Utilities.globalQueue.postRunnable(this, 1000);
                }
            }
        }
    }
}
