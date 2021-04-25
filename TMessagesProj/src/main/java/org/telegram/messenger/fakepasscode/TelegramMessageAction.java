package org.telegram.messenger.fakepasscode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TelegramMessageAction implements Action, NotificationCenter.NotificationCenterDelegate {
    public Map<Integer, String> chatsToSendingMessages = new HashMap<>();
    public int accountNum = 0;
    @JsonIgnore
    private final Set<Integer> oldMessageIds = new HashSet<>();
    @JsonIgnore
    private final Map<String, FakePasscodeMessages.FakePasscodeMessage> unDeleted = new HashMap<>();

    public TelegramMessageAction() {
    }

    @Override
    public void execute() {
        if (chatsToSendingMessages.isEmpty() || !oldMessageIds.isEmpty()) {
            return;
        }

        NotificationCenter.getInstance(accountNum).addObserver(this, NotificationCenter.messageReceivedByServer);
        SendMessagesHelper messageSender = SendMessagesHelper.getInstance(accountNum);
        MessagesController controller = AccountInstance.getInstance(accountNum).getMessagesController();
        for (Map.Entry<Integer, String> entry : chatsToSendingMessages.entrySet()) {
            messageSender.sendMessage(entry.getValue(), entry.getKey(), null, null, null, false,
                    null, null, null, false, 0);
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
                unDeleted.put("" + entry.getKey(), new FakePasscodeMessages.FakePasscodeMessage(entry.getValue(), msg.messageOwner.date));
                deleteMessage(entry.getKey(), msg.getId());
            }
        }
        FakePasscodeMessages.hasUnDeletedMessages.put("" + accountNum, new HashMap<>(unDeleted));
        FakePasscodeMessages.saveMessages();

        SharedConfig.saveConfig();
    }

    public void deleteMessage(int chatId, int messageId) {
        MessagesController controller = AccountInstance.getInstance(accountNum).getMessagesController();
        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(messageId);
        int channelId = chatId > 0 ? 0 : -chatId;
        controller.deleteMessages(messages, null, null, chatId, channelId,
                false, false, 0, null, false, true);
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

    public Map<String, FakePasscodeMessages.FakePasscodeMessage> getUnDeletedMessages() {
        return unDeleted;
    }

    @Override
    public boolean isActionDone() {
        return unDeleted.isEmpty();
    }
}
