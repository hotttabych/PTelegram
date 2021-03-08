/*
 * This is the source code of Telegram for Android v. 5.x.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.fakepasscode.SmsAction;
import org.telegram.messenger.fakepasscode.SmsMessage;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.function.Consumer;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FakePasscodeSmsActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private SmsAction action;

    private int rowCount;

    private int firstSmsRow;
    private int lastSmsRow;
    private int addSmsRow;
    private int smsDetailsRow;
    private int sendOnlyIfDisconnectedRow;

    public FakePasscodeSmsActivity(SmsAction action) {
        super();
        this.action = action;
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

        actionBar.setTitle(LocaleController.getString("FakePasscodeSmsActionTitle", R.string.FakePasscodeSmsActionTitle));
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
            if (!view.isEnabled()) {
                return;
            }
            if (position == sendOnlyIfDisconnectedRow) {
                TextCheckCell cell = (TextCheckCell) view;
                action.onlyIfDisconnected = !action.onlyIfDisconnected;
                cell.setChecked(action.onlyIfDisconnected);
                SharedConfig.saveConfig();
            } if (firstSmsRow <= position && position <= lastSmsRow) {
                SmsMessage message = action.messages.get(position - firstSmsRow);
                FakePasscodeDialogBuilder.Template template = new FakePasscodeDialogBuilder.Template();
                template.type = FakePasscodeDialogBuilder.DialogType.EDIT;
                template.title = LocaleController.getString("FakePasscodeChangeSMS", R.string.FakePasscodeChangeSMS);
                template.addEditTemplate(message.phoneNumber, LocaleController.getString("Phone", R.string.Phone), true);
                template.addEditTemplate(message.text, LocaleController.getString("Message", R.string.Message), false);
                template.positiveListener = edits -> {
                    message.phoneNumber = edits.get(0).getText().toString();
                    message.text = edits.get(1).getText().toString();
                    SharedConfig.saveConfig();
                    TextSettingsCell cell = (TextSettingsCell) view;
                    cell.setTextAndValue(message.phoneNumber, message.text, true);
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                };
                template.negativeListener = (dlg, whichButton) -> {
                    action.messages.remove(position - firstSmsRow);
                    updateRows();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                };
                AlertDialog dialog = FakePasscodeDialogBuilder.build(getParentActivity(), template);
                showDialog(dialog);
            } else if (position == addSmsRow) {
                FakePasscodeDialogBuilder.Template template = new FakePasscodeDialogBuilder.Template();
                template.type = FakePasscodeDialogBuilder.DialogType.ADD;
                template.title = LocaleController.getString("FakePasscodeChangeSMS", R.string.FakePasscodeChangeSMS);
                template.addEditTemplate("", LocaleController.getString("Phone", R.string.Phone), true);
                template.addEditTemplate("", LocaleController.getString("Message", R.string.Message), false);
                template.positiveListener = edits -> {
                    action.addMessage(edits.get(0).getText().toString(), edits.get(1).getText().toString());
                    updateRows();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
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

        if (!action.messages.isEmpty()) {
            firstSmsRow = rowCount++;
            lastSmsRow = firstSmsRow + action.messages.size() - 1;
            rowCount = lastSmsRow + 1;
        } else {
            firstSmsRow = -1;
            lastSmsRow = -1;
        }
        addSmsRow = rowCount++;
        smsDetailsRow = rowCount++;
        sendOnlyIfDisconnectedRow = rowCount++;
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
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 3:
                default:
                    view = new ShadowSectionCell(mContext);
                    Drawable drawable = Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow);
                    CombinedDrawable combinedDrawable = new CombinedDrawable(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray)), drawable);
                    combinedDrawable.setFullsize(true);
                    view.setBackgroundDrawable(combinedDrawable);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == sendOnlyIfDisconnectedRow) {
                        textCell.setTextAndCheck(LocaleController.getString("FakePasscodeSmsSendOnlyIfDisconnected", R.string.FakePasscodeSmsSendOnlyIfDisconnected), action.onlyIfDisconnected, true);
                    }
                    break;
                }
                case 1: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (firstSmsRow <= position && position <= lastSmsRow) {
                        SmsMessage message = action.messages.get(position - firstSmsRow);
                        textCell.setTextAndValue(message.phoneNumber, message.text, true);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    } else if (position == addSmsRow) {
                        textCell.setText(LocaleController.getString("FakePasscodeAddSms", R.string.FakePasscodeAddSms), true);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlueText4);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == sendOnlyIfDisconnectedRow) {
                return 0;
            } else if (firstSmsRow <= position && position <= lastSmsRow || position == addSmsRow) {
                return 1;
            } else if (position == smsDetailsRow) {
                return 3;
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
