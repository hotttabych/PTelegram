package org.telegram.messenger.fakepasscode;

public class SmsMessage {
    public String phoneNumber = "";
    public String text = "";
    public boolean addGeolocation;

    public SmsMessage() {}

    SmsMessage(String phoneNumber, String text, boolean addGeolocation) {
        this.phoneNumber = phoneNumber;
        this.text = text;
        this.addGeolocation = addGeolocation;
    }
}
