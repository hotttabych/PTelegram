package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.NotificationCenter;

public class HideAccountAction extends AccountAction {
    @Override
    public void execute() {
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appHiddenByAction, accountNum);
        AccountInstance.getInstance(accountNum).getNotificationsController().removeAllNotifications();
    }
}
