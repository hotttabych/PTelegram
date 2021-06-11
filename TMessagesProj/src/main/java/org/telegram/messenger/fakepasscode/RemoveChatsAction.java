package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemoveChatsAction extends AccountAction {
    public ArrayList<Integer> chatsToRemove = new ArrayList<>();
    public ArrayList<Integer> removedChats = new ArrayList<>();

    public RemoveChatsAction() {}

    public RemoveChatsAction(int accountNum, ArrayList<Integer> chatsToRemove) {
        this.accountNum = accountNum;
        this.chatsToRemove = chatsToRemove;
    }

    public void execute() {
        removedChats.clear();
        if (chatsToRemove.isEmpty()) {
            return;
        }
        clearFolders();
        MessagesController messageController = getMessagesController();
        for (Integer id : chatsToRemove) {
            TLRPC.Chat chat;
            TLRPC.User user = null;
            if (id > 0) {
                user = messageController.getUser(id);
                chat = null;
            } else {
                chat = messageController.getChat(-id);
            }
            if (chat != null) {
                if (ChatObject.isNotInChat(chat)) {
                    messageController.deleteDialog(id, 0, false);
                } else {
                    TLRPC.User currentUser = messageController.getUser(getAccount().getUserConfig().getClientUserId());
                    messageController.deleteParticipantFromChat((int) -id, currentUser, null);
                }
            } else {
                messageController.deleteDialog(id, 0, false);
                boolean isBot = user != null && user.bot && !MessagesController.isSupportUser(user);
                if (isBot) {
                    messageController.blockPeer(id);
                }
            }
        }
        removedChats = chatsToRemove;
        chatsToRemove = new ArrayList<>();
        SharedConfig.saveConfig();
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

        folder.alwaysShow.removeAll(chatsToRemove);
        folder.neverShow.removeAll(chatsToRemove);
        for (Integer chatId : chatsToRemove) {
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
        if (!Collections.disjoint(folder.alwaysShow, chatsToRemove)) {
            return true;
        }
        if (!Collections.disjoint(folder.neverShow, chatsToRemove)) {
            return true;
        }
        if (!Collections.disjoint(getFolderPinnedDialogs(folder), chatsToRemove)) {
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
}
