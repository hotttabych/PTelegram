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

    private EditTextCaption createEditText(String text, String hint, boolean singleLine) {
        EditTextCaption messageEditText = new EditTextCaption(getParentActivity());
        messageEditText.setText(text);
        messageEditText.setHint(hint);
        messageEditText.setSingleLine(singleLine);
        if (!singleLine) {
            messageEditText.setMaxLines(6);
        }
        messageEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        messageEditText.setGravity(Gravity.BOTTOM);
        messageEditText.setPadding(0, AndroidUtilities.dp(11), 0, AndroidUtilities.dp(12));
        messageEditText.setTextColor(Theme.getColor(Theme.key_chat_messagePanelText));
        messageEditText.setHintColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        messageEditText.setHintTextColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        messageEditText.setCursorColor(Theme.getColor(Theme.key_chat_messagePanelCursor));
        return messageEditText;
    }

    private LinearLayout createAlertLayout(Context context, EditText phoneEditText, EditText messageEditText) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(30, 0, 30, 0);
        layout.addView(phoneEditText, lp);
        layout.addView(messageEditText, lp);
        return layout;
    }

    private void addPositiveButtonListener(AlertDialog dialog, EditText phone, EditText message, Consumer<View> action) {
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                if (phone.getText().toString().isEmpty()) {
                    phone.setError("Phone");
                }
                if (message.getText().toString().isEmpty()) {
                    message.setError("Message");
                }
                if (!phone.getText().toString().isEmpty() && !message.getText().toString().isEmpty()) {
                    action.accept(view);
                    dialog.dismiss();
                }
            });
        });
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
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getParentActivity());
                dialogBuilder.setTitle(LocaleController.getString("FakePasscodeChangeSMS", R.string.FakePasscodeChangeSMS));

                SmsMessage message = action.messages.get(position - firstSmsRow);
                EditText phoneEditText = createEditText(message.phoneNumber, LocaleController.getString("PrivacyPhone", R.string.PrivacyPhone), true);
                EditText messageEditText = createEditText(message.text, LocaleController.getString("Message", R.string.Message), false);
                LinearLayout layout = createAlertLayout(dialogBuilder.getContext(), phoneEditText, messageEditText);
                dialogBuilder.setView(layout);

                dialogBuilder.setPositiveButton(LocaleController.getString("Change", R.string.Change), null);
                dialogBuilder.setNeutralButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, whichButton) -> {});
                dialogBuilder.setNegativeButton(LocaleController.getString("Delete", R.string.Delete), (dialog, whichButton) -> {
                    action.messages.remove(position - firstSmsRow);
                    updateRows();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                });
                AlertDialog dialog = dialogBuilder.create();
                addPositiveButtonListener(dialog, phoneEditText, messageEditText, button -> {
                    message.text = messageEditText.getText().toString();
                    message.phoneNumber = phoneEditText.getText().toString();
                    SharedConfig.saveConfig();
                    TextSettingsCell cell = (TextSettingsCell) view;
                    cell.setTextAndValue(message.phoneNumber, message.text, true);
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                });
                dialog.show();
            } else if (position == addSmsRow) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getParentActivity());
                dialogBuilder.setTitle(LocaleController.getString("FakePasscodeChangeSMS", R.string.FakePasscodeChangeSMS));

                EditText phoneEditText = createEditText("", LocaleController.getString("PrivacyPhone", R.string.PrivacyPhone), true);
                EditText messageEditText = createEditText("", LocaleController.getString("Message", R.string.Message), false);
                LinearLayout layout = createAlertLayout(dialogBuilder.getContext(), phoneEditText, messageEditText);
                dialogBuilder.setView(layout);

                dialogBuilder.setPositiveButton(LocaleController.getString("Add", R.string.Add), null);
                dialogBuilder.setNeutralButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, whichButton) -> {});

                AlertDialog dialog = dialogBuilder.create();
                addPositiveButtonListener(dialog, phoneEditText, messageEditText, button -> {
                    action.addMessage(phoneEditText.getText().toString(), messageEditText.getText().toString());
                    updateRows();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                });
                dialog.show();
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
