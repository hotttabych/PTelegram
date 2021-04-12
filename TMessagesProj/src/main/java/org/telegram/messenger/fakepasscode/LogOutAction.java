package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class LogOutAction implements Action {
    public int accountNum = 0;
    private static final int WAIT_TIME = 10;
    private FakePasscode fakePasscode;

    public LogOutAction() {
        fakePasscode = new FakePasscode();
    }

    public LogOutAction(int accountNum, FakePasscode fakePasscode) {
        this.accountNum = accountNum;
        this.fakePasscode = fakePasscode;
    }

    @Override
    public void execute() {
        try {
            Thread.sleep(WAIT_TIME);
        } catch (Exception ignored) {
        }

        boolean isActionsReady = true;
        for (Action action : fakePasscode.actions()) {
            isActionsReady = isActionsReady && action.isActionDone();
        }

        if (!isActionsReady && !fakePasscode.telegramMessageAction.isEmpty()) {
            TelegramMessageAction action = fakePasscode.findTelegramMessageAction(accountNum);
            if (action != null) {
                FakePasscodeMessages.hasUnDeletedMessages.put(accountNum,
                        new HashMap<>(action.getMessagesLeftToSend()));
                FakePasscodeMessages.saveMessages();
            }
        }
        SharedConfig.saveConfig();
        MessagesController.getInstance(accountNum).performLogout(1);
    }

    @Override
    public boolean isActionDone() {
        return true;
    }
}
