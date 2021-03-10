package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
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
    public SosMessageAction familySosMessageAction;
    public SosMessageAction trustedContactSosMessageAction;
    public SmsAction smsAction = new SmsAction();
    public List<TelegramMessageAction> telegramMessageAction = new ArrayList<>();
    public List<DeleteContactsAction> deleteContactsActions = new ArrayList<>();
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
        result.addAll(terminateOtherSessionsActions);
        result.addAll(logOutActions);
        return result;
    }

    public AccountActions getAccountActions(int accountNum) {
        AccountActions actions = new AccountActions(accountNum, this);
        actions.removeChatsAction = removeChatsActions.stream()
                .filter(a -> a.accountNum == accountNum).findFirst().orElse(null);
        actions.deleteContactsAction = deleteContactsActions.stream()
                .filter(a -> a.accountNum == accountNum).findFirst().orElse(null);
        actions.terminateOtherSessionsAction = terminateOtherSessionsActions.stream()
                .filter(a -> a.accountNum == accountNum).findFirst().orElse(null);
        actions.logOutAction = logOutActions.stream()
                .filter(a -> a.accountNum == accountNum).findFirst().orElse(null);
        actions.messageAction = findOrAddTelegramMessageAction(accountNum);
        return actions;
    }

    private TelegramMessageAction findOrAddTelegramMessageAction(int accountNum) {
        for (TelegramMessageAction action : telegramMessageAction) {
            if (action.accountNum == accountNum) {
                return action;
            }
        }
        TelegramMessageAction action = new TelegramMessageAction();
        action.accountNum = accountNum;
        telegramMessageAction.add(action);
        return action;
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
            smsAction.addMessage(familySosMessageAction.phoneNumber, familySosMessageAction.message);
            familySosMessageAction = null;
        }
        if (trustedContactSosMessageAction != null) {
            smsAction.addMessage(trustedContactSosMessageAction.phoneNumber, trustedContactSosMessageAction.message);
            trustedContactSosMessageAction = null;
        }
    }

    private void removeAccount(int accountNum) {
        removeChatsActions = removeChatsActions.stream()
                .filter(a -> a.accountNum != accountNum).collect(Collectors.toList());
        deleteContactsActions = deleteContactsActions.stream()
                .filter(a -> a.accountNum != accountNum).collect(Collectors.toList());
        terminateOtherSessionsActions = terminateOtherSessionsActions.stream()
                .filter(a -> a.accountNum != accountNum).collect(Collectors.toList());
        logOutActions = logOutActions.stream()
                .filter(a -> a.accountNum != accountNum).collect(Collectors.toList());
        telegramMessageAction = telegramMessageAction.stream()
                .filter(a -> a.accountNum != accountNum).collect(Collectors.toList());
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
        return accountActions.removeChatsAction.removedChats.contains(Long.valueOf(dialogId).intValue());
    }
}
