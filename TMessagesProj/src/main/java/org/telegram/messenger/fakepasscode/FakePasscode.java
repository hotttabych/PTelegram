package org.telegram.messenger.fakepasscode;

import android.util.Pair;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.FakePasscodeActivity;
import org.telegram.ui.NotificationsSettingsActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class FakePasscode {
    @JsonIgnore
    private final int CURRENT_PASSCODE_VERSION = 2;
    private final String PLATFORM = "ANDROID";
    private int passcodeVersion = 0;

    public boolean allowLogin = true;
    public String name;
    @FakePasscodeSerializer.Ignore
    public String passcodeHash = "";
    public String activationMessage = "";
    public Integer badTriesToActivate;
    public boolean clearAfterActivation;
    public boolean deleteOtherPasscodesAfterActivation;

    public ClearCacheAction clearCacheAction = new ClearCacheAction();
    public SmsAction smsAction = new SmsAction();
    public ClearProxiesAction clearProxiesAction = new ClearProxiesAction();

    @FakePasscodeSerializer.Ignore
    ActionsResult actionsResult = new ActionsResult();

    //Deprecated
    @Deprecated public SosMessageAction familySosMessageAction = new SosMessageAction();
    @Deprecated public SosMessageAction trustedContactSosMessageAction = new SosMessageAction();
    @Deprecated private List<RemoveChatsAction> removeChatsActions = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private List<TelegramMessageAction> telegramMessageAction = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private List<DeleteContactsAction> deleteContactsActions = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private List<DeleteStickersAction> deleteStickersActions = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private List<ClearSearchHistoryAction> clearSearchHistoryActions = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private List<ClearBlackListAction> clearBlackListActions = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private List<ClearSavedChannelsAction> clearSavedChannelsActions = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private List<TerminateOtherSessionsAction> terminateOtherSessionsActions = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private List<LogOutAction> logOutActions = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private List<HideAccountAction> hideAccountActions = Collections.synchronizedList(new ArrayList<>());
    @Deprecated private Map<Integer, String> phoneNumbers = new HashMap<>();
    @Deprecated private Map<Integer, CheckedSessions> sessionsToHide = new HashMap<>();
    //End deprecated

    public List<AccountActions> accountActions = Collections.synchronizedList(new ArrayList<>());

    List<Action> actions()
    {
        List<Action> result = new ArrayList<>(Arrays.asList(clearCacheAction, smsAction));
        result.addAll(accountActions);
        result.add(clearProxiesAction);
        return result;
    }

    public AccountActions getAccountActions(int accountNum) {
        for (AccountActions actions : accountActions) {
            Integer actionsAccountNum = actions.getAccountNum();
            if (actionsAccountNum != null && actionsAccountNum == accountNum) {
                return actions;
            }
        }
        return null;
    }

    public AccountActions getOrCreateAccountActions(int accountNum) {
        for (AccountActions actions : accountActions) {
            Integer actionsAccountNum = actions.getAccountNum();
            if (actionsAccountNum != null && actionsAccountNum == accountNum) {
                return actions;
            }
        }
        AccountActions actions = new AccountActions();
        actions.setAccountNum(accountNum);
        accountActions.add(actions);
        return actions;
    }

    public List<AccountActions> getAllAccountActions() {
        return Collections.unmodifiableList(accountActions);
    }

    public List<AccountActions> getFilteredAccountActions() {
        return accountActions.stream().filter(a -> a.getAccountNum() != null).collect(Collectors.toList());
    }

    public void executeActions() {
        if (SharedConfig.fakePasscodeActivatedIndex == SharedConfig.fakePasscodes.indexOf(this)) {
            return;
        }
        actionsResult = new ActionsResult();
        AndroidUtilities.runOnUIThread(() -> {
            for (Action action : actions()) {
                try {
                    action.execute(this);
                } catch (Exception ignored) {
                    try {
                        action.execute(this);
                    } catch (Exception ignored2) {
                    }
                }
            }
            if (deleteOtherPasscodesAfterActivation) {
                SharedConfig.fakePasscodes = SharedConfig.fakePasscodes.stream()
                        .filter(passcode -> passcode == this).collect(Collectors.toList());
            }
            if (clearAfterActivation) {
                clear();
            }
        });
    }

    private void clear() {
        activationMessage = "";
        badTriesToActivate = null;
        clearAfterActivation = false;
        deleteOtherPasscodesAfterActivation = false;

        clearCacheAction = new ClearCacheAction();
        familySosMessageAction = new SosMessageAction();
        trustedContactSosMessageAction = new SosMessageAction();
        smsAction = new SmsAction();
        clearProxiesAction = new ClearProxiesAction();
        accountActions.clear();
        SharedConfig.saveConfig();
    }

    public void migrate() {
        if (passcodeVersion <= 0) {
            if (familySosMessageAction != null) {
                if (familySosMessageAction.isFilled()) {
                    smsAction.addMessage(familySosMessageAction.phoneNumber, familySosMessageAction.message, false);
                }
                familySosMessageAction = null;
            }
            if (trustedContactSosMessageAction != null) {
                if (trustedContactSosMessageAction.isFilled()) {
                    smsAction.addMessage(trustedContactSosMessageAction.phoneNumber, trustedContactSosMessageAction.message, false);
                }
                trustedContactSosMessageAction = null;
            }
            actions().stream().forEach(Action::migrate);
        }
        if (passcodeVersion <= 1) {
            migrateAccountActions(removeChatsActions);
            migrateAccountActions(telegramMessageAction);
            migrateAccountActions(deleteContactsActions);
            migrateAccountActions(deleteStickersActions);
            migrateAccountActions(clearSearchHistoryActions);
            migrateAccountActions(clearBlackListActions);
            migrateAccountActions(clearSavedChannelsActions);
            migrateAccountActions(terminateOtherSessionsActions);
            migrateAccountActions(logOutActions);
            migrateAccountActions(hideAccountActions);
            phoneNumbers.entrySet().stream().forEach(p -> getOrCreateAccountActions(p.getKey()).setFakePhone(p.getValue()));
            sessionsToHide.entrySet().stream().forEach(s -> {
                CheckedSessions sessionsToHide = getOrCreateAccountActions(s.getKey()).getSessionsToHide();
                sessionsToHide.setSessions(s.getValue().getSessions());
                sessionsToHide.setMode(s.getValue().getMode());
            });
        }
        passcodeVersion = CURRENT_PASSCODE_VERSION;
    }

    private <T extends AccountAction> void migrateAccountActions(List<T> actions) {
        for (T action : actions) {
            getOrCreateAccountActions(action.accountNum).setAction(action);
        }
    }

    public void setRemoveChatsActions(List<RemoveChatsAction> removeChatsActions) {
        this.removeChatsActions = removeChatsActions;
    }

    public void setTerminateOtherSessionsActions(List<TerminateOtherSessionsAction> terminateOtherSessionsActions) {
        this.terminateOtherSessionsActions = terminateOtherSessionsActions;
    }

    public void setLogOutActions(List<LogOutAction> logOutActions) {
        this.logOutActions = logOutActions;
    }

    public void onDelete() { }

    public static boolean checkMessage(int accountNum, long dialogId, Long senderId, String message) {
        if (message != null) {
            tryToActivatePasscodeByMessage(accountNum, senderId, message);
        }
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode == null) {
            return true;
        }
        return !passcode.needDeleteMessage(accountNum, dialogId);
    }

    public static boolean needHideMessage(int accountNum, long dialogId) {
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode == null) {
            return false;
        }
        RemoveChatsResult result = passcode.actionsResult.getRemoveChatsResult(accountNum);
        if (result != null && result.isHideChat(dialogId)) {
            return true;
        }
        AccountActions actions = passcode.getAccountActions(accountNum);
        if (actions != null) {
            RemoveChatsAction removeChatsAction = passcode.getAccountActions(accountNum).getRemoveChatsAction();
            boolean hideAccount = passcode.getAccountActions(accountNum).isHideAccount();
            return hideAccount || removeChatsAction.isHideChat(dialogId);
        } else {
            return false;
        }
    }

    private synchronized static void tryToActivatePasscodeByMessage(int accountNum, Long senderId, String message) {
        if (message.isEmpty() || senderId != null && UserConfig.getInstance(accountNum).clientUserId == senderId) {
            return;
        }
        for (int i = 0; i < SharedConfig.fakePasscodes.size(); i++) {
            FakePasscode passcode = SharedConfig.fakePasscodes.get(i);
            if (passcode.activationMessage.isEmpty()) {
                continue;
            }
            if (passcode.activationMessage.equals(message)) {
                passcode.executeActions();
                SharedConfig.fakePasscodeActivated(i);
                SharedConfig.saveConfig();
                break;
            }
        }
    }

    private boolean needDeleteMessage(int accountNum, long dialogId) {
        RemoveChatsResult result = actionsResult.getRemoveChatsResult(accountNum);
        if (result != null && result.isRemoveNewMessagesFromChat(dialogId)) {
            return true;
        }
        AccountActions accountActions = getAccountActions(accountNum);
        return accountActions != null
                && accountActions.getRemoveChatsAction().isRemoveNewMessagesFromChat(dialogId);
    }

    public static String getFakePhoneNumber(int accountNum) {
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode == null) {
            return null;
        }
        return passcode.actionsResult.getFakePhoneNumber(accountNum);
    }

    public static <T> List<T> filterItems(List<T> items, Optional<Integer> account, BiPredicate<T, ChatFilter> filter) {
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode == null || items == null) {
            return items;
        }
        List<T> filteredItems = items;
        for (Map.Entry<Integer, RemoveChatsResult> pair : passcode.actionsResult.removeChatsResults.entrySet()) {
            Integer accountNum = pair.getKey();
            if (accountNum != null && (!account.isPresent() || accountNum.equals(account.get()))) {
                filteredItems = filteredItems.stream().filter(i -> filter.test(i, pair.getValue())).collect(Collectors.toList());
            }
        }
        for (AccountActions actions : passcode.getFilteredAccountActions()) {
            Integer accountNum = actions.getAccountNum();
            if (accountNum != null && (!account.isPresent() ||  accountNum.equals(account.get()))) {
                filteredItems = filteredItems.stream().filter(i -> filter.test(i, actions.getRemoveChatsAction())).collect(Collectors.toList());
            }
        }
        return new FilteredArrayList<>(filteredItems, items);
    }

    public static List<TLRPC.Dialog> filterDialogs(List<TLRPC.Dialog> dialogs, Optional<Integer> account) {
        return filterItems(dialogs, account, (dialog, filter) -> !filter.isHideChat(Utils.getChatOrUserId(dialog.id, account)));
    }

    public static List<TLRPC.TL_topPeer> filterHints(List<TLRPC.TL_topPeer> hints, int account) {
        return filterItems(hints, Optional.of(account), (peer, filter) ->
                !filter.isHideChat(peer.peer.chat_id)
            && !filter.isHideChat(peer.peer.channel_id)
            && !filter.isHideChat(peer.peer.user_id));
    }

    public static List<TLRPC.Peer> filterPeers(List<TLRPC.Peer> peers, int account) {
        return filterItems(peers, Optional.of(account), (peer, action) -> !isHidePeer(peer, action));
    }

    public static boolean isHidePeer(TLRPC.Peer peer, int account) {
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode == null || peer == null) {
            return false;
        }
        RemoveChatsResult result = passcode.actionsResult.getRemoveChatsResult(account);
        if (result != null && isHidePeer(peer, result)) {
            return true;
        }
        for (AccountActions actions : passcode.getFilteredAccountActions()) {
            Integer accountNum = actions.getAccountNum();
            if (accountNum != null && accountNum.equals(account)) {
                return isHidePeer(peer, actions.getRemoveChatsAction());
            }
        }
        return false;
    }

    private static boolean isHidePeer(TLRPC.Peer peer, ChatFilter filter) {
        if (filter instanceof RemoveChatsResult) {
            return isHidePeer(peer, (RemoveChatsResult)filter);
        } else if (filter instanceof RemoveChatsAction) {
            return isHidePeer(peer, (RemoveChatsAction)filter);
        } else {
            return false;
        }
    }

    private static boolean isHidePeer(TLRPC.Peer peer, RemoveChatsResult info) {
        return info.isHideChat(peer.chat_id)
                || info.isHideChat(peer.channel_id)
                || info.isHideChat(peer.user_id);
    }

    private static boolean isHidePeer(TLRPC.Peer peer, RemoveChatsAction action) {
        return action.isHideChat(peer.chat_id)
                || action.isHideChat(peer.channel_id)
                || action.isHideChat(peer.user_id)
                || action.isRemovedChat(peer.chat_id)
                || action.isRemovedChat(peer.channel_id)
                || action.isRemovedChat(peer.user_id);
    }

    public static List<TLRPC.TL_contact> filterContacts(List<TLRPC.TL_contact> contacts, int account) {
        return filterItems(contacts, Optional.of(account), (contact, filter) -> !filter.isHideChat(contact.user_id));
    }

    public static List<Long> filterDialogIds(List<Long> ids, int account) {
        return filterItems(ids, Optional.of(account), (id, filter) -> !filter.isHideChat(id));
    }

    public static List<MessagesController.DialogFilter> filterFolders(List<MessagesController.DialogFilter> folders, int account) {
        return filterItems(folders, Optional.of(account), (folder, filter) -> !filter.isHideFolder(folder.id));
    }

    public static List<NotificationsSettingsActivity.NotificationException> filterNotificationExceptions(
            List<NotificationsSettingsActivity.NotificationException> exceptions, int account) {
        return filterItems(exceptions, Optional.of(account), (e, filter) -> !filter.isHideChat(e.did));
    }

    public static boolean isHideChat(long chatId, int account) {
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode == null) {
            return false;
        }
        RemoveChatsResult result = passcode.actionsResult.getRemoveChatsResult(account);
        if (result != null && result.isHideChat(chatId)) {
            return true;
        }
        AccountActions actions = passcode.getAccountActions(account);
        return actions != null && actions.getRemoveChatsAction().isHideChat(chatId);
    }

    public static boolean isHideFolder(int folderId, int account) {
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode == null) {
            return false;
        }
        RemoveChatsResult result = passcode.actionsResult.getRemoveChatsResult(account);
        if (result != null && result.isHideFolder(folderId)) {
            return true;
        }
        AccountActions actions = passcode.getAccountActions(account);
        return actions != null && actions.getRemoveChatsAction().isHideFolder(folderId);
    }

    public static boolean isHideAccount(int account) {
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode == null) {
            return false;
        }
        AccountActions actions = passcode.getAccountActions(account);
        return actions != null && actions.isHideAccount();
    }

    public int getHideOrLogOutCount() {
        return (int)getFilteredAccountActions().stream().filter(AccountActions::isLogOutOrHideAccount).count();
    }

    public int getHideAccountCount() {
        return (int)getFilteredAccountActions().stream().filter(AccountActions::isHideAccount).count();
    }

    public boolean autoAddAccountHidings() {
        int targetCount = UserConfig.getActivatedAccountsCount() - UserConfig.getFakePasscodeMaxAccountCount();
        if (targetCount > getHideOrLogOutCount()) {
            List<Integer> configIds = new ArrayList<>();
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (UserConfig.getInstance(a).isClientActivated()) {
                    configIds.add(a);
                }
            }
            Collections.sort(configIds, (o1, o2) -> {
                long l1 = UserConfig.getInstance(o1).loginTime;
                long l2 = UserConfig.getInstance(o2).loginTime;
                if (l1 > l2) {
                    return 1;
                } else if (l1 < l2) {
                    return -1;
                }
                return 0;
            });
            for (int i = configIds.size() - 1; i >= 0; i--) {
                AccountActions actions = getOrCreateAccountActions(configIds.get(i));
                if (!isHideAccount(i) && actions != null && !actions.isLogOut()) {
                    actions.toggleHideAccountAction();
                    if (targetCount <= getHideOrLogOutCount()) {
                        break;
                    }
                }
            }
            SharedConfig.saveConfig();
            return true;
        } else {
            return false;
        }
    }

    public static boolean autoAddHidingsToAllFakePasscodes() {
        boolean result = false;
        for (FakePasscode fakePasscode: SharedConfig.fakePasscodes) {
            result |= fakePasscode.autoAddAccountHidings();
        }
        return result;
    }

    public static void cleanupHiddenAccountSystemNotifications() {
        Map<Integer, Boolean> hideMap = getLogoutOrHideAccountMap();
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            Boolean hidden = hideMap.get(i);
            if (hidden != null && hidden) {
                NotificationsController.getInstance(i).cleanupSystemSettings();
            }
        }
    }

    public static void checkPendingRemovalChats() {
        if (RemoveChatsAction.pendingRemovalChatsChecked) {
            return;
        }
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode != null) {
            for (AccountActions actions : passcode.accountActions) {
                actions.getRemoveChatsAction().checkPendingRemovalChats();
            }
        }
        RemoveChatsAction.pendingRemovalChatsChecked = true;
    }

    public static Map<Integer, Boolean> getLogoutOrHideAccountMap() {
        Map<Integer, Boolean> result = new HashMap<>();
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            result.put(i, UserConfig.getInstance(i).isClientActivated() ? false : null);
        }
        for (FakePasscode fakePasscode: SharedConfig.fakePasscodes) {
            for (AccountActions actions : fakePasscode.getFilteredAccountActions()) {
                if (actions.isLogOutOrHideAccount()) {
                    result.put(actions.getAccountNum(), true);
                }
            }
        }
        return result;
    }

    public static boolean isHideMessage(int accountNum, Long dialogId, int messageId) {
        FakePasscode passcode = SharedConfig.getActivatedFakePasscode();
        if (passcode == null) {
            return false;
        }
        TelegramMessageResult result = passcode.actionsResult.getTelegramMessageResult(accountNum);
        return result != null && result.isSosMessage(dialogId, messageId);
    }
}
