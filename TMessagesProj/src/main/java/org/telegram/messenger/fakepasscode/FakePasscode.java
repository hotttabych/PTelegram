package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakePasscode implements NotificationCenter.NotificationCenterDelegate {
    public boolean allowLogin = true;
    public String name;
    public String passcodeHash = "";
    public String activationMessage = "";
    public Integer badTriesToActivate;

    public ClearCacheAction clearCacheAction = new ClearCacheAction();
    public List<RemoveChatsAction> removeChatsActions = new ArrayList<>();
    public SosMessageAction familySosMessageAction = new SosMessageAction();
    public SosMessageAction trustedContactSosMessageAction = new SosMessageAction();
    public SmsAction smsAction = new SmsAction();
    public ClearProxiesAction clearProxiesAction = new ClearProxiesAction();
    public List<TelegramMessageAction> telegramMessageAction = new ArrayList<>();
    public List<DeleteContactsAction> deleteContactsActions = new ArrayList<>();
    public List<DeleteStickersAction> deleteStickersActions = new ArrayList<>();
    public List<ClearSearchHistoryAction> clearSearchHistoryActions = new ArrayList<>();
    public List<TerminateOtherSessionsAction> terminateOtherSessionsActions = new ArrayList<>();
    public List<LogOutAction> logOutActions = new ArrayList<>();

    public Map<Integer, String> phoneNumbers = new HashMap<>();

    public FakePasscode() {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.appDidLogout);
        }
    }

    List<Action> actions()
    {
        List<Action> result = new ArrayList<>(Arrays.asList(clearCacheAction, smsAction));
        result.addAll(removeChatsActions);
        result.addAll(telegramMessageAction);
        result.addAll(deleteContactsActions);
        result.addAll(deleteStickersActions);
        result.addAll(clearSearchHistoryActions);
        result.addAll(terminateOtherSessionsActions);
        result.addAll(logOutActions);
        result.add(clearProxiesAction);
        return result;
    }

    public AccountActions getAccountActions(int accountNum) {
        return new AccountActions(accountNum, this);
    }

    public void executeActions() {
        if (SharedConfig.fakePasscodeActivatedIndex == SharedConfig.fakePasscodes.indexOf(this)) {
            return;
        }
        for (Action action : actions()) {
            try {
                action.execute();
            } catch (Exception ignored) {
            }
        }
    }

    public void migrate() {
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
        actions().forEach(Action::migrate);
    }

    private void removeAccount(int accountNum) {
        removeChatsActions.removeIf(a -> a.accountNum == accountNum);
        deleteContactsActions.removeIf(a -> a.accountNum == accountNum);
        deleteStickersActions.removeIf(a -> a.accountNum == accountNum);
        clearSearchHistoryActions.removeIf(a -> a.accountNum == accountNum);
        terminateOtherSessionsActions.removeIf(a -> a.accountNum == accountNum);
        logOutActions.removeIf(a -> a.accountNum == accountNum);
        telegramMessageAction.removeIf(a -> a.accountNum == accountNum);
    }

    public void onDelete() {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).removeObserver(this, NotificationCenter.appDidLogout);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.appDidLogout) {
            removeAccount(account);
        }
    }

    public static boolean checkMessage(int accountNum, int dialogId, Integer senderId, String message) {
        if (message != null) {
            tryToActivatePasscodeByMessage(accountNum, senderId, message);
        }
        if (SharedConfig.fakePasscodeActivatedIndex == -1) {
            return true;
        }
        FakePasscode passcode = SharedConfig.fakePasscodes.get(SharedConfig.fakePasscodeActivatedIndex);
        return !passcode.needIgnoreMessage(accountNum, dialogId);
    }

    private static void tryToActivatePasscodeByMessage(int accountNum, Integer senderId, String message) {
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
                SharedConfig.fakePasscodeActivatedIndex = i;
                SharedConfig.saveConfig();
                break;
            }
        }
    }

    private boolean needIgnoreMessage(int accountNum, int dialogId) {
        AccountActions accountActions = getAccountActions(accountNum);
        RemoveChatsAction action = accountActions.getRemoveChatsAction();
        if (action == null || action.removedChats == null) {
            return false;
        }
        return action.removedChats.contains(Long.valueOf(dialogId).intValue());
    }

    public static String getFakePhoneNumber(int accountNum) {
        if (SharedConfig.fakePasscodeActivatedIndex == -1) {
            return null;
        }
        FakePasscode passcode = SharedConfig.fakePasscodes.get(SharedConfig.fakePasscodeActivatedIndex);
        if (!passcode.phoneNumbers.containsKey(accountNum)) {
            return null;
        }
        return passcode.phoneNumbers.get(accountNum);
    }
}
