    package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@FakePasscodeSerializer.ToggleSerialization
public class LogOutAction extends AccountAction {
    private static final int WAIT_TIME = 0;

    public LogOutAction() {}

    public LogOutAction(int accountNum) {
        this.accountNum = accountNum;
    }

    @Override
    public void execute(FakePasscode fakePasscode) {
        fakePasscode.actionsResult.hiddenAccounts.remove(accountNum);
        MessagesController.getInstance(accountNum).performLogout(1);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appDidLogoutByAction, accountNum);
    }

    public void hideAccount(FakePasscode fakePasscode) {
        fakePasscode.actionsResult.hiddenAccounts.add(accountNum);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appHiddenByAction, accountNum);
        AccountInstance.getInstance(accountNum).getNotificationsController().removeAllNotifications();
    }
}
