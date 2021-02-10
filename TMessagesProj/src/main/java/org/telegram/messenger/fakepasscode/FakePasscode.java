package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.SharedConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FakePasscode {
    public boolean allowLogin = true;
    public String name = "Fake passcode";
    public String passcodeHash = "";
    public ClearCacheAction clearCacheAction = new ClearCacheAction();
    public List<RemoveChatsAction> removeChatsActions = new ArrayList<>();
    public SosMessageAction familySosMessageAction = new SosMessageAction();
    public SosMessageAction trustedContactSosMessageAction = new SosMessageAction();
    public List<TerminateOtherSessionsAction> terminateOtherSessionsActions = new ArrayList<>();
    public List<LogOutAction> logOutActions = new ArrayList<>();

    List<Action> actions()
    {
        List<Action> result = new ArrayList<>(Arrays.asList(clearCacheAction, familySosMessageAction,
                trustedContactSosMessageAction));
        result.addAll(removeChatsActions);
        result.addAll(terminateOtherSessionsActions);
        result.addAll(logOutActions);
        return result;
    }

    public RemoveChatsAction findRemoveChatsAction(int accountNum) {
        for (RemoveChatsAction action : removeChatsActions) {
            if (action.accountNum == accountNum) {
                return action;
            }
        }
        return null;
    }

    public ArrayList<Integer> findChatsToRemove(int accountNum) {
        for (RemoveChatsAction action : removeChatsActions) {
            if (action.accountNum == accountNum) {
                return action.chatsToRemove;
            }
        }
        return new ArrayList<>();
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
}
