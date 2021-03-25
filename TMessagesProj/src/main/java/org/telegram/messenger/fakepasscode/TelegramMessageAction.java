package org.telegram.messenger.fakepasscode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TelegramMessageAction implements Action, NotificationCenter.NotificationCenterDelegate {
    public Map<Integer, String> chatsToSendingMessages = new HashMap<>();
    public int accountNum = 0;
    @JsonIgnore
    private Set<Integer> oldMessageIds = new HashSet<>();
    @JsonIgnore
    private Date startDate = null;

    @Override
    public void execute() {
        if (chatsToSendingMessages.isEmpty()) {
            return;
        }
        NotificationCenter.getInstance(accountNum).addObserver(this, NotificationCenter.messageReceivedByServer);

        SendMessagesHelper messageSender = SendMessagesHelper.getInstance(accountNum);
        MessagesController controller = AccountInstance.getInstance(accountNum).getMessagesController();
        for (Map.Entry<Integer, String> entry : chatsToSendingMessages.entrySet()) {
            messageSender.sendMessage(entry.getValue(), entry.getKey(), null, null, null, false,
                        null, null, null, true, 0);
            MessageObject msg = null;
            for (int i = 0; i < controller.dialogMessage.size(); ++i) {
                if (controller.dialogMessage.valueAt(i).messageText != null &&
                        entry.getValue().contentEquals(controller.dialogMessage.valueAt(i).messageText)) {
                    msg = controller.dialogMessage.valueAt(i);
                    break;
                }
            }

            if (msg != null) {
                oldMessageIds.add(msg.getId());
                deleteMessage(entry.getKey(), msg.getId());
            }
        }
        startDate = new Date();

        SharedConfig.saveConfig();
    }

    private void deleteMessage(int chatId, int messageId) {
        MessagesController controller = AccountInstance.getInstance(accountNum).getMessagesController();
        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(messageId);
        int channelId = chatId > 0 ? 0 : -chatId;
        controller.deleteMessages(messages, null, null, chatId, channelId, false, false);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        MessagesController controller = AccountInstance.getInstance(accountNum).getMessagesController();
        int oldId = (int)args[0];
        TLRPC.Message message = (TLRPC.Message) args[2];
        if (message == null || !oldMessageIds.contains(oldId)) {
            return;
        }
        deleteMessage(Long.valueOf(message.dialog_id).intValue(), message.id);
        oldMessageIds.remove(oldId);
    }

    @Override
    public boolean isActionDone() {
        return oldMessageIds.isEmpty();
    }

    @Override
    public Date getStartTime() {
        return startDate; // Dummy realization. TODO
    }
}
