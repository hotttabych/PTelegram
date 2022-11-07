package org.telegram.messenger.fakepasscode;

import java.util.ArrayList;
import java.util.List;

public class SmsAction implements Action {

    public List<SmsMessage> messages = new ArrayList<>();
    public boolean onlyIfDisconnected = false;

    @Override
    public void execute(FakePasscode fakePasscode) {

    }
}
