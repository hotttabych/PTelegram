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
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
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
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FakePasscodeAccountActionsActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private AccountActions actions;

    private int rowCount;

    private int changeTelegramMessageRow;
    private int messagesDetailRow;

    private final HashMap<Integer, Integer> positionToPeerId = new HashMap<>();
    private final HashMap<Integer, TextSettingsCell> positionToTelegramMessageCell = new HashMap<>();

    private int changeChatsToRemoveRow;
    private int terminateAllOtherSessionsRow;
    private int logOutRow;
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
                return;
            }
            if (position == changeTelegramMessageRow) {
                Map<Integer, String> chats = actions.messageAction.chatsToSendingMessages;
                FilterUsersActivity fragment = new FilterUsersActivity(null,
                        new ArrayList<>(chats.keySet()), 0, true);
                fragment.setDelegate((ids, flags) -> {
                    Map<Integer, String> oldMessages = new HashMap<>(chats);
                    chats.clear();
                    for (int id : ids) {
                        chats.put(id, oldMessages.getOrDefault(id, ""));
                    }

                    SharedConfig.saveConfig();
                    updateRows();
                });
                presentFragment(fragment);
            } else if (positionToPeerId.containsKey(position)) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getParentActivity());
                final EditText edittext = new EditText(getParentActivity());
                int id = positionToPeerId.get(position);
                edittext.setText(actions.messageAction.chatsToSendingMessages.getOrDefault(id, ""));
                String title = LocaleController.getString("ChangeTelegramMessage", R.string.ChangeTelegramMessage)
                        + " " + getTelegramMessageTitleByPosition(position);
                alert.setTitle(title);
                alert.setView(edittext);
                alert.setPositiveButton(LocaleController.getString("Done", R.string.Done), (dialog, whichButton) -> {
                    String message = edittext.getText().toString();
                    actions.messageAction.chatsToSendingMessages.put(id, message);
                    if (positionToTelegramMessageCell.containsKey(position)) {
                        positionToTelegramMessageCell.get(position)
                                .setTextAndValue(title, message, true);
                    }
                    SharedConfig.saveConfig();
                });

                alert.show();
            } else if (position == changeChatsToRemoveRow) {
                FilterUsersActivity fragment = new FilterUsersActivity(null, actions.getChatsToRemove(), 0);
                fragment.setDelegate((ids, flags) -> {
                    actions.setChatsToRemove(ids);
                });
                presentFragment(fragment);
            } else if (position == terminateAllOtherSessionsRow) {
                TextCheckCell cell = (TextCheckCell) view;
                actions.changeTerminateActionState();
                cell.setChecked(actions.isTerminateOtherSessions());
            } else if (position == logOutRow) {
                TextCheckCell cell = (TextCheckCell) view;
                actions.changeLogOutActionState();
                cell.setChecked(actions.isLogOut());
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
        positionToPeerId.clear();
        positionToTelegramMessageCell.clear();
        for (int id : actions.messageAction.chatsToSendingMessages.keySet()) {
            positionToPeerId.put(rowCount++, id);
        }
        messagesDetailRow = rowCount++;

        changeChatsToRemoveRow = rowCount++;
        terminateAllOtherSessionsRow = rowCount++;
        logOutRow = rowCount++;
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
            return position != actionsDetailRow && position != messagesDetailRow;
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
                    if (position == terminateAllOtherSessionsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TerminateAllOtherSessionsOnFakeLogin", R.string.TerminateAllOtherSessionsOnFakeLogin),
                                actions.isTerminateOtherSessions(), true);
                    } else if (position == logOutRow) {
                        textCell.setTextAndCheck(LocaleController.getString("LogOutOnFakeLogin", R.string.LogOutOnFakeLogin),
                                actions.isLogOut(), true);
                    }
                    break;
                }
                case 1: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == changeTelegramMessageRow) {
                        textCell.setTextAndValue(LocaleController.getString("ChangeTelegramMessage", R.string.ChangeTelegramMessage),
                                String.valueOf(actions.messageAction.chatsToSendingMessages.size()), true);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    } else if (positionToPeerId.containsKey(position)) {
                        String title = getTelegramMessageTitleByPosition(position);
                        textCell.setTextAndValue(LocaleController.getString("ChangeTelegramMessage", R.string.ChangeTelegramMessage) + " " + title,
                                actions.messageAction.chatsToSendingMessages.getOrDefault(positionToPeerId.get(position), ""),
                                true);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                        positionToTelegramMessageCell.put(position, textCell);
                    } else if (position == changeChatsToRemoveRow) {
                        textCell.setTextAndValue(LocaleController.getString("ChatsToRemove", R.string.ChatsToRemove),
                                String.valueOf(actions.getChatsToRemove().size()), true);
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
                    } else if  (position == actionsDetailRow) {
                        cell.setText(LocaleController.getString("FakePasscodeActionsInfo", R.string.FakePasscodeActionsInfo));
                        cell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == terminateAllOtherSessionsRow || position == logOutRow) {
                return 0;
            } else if (position == changeChatsToRemoveRow || position == changeTelegramMessageRow
                    || positionToPeerId.containsKey(position)) {
                return 1;
            } else if (position == messagesDetailRow || position == actionsDetailRow) {
                return 2;
            }
            return 0;
        }
    }

    private String getTelegramMessageTitleByPosition(int position) {
        AccountInstance account = AccountInstance.getInstance(currentAccount);
        MessagesController messagesController = account.getMessagesController();
        TLRPC.Chat chat;
        TLRPC.User user = null;
        int id = positionToPeerId.get(position);
        String title = "";
        if (id > 0) {
            user = messagesController.getUser(id);
            chat = null;
        } else {
            chat = messagesController.getChat(-id);
        }
        if (chat != null && ChatObject.canSendMessages(chat)) {
            title = chat.title;
        } else if (user != null) {
            if (user.first_name != null && user.last_name != null) {
                title = user.first_name + " " + user.last_name;
            } else if (user.first_name != null) {
                title = user.first_name;
            } else if (user.last_name != null) {
                title = user.last_name;
            }
        }
        return title;
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
