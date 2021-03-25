package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.MessagesController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogOutAction implements Action {
    public int accountNum = 0;
    private static final int WAIT_TIME = 1000;
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
        boolean is_all_actions_ready = false;
        while (!is_all_actions_ready) {
            is_all_actions_ready = true;
            for (Action action : fakePasscode.actions()) {
                Date current_date = new Date();
                if (!action.isActionDone() && action.getStartTime() != null &&
                        (current_date.getTime() - action.getStartTime().getTime()) > WAIT_TIME) {
                    is_all_actions_ready = true;
                    break;
                } else {
                    is_all_actions_ready = is_all_actions_ready && action.isActionDone();
                }
            }
        }

        MessagesController.getInstance(accountNum).performLogout(1);
    }

    @Override
    public boolean isActionDone() {
        return true;
    }

    @Override
    public Date getStartTime() {
        return null; // Dummy realization. TODO
    }
}
