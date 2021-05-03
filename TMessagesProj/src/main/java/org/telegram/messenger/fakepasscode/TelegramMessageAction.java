package org.telegram.messenger.fakepasscode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TelegramMessageAction extends AccountAction implements NotificationCenter.NotificationCenterDelegate {
    public static class Entry {
        public Entry() {}

        public Entry(int userId, String text, boolean addGeolocation) {
            this.userId = userId;
            this.text = text;
            this.addGeolocation = addGeolocation;
        }

        public int userId;
        public String text;
        public boolean addGeolocation;
    }

    public List<Entry> entries = new ArrayList<>();

    @Deprecated
    public Map<Integer, String> chatsToSendingMessages = new HashMap<>();

    @JsonIgnore
    private final Set<Integer> oldMessageIds = new HashSet<>();
    @JsonIgnore
    private final Map<String, FakePasscodeMessages.FakePasscodeMessage> unDeleted = new HashMap<>();

    public TelegramMessageAction() {
    }

    @Override
    public void execute() {
        if ((chatsToSendingMessages.isEmpty() && entries.isEmpty()) || !oldMessageIds.isEmpty()) {
            return;
        }

        NotificationCenter.getInstance(accountNum).addObserver(this, NotificationCenter.messageReceivedByServer);

        SendMessagesHelper messageSender = SendMessagesHelper.getInstance(accountNum);
        MessagesController controller = AccountInstance.getInstance(accountNum).getMessagesController();
        String geolocation = Utils.getLastLocationString();
        for (Entry entry : entries) {
            String text = entry.text;
            if (entry.addGeolocation) {
                text += geolocation;
            }
            TLRPC.Message oldMessage = controller.dialogMessagesByIds.get(controller.dialogs_dict.get(entry.userId).top_message).messageOwner;
            messageSender.sendMessage(text, entry.userId, null, null, null, false,
                        null, null, null, true, 0);
            MessageObject msg = null;
            for (int i = 0; i < controller.dialogMessage.size(); ++i) {
                if (controller.dialogMessage.valueAt(i).messageText != null &&
                        text.contentEquals(controller.dialogMessage.valueAt(i).messageText)) {
                    msg = controller.dialogMessage.valueAt(i);
                    break;
                }
            }

            if (msg != null) {
                oldMessageIds.add(msg.getId());
                unDeleted.put("" + entry.userId, new FakePasscodeMessages.FakePasscodeMessage(entry.text, msg.messageOwner.date,
                        oldMessage));
                deleteMessage(entry.userId, msg.getId());
            }
        }
        FakePasscodeMessages.hasUnDeletedMessages.put("" + accountNum, new HashMap<>(unDeleted));
        FakePasscodeMessages.saveMessages();
    }

    private void deleteMessage(int chatId, int messageId) {
        MessagesController controller = AccountInstance.getInstance(accountNum).getMessagesController();
        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(messageId);
        int channelId = chatId > 0 ? 0 : -chatId;
        controller.deleteMessages(messages, null, null, chatId, channelId,
                false, false, false, 0, null, false, true);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (account != accountNum) {
            return;
        }

        int oldId = (int) args[0];
        TLRPC.Message message = (TLRPC.Message) args[2];
        if (message == null || !oldMessageIds.contains(oldId)) {
            return;
        }
        oldMessageIds.remove(oldId);
        deleteMessage(Long.valueOf(message.dialog_id).intValue(), message.id);
    }

    @Override
    public void migrate() {
        if (!chatsToSendingMessages.isEmpty()) {
            entries = chatsToSendingMessages.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue(), false)).collect(Collectors.toList());
        }
    }

    public Map<String, FakePasscodeMessages.FakePasscodeMessage> getUnDeletedMessages() {
        return unDeleted;
    }
}
