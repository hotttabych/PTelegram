/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.telegram.messenger.fakepasscode.Utils;
import org.telegram.tgnet.ConnectionsManager;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("screen off");
            }
            ConnectionsManager.getInstance(UserConfig.selectedAccount).setAppPaused(true, true);
            ApplicationLoader.isScreenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("screen on");
            }
            ConnectionsManager.getInstance(UserConfig.selectedAccount).setAppPaused(false, true);
            ApplicationLoader.isScreenOn = true;
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.screenStateChanged);
        if (!SharedConfig.isFakePasscodeActivated() && intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Runnable executeOnScreenLockAction = () -> {
                if (SharedConfig.onScreenLockAction == 1) {
                    SharedConfig.appLocked = true;
                    SharedConfig.saveConfig();
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(homeIntent);
                } else if (SharedConfig.onScreenLockAction == 2) {
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.shouldKillApp);
                }
            };
            if (SharedConfig.onScreenLockActionClearCache) {
                Utils.clearCache(executeOnScreenLockAction);
            } else {
                executeOnScreenLockAction.run();
            }
        }
    }
}
