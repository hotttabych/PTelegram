package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.fakepasscode.Update30;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.DialogBuilder.DialogTemplate;
import org.telegram.ui.DialogBuilder.DialogType;
import org.telegram.ui.DialogBuilder.FakePasscodeDialogBuilder;

import java.io.File;
import java.util.ArrayList;

public class TesterSettingsActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private int rowCount;

    private int sessionTerminateActionWarningRow;
    private int triggerUpdateRow;
    private int updateChannelIdRow;
    private int updateChannelUsernameRow;

    public TesterSettingsActivity() {
        super();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    @Override
    public View createView(Context context) {
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
        listView.setOnItemClickListener((view, position) -> {
            if (position == sessionTerminateActionWarningRow) {
                SharedConfig.showSessionsTerminateActionWarning = !SharedConfig.showSessionsTerminateActionWarning;
                SharedConfig.saveConfig();
                ((TextCheckCell) view).setChecked(SharedConfig.showSessionsTerminateActionWarning);
            } else if (position == triggerUpdateRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                builder.setMessage(AndroidUtilities.replaceTags("A new version of partisan telegram has been released. Would you like to go to install it?"));
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
                    File internalTelegramApk = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "telegram.apk");
                    if (internalTelegramApk.exists()) {
                        if (Update30.isUpdaterInstalled(getParentActivity())) {
                            Thread thread = new Thread(this::makeAndSendZip);
                            thread.start();
                        } else {
                            try {
                                File internalUpdaterApk = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), "updater.apk");
                                if (internalUpdaterApk.exists()) {
                                    Update30.installUpdater(getParentActivity(), internalUpdaterApk);
                                    Update30.waitForUpdaterInstallation(getParentActivity(), () -> {
                                        Thread thread = new Thread(TesterSettingsActivity.this::makeAndSendZip);
                                        thread.start();
                                    });
                                    return;
                                }
                            } catch (Exception ignored) {
                            }
                            Toast.makeText(context, "The apk file does not exist", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(context, "The apk file does not exist", Toast.LENGTH_LONG).show();
                    }
                });
                showDialog(builder.create());
            } else if (position == updateChannelIdRow) {
                DialogTemplate template = new DialogTemplate();
                template.type = DialogType.EDIT;
                template.title = "Update Channel Id";
                long id = SharedConfig.updateChannelIdOverride;
                template.addNumberEditTemplate(id != 0 ? Long.toString(id) : "", "Channel Id", true);
                template.positiveListener = views -> {
                    long newId = Long.parseLong(((EditTextCaption)views.get(0)).getText().toString());
                    SharedConfig.updateChannelIdOverride = newId;
                    SharedConfig.saveConfig();
                    TextSettingsCell cell = (TextSettingsCell) view;
                    cell.setTextAndValue("Update Channel Id", newId != 0 ? Long.toString(SharedConfig.updateChannelIdOverride) : "", true);
                };
                template.negativeListener = (dlg, whichButton) -> {
                    SharedConfig.updateChannelIdOverride = 0;
                    SharedConfig.saveConfig();
                    TextSettingsCell cell = (TextSettingsCell) view;
                    cell.setTextAndValue("Update Channel Id", "", true);
                };
                AlertDialog dialog = FakePasscodeDialogBuilder.build(getParentActivity(), template);
                showDialog(dialog);
            } else if (position == updateChannelUsernameRow) {
                DialogTemplate template = new DialogTemplate();
                template.type = DialogType.EDIT;
                template.title = "Update Channel Username";
                template.addEditTemplate(SharedConfig.updateChannelUsernameOverride, "Channel Username", true);
                template.positiveListener = views -> {
                    SharedConfig.updateChannelUsernameOverride = ((EditTextCaption)views.get(0)).getText().toString();
                    SharedConfig.saveConfig();
                    TextSettingsCell cell = (TextSettingsCell) view;
                    cell.setTextAndValue("Update Channel Username", SharedConfig.updateChannelUsernameOverride, false);
                };
                template.negativeListener = (dlg, whichButton) -> {
                    SharedConfig.updateChannelUsernameOverride = "";
                    SharedConfig.saveConfig();
                    TextSettingsCell cell = (TextSettingsCell) view;
                    cell.setTextAndValue("Update Channel Username", SharedConfig.updateChannelUsernameOverride, false);
                };
                AlertDialog dialog = FakePasscodeDialogBuilder.build(getParentActivity(), template);
                showDialog(dialog);
            }
        });

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

        sessionTerminateActionWarningRow = rowCount++;
        if (SharedConfig.activatedTesterSettingType == 2) {
            triggerUpdateRow = rowCount++;
        }
        updateChannelIdRow = rowCount++;
        updateChannelUsernameRow = rowCount++;
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

    private void makeAndSendZip() {
        AlertDialog[] progressDialog = new AlertDialog[1];
        AndroidUtilities.runOnUIThread(() -> {
            progressDialog[0] = new AlertDialog(getParentActivity(), 3);
            progressDialog[0].setCanCancel(false);
            progressDialog[0].showDelayed(300);
        });
        Update30.makeZip(getParentActivity(), (zipFile, fullZipFile, passwordBytes, failed) -> {
            if (!failed) {
                Update30.startUpdater(getParentActivity(), zipFile, fullZipFile, passwordBytes);
            }
        });
        progressDialog[0].dismiss();
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return true;
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
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
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
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == sessionTerminateActionWarningRow) {
                        textCell.setTextAndCheck("Show terminate sessions warning",
                                SharedConfig.showSessionsTerminateActionWarning, true);
                    }
                    break;
                } case 1: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == triggerUpdateRow) {
                        textCell.setText("Trigger update", true);
                    } else if (position == updateChannelIdRow) {
                        long id = SharedConfig.updateChannelIdOverride;
                        textCell.setTextAndValue("Update Channel Id", id != 0 ? Long.toString(id) : "", true);
                    } else if (position == updateChannelUsernameRow) {
                        textCell.setTextAndValue("Update Channel Username", SharedConfig.updateChannelUsernameOverride, false);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == sessionTerminateActionWarningRow) {
                return 0;
            } else if (position == triggerUpdateRow || position == updateChannelIdRow || position == updateChannelUsernameRow) {
                return 1;
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

