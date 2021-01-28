package org.telegram.messenger.fakepasscode;

import android.telephony.SmsManager;

import org.telegram.messenger.SharedConfig;

public class SosMessageAction implements Action {
    public static String phoneNumber = "";
    public static String message = "";

    @Override
    public void execute() {
        SmsManager manager = SmsManager.getDefault();
        manager.sendTextMessage(phoneNumber, null, message, null, null);
    }
}
