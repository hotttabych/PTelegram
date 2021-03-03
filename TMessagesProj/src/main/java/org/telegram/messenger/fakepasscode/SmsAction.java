package org.telegram.messenger.fakepasscode;

import android.telephony.SmsManager;

import org.telegram.messenger.SharedConfig;

import java.util.ArrayList;
import java.util.List;

public class SmsAction implements Action{

    public List<SmsMessage> messages = new ArrayList<>();

    @Override
    public void execute() {
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
