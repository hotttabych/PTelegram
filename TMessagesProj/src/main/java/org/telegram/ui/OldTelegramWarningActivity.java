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
import org.telegram.ui.DialogBuilder.DialogButtonWithTimer;

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
        startMessagingButton.setText(getString("StartMessagingAnyway"));
        startMessagingButton.setGravity(Gravity.CENTER);
        startMessagingButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        frameContainerView.addView(startMessagingButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 16, 0, 16, 76));
        startMessagingButton.setOnClickListener(view -> {
            DialogDismissedInfo dialogInfo = new DialogDismissedInfo();
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString("UpdateNotCompletedTitle"));
            builder.setMessage(getString("UpdateNotCompletedMessage"));
            builder.setPositiveButton(getString("Cancel"), null);
            builder.setOnDismissListener(d -> dialogInfo.isDismissed = true);
            AlertDialog dialog = builder.create();
            DialogButtonWithTimer.setButton(dialog, AlertDialog.BUTTON_NEGATIVE, getString("Continue"), 5, (dlg, which) -> {
                if (startPressed) {
                    return;
                }
                startPressed = true;
                presentFragment(new LoginActivity().setIntroView(frameContainerView, startMessagingButton), true);
            });
            showDialog(dialog);
        });

        backToOldTelegramButton = new InternalButton(context);
        backToOldTelegramButton.setText(getString("BackToOldTelegram"));
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

        headerTextView.setText(getString("UpdateTitle"));
        messageTextView.setText(getString("UpdateMessage"));

        fragmentView = scrollView;

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.configLoaded);
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
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.configLoaded);
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
        if (id == NotificationCenter.suggestedLangpack || id == NotificationCenter.configLoaded) {
            checkContinueText();
        } if (id == NotificationCenter.telegramDataReceived) {
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

    private static String getString(String key) {
        String locale = LocaleController.getInstance().getSystemDefaultLocale().getLanguage();
        if (locale.equals("ru")) {
            switch (key) {
                case "UpdateTitle": return "Обновление";
                case "UpdateMessage": return "Обновление партизанского телеграма ещё не закончено. Откройте старое приложение и продолжите обновлять.";
                case "BackToOldTelegram": return "Вернуться к старому PTelegram";
                case "StartMessagingAnyway": return "Продолжить без переноса данных";
                case "UpdateNotCompletedTitle": return "Обновление не завершено";
                case "UpdateNotCompletedMessage": return "Данные из старого приложения ещё не перенесены. Если продолжите, Вам придётся настраивать приложение заново.";
                case "Continue": return "Продолжить";
                case "Cancel": return "Отмена";
            }
        } else if (locale.equals("be")) {
            switch (key) {
                case "UpdateTitle": return "Абнаўленне";
                case "UpdateMessage": return "Абнаўленне партызанскага тэлеграма яшчэ не скончана. Адчыніце стары дадатак і працягніце абнаўляць.";
                case "BackToOldTelegram": return "Вярнуцца да старога PTelegram";
                case "StartMessagingAnyway": return "Працягнуць без пераносу дадзеных";
                case "UpdateNotCompletedTitle": return "Абнаўленне не скончана";
                case "UpdateNotCompletedMessage": return "Дадзеныя са старога прыкладання яшчэ не перанесены. Калі працягнеце, Вам давядзецца наладжваць прыкладанне зноўку.";
                case "Continue": return "Працягнуць";
                case "Cancel": return "Скасаваць";
            }
        } else if (locale.equals("uk")) {
            switch (key) {
                case "UpdateTitle": return "Оновлення";
                case "UpdateMessage": return "Оновлення партизанського телеграма ще не закінчено. Відкрийте стару програму та продовжуйте оновлювати.";
                case "BackToOldTelegram": return "Повернутися до старого PTelegram";
                case "StartMessagingAnyway": return "Продовжити без перенесення даних";
                case "UpdateNotCompletedTitle": return "Оновлення не завершено";
                case "UpdateNotCompletedMessage": return "Дані зі старої програми ще не перенесені. Якщо продовжите, Вам доведеться налаштовувати програму заново.";
                case "Continue": return "Продовжити";
                case "Cancel": return "Скасувати";
            }
        } else if (locale.equals("pl")) {
            switch (key) {
                case "UpdateTitle": return "Aktualizacja";
                case "UpdateMessage": return "Aktualizacja PTelegram nie została jeszcze zakończona. Otwórz starą aplikację i kontynuuj aktualizację.";
                case "BackToOldTelegram": return "Powrót do starego PTelegrama";
                case "StartMessagingAnyway": return "Kontynuuj bez przenoszenia danych";
                case "UpdateNotCompletedTitle": return "Aktualizacja nie została ukończona";
                case "UpdateNotCompletedMessage": return "Dane ze starej aplikacji nie zostały jeszcze zmigrowane. Jeśli będziesz kontynuować, będziesz musiał ponownie skonfigurować aplikację.";
                case "Continue": return "Kontynuuj";
                case "Cancel": return "Anuluj";
            }
        } else if (locale.equals("fa")) {
            switch (key) {
                case "UpdateTitle": return "به روز رسانی";
                case "UpdateMessage": return "آپدیت PTelegram هنوز تمام نشده است. برنامه قدیمی را باز کنید و به روز رسانی ادامه دهید.";
                case "BackToOldTelegram": return "بازگشت به PTelegram قدیمی";
                case "StartMessagingAnyway": return "بدون انتقال داده ادامه دهید";
                case "UpdateNotCompletedTitle": return "به روز رسانی کامل نشده است";
                case "UpdateNotCompletedMessage": return "داده های برنامه قدیمی هنوز منتقل نشده است. اگر ادامه دهید، باید دوباره برنامه را راه اندازی کنید.";
                case "Continue": return "ادامه";
                case "Cancel": return "لغو";
            }
        } else {
            switch (key) {
                case "UpdateTitle": return "Update";
                case "UpdateMessage": return "The Partisan Telegram update is not finished yet. Open the old application and continue updating.";
                case "BackToOldTelegram": return "Back to Old PTelegram";
                case "StartMessagingAnyway": return "Continue without transferring data";
                case "UpdateNotCompletedTitle": return "Update Not Completed";
                case "UpdateNotCompletedMessage": return "The data from the old application has not been transferred yet. If you continue, you will have to set up the application again.";
                case "Continue": return "Continue";
                case "Cancel": return "Cancel";
            }
        }
        return null;
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
}
