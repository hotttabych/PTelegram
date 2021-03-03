package org.telegram.messenger.fakepasscode;

public class SmsMessage {
    public String phoneNumber = "";
    public String text = "";

    public SmsMessage() {}

    SmsMessage(String phoneNumber, String text) {
        this.phoneNumber = phoneNumber;
        this.text = text;
    }
}
