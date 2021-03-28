package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FakePasscode implements NotificationCenter.NotificationCenterDelegate {
    public boolean allowLogin = true;
    public String name;
    public String passcodeHash = "";
    public ClearCacheAction clearCacheAction = new ClearCacheAction();
    public List<RemoveChatsAction> removeChatsActions = new ArrayList<>();
    public SosMessageAction familySosMessageAction = new SosMessageAction();
    public SosMessageAction trustedContactSosMessageAction = new SosMessageAction();
    public SmsAction smsAction = new SmsAction();
    public ClearProxiesAction clearProxiesAction = new ClearProxiesAction();
    public List<TelegramMessageAction> telegramMessageAction = new ArrayList<>();
    public List<DeleteContactsAction> deleteContactsActions = new ArrayList<>();
    public List<DeleteStickersAction> deleteStickersActions = new ArrayList<>();
    public List<TerminateOtherSessionsAction> terminateOtherSessionsActions = new ArrayList<>();
    public List<LogOutAction> logOutActions = new ArrayList<>();

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
        result.addAll(terminateOtherSessionsActions);
        result.addAll(logOutActions);
        result.add(clearProxiesAction);
        return result;
    }

    public AccountActions getAccountActions(int accountNum) {
        return new AccountActions(accountNum, this);
    }

    public void executeActions() {
        if (SharedConfig.fakePasscodeLoginedIndex == SharedConfig.fakePasscodes.indexOf(this)) {
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
                smsAction.addMessage(familySosMessageAction.phoneNumber, familySosMessageAction.message);
            }
            familySosMessageAction = null;
        }
        if (trustedContactSosMessageAction != null) {
            if (trustedContactSosMessageAction.isFilled()) {
                smsAction.addMessage(trustedContactSosMessageAction.phoneNumber, trustedContactSosMessageAction.message);
            }
            trustedContactSosMessageAction = null;
        }
    }

    private void removeAccount(int accountNum) {
        removeChatsActions.removeIf(a -> a.accountNum == accountNum);
        deleteContactsActions.removeIf(a -> a.accountNum == accountNum);
        deleteStickersActions.removeIf(a -> a.accountNum == accountNum);
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

    public static boolean needIgnoreMessage(int accountNum, int dialogId) {
        if (SharedConfig.fakePasscodeLoginedIndex == -1) {
            return false;
        }
        FakePasscode passcode = SharedConfig.fakePasscodes.get(SharedConfig.fakePasscodeLoginedIndex);
        AccountActions accountActions = passcode.getAccountActions(accountNum);
        RemoveChatsAction action = accountActions.getRemoveChatsAction();
        if (action == null || action.removedChats == null) {
            return false;
        }
        return action.removedChats.contains(Long.valueOf(dialogId).intValue());
    }
}
