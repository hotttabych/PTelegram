package org.telegram.messenger.fakepasscode;

import java.util.HashMap;
import java.util.Map;

public class ActionsResult {
    public Map<Integer, RemoveChatsResult> removeChatsResults = new HashMap<>();
    public Map<Integer, TelegramMessageResult> telegramMessageResults = new HashMap<>();
    public Map<Integer, String> fakePhoneNumbers = new HashMap<>();

    public RemoveChatsResult getRemoveChatsResult(int accountNum) {
        return removeChatsResults.get(accountNum);
    }

    public RemoveChatsResult getOrCreateRemoveChatsResult(int accountNum) {
        return putIfAbsent(removeChatsResults, accountNum, new RemoveChatsResult());
    }

    public TelegramMessageResult getTelegramMessageResult(int accountNum) {
        return telegramMessageResults.get(accountNum);
    }

    public TelegramMessageResult getOrCreateTelegramMessageResult(int accountNum) {
        return putIfAbsent(telegramMessageResults, accountNum, new TelegramMessageResult());
    }

    public void putFakePhoneNumber(int accountNum, String phoneNumber) {
        fakePhoneNumbers.put(accountNum, phoneNumber);
    }

    public String getFakePhoneNumber(int accountNum) {
        return fakePhoneNumbers.get(accountNum);
    }

    private static <T> T putIfAbsent(Map<Integer, T> map, int accountNum, T value) {
        T result = map.get(accountNum);
        if (result == null) {
            result = value;
            map.put(accountNum, result);
        }
        return result;
    }
}
