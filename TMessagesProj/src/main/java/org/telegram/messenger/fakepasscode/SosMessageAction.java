package org.telegram.messenger.fakepasscode;

import android.telephony.SmsManager;

import org.telegram.messenger.SharedConfig;

import java.util.Date;

public class SosMessageAction implements Action {
    public boolean enabled = false;
    public String phoneNumber = "";
    public String message = "";

    @Override
    public void execute() {
        SmsManager manager = SmsManager.getDefault();
        manager.sendTextMessage(phoneNumber, null, message, null, null);
    }

    @Override
    public boolean isActionDone() {
        return true;
    }
}
