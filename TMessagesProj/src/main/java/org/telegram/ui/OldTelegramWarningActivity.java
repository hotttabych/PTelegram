package org.telegram.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.SimpleThemeDescription;
import org.telegram.ui.Components.voip.CellFlickerDrawable;

import java.util.ArrayList;
import java.util.List;

public class OldTelegramWarningActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private int currentAccount = UserConfig.selectedAccount;

    TextView headerTextView;
    TextView messageTextView;
    private TextView startMessagingButton;
    private TextView backToOldTelegramButton;
    private FrameLayout frameLayout2;
    private FrameLayout frameContainerView;

    private boolean startPressed = false;

    private LocaleController.LocaleInfo localeInfo;

    private boolean destroyed;
    private boolean telegramDataReceiver;

    private AlertDialog progressDialog;

    public OldTelegramWarningActivity(boolean telegramDataReceiver) {
        this.telegramDataReceiver = telegramDataReceiver;
    }

    @Override
    public View createView(Context context) {
        actionBar.setAddToContainer(false);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        RLottieImageView themeIconView = new RLottieImageView(context);
        FrameLayout themeFrameLayout = new FrameLayout(context);
        themeFrameLayout.addView(themeIconView, LayoutHelper.createFrame(28, 28, Gravity.CENTER));

        int themeMargin = 4;
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

                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) themeFrameLayout.getLayoutParams();
                int newTopMargin = AndroidUtilities.dp(themeMargin) + (AndroidUtilities.isTablet() ? 0 : AndroidUtilities.statusBarHeight);
                if (marginLayoutParams.topMargin != newTopMargin) {
                    marginLayoutParams.topMargin = newTopMargin;
                    themeFrameLayout.requestLayout();
                }
            }
        };
        scrollView.addView(frameContainerView, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP));

        themeFrameLayout.setOnClickListener(v -> {
            if (DrawerProfileCell.switchingTheme) return;
            DrawerProfileCell.switchingTheme = true;

            String dayThemeName = "Blue";
            String nightThemeName = "Night";

            Theme.ThemeInfo themeInfo;
            boolean toDark;
            if (toDark = !Theme.isCurrentThemeDark()) {
                themeInfo = Theme.getTheme(nightThemeName);
            } else {
                themeInfo = Theme.getTheme(dayThemeName);
            }

            Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE;
            Theme.saveAutoNightThemeConfig();
            Theme.cancelAutoNightThemeCallbacks();

            themeIconView.playAnimation();

            int[] pos = new int[2];
            themeIconView.getLocationInWindow(pos);
            pos[0] += themeIconView.getMeasuredWidth() / 2;
            pos[1] += themeIconView.getMeasuredHeight() / 2;
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, themeInfo, false, pos, -1, toDark, themeIconView);
            themeIconView.setContentDescription(LocaleController.getString(toDark ? R.string.AccDescrSwitchToDayTheme : R.string.AccDescrSwitchToDayTheme));
        });

        frameLayout2 = new FrameLayout(context);
        frameContainerView.addView(frameLayout2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 78, 0, 0));

        startMessagingButton = new TextView(context);
        startMessagingButton.setText(LocaleController.getString(R.string.StartMessagingAnyway));
        startMessagingButton.setGravity(Gravity.CENTER);
        startMessagingButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        frameContainerView.addView(startMessagingButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 16, 0, 16, 76));
        startMessagingButton.setOnClickListener(view -> {
            if (startPressed) {
                return;
            }
            startPressed = true;

            presentFragment(new LoginActivity().setIntroView(frameContainerView, startMessagingButton), true);
            destroyed = true;
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







        frameContainerView.addView(themeFrameLayout, LayoutHelper.createFrame(64, 64, Gravity.TOP | Gravity.RIGHT, 0, themeMargin, themeMargin, 0));

        fragmentView = scrollView;

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.configLoaded);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.telegramDataReceived);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.telegramDataReceivingError);
        ConnectionsManager.getInstance(currentAccount).updateDcSettings();
        LocaleController.getInstance().loadRemoteLanguages(currentAccount);
        checkContinueText();

        updateColors(false);

        if (telegramDataReceiver) {
            AndroidUtilities.runOnUIThread(() -> {
                AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
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
        destroyed = true;
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.configLoaded);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.telegramDataReceived);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.telegramDataReceivingError);
        MessagesController.getGlobalMainSettings().edit().putLong("intro_crashed_time", 0).apply();
    }

    private void checkContinueText() {
        LocaleController.LocaleInfo englishInfo = null;
        LocaleController.LocaleInfo systemInfo = null;
        LocaleController.LocaleInfo currentLocaleInfo = LocaleController.getInstance().getCurrentLocaleInfo();
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
        TLRPC.TL_langpack_getStrings req = new TLRPC.TL_langpack_getStrings();
        if (systemInfo != currentLocaleInfo) {
            req.lang_code = systemInfo.getLangCode();
            localeInfo = systemInfo;
        } else {
            req.lang_code = englishInfo.getLangCode();
            localeInfo = englishInfo;
        }
        req.keys.add("ContinueOnThisLanguage");
        String finalSystemLang = systemLang;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (response != null) {
                TLRPC.Vector vector = (TLRPC.Vector) response;
                if (vector.objects.isEmpty()) {
                    return;
                }
                final TLRPC.LangPackString string = (TLRPC.LangPackString) vector.objects.get(0);
                if (string instanceof TLRPC.TL_langPackString) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (!destroyed) {
                            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                            preferences.edit().putString("language_showed2", finalSystemLang.toLowerCase()).apply();
                        }
                    });
                }
            }
        }, ConnectionsManager.RequestFlagWithoutLogin);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.suggestedLangpack || id == NotificationCenter.configLoaded) {
            checkContinueText();
        } else if (id == NotificationCenter.telegramDataReceived) {
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
        return SimpleThemeDescription.createThemeDescriptions(() -> updateColors(true), Theme.key_windowBackgroundWhite,
                Theme.key_windowBackgroundWhiteBlueText4, Theme.key_chats_actionBackground, Theme.key_chats_actionPressedBackground,
                Theme.key_featuredStickers_buttonText, Theme.key_windowBackgroundWhiteBlackText, Theme.key_windowBackgroundWhiteGrayText3
        );
    }

    private void updateColors(boolean fromTheme) {
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
}
