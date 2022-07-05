package org.telegram.ui;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.os.Build;
import android.util.Log;
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
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.voip.CellFlickerDrawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Update30Activity extends BaseFragment implements Update30.MakeZipDelegate {
    enum Step {
        INSTALL_UPDATER,
        INSTALL_UPDATER_FAILED,
        INSTALL_UPDATER_LOCKED,

        DOWNLOAD_TELEGRAM,
        DOWNLOAD_TELEGRAM_FAILED,
        DOWNLOAD_TELEGRAM_LOCKED,
        DOWNLOAD_TELEGRAM_COMPLETED,

        MAKE_ZIP,
        MAKE_ZIP_FAILED,
        MAKE_ZIP_LOCKED,
        MAKE_ZIP_COMPLETED;

        Step simplify() {
            switch (this) {
                case INSTALL_UPDATER:
                case INSTALL_UPDATER_FAILED:
                case INSTALL_UPDATER_LOCKED:
                default:
                    return INSTALL_UPDATER;
                case DOWNLOAD_TELEGRAM:
                case DOWNLOAD_TELEGRAM_FAILED:
                case DOWNLOAD_TELEGRAM_LOCKED:
                case DOWNLOAD_TELEGRAM_COMPLETED:
                    return DOWNLOAD_TELEGRAM;
                case MAKE_ZIP:
                case MAKE_ZIP_FAILED:
                case MAKE_ZIP_LOCKED:
                case MAKE_ZIP_COMPLETED:
                    return MAKE_ZIP;
            }
        }
    }

    RelativeLayout relativeLayout;

    private Step step;

    private final MessageObject messageObject;

    private TextView titleTextView;
    private TextView descriptionText;
    private RadialProgressView progressBar;
    private TextView buttonTextView;

    private int progress;
    private LocalDateTime lastProgressUpdateTime;
    private File zipFile;
    private File fullZipFile;
    private byte[] passwordBytes;

    private int TAG;
    private boolean destroyed;
    private long spaceSizeNeeded;

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

        progressBar = new RadialProgressView(context);
        progressBar.setSize(AndroidUtilities.dp(32));
        relativeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        relativeParams.addRule(RelativeLayout.BELOW, descriptionText.getId());
        progressBar.setVisibility(View.GONE);
        relativeLayout.addView(progressBar, relativeParams);

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
                    cellFlickerDrawable.draw(canvas, AndroidUtilities.rectTmp, AndroidUtilities.dp(4), null);
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
        buttonTextView.setOnClickListener(v -> buttonClicked());

        if (!Update30.isUpdaterInstalled(getParentActivity())) {
            setStep(Step.INSTALL_UPDATER);
        } else if (!isTelegramFileDownloaded()) {
            setStep(Step.DOWNLOAD_TELEGRAM);
            downloadTelegramApk();
        } else {
            setStep(Step.MAKE_ZIP);
            makeZip();
        }

        new Thread(this::checkThread).start();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (step.simplify() == Step.INSTALL_UPDATER && Update30.isUpdaterInstalled(getParentActivity())) {
            downloadTelegramApk();
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        destroyed = true;
    }

    private synchronized void setStep(Step step) {
        this.step = step;
        AndroidUtilities.runOnUIThread(this::updateUI);
    }

    private void updateUI() {
        titleTextView.setText(getStepName(step));
        descriptionText.setText(getStepDescription());
        buttonTextView.setText(getButtonName());
        buttonTextView.setEnabled(getStepButtonEnabled(step));
        progressBar.setVisibility(step == Step.MAKE_ZIP ? View.VISIBLE : View.GONE);
    }

    private static String getStepName(Step step) {
        Step simplifiedStep = step.simplify();
        if (simplifiedStep == Step.INSTALL_UPDATER) {
            return LocaleController.getString(R.string.Step1);
        } else if (simplifiedStep == Step.DOWNLOAD_TELEGRAM) {
            return LocaleController.getString(R.string.Step2);
        } else if (simplifiedStep == Step.MAKE_ZIP) {
            return LocaleController.getString(R.string.Step3);
        } else {
            return null;
        }
    }

    private String getStepDescription() {
        switch (step) {
            case INSTALL_UPDATER:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return LocaleController.getString(R.string.InstallUpdaterNoPermissionDescription);
                } else {
                    return LocaleController.getString(R.string.InstallUpdaterDescription);
                }
            case INSTALL_UPDATER_FAILED:
                return LocaleController.getString(R.string.InstallUpdaterFailedDescription);
            case DOWNLOAD_TELEGRAM:
                return String.format(LocaleController.getString(R.string.DownloadTelegramDescription), progress);
            case DOWNLOAD_TELEGRAM_FAILED:
                return LocaleController.getString(R.string.DownloadTelegramFailedDescription);
            case DOWNLOAD_TELEGRAM_COMPLETED:
                return LocaleController.getString(R.string.DownloadTelegramCompleteDescription);
            case MAKE_ZIP:
                return LocaleController.getString(R.string.MakeDataDescription);
            case MAKE_ZIP_FAILED:
                return LocaleController.getString(R.string.MakeDataFailedDescription);
            case MAKE_ZIP_COMPLETED:
                return LocaleController.getString(R.string.MakeDataCompleteDescription);
            case INSTALL_UPDATER_LOCKED:
            case DOWNLOAD_TELEGRAM_LOCKED:
            case MAKE_ZIP_LOCKED:
                return String.format(LocaleController.getString(R.string.NoSpaceForStep), spaceSizeNeeded / 1024 / 1024);
        }
    }

    private String getButtonName() {
        switch (step) {
            case INSTALL_UPDATER:
            case INSTALL_UPDATER_FAILED:
            case INSTALL_UPDATER_LOCKED:
            default:
                return LocaleController.getString(R.string.InstallUpdater);
            case DOWNLOAD_TELEGRAM:
            case DOWNLOAD_TELEGRAM_COMPLETED:
            case DOWNLOAD_TELEGRAM_LOCKED:
                return LocaleController.getString(R.string.GoToNextStep);
            case MAKE_ZIP:
            case MAKE_ZIP_COMPLETED:
            case MAKE_ZIP_LOCKED:
                return LocaleController.getString(R.string.GoToUpdater);
            case DOWNLOAD_TELEGRAM_FAILED:
            case MAKE_ZIP_FAILED:
                return LocaleController.getString(R.string.Retry);
        }
    }

    private static boolean getStepButtonEnabled(Step step) {
        switch (step) {
            case INSTALL_UPDATER:
            case INSTALL_UPDATER_FAILED:
            case DOWNLOAD_TELEGRAM_FAILED:
            case DOWNLOAD_TELEGRAM_COMPLETED:
            case MAKE_ZIP_FAILED:
            case MAKE_ZIP_COMPLETED:
                return true;
            default:
                return false;
        }
    }

    private synchronized void buttonClicked() {
        if (step == Step.INSTALL_UPDATER || step == Step.INSTALL_UPDATER_FAILED) {
            File internalUpdaterApk = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "updater.apk");
            copyUpdaterFileFromAssets(internalUpdaterApk);
            Update30.installUpdater(getParentActivity(), internalUpdaterApk);
            Update30.waitForUpdaterInstallation(getParentActivity(), this::downloadTelegramApk);
        } else if (step == Step.DOWNLOAD_TELEGRAM_FAILED) {
            downloadTelegramApk();
        } else if (step == Step.DOWNLOAD_TELEGRAM_COMPLETED) {
            makeZip();
        } else if (step == Step.MAKE_ZIP_FAILED) {
            makeZip();
        } else if (step == Step.MAKE_ZIP_COMPLETED) {
            if (!Update30.isUpdaterInstalled(getParentActivity())) {
                setStep(Step.INSTALL_UPDATER);
            } else if (!isTelegramFileDownloaded()) {
                downloadTelegramApk();
            } else if (Build.VERSION.SDK_INT >= 24 && !fullZipFile.exists() || Build.VERSION.SDK_INT < 24 && !zipFile.exists()) {
                makeZip();
            } else {
                Update30.startUpdater(getParentActivity(), zipFile, fullZipFile, passwordBytes);
            }
        }
    }

    @Override
    public void makeZipCompleted(File zipFile, File fullZipFile, byte[] passwordBytes) {
        AndroidUtilities.runOnUIThread(() -> {
            this.zipFile = zipFile;
            this.fullZipFile = fullZipFile;
            this.passwordBytes = passwordBytes;
            setStep(Step.MAKE_ZIP_COMPLETED);
        });
    }

    @Override
    public void makeZipFailed(Update30.MakeZipFailReason reason) {
        AndroidUtilities.runOnUIThread(() -> {
            if (reason == Update30.MakeZipFailReason.NO_TELEGRAM_APK) {
                downloadTelegramApk();
            } else {
                setStep(Step.MAKE_ZIP_FAILED);
            }
        });
    }

    private class FileDownloadListener implements DownloadController.FileDownloadProgressListener {
        @Override
        public void onFailedDownload(String fileName, boolean canceled) {
            setStep(Step.DOWNLOAD_TELEGRAM_FAILED);
        }

        @Override
        public void onSuccessDownload(String fileName) {
            telegramApkDownloaded();
        }

        @Override
        public void onProgressDownload(String fileName, long downloadSize, long totalSize) {
            lastProgressUpdateTime = LocalDateTime.now();
            long downloadedPercent = Math.round(((double)downloadSize / totalSize) * 100);
            progress = (int)downloadedPercent;
            AndroidUtilities.runOnUIThread(Update30Activity.this::updateUI);
        }

        @Override
        public void onProgressUpload(String fileName, long downloadSize, long totalSize, boolean isEncrypted) {
        }

        @Override
        public int getObserverTag() {
            return TAG;
        }
    }

    private void downloadTelegramApk() {
        File internalUpdaterApk = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "Telegram.apk");
        if (internalUpdaterApk.exists()) {
            if (!internalUpdaterApk.delete()) {
                setStep(Step.DOWNLOAD_TELEGRAM_FAILED);
            }
        }
        if (messageObject.getDocument().size > getFreeMemorySize()) {
            spaceSizeNeeded = messageObject.getDocument().size;
            setStep(Step.DOWNLOAD_TELEGRAM_LOCKED);
            return;
        }
        lastProgressUpdateTime = LocalDateTime.now();
        setStep(Step.DOWNLOAD_TELEGRAM);
        progress = 0;
        TLRPC.Document document = messageObject.getDocument();
        FileDownloadListener downloadListener = new FileDownloadListener();
        getFileLoader().loadFile(document, messageObject, 0, 0);
        getDownloadController().addLoadingFileObserver(FileLoader.getAttachFileName(document), messageObject, downloadListener);
    }

    private void makeZip() {
        long zipSize = calculateZipSize();
        if (zipSize > getFreeMemorySize()) {
            spaceSizeNeeded = zipSize;
            setStep(Step.MAKE_ZIP_LOCKED);
            return;
        }
        setStep(Step.MAKE_ZIP);
        new Thread(() -> Update30.makeZip(getParentActivity(), this)).start();
    }

    private void copyUpdaterFileFromAssets(File dest) {
        try {
            if (dest.exists()) {
                if (!dest.delete()) {
                    throw new Exception();
                }
            }
            InputStream inStream = ApplicationLoader.applicationContext.getAssets().open("updater.apk");
            OutputStream outStream = new FileOutputStream(dest);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            inStream.close();
            outStream.close();
        } catch (Exception e) {
            Log.e("Update30", "copyUpdaterFileFromAssets error ", e);
            if (dest.exists()) {
                dest.delete();
            }
            setStep(Step.INSTALL_UPDATER_FAILED);
        }
    }

    private void telegramApkDownloaded() {
        File src = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_FILES), messageObject.getDocument().file_name_fixed);
        File dest = getTelegramFile();
        if (dest.exists()) {
            if (!dest.delete()) {
                setStep(Step.DOWNLOAD_TELEGRAM_FAILED);
                return;
            }
        }
        if (!src.renameTo(dest)) {
            setStep(Step.DOWNLOAD_TELEGRAM_FAILED);
            return;
        }
        setStep(Step.DOWNLOAD_TELEGRAM_COMPLETED);
    }

    private void checkThread() {
        while (!destroyed) {
            try {
                synchronized (this) {
                    long freeSize = getFreeMemorySize();
                    if (step == Step.INSTALL_UPDATER || step == Step.INSTALL_UPDATER_FAILED) {
                        if (calculateUpdaterSize() > freeSize) {
                            setStep(Step.INSTALL_UPDATER_LOCKED);
                        }
                    } else if (step == Step.INSTALL_UPDATER_LOCKED) {
                        if (calculateUpdaterSize() <= freeSize) {
                            setStep(Step.INSTALL_UPDATER);
                        }
                    } else if (step == Step.DOWNLOAD_TELEGRAM_LOCKED) {
                        if (messageObject.getDocument().size <= freeSize) {
                            downloadTelegramApk();
                        }
                    } else if (step == Step.DOWNLOAD_TELEGRAM) {
                        if (ChronoUnit.SECONDS.between(lastProgressUpdateTime, LocalDateTime.now()) > 5) {
                            File mediaDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_FILES);
                            String telegramFileName = messageObject.getDocument().file_name_fixed;
                            File telegramFile = new File(mediaDir, telegramFileName);
                            if (telegramFile.exists()) {
                                if (telegramFile.length() == messageObject.getDocument().size) {
                                    telegramApkDownloaded();
                                }
                            }
                        }
                    } else if (step == Step.MAKE_ZIP_LOCKED) {
                        if (calculateZipSize() <= freeSize) {
                            makeZip();
                        }
                    }
                }
                Thread.sleep(100);
            } catch (Exception e) {
                Log.e("Update30", "copyUpdaterFileFromAssets error ", e);
            }
        }
    }

    private long getFreeMemorySize() {
        File internalStorageFile = getParentActivity().getFilesDir();
        return internalStorageFile.getFreeSpace();
    }

    private long calculateUpdaterSize() {
        try {
            AssetFileDescriptor fd = ApplicationLoader.applicationContext.getAssets().openFd("updater.apk");
            return fd.getLength();
        } catch (IOException ignored) {
            return 10 * 1024 * 1024;
        }
    }

    private long calculateZipSize() {
        File filesDir = getParentActivity().getFilesDir();
        long size = calculateDirSize(filesDir)
                + calculateDirSize(new File(filesDir.getParentFile(), "shared_prefs"));
        if (Build.VERSION.SDK_INT >= 24) {
            size *= 2;
            size += getTelegramFile().length();
        }
        return size;
    }

    private static long calculateDirSize(File dir) {
        if (!dir.exists()) {
            return 0;
        }
        long result = 0;
        File[] fileList = dir.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    result += calculateDirSize(file);
                } else {
                    result += file.length();
                }
            }
        }
        return result;
    }

    private File getTelegramFile() {
        return new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "telegram.apk");
    }

    private boolean isTelegramFileDownloaded() {
        return getTelegramFile().exists() && getTelegramFile().length() == messageObject.getDocument().size;
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
