package org.telegram.messenger.fakepasscode;

import java.util.ArrayList;

public class RemoveChatsResult implements ChatFilter {
    ArrayList<Long> removeNewMessagesChats = new ArrayList<>();
    ArrayList<Long> hiddenChats = new ArrayList<>();
    ArrayList<Integer> hiddenFolders = new ArrayList<>();

    @Override
    public boolean isRemoveNewMessagesFromChat(long chatId) {
        if (removeNewMessagesChats == null || removeNewMessagesChats.isEmpty()) {
            return false;
        }
        return removeNewMessagesChats.contains(chatId);
    }

    @Override
    public boolean isHideChat(long chatId) {
        return hiddenChats != null && (hiddenChats.contains(chatId) || hiddenChats.contains(-chatId));
    }

    @Override
    public boolean isHideFolder(int folderId) {
        if (hiddenFolders == null || hiddenFolders.isEmpty()) {
            return false;
        }
        return hiddenFolders.contains(folderId);
    }
}
