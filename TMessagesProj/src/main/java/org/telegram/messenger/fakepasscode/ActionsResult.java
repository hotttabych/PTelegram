package org.telegram.messenger.fakepasscode;

import java.util.HashMap;
import java.util.Map;

public class ActionsResult {
    public Map<Integer, RemoveChatsResult> removeChatsResults = new HashMap<>();

    public RemoveChatsResult getRemoveChatsResult(int accountNum) {
        return removeChatsResults.getOrDefault(accountNum, null);
    }

    public RemoveChatsResult getOrCreateAccountActions(int accountNum) {
        RemoveChatsResult result = removeChatsResults.getOrDefault(accountNum, null);
        if (result == null) {
            result = new RemoveChatsResult();
            removeChatsResults.put(accountNum, result);
        }
        return result;
    }
}
