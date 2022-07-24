package org.telegram.messenger.fakepasscode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TelegramMessageAction extends AccountAction implements NotificationCenter.NotificationCenterDelegate {
    public static class Entry {
        public Entry() {}

        public Entry(long userId, String text, boolean addGeolocation) {
            this.userId = userId;
            this.text = text;
            this.addGeolocation = addGeolocation;
            this.dialogDeleted = false;
        }

        public Entry copy() {
            Entry entry = new Entry(userId, text, addGeolocation);
            entry.dialogDeleted = dialogDeleted;
            return entry;
        }

        public long userId;
        public String text;
        public boolean addGeolocation;
        @JsonIgnore
        public boolean dialogDeleted = false;
    }

    public List<Entry> entries = new ArrayList<>();
    public static boolean allowReloadDialogsByMessage = true;
    public static TelegramMessageAction activeAction = null;

    @Deprecated
    public Map<Integer, String> chatsToSendingMessages = new HashMap<>();

    @JsonIgnore
    private final Set<Integer> oldMessageIds = new HashSet<>();

    @JsonIgnore
    public List<Entry> sentEntries = new ArrayList<>();

    @JsonIgnore
    private FakePasscode fakePasscode;

    public TelegramMessageAction() {
    }

    @Override
    public void execute(FakePasscode fakePasscode) {
        if ((chatsToSendingMessages.isEmpty() && entries.isEmpty())) {
            return;
        }
        this.fakePasscode = fakePasscode;
        FakePasscodeMessages.hasUnDeletedMessages.clear();
        FakePasscodeMessages.saveMessages();

        getMessagesController().forceResetDialogs();

        getNotificationCenter().addObserver(this, NotificationCenter.messageReceivedByServer);
        getNotificationCenter().addObserver(this, NotificationCenter.dialogDeletedByAction);
        for (Entry entry : entries) {
            sendMessage(entry);
        }

        FakePasscodeMessages.saveMessages();
        sentEntries = entries.stream().map(Entry::copy).collect(Collectors.toList());
        SharedConfig.saveConfig();
    }

    private void sendMessage(Entry entry) {
        String geolocation = Utils.getLastLocationString();
        String text = entry.text;
        if (entry.addGeolocation) {
            text += geolocation;
        }
        allowReloadDialogsByMessage = false;
        activeAction = this;
        getSendMessagesHelper().sendMessage(text, entry.userId, null, null, null, false,
                null, null, null, true, 0, null);
        allowReloadDialogsByMessage = true;
        activeAction = null;
        entry.dialogDeleted = false;
    }

    public static void sosMessageSent(TLRPC.Message message) {
        if (activeAction == null) {
            return;
        }
        activeAction.messageSent(message);
    }

    private void messageSent(TLRPC.Message message) {
        oldMessageIds.add(message.id);
        fakePasscode.actionsResult.getOrCreateTelegramMessageResult(accountNum)
                .addMessage(message.dialog_id, message.id);
        TLRPC.Message prevMessage = null;
        TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(message.dialog_id);
        if (dialog != null) {
            MessageObject messageObject = getMessagesController().dialogMessagesByIds.get(dialog.top_message);
            if (messageObject != null) {
                prevMessage = messageObject.messageOwner;
            }
        }
        FakePasscodeMessages.FakePasscodeMessage fakePasscodeMessage;
        fakePasscodeMessage = new FakePasscodeMessages.FakePasscodeMessage(message.message, message.date, prevMessage);
        FakePasscodeMessages.addFakePasscodeMessage(accountNum, message.dialog_id, fakePasscodeMessage);
        deleteMessage(message.dialog_id, message.id);
    }

    private void deleteMessage(long chatId, int messageId) {
        MessagesController controller = getMessagesController();
        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(messageId);
        if (ChatObject.isChannel(chatId, accountNum) || ChatObject.isChannel(-chatId, accountNum)) {
            // messages in channels are always deleted for everyone
        } else {
            controller.deleteMessages(messages, null, null, chatId,
                    false, false, false, 0, null, false, true);
        }
    }

    AccountInstance getAccountInstance() {
        return AccountInstance.getInstance(accountNum);
    }

    MessagesController getMessagesController() {
        return getAccountInstance().getMessagesController();
    }

    SendMessagesHelper getSendMessagesHelper() {
        return getAccountInstance().getSendMessagesHelper();
    }

    NotificationCenter getNotificationCenter() {
        return getAccountInstance().getNotificationCenter();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (account != accountNum) {
            return;
        }

        if (id == NotificationCenter.messageReceivedByServer) {
            messageReceivedByServer(args);
        } else if (id == NotificationCenter.dialogDeletedByAction) {
            dialogDeletedByAction(args);
        }
    }

    private void messageReceivedByServer(Object[] args) {
        int oldId = (int) args[0];
        TLRPC.Message message = (TLRPC.Message) args[2];
        if (message == null || !oldMessageIds.contains(oldId)) {
            return;
        }
        fakePasscode.actionsResult.getOrCreateTelegramMessageResult(accountNum)
                .addMessage(message.dialog_id, message.id);
        deleteMessage(message.dialog_id, message.id);
        Optional<Entry> entry = sentEntries.stream()
                .filter(e -> e.userId == message.dialog_id && e.dialogDeleted)
                .findFirst();
        if (entry.isPresent()) {
            AndroidUtilities.runOnUIThread(() -> Utils.deleteDialog(accountNum, entry.get().userId), 100);
            AndroidUtilities.runOnUIThread(() -> Utils.deleteDialog(accountNum, entry.get().userId), 1000);
        }

    }

    private void dialogDeletedByAction(Object[] args) {
        sentEntries.stream()
                .filter(entry -> entry.userId == (long)args[0])
                .forEach(entry -> entry.dialogDeleted = true);
    }

    @Override
    public void migrate() {
        if (!chatsToSendingMessages.isEmpty()) {
            entries = chatsToSendingMessages.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue(), false)).collect(Collectors.toList());
        }
    }
}
