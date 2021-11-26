/*
 * This is the source code of Telegram for Android v. 5.x.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.fakepasscode.AccountActions;
import org.telegram.tgnet.TLRPC;
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

import java.util.ArrayList;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FakePasscodeAccountActionsActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private AccountActions actions;

    private int rowCount;

    private int changeTelegramMessageRow;
    private int messagesDetailRow;

    private int changePhoneRow;
    private int phoneDetailRow;

    private int changeChatsToRemoveRow;
    private int deleteAllContactsRow;
    private int deleteAllStickersRow;
    private int clearSearchHistoryRow;
    private int clearBlackListRow;
    private int terminateAllOtherSessionsRow;
    private int logOutRow;
    private int hideAccountRow;
    private int actionsDetailRow;

    public FakePasscodeAccountActionsActivity(AccountActions actions) {
        super();
        this.actions = actions;
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

        TLRPC.User user = UserConfig.getInstance(actions.accountNum).getCurrentUser();
        String activityTitle = "";
        if (user.first_name != null && user.last_name != null) {
            activityTitle = user.first_name + " " + user.last_name;
        } else if (user.first_name != null) {
            activityTitle = user.first_name;
        } else if (user.last_name != null) {
            activityTitle = user.last_name;
        }
        actionBar.setTitle(activityTitle);
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
                if (position == hideAccountRow) {
                    TextCheckCell cell = (TextCheckCell) view;
                    String title;
                    String message;
                    if (cell.isChecked()) {
                        title = LocaleController.getString("CannotRemoveHiding", R.string.CannotRemoveHiding);
                        message = String.format(LocaleController.getString("CannotShowManyAccounts", R.string.CannotShowManyAccounts),
                                UserConfig.FAKE_PASSCODE_MAX_ACCOUNT_COUNT);
                    } else {
                        title = LocaleController.getString("CannotHideAccount", R.string.CannotHideAccount);
                        if (UserConfig.getActivatedAccountsCount() == 1) {
                            message = LocaleController.getString("CannotHideSingleAccount", R.string.CannotHideSingleAccount);
                        } else {
                            message = LocaleController.getString("CannotHideAllAccounts", R.string.CannotHideAllAccounts);
                        }
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(message);
                    builder.setTitle(title);
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                    AlertDialog alertDialog = builder.create();
                    showDialog(alertDialog);
                }
                return;
            }
            if (position == changeTelegramMessageRow) {
                presentFragment(new FakePasscodeTelegramMessagesActivity(actions.getMessageAction()));
            } else if (position == changePhoneRow) {
                DialogTemplate template = new DialogTemplate();
                template.type = DialogType.EDIT;
                template.title = LocaleController.getString("FakePhoneNumber", R.string.FakePhoneNumber);
                template.addPhoneEditTemplate(actions.getPhone().isEmpty() ? "" : "+" + actions.getPhone(), LocaleController.getString("FakePhoneNumber", R.string.FakePhoneNumber), true);
                template.positiveListener = views -> {
                    actions.setPhone(((EditTextCaption)views.get(0)).getText().toString()
                            .replace("+", "").replace("-", "").replace(" ", ""));
                    SharedConfig.saveConfig();
                    TextSettingsCell cell = (TextSettingsCell) view;
                    String value = actions.getPhone().isEmpty() ? LocaleController.getString("Disabled", R.string.Disabled) : PhoneFormat.getInstance().format("+" + actions.getPhone());
                    cell.setTextAndValue(LocaleController.getString("ActivationMessage", R.string.ActivationMessage), value, false);
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                };
                template.negativeListener = (dlg, whichButton) -> {
                    actions.removePhone();
                    TextSettingsCell cell = (TextSettingsCell) view;
                    cell.setTextAndValue(LocaleController.getString("ActivationMessage", R.string.ActivationMessage), LocaleController.getString("Disabled", R.string.Disabled), false);
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                };
                AlertDialog dialog = FakePasscodeDialogBuilder.build(getParentActivity(), template);
                showDialog(dialog);
            } else if (position == changeChatsToRemoveRow) {
                presentFragment(new FakePasscodeRemoveChatsActivity(actions.getRemoveChatsAction()));
            } else if (position == deleteAllContactsRow) {
                TextCheckCell cell = (TextCheckCell) view;
                actions.toggleDeleteContactsAction();
                cell.setChecked(actions.isDeleteContacts());
            } else if (position == deleteAllStickersRow) {
                TextCheckCell cell = (TextCheckCell) view;
                actions.toggleDeleteStickersAction();
                cell.setChecked(actions.isDeleteStickers());
            } else if (position == clearSearchHistoryRow) {
                TextCheckCell cell = (TextCheckCell) view;
                actions.toggleClearSearchHistoryAction();
                cell.setChecked(actions.isClearSearchHistory());
            } else if (position == clearBlackListRow) {
                TextCheckCell cell = (TextCheckCell) view;
                actions.toggleClearBlackListAction();
                cell.setChecked(actions.isClearBlackList());
            } else if (position == terminateAllOtherSessionsRow) {
                TextCheckCell cell = (TextCheckCell) view;
                actions.toggleTerminateOtherSessionsAction();
                boolean terminate = actions.isTerminateOtherSessions();
                cell.setChecked(terminate);
                if (terminate) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("TerminateOtherSessionsWarningTitle", R.string.TerminateOtherSessionsWarningTitle));
                    builder.setMessage(LocaleController.getString("TerminateOtherSessionsWarningMessage", R.string.TerminateOtherSessionsWarningMessage));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                    showDialog(builder.create());
                }
            } else if (position == logOutRow) {
                TextCheckCell cell = (TextCheckCell) view;
                actions.toggleLogOutAction();
                cell.setChecked(actions.isLogOut());
                if (!actions.isLogOut()) {
                    int targetHideCount = UserConfig.getActivatedAccountsCount() - UserConfig.FAKE_PASSCODE_MAX_ACCOUNT_COUNT;
                    if (!actions.isHideAccount() && actions.getFakePasscode().getHideOrLogOutCount() < targetHideCount) {
                        actions.toggleHideAccountAction();
                    }
                } else {
                    if (actions.isHideAccount()) {
                        actions.toggleHideAccountAction();
                    }
                }
                if (listAdapter != null) {
                    updateRows();
                    listAdapter.notifyDataSetChanged();
                }
                ContactsController.getInstance(actions.accountNum).checkAppAccount();
            } else if (position == hideAccountRow) {
                TextCheckCell cell = (TextCheckCell) view;
                actions.toggleHideAccountAction();
                cell.setChecked(actions.isHideAccount());
                final int maxAccountHidings = UserConfig.MAX_ACCOUNT_COUNT - UserConfig.FAKE_PASSCODE_MAX_ACCOUNT_COUNT;
                if (actions.isHideAccount() && actions.getFakePasscode().getHideOrLogOutCount() > maxAccountHidings) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    String message = String.format(LocaleController.getString("TooManyAccountsHiddenDescription", R.string.TooManyAccountsHiddenDescription),
                            maxAccountHidings);
                    builder.setMessage(message);
                    builder.setTitle(LocaleController.getString("TooManyAccountsHiddenTitle", R.string.TooManyAccountsHiddenTitle));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                    showDialog(builder.create());
                }
                ContactsController.getInstance(actions.accountNum).checkAppAccount();
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

        changeTelegramMessageRow = rowCount++;
        messagesDetailRow = rowCount++;

        changePhoneRow = rowCount++;
        phoneDetailRow = rowCount++;

        changeChatsToRemoveRow = rowCount++;
        deleteAllContactsRow = rowCount++;
        deleteAllStickersRow = rowCount++;
        clearSearchHistoryRow = rowCount++;
        clearBlackListRow = rowCount++;
        terminateAllOtherSessionsRow = rowCount++;
        logOutRow = rowCount++;
        if (!actions.isLogOut()) {
            hideAccountRow = rowCount++;
        } else {
            hideAccountRow = -1;
        }
        actionsDetailRow = rowCount++;
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
            return position != actionsDetailRow && position != messagesDetailRow && position != phoneDetailRow;
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
                case 3:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                default:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == deleteAllContactsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("SyncContactsDelete", R.string.SyncContactsDelete),
                                actions.isDeleteContacts(), true);
                    } else if (position == deleteAllStickersRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DeleteStickers", R.string.DeleteStickers),
                                actions.isDeleteStickers(), true);
                    } else if (position == clearSearchHistoryRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ClearSearchAlertTitle", R.string.ClearSearchAlertTitle),
                                actions.isClearSearchHistory(), true);
                    } else if (position == clearBlackListRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ClearBlackList", R.string.ClearBlackList),
                                actions.isClearBlackList(), true);
                    } else if (position == terminateAllOtherSessionsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TerminateAllOtherSessionsOnFakeLogin", R.string.TerminateAllOtherSessionsOnFakeLogin),
                                actions.isTerminateOtherSessions(), true);
                    } else if (position == logOutRow) {
                        textCell.setTextAndCheck(LocaleController.getString("LogOutOnFakeLogin", R.string.LogOutOnFakeLogin),
                                actions.isLogOut(), false);
                    }
                    break;
                }
                case 1: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == changeTelegramMessageRow) {
                        textCell.setTextAndValue(LocaleController.getString("SendTelegramMessages", R.string.SendTelegramMessages),
                                String.valueOf(actions.getMessageAction().entries.size()), false);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    } else if (position == changePhoneRow) {
                        String value;
                        if (actions.getPhone().isEmpty()) {
                            value = LocaleController.getString("Disabled", R.string.Disabled);
                        } else {
                            value = PhoneFormat.getInstance().format("+" + actions.getPhone());
                        }
                        textCell.setTextAndValue(LocaleController.getString("FakePhoneNumber", R.string.FakePhoneNumber), value, true);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    } else if (position == changeChatsToRemoveRow) {
                        textCell.setTextAndValue(LocaleController.getString("ChatsToRemove", R.string.ChatsToRemove),
                                String.valueOf(actions.getChatsToRemoveCount()), true);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    }
                    break;
                }
                case 2: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == messagesDetailRow) {
                        cell.setText(LocaleController.getString("FakePasscodeTelegramMessageInfo", R.string.FakePasscodeTelegramMessageInfo));
                        cell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else if (position == phoneDetailRow) {
                        cell.setText(LocaleController.getString("FakePhoneNumberInfo", R.string.FakePhoneNumberInfo));
                        cell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else if (position == actionsDetailRow) {
                        cell.setText(LocaleController.getString("FakePasscodeActionsInfo", R.string.FakePasscodeActionsInfo));
                        cell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == hideAccountRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HideAccount", R.string.HideAccount),
                                actions.isHideAccount(), false);
                    }
                    break;
                }
            }
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            if (holder.getItemViewType() == 3) {
                TextCheckCell textCell = (TextCheckCell) holder.itemView;

                int hiddenAccountCount = actions.getFakePasscode().getHideOrLogOutCount();
                int accountCount = UserConfig.getActivatedAccountsCount();
                boolean enabled = actions.isHideAccount() && (accountCount - hiddenAccountCount
                        < UserConfig.FAKE_PASSCODE_MAX_ACCOUNT_COUNT)
                        || !actions.isHideAccount() && ((hiddenAccountCount < accountCount - 1) || actions.isLogOut());

                textCell.setEnabled(enabled, null);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == deleteAllContactsRow || position == deleteAllStickersRow || position == clearSearchHistoryRow
                    || position == clearBlackListRow || position == terminateAllOtherSessionsRow || position == logOutRow) {
                return 0;
            } else if (position == changeChatsToRemoveRow || position == changePhoneRow ||  position == changeTelegramMessageRow) {
                return 1;
            } else if (position == messagesDetailRow || position == phoneDetailRow || position == actionsDetailRow) {
                return 2;
            } else if (position == hideAccountRow) {
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
