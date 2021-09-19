package org.telegram.messenger.fakepasscode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoveChatsAction extends AccountAction implements NotificationCenter.NotificationCenterDelegate {

    public static class RemoveChatEntry {
        public int chatId;
        public boolean isClearChat;
        public boolean isExitFromChat;
        public boolean isDeleteNewMessages;
        public boolean isDeleteFromCompanion;
        public String title;

        public RemoveChatEntry() {}
        public RemoveChatEntry(int chatId) {

        }
        public RemoveChatEntry(int chatId, String title) {
            this.chatId = chatId;
            isClearChat = false;
            isExitFromChat = true;
            isDeleteNewMessages = true;
            isDeleteFromCompanion = false;
            this.title = title;
        }

        public RemoveChatEntry copy() {
            RemoveChatEntry copy = new RemoveChatEntry();
            copy.chatId = chatId;
            copy.isClearChat = isClearChat;
            copy.isExitFromChat = isExitFromChat;
            copy.isDeleteNewMessages = isDeleteNewMessages;
            copy.isDeleteFromCompanion = isDeleteFromCompanion;
            copy.title = title;
            return copy;
        }
    }

    @Deprecated
    private ArrayList<Integer> chatsToRemove = new ArrayList<>();
    private List<RemoveChatEntry> chatEntriesToRemove = new ArrayList<>();
    private ArrayList<Integer> removedChats = new ArrayList<>(); // Chats to delete new messages
    private ArrayList<Integer> hiddenChats = new ArrayList<>();

    @JsonIgnore
    private final Set<Integer> pendingRemovalChats = new HashSet<>();

    public RemoveChatsAction() {}

    public RemoveChatsAction(int accountNum, ArrayList<Integer> chatsToRemove) {
        this.accountNum = accountNum;
        this.chatsToRemove = chatsToRemove;
    }

    public List<RemoveChatEntry> getChatEntriesToRemove() {
        return chatEntriesToRemove;
    }

    public boolean isRemoveNewMessagesFromChat(int chatId) {
        if (removedChats == null) {
            return false;
        }
        return removedChats.contains(chatId);
    }

    public boolean isHideChat(int chatId) {
        if (hiddenChats == null) {
            return false;
        }
        return hiddenChats.contains(chatId);
    }

    public boolean contains(int chatId) {
        return chatEntriesToRemove.stream().anyMatch(e -> e.chatId == chatId);
    }

    public void add(int chatId, String title) {
        RemoveChatEntry entry = new RemoveChatEntry(chatId, title);
        add(entry);
    }

    public void add(RemoveChatEntry entry) {
        chatEntriesToRemove.add(entry);
    }

    public void remove(int chatId) {
        chatEntriesToRemove.removeIf(e -> e.chatId == chatId);
    }

    public RemoveChatEntry get(int chatId) {
        return chatEntriesToRemove.stream().filter(e -> e.chatId == chatId).findAny().orElse(null);
    }

    public Set<Integer> getIds() {
        return chatEntriesToRemove.stream().map(e -> e.chatId).collect(Collectors.toSet());
    }

    public void execute() {
        removedChats.clear();
        if (chatEntriesToRemove.isEmpty()) {
            return;
        }
        clearFolders();
        NotificationCenter notificationCenter = NotificationCenter.getInstance(accountNum);
        for (RemoveChatEntry entry : chatEntriesToRemove) {
            if (entry.isClearChat) {
                if (entry.isExitFromChat) {
                    synchronized (pendingRemovalChats) {
                        if (pendingRemovalChats.isEmpty()) {
                            notificationCenter.addObserver(this, NotificationCenter.dialogCleared);
                        }
                        pendingRemovalChats.add(entry.chatId);
                    }
                }
                getMessagesController().deleteAllMessagesFromDialog(entry.chatId, UserConfig.getInstance(accountNum).clientUserId);
            } else if (entry.isExitFromChat) {
                Utils.deleteDialog(accountNum, entry.chatId, entry.isDeleteFromCompanion);
                notificationCenter.postNotificationName(NotificationCenter.dialogDeletedByAction, entry.chatId);
            }
        }
        removedChats = chatEntriesToRemove.stream().filter(e -> e.isExitFromChat && e.isDeleteNewMessages).map(e -> e.chatId).collect(Collectors.toCollection(ArrayList::new));
        hiddenChats = chatEntriesToRemove.stream().filter(e -> !e.isExitFromChat).map(e -> e.chatId).collect(Collectors.toCollection(ArrayList::new));
        SharedConfig.saveConfig();
        notificationCenter.postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    private AccountInstance getAccount() {
        return AccountInstance.getInstance(accountNum);
    }

    private MessagesController getMessagesController() {
        return getAccount().getMessagesController();
    }

    private MessagesStorage getMessagesStorage() {
        return getAccount().getMessagesStorage();
    }

    private void clearFolders() {
        for (MessagesController.DialogFilter folder : getMessagesController().dialogFilters) {
            clearFolder(folder);
        }
    }

    private void clearFolder(MessagesController.DialogFilter folder) {
        if (!folderHasDialogs(folder)) {
            return;
        }

        List<Integer> idsToRemove = chatEntriesToRemove.stream().map(e -> e.chatId).collect(Collectors.toList());
        folder.alwaysShow.removeAll(idsToRemove);
        folder.neverShow.removeAll(idsToRemove);
        for (Integer chatId : idsToRemove) {
            if (folder.pinnedDialogs.get(chatId) == null) {
                continue;
            }
            folder.pinnedDialogs.remove(chatId);
        }
        List<Integer> pinnedDialogs = getFolderPinnedDialogs(folder);

        if (folder.alwaysShow.isEmpty() && folder.pinnedDialogs.size() == 0) {
            TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
            req.id = folder.id;
            getAccount().getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                getMessagesController().removeFilter(folder);
                getMessagesStorage().deleteDialogFilter(folder);
            }));
        } else {
            TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
            req.id = folder.id;
            req.flags |= 1;
            req.filter = new TLRPC.TL_dialogFilter();
            req.filter.contacts = (folder.flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0;
            req.filter.non_contacts = (folder.flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0;
            req.filter.groups = (folder.flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0;
            req.filter.broadcasts = (folder.flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0;
            req.filter.bots = (folder.flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0;
            req.filter.exclude_muted = (folder.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0;
            req.filter.exclude_read = (folder.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0;
            req.filter.exclude_archived = (folder.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0;
            req.filter.id = folder.id;
            req.filter.title = folder.name;
            fillPeerArray(folder.alwaysShow, req.filter.include_peers);
            fillPeerArray(folder.neverShow, req.filter.exclude_peers);
            fillPeerArray(pinnedDialogs, req.filter.pinned_peers);
        }
    }

    private boolean folderHasDialogs(MessagesController.DialogFilter folder) {
        List<Integer> idsToRemove = chatEntriesToRemove.stream().map(e -> e.chatId).collect(Collectors.toList());
        if (!Collections.disjoint(folder.alwaysShow, idsToRemove)) {
            return true;
        }
        if (!Collections.disjoint(folder.neverShow, idsToRemove)) {
            return true;
        }
        if (!Collections.disjoint(getFolderPinnedDialogs(folder), idsToRemove)) {
            return true;
        }
        return false;
    }


    private List<Integer> getFolderPinnedDialogs(MessagesController.DialogFilter folder) {
        List<Integer> pinnedDialogs = new ArrayList<>();
        for (int a = 0, N = folder.pinnedDialogs.size(); a < N; a++) {
            int key = (int) folder.pinnedDialogs.keyAt(a);
            if (key == 0) {
                continue;
            }
            pinnedDialogs.add(key);
        }
        return pinnedDialogs;
    }

    private void fillPeerArray(List<Integer> fromArray, List<TLRPC.InputPeer> toArray) {
        for (int a = 0, N = fromArray.size(); a < N; a++) {
            long did = fromArray.get(a);
            int lowerId = (int) did;
            if (lowerId != 0) {
                if (lowerId > 0) {
                    TLRPC.User user = getMessagesController().getUser(lowerId);
                    if (user != null) {
                        TLRPC.InputPeer inputPeer = new TLRPC.TL_inputPeerUser();
                        inputPeer.user_id = lowerId;
                        inputPeer.access_hash = user.access_hash;
                        toArray.add(inputPeer);
                    }
                } else {
                    TLRPC.Chat chat = getMessagesController().getChat(-lowerId);
                    if (chat != null) {
                        if (ChatObject.isChannel(chat)) {
                            TLRPC.InputPeer inputPeer = new TLRPC.TL_inputPeerChannel();
                            inputPeer.channel_id = -lowerId;
                            inputPeer.access_hash = chat.access_hash;
                            toArray.add(inputPeer);
                        } else {
                            TLRPC.InputPeer inputPeer = new TLRPC.TL_inputPeerChat();
                            inputPeer.chat_id = -lowerId;
                            toArray.add(inputPeer);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void migrate() {
        for (Integer chatId : chatsToRemove) {
            chatEntriesToRemove.add(new RemoveChatEntry(chatId));
        }
        chatsToRemove.clear();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id != NotificationCenter.dialogCleared || account != accountNum || args.length < 1 || !(args[0] instanceof Integer)) {
            return;
        }

        int dialogId = (int)args[0];
        NotificationCenter notificationCenter = NotificationCenter.getInstance(accountNum);

        synchronized (pendingRemovalChats) {
            if (!pendingRemovalChats.contains(dialogId)) {
                return;
            }
            pendingRemovalChats.remove(dialogId);
            if (pendingRemovalChats.isEmpty()) {
                notificationCenter.removeObserver(this, NotificationCenter.dialogCleared);
            }
        }

        Utils.deleteDialog(accountNum, dialogId);
        notificationCenter.postNotificationName(NotificationCenter.dialogDeletedByAction, dialogId);
    }
}
