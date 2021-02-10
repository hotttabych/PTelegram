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
    public SosMessageAction sosMessageAction = new SosMessageAction();

    List<Action> actions()
    {
        List<Action> result = new ArrayList<>(Arrays.asList(clearCacheAction, sosMessageAction));
        result.addAll(removeChatsActions);
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

    public void executeActions() {
        for (Action action : actions()) {
            try {
                action.execute();
            } catch (Exception ignored) {
            }
        }
    }
}
