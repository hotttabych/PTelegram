package org.telegram.messenger.fakepasscode;

import android.telephony.SmsManager;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.telegram.messenger.SharedConfig;

@Deprecated
public class SosMessageAction implements Action {
    public boolean enabled = false;
    public String phoneNumber = "";
    public String message = "";

    @JsonIgnore
    public boolean isFilled() {
        return phoneNumber != null && !phoneNumber.isEmpty() && message != null && !message.isEmpty();
    }

    @Override
    public void execute() {
        SmsManager manager = SmsManager.getDefault();
        if (enabled && !phoneNumber.isEmpty() && !message.isEmpty()) {
            manager.sendTextMessage(phoneNumber, null, message, null, null);
        }
    }
}
