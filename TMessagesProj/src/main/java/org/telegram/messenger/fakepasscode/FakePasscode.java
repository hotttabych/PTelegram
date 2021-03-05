package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakePasscode {
    public boolean allowLogin = true;
    public String name = LocaleController.getString("FakePasscode", R.string.FakePasscode);
    public String passcodeHash = "";
    public ClearCacheAction clearCacheAction = new ClearCacheAction();
    public List<RemoveChatsAction> removeChatsActions = new ArrayList<>();
    public SosMessageAction familySosMessageAction;
    public SosMessageAction trustedContactSosMessageAction;
    public SmsAction smsAction = new SmsAction();
    public List<TelegramMessageAction> telegramMessageAction = new ArrayList<>();
    public List<TerminateOtherSessionsAction> terminateOtherSessionsActions = new ArrayList<>();
    public List<LogOutAction> logOutActions = new ArrayList<>();

    List<Action> actions()
    {
        List<Action> result = new ArrayList<>(Arrays.asList(clearCacheAction, smsAction));
        result.addAll(removeChatsActions);
        result.addAll(telegramMessageAction);
        result.addAll(terminateOtherSessionsActions);
        result.addAll(logOutActions);
        return result;
    }

    public AccountActions getAccountActions(int accountNum) {
        AccountActions actions = new AccountActions(accountNum, this);
        actions.removeChatsAction = removeChatsActions.stream()
                .filter(a -> a.accountNum == accountNum).findFirst().orElse(null);
        actions.terminateOtherSessionsAction = terminateOtherSessionsActions.stream()
                .filter(a -> a.accountNum == accountNum).findFirst().orElse(null);
        actions.logOutAction = logOutActions.stream()
                .filter(a -> a.accountNum == accountNum).findFirst().orElse(null);
        return actions;
    public RemoveChatsAction findRemoveChatsAction(int accountNum) {
        for (RemoveChatsAction action : removeChatsActions) {
            if (action.accountNum == accountNum) {
                return action;
            }
        }
        return null;
    }

    public TelegramMessageAction findTelegramMessageAction(int accountNum) {
        for (TelegramMessageAction action : telegramMessageAction) {
            if (action.accountNum == accountNum) {
                return action;
            }
        }
        return null;
    }

    public TelegramMessageAction findOrAddTelegramMessageAction(int accountNum) {
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

    public ArrayList<Integer> findChatsToRemove(int accountNum) {
        for (RemoveChatsAction action : removeChatsActions) {
            if (action.accountNum == accountNum) {
                return action.chatsToRemove;
            }
        }
        return new ArrayList<>();
    }

    public Map<Integer, String> findContactsToSendMessages(int accountNum) {
        for (TelegramMessageAction action : telegramMessageAction) {
            if (action.accountNum == accountNum) {
                return action.chatsToSendingMessages;
            }
        }
        return new HashMap<>();
    }

    public boolean terminateSessionsOnFakeLogin(Integer account) {
        return terminateOtherSessionsActions.stream().anyMatch(a -> a.accountNum == account);
    }

    public boolean logOutAccountOnFakeLogin(Integer account) {
        return logOutActions.stream().anyMatch(a -> a.accountNum == account);
    }

    public void executeActions() {
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
}
