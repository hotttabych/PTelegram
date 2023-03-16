package org.telegram.messenger.fakepasscode;

import static org.telegram.messenger.MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RemoveChatsAction extends AccountAction implements NotificationCenter.NotificationCenterDelegate, ChatFilter {

    public static class RemoveChatEntry {
        public long chatId;
        public boolean isClearChat;
        public boolean isExitFromChat;
        public boolean isDeleteNewMessages;
        public boolean isDeleteFromCompanion;
        public String title;

        public RemoveChatEntry() {}
        public RemoveChatEntry(long chatId) {
            this(chatId, "Unknown");
        }
        public RemoveChatEntry(long chatId, String title) {
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

    private List<RemoveChatEntry> chatEntriesToRemove = new ArrayList<>();
    @FakePasscodeSerializer.Ignore
    private ArrayList<Long> removedChats = new ArrayList<>(); // Chats to delete new messages
    @FakePasscodeSerializer.Ignore
    private ArrayList<Long> realRemovedChats = new ArrayList<>(); // Removed chats
    @FakePasscodeSerializer.Ignore
    private ArrayList<Long> hiddenChats = new ArrayList<>();
    @FakePasscodeSerializer.Ignore
    private ArrayList<Integer> hiddenFolders = new ArrayList<>();

    @FakePasscodeSerializer.Ignore
    private final Set<Long> pendingRemovalChats = new HashSet<>();
    @JsonIgnore
    public static volatile boolean pendingRemovalChatsChecked = false;
    @JsonIgnore
    private boolean isDialogEndAlreadyReached = false;

    @JsonIgnore
    private FakePasscode fakePasscode;

    public RemoveChatsAction() {}

    void clear() {
        chatEntriesToRemove = new ArrayList<>();
        SharedConfig.saveConfig();
    }

    public List<RemoveChatEntry> getChatEntriesToRemove() {
        return chatEntriesToRemove;
    }

    @Override
    public boolean isRemoveNewMessagesFromChat(long chatId) {
        if (removedChats == null || removedChats.isEmpty()) {
            return false;
        }
        return removedChats.contains(chatId);
    }

    @Override
    public boolean isHideChat(long chatId) {
        if (hiddenChats != null && (hiddenChats.contains(chatId) || hiddenChats.contains(-chatId))) {
            return true;
        } else if (pendingRemovalChats.contains(chatId) || pendingRemovalChats.contains(-chatId)) {
            return true;
        } else if (realRemovedChats != null && (realRemovedChats.contains(chatId) || realRemovedChats.contains(-chatId))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isHideFolder(int folderId) {
        if (hiddenFolders == null || hiddenFolders.isEmpty()) {
            return false;
        }
        return hiddenFolders.contains(folderId);
    }

    public boolean contains(long chatId) {
        return chatEntriesToRemove.stream().anyMatch(e -> e.chatId == chatId);
    }

    public void add(long chatId, String title) {
        RemoveChatEntry entry = new RemoveChatEntry(chatId, title);
        add(entry);
    }

    public void add(RemoveChatEntry entry) {
        chatEntriesToRemove.add(entry);
    }

    public void remove(long chatId) {
        chatEntriesToRemove.removeIf(e -> e.chatId == chatId);
    }

    public RemoveChatEntry get(long chatId) {
        return chatEntriesToRemove.stream().filter(e -> e.chatId == chatId).findAny().orElse(null);
    }

    public Set<Long> getIds() {
        return chatEntriesToRemove.stream().map(e -> e.chatId).collect(Collectors.toSet());
    }

    @Override
    public synchronized void execute(FakePasscode fakePasscode) {
        this.fakePasscode = fakePasscode;
        clearOldValues();
        if (chatEntriesToRemove.isEmpty()) {
            SharedConfig.saveConfig();
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
            return;
        }
        if (chatEntriesToRemove.stream().anyMatch(c -> c.isExitFromChat)) {
            if (Utils.loadAllDialogs(accountNum)) {
                isDialogEndAlreadyReached = false;
                getNotificationCenter().addObserver(this, NotificationCenter.dialogsNeedReload);
                fakePasscode.actionsResult.actionsPreventsLogoutAction.add(this);
            }
        }

        boolean foldersCleared = clearFolders();
        removeChats();
        saveResults();
        if (!realRemovedChats.isEmpty()) {
            fakePasscode.actionsResult.actionsPreventsLogoutAction.add(this);
        }
        unpinHiddenDialogs();
        SharedConfig.saveConfig();
        getMessagesStorage().removeChatsActionExecuted();
        postNotifications(foldersCleared);
        LongSparseIntArray dialogsToUpdate = new LongSparseIntArray(hiddenChats.size());
        hiddenChats.stream().forEach(c -> dialogsToUpdate.put(c, 0));
        getAccount().getNotificationsController().processDialogsUpdateRead(dialogsToUpdate);
        Utilities.globalQueue.postRunnable(this::checkChatsRemoved, 3000);
    }

    private void removeChats() {
        for (RemoveChatEntry entry : chatEntriesToRemove) {
            if (entry.isClearChat && Utils.isNetworkConnected() && isChat(entry.chatId)) {
                if (entry.isExitFromChat) {
                    synchronized (pendingRemovalChats) {
                        if (pendingRemovalChats.isEmpty()) {
                            getNotificationCenter().addObserver(this, NotificationCenter.dialogCleared);
                        }
                        pendingRemovalChats.add(entry.chatId);
                    }
                }
                getMessagesController().deleteAllMessagesFromDialogByUser(getUserConfig().clientUserId, entry.chatId, null);
            } else if (entry.isExitFromChat) {
                Utils.deleteDialog(accountNum, entry.chatId, entry.isDeleteFromCompanion);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogDeletedByAction, entry.chatId);
            }
        }
    }

    private void clearOldValues() {
        synchronized (RemoveChatsAction.class) {
            pendingRemovalChatsChecked = true;
        }
        if (removedChats != null) {
            removedChats.clear();
        }
        if (realRemovedChats != null) {
            realRemovedChats.clear();
        }
        if (hiddenChats != null) {
            hiddenChats.clear();
        }
        if (hiddenFolders != null) {
            hiddenFolders.clear();
        }
        synchronized (pendingRemovalChats) {
            pendingRemovalChats.clear();
        }
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

    private boolean clearFolders() {
        boolean cleared = false;
        ArrayList<MessagesController.DialogFilter> filters = new ArrayList<>(getMessagesController().dialogFilters);
        for (MessagesController.DialogFilter folder : filters) {
            cleared |= clearFolder(folder);
        }
        return cleared;
    }

    private boolean clearFolder(MessagesController.DialogFilter folder) {
        if (!folderHasDialogs(folder)) {
            return false;
        }

        List<Long> idsToRemove = chatEntriesToRemove.stream().filter(e -> e.isExitFromChat).map(e -> e.chatId).collect(Collectors.toList());
        boolean changed = folder.alwaysShow.removeAll(idsToRemove);
        changed |= folder.neverShow.removeAll(idsToRemove);
        for (Long chatId : idsToRemove) {
            if (folder.pinnedDialogs.get(chatId.intValue(), Integer.MIN_VALUE) != Integer.MIN_VALUE) {
                changed = true;
                folder.pinnedDialogs.delete(chatId.intValue());
            }
        }
        List<Long> pinnedDialogs = getFolderPinnedDialogs(folder);

        if (folder.alwaysShow.isEmpty() && folder.pinnedDialogs.size() == 0
                && (folder.flags & DIALOG_FILTER_FLAG_ALL_CHATS) == 0) {
            TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
            req.id = folder.id;
            getMessagesController().removeFilter(folder);
            getMessagesStorage().deleteDialogFilter(folder);
            getAccount().getConnectionsManager().sendRequest(req, (response, error) -> { });
            return true;
        } else if (changed) {
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
            getAccount().getConnectionsManager().sendRequest(req, (response, error) -> { });
        }
        if ((folder.flags & DIALOG_FILTER_FLAG_ALL_CHATS) == 0) {
            Set<Long> idsToHide = chatEntriesToRemove.stream().filter(e -> !e.isExitFromChat).map(e -> e.chatId).collect(Collectors.toSet());
            if (folder.alwaysShow.stream().allMatch(idsToHide::contains)) {
                hiddenFolders.add(folder.id);
            }
        }
        return false;
    }

    private boolean folderHasDialogs(MessagesController.DialogFilter folder) {
        List<Long> idsToRemove = chatEntriesToRemove.stream().map(e -> e.chatId).collect(Collectors.toList());
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


    private List<Long> getFolderPinnedDialogs(MessagesController.DialogFilter folder) {
        List<Long> pinnedDialogs = new ArrayList<>();
        for (int a = 0, N = folder.pinnedDialogs.size(); a < N; a++) {
            long key = folder.pinnedDialogs.keyAt(a);
            if (key == 0) {
                continue;
            }
            pinnedDialogs.add(key);
        }
        return pinnedDialogs;
    }

    private void fillPeerArray(List<Long> fromArray, List<TLRPC.InputPeer> toArray) {
        for (int a = 0, N = fromArray.size(); a < N; a++) {
            long did = fromArray.get(a);
            if (did != 0) {
                if (did > 0) {
                    TLRPC.User user = getMessagesController().getUser(did);
                    if (user != null) {
                        TLRPC.InputPeer inputPeer = new TLRPC.TL_inputPeerUser();
                        inputPeer.user_id = did;
                        inputPeer.access_hash = user.access_hash;
                        toArray.add(inputPeer);
                    }
                } else {
                    TLRPC.Chat chat = getMessagesController().getChat(-did);
                    if (chat != null) {
                        if (ChatObject.isChannel(chat)) {
                            TLRPC.InputPeer inputPeer = new TLRPC.TL_inputPeerChannel();
                            inputPeer.channel_id = -did;
                            inputPeer.access_hash = chat.access_hash;
                            toArray.add(inputPeer);
                        } else {
                            TLRPC.InputPeer inputPeer = new TLRPC.TL_inputPeerChat();
                            inputPeer.chat_id = -did;
                            toArray.add(inputPeer);
                        }
                    }
                }
            }
        }
    }

    private void saveResults() {
        RemoveChatsResult result = fakePasscode.actionsResult.getOrCreateRemoveChatsResult(accountNum);
        result.removeNewMessagesChats = removedChats = getFilteredEntriesIds(e -> e.isExitFromChat && e.isDeleteNewMessages);
        result.removedChats = realRemovedChats = getFilteredEntriesIds(e -> e.isExitFromChat && !DialogObject.isEncryptedDialog(e.chatId));
        result.hiddenChats = hiddenChats = getFilteredEntriesIds(e -> !e.isExitFromChat);
        result.hiddenFolders = hiddenFolders;
        chatEntriesToRemove = getFilteredEntries(e -> !e.isExitFromChat || !DialogObject.isEncryptedDialog(e.chatId));
    }

    private ArrayList<RemoveChatEntry> getFilteredEntries(Predicate<RemoveChatEntry> filter) {
        return chatEntriesToRemove.stream()
                .filter(filter)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<Long> getFilteredEntriesIds(Predicate<RemoveChatEntry> filter) {
        return chatEntriesToRemove.stream()
                .filter(filter)
                .map(e -> e.chatId)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void postNotifications(boolean foldersCleared) {
        if (!hiddenChats.isEmpty() || !realRemovedChats.isEmpty()) {
            getNotificationCenter().postNotificationName(NotificationCenter.dialogHiddenByAction);
        }
        if (!hiddenFolders.isEmpty()) {
            getNotificationCenter().postNotificationName(NotificationCenter.foldersHiddenByAction);
        }
        if (foldersCleared) {
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
        }
        getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    private void unpinHiddenDialogs() {
        for (Long did : hiddenChats) {
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(did);
            if (dialog != null && dialog.pinned) {
                getMessagesController().pinDialog(did, false, null, -1);
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (account == accountNum) {
            if (id == NotificationCenter.dialogCleared) {
                if (args.length > 0 && args[0] instanceof Long) {
                    deletePendingChat((long)args[0]);
                }
            } else if (id == NotificationCenter.dialogsNeedReload) {
                if (!isDialogEndAlreadyReached && !Utils.loadAllDialogs(accountNum)) {
                    getNotificationCenter().removeObserver(this, NotificationCenter.dialogsNeedReload);
                    isDialogEndAlreadyReached = true;
                    try {
                        execute(fakePasscode);
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) {
                            Log.e("FakePasscode", "Error", e);
                        }
                    }
                }
            }
        }
    }

    private boolean isChat(long dialogId)  {
        TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
        return chat != null && (!ChatObject.isChannel(chat) || chat.megagroup);
    }

    public void checkPendingRemovalChats() {
        synchronized (pendingRemovalChats) {
            List<Long> pendingRemovalChatsCopy = new ArrayList<>(pendingRemovalChats);
            for (long dialogId : pendingRemovalChatsCopy) {
                deletePendingChat(dialogId);
            }
        }
    }

    private void deletePendingChat(long dialogId) {
        FakePasscode fakePasscode = FakePasscodeUtils.getActivatedFakePasscode();
        if (fakePasscode == null || fakePasscode.getAllAccountActions().stream().noneMatch(a -> a.getRemoveChatsAction() == this)) {
            return;
        }

        synchronized (pendingRemovalChats) {
            if (!pendingRemovalChats.contains(dialogId)) {
                return;
            }
            pendingRemovalChats.remove(dialogId);
            if (pendingRemovalChats.isEmpty()) {
                getNotificationCenter().removeObserver(this, NotificationCenter.dialogCleared);
            }
        }

        Utils.deleteDialog(accountNum, dialogId);
        getNotificationCenter().postNotificationName(NotificationCenter.dialogDeletedByAction, dialogId);
    }

    private NotificationCenter getNotificationCenter() {
        return NotificationCenter.getInstance(accountNum);
    }

    private synchronized void checkChatsRemoved() {
        if (fakePasscode == null) {
            return;
        }
        if (Utils.isDialogsLeft(accountNum, new HashSet<>(realRemovedChats))) {
            Utilities.globalQueue.postRunnable(this::checkChatsRemoved, 1000);
        } else {
            fakePasscode.actionsResult.actionsPreventsLogoutAction.remove(this);
            realRemovedChats = new ArrayList<>();
            if (fakePasscode != null) {
                RemoveChatsResult removeChatsResult = fakePasscode.actionsResult.getRemoveChatsResult(accountNum);
                if (removeChatsResult != null) {
                    removeChatsResult.removedChats = new ArrayList<>();
                }
            }
            SharedConfig.saveConfig();
        }
    }
}
