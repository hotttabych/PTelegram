package org.telegram.messenger.fakepasscode;

import android.telephony.SmsManager;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

import java.util.ArrayList;
import java.util.List;

public class SmsAction implements Action {

    public List<SmsMessage> messages = new ArrayList<>();
    public boolean onlyIfDisconnected = false;

    @Override
    public void execute() {
        if (onlyIfDisconnected) {
            if (!isConnected()) {
                sendMessages();
            }
        } else {
            sendMessages();
        }
    }

    private boolean isConnected() {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            AccountInstance account = AccountInstance.getInstance(i);
            ConnectionsManager connectionsManager = account.getConnectionsManager();
            int connectionState = connectionsManager.getConnectionState();
            if (connectionState != ConnectionsManager.ConnectionStateWaitingForNetwork) {
                return true;
            }
        }
        return false;
    }

    private void sendMessages() {
        SmsManager manager = SmsManager.getDefault();
        for (SmsMessage msg: messages) {
            if (!msg.phoneNumber.isEmpty() && !msg.text.isEmpty()) {
                manager.sendTextMessage(msg.phoneNumber, null, msg.text, null, null);
            }
        }
    }

    public void addMessage(String phoneNumber, String text) {
        messages.add(new SmsMessage(phoneNumber, text));
        SharedConfig.saveConfig();
    }
}
