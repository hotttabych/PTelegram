package org.telegram.messenger.fakepasscode;

public interface ChatFilter {
    boolean isRemoveNewMessagesFromChat(long chatId);
    boolean isHideChat(long chatId);
    boolean isHideFolder(int folderId);
}
