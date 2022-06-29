package org.telegram.messenger.fakepasscode;

import android.telephony.SmsManager;

import org.telegram.messenger.SharedConfig;

import java.util.ArrayList;
import java.util.List;

public class SmsAction implements Action {

    public List<SmsMessage> messages = new ArrayList<>();
    public boolean onlyIfDisconnected = false;

    @Override
    public void execute(FakePasscode fakePasscode) {
        if (onlyIfDisconnected) {
            if (!Utils.isNetworkConnected()) {
                sendMessages();
            }
        } else {
            sendMessages();
        }
    }

    private void sendMessages() {
        SmsManager manager = SmsManager.getDefault();
        String geolocation = Utils.getLastLocationString();
        for (SmsMessage msg: messages) {
            if (!msg.phoneNumber.isEmpty() && !msg.text.isEmpty()) {
                String text = msg.text;
                if (msg.addGeolocation) {
                    text += geolocation;
                }
                manager.sendTextMessage(msg.phoneNumber, null, text, null, null);
            }
        }
    }

    public void addMessage(String phoneNumber, String text, boolean addGeolocation) {
        messages.add(new SmsMessage(phoneNumber, text, addGeolocation));
        SharedConfig.saveConfig();
    }
}
