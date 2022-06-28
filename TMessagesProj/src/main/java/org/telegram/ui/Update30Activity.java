package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.fakepasscode.Update30;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.voip.CellFlickerDrawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Update30Activity extends BaseFragment implements Update30.MakeZipDelegate {
    enum Step {
        INSTALL_UPDATER,
        DOWNLOAD_TELEGRAM,
        MAKE_ZIP
    }

    RelativeLayout relativeLayout;

    private Step step;

    private MessageObject messageObject;

    private FileDownloadListener downloadListener;

    private TextView titleTextView;
    private TextView descriptionText;
    private TextView buttonTextView;

    private boolean telegramDownloadFailed;
    private boolean zipFailed;
    private File zipFile;
    private File fullZipFile;
    private byte[] passwordBytes;

    int TAG;

    public Update30Activity(MessageObject messageObject) {
        super();
        this.messageObject = messageObject;
    }

    @Override
    public View createView(Context context) {
        TAG = getDownloadController().generateObserverTag();

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        actionBar.setTitle(LocaleController.getString(R.string.Updater30ActivityTitle));
        frameLayout.setTag(Theme.key_windowBackgroundGray);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        relativeLayout = new RelativeLayout(context);
        frameLayout.addView(relativeLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        descriptionText = new TextView(context);
        descriptionText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6));
        descriptionText.setGravity(Gravity.CENTER_HORIZONTAL);
        descriptionText.setLineSpacing(AndroidUtilities.dp(2), 1);
        descriptionText.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            descriptionText.setId(View.generateViewId());
        } else {
            descriptionText.setId(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE / 2, Integer.MAX_VALUE));
        }
        RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        relativeParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        relativeLayout.addView(descriptionText, relativeParams);

        titleTextView = new TextView(context);
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleTextView.setGravity(Gravity.BOTTOM);
        titleTextView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        relativeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        relativeParams.addRule(RelativeLayout.ABOVE, descriptionText.getId());
        relativeLayout.addView(titleTextView, relativeParams);

        buttonTextView = new TextView(context) {
            CellFlickerDrawable cellFlickerDrawable;

            @Override
            protected void onDraw(Canvas canvas) {
                if (isEnabled()) {
                    super.onDraw(canvas);
                    if (cellFlickerDrawable == null) {
                        cellFlickerDrawable = new CellFlickerDrawable();
                        cellFlickerDrawable.drawFrame = false;
                        cellFlickerDrawable.repeatProgress = 2f;
                    }
                    cellFlickerDrawable.setParentWidth(getMeasuredWidth());
                    AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                    cellFlickerDrawable.draw(canvas, AndroidUtilities.rectTmp, AndroidUtilities.dp(4));
                    invalidate();
                } else {
                    super.onDraw(canvas);
                }
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                if (enabled) {
                    buttonTextView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
                } else {
                    buttonTextView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_picker_disabledButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
                }
            }
        };

        buttonTextView.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        buttonTextView.setGravity(Gravity.CENTER);
        buttonTextView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        buttonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        buttonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        buttonTextView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        relativeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        relativeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        relativeParams.setMargins(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), AndroidUtilities.dp(32));
        relativeLayout.addView(buttonTextView, relativeParams);
        buttonTextView.setOnClickListener(v -> {
            if (step == Step.INSTALL_UPDATER) {
                File internalUpdaterApk = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "updater.apk");
                if (!internalUpdaterApk.exists()) {
                    copyUpdaterFileFromAssets(internalUpdaterApk);
                }
                Update30.installUpdater(getParentActivity(), internalUpdaterApk);
                Update30.waitForUpdaterInstallation(getParentActivity(), () -> {
                    AndroidUtilities.runOnUIThread(() -> setStep2(0, false));
                    downloadTelegramApk();
                });
            } else if (step == Step.DOWNLOAD_TELEGRAM) {
                if (telegramDownloadFailed) {
                    telegramDownloadFailed = false;
                    setStep2(0, false);
                    downloadTelegramApk();
                } else {
                    setStep3(true);
                    makeZip();
                }
            } else if (step == Step.MAKE_ZIP) {
                if (zipFailed) {
                    zipFailed = false;
                    setStep3(true);
                    makeZip();
                } else {
                    Update30.startUpdater(getParentActivity(), zipFile, fullZipFile, passwordBytes);
                }
            }
        });

        if (!Update30.isUpdaterInstalled(getParentActivity())) {
            setStep1();
        } else if (!new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "telegram.apk").exists()) {
            setStep2(0, false);
            downloadTelegramApk();
        } else {
            setStep3(true);
            makeZip();
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void downloadTelegramApk() {
        telegramDownloadFailed = false;
        TLRPC.Document document = messageObject.getDocument();
        downloadListener = new FileDownloadListener();
        getFileLoader().loadFile(document, messageObject, 0, 0);
        getDownloadController().addLoadingFileObserver(FileLoader.getAttachFileName(document), messageObject, downloadListener);
    }

    private void copyUpdaterFileFromAssets(File dest) {
        try {
            InputStream inStream = ApplicationLoader.applicationContext.getAssets().open("updater.apk");
            OutputStream outStream = new FileOutputStream(dest);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            inStream.close();
            outStream.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void makeZipCompleted(File zipFile, File fullZipFile, byte[] passwordBytes, boolean failed) {
        AndroidUtilities.runOnUIThread(() -> {
            if (failed) {
                zipFailed = true;
                setStep3(false);
            } else {
                this.zipFile = zipFile;
                this.fullZipFile = fullZipFile;
                this.passwordBytes = passwordBytes;
                setStep3(false);
            }
        });
    }

    private class FileDownloadListener implements DownloadController.FileDownloadProgressListener {

        @Override
        public void onFailedDownload(String fileName, boolean canceled) {
            telegramDownloadFailed = true;
            AndroidUtilities.runOnUIThread(() -> setStep2(0, false));
        }

        @Override
        public void onSuccessDownload(String fileName) {
            File src = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), fileName);
            File dest = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "telegram.apk");
            if (dest.exists()) {
                dest.delete();
            }
            src.renameTo(dest);
            AndroidUtilities.runOnUIThread(() -> setStep2(0, true));
        }

        @Override
        public void onProgressDownload(String fileName, long downloadSize, long totalSize) {
            long downloadedPercent = Math.round(((double)downloadSize / totalSize) * 100);
            AndroidUtilities.runOnUIThread(() -> {
                setStep2((int)downloadedPercent, false);
            });
        }

        @Override
        public void onProgressUpload(String fileName, long downloadSize, long totalSize, boolean isEncrypted) {
        }

        @Override
        public int getObserverTag() {
            return TAG;
        }
    }

    private void setStep1() {
        titleTextView.setText(LocaleController.getString(R.string.Step1));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            descriptionText.setText(LocaleController.getString(R.string.InstallUpdaterNoPermissionDescription));
        } else {
            descriptionText.setText(LocaleController.getString(R.string.InstallUpdaterDescription));
        }
        buttonTextView.setText(LocaleController.getString(R.string.InstallUpdater));
        step = Step.INSTALL_UPDATER;
    }

    private void setStep2(int progress, boolean completed) {
        titleTextView.setText(LocaleController.getString(R.string.Step2));
        if (telegramDownloadFailed) {
            descriptionText.setText(LocaleController.getString(R.string.DownloadTelegramFailedDescription));
            buttonTextView.setText(LocaleController.getString(R.string.Retry));
        } else {
            if (!completed) {
                descriptionText.setText(String.format(LocaleController.getString(R.string.DownloadTelegramDescription), progress));
            } else {
                descriptionText.setText(LocaleController.getString(R.string.DownloadTelegramCompleteDescription));
            }
            buttonTextView.setText(LocaleController.getString(R.string.GoToNextStep));
        }
        buttonTextView.setEnabled(completed || telegramDownloadFailed);
        step = Step.DOWNLOAD_TELEGRAM;
    }

    private void setStep3(boolean isZipping) {
        titleTextView.setText(LocaleController.getString(R.string.Step3));
        if (zipFailed) {
            descriptionText.setText(LocaleController.getString(R.string.MakeDataFailedDescription));
            buttonTextView.setText(LocaleController.getString(R.string.Retry));
        } else {
            if (isZipping) {
                descriptionText.setText(LocaleController.getString(R.string.MakeDataDescription));
            } else {
                descriptionText.setText(LocaleController.getString(R.string.MakeDataCompleteDescription));
            }
            buttonTextView.setText(LocaleController.getString(R.string.GoToUpdater));
        }
        buttonTextView.setEnabled(!isZipping);
        step = Step.MAKE_ZIP;
    }

    private void makeZip() {
        zipFailed = false;
        new Thread(() -> Update30.makeZip(getParentActivity(), this)).start();
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM | ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_actionBarDefaultSubmenuItemIcon));

        return themeDescriptions;
    }
}
