package org.telegram.messenger.fakepasscode;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class TelegramMessageResult {
    private List<Pair<Long, Integer>> sosMessagesIds = new ArrayList<>();

    public void addMessage(long dialogId, int messageId) {
        sosMessagesIds.add(new Pair<>(dialogId, messageId));
    }

    public boolean isSosMessage(long dialogId, int messageId) {
        return sosMessagesIds.stream().anyMatch(p ->
                (p.first == dialogId || p.first == -dialogId) && p.second == messageId
        );
    }
}
