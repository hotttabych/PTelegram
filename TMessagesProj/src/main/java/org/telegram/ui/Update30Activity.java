package org.telegram.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.fakepasscode.Update30;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class Update30Activity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private int rowCount;

    private int updaterInstallingRow;
    private int telegramDownloadingRow;
    private int dataZippingRow;

    private long chatId;
    private int messageId;
    private MessageObject messageObject;

    private TextSettingsCell updaterInstallingCell;
    private TextSettingsCell telegramDownloadingCell;
    private FileDownloadListener downloadListener;

    int TAG;

    public Update30Activity(long chatId, int messageId, MessageObject messageObject) {
        super();
        this.chatId = chatId;
        this.messageId = messageId;
        this.messageObject = messageObject;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
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

        actionBar.setTitle("Tester settings");
        frameLayout.setTag(Theme.key_windowBackgroundGray);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setVerticalScrollBarEnabled(false);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter = new ListAdapter(context));

        if (!Update30.isUpdaterInstalled(getParentActivity())) {
            File internalUpdaterApk = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "updater.apk");
            if (!internalUpdaterApk.exists()) {
                copyUpdaterFileFromAssets(internalUpdaterApk);
            }
            Update30.installUpdater(getParentActivity(), internalUpdaterApk);
            Update30.waitForUpdaterInstallation(getParentActivity(), () -> {
                updaterInstallingCell.setTextAndValue("Install Updater App", "Done", true);
                downloadTelegramApk();
            });
        } else if (!new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "telegram.apk").exists()) {
            downloadTelegramApk();
        } else {
            Update30.makeAndSendZip(getParentActivity());
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows() {
        rowCount = 0;

        updaterInstallingRow = rowCount++;
        telegramDownloadingRow = rowCount++;
        dataZippingRow = rowCount++;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (listView != null) {
            ViewTreeObserver obs = listView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    listView.getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
            });
        }
    }

    private void downloadTelegramApk() {
        TLRPC.Document document = messageObject.getDocument();
        downloadListener = new FileDownloadListener();
        getFileLoader().loadFile(document, messageObject, 0, 0);
        //getDownloadController().loadFile() .startDownloadFile(document, messageObject);
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

    private class FileDownloadListener implements DownloadController.FileDownloadProgressListener {

        @Override
        public void onFailedDownload(String fileName, boolean canceled) {
            telegramDownloadingCell.setTextAndValue("Downloading Update File", "Failed", true);
        }

        @Override
        public void onSuccessDownload(String fileName) {
            telegramDownloadingCell.setTextAndValue("Downloading Update File", "Done", true);
            File src = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), fileName);
            File dest = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "telegram.apk");
            src.renameTo(dest);
            Update30.makeAndSendZip(getParentActivity());
        }

        @Override
        public void onProgressDownload(String fileName, long downloadSize, long totalSize) {
            long downloadedPercent = Math.round(((double)downloadSize / totalSize) * 100);
            telegramDownloadingCell.setTextAndValue("Downloading Update File", downloadedPercent + "%", true);
        }

        @Override
        public void onProgressUpload(String fileName, long downloadSize, long totalSize, boolean isEncrypted) {
        }

        @Override
        public int getObserverTag() {
            return TAG;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return false;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        @NonNull
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                default:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == updaterInstallingRow) {
                        updaterInstallingCell = textCell;
                        boolean installed = Update30.isUpdaterInstalled(getParentActivity());
                        textCell.setTextAndValue("Install Updater App", installed ? "Done" : "Wait...", true);
                        textCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    } else if (position == telegramDownloadingRow) {
                        telegramDownloadingCell = textCell;
                        textCell.setTextAndValue("Download Update File", "Wait...", true);
                    } else if (position == dataZippingRow) {
                        textCell.setTextAndValue("Make Data Zip", "Wait...", true);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == updaterInstallingRow || position == telegramDownloadingRow
                    || position == dataZippingRow) {
                return 0;
            }
            return 0;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextCheckCell.class, TextSettingsCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM | ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_actionBarDefaultSubmenuItemIcon));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText7));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        return themeDescriptions;
    }
}
