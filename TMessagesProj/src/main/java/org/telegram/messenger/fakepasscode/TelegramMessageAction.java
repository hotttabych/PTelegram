package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Adapters.MessagesSearchAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelegramMessageAction implements Action, NotificationCenter.NotificationCenterDelegate {
    public Map<Integer, String> chatsToSendingMessages = new HashMap<>();
    public int accountNum = 0;

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
            MessageObject obj = null;
            for (int i = 0; i < controller.dialogMessage.size(); ++i) {
                if (controller.dialogMessage.valueAt(i).messageText != null &&
                        entry.getValue().contentEquals(controller.dialogMessage.valueAt(i).messageText)) {
                    obj = controller.dialogMessage.valueAt(i);
                    break;
                }
            }

            ArrayList<Integer> messages = new ArrayList<>();
            if (obj != null) {
                messages.add(obj.getId());
            }
            if (entry.getKey() > 0) {
                controller.deleteMessages(messages, null, null, entry.getKey(),
                        0, false, false);
            } else {
                controller.deleteMessages(messages, null, null, entry.getKey(),
                        -entry.getKey(), false, false);
            }
        }

        SharedConfig.saveConfig();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        MessagesController controller = AccountInstance.getInstance(accountNum).getMessagesController();
        TLRPC.Message message = (TLRPC.Message) args[2];
        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(message.id);
        if (message.dialog_id > 0) {
            controller.deleteMessages(messages, null, null, message.dialog_id,
                    0, false, false);
        } else {
            controller.deleteMessages(messages, null, null, message.dialog_id,
                    -Long.valueOf(message.dialog_id).intValue(), false, false);
        }
        NotificationCenter.getInstance(accountNum).removeObserver(this, NotificationCenter.messageReceivedByServer);
    }
}
