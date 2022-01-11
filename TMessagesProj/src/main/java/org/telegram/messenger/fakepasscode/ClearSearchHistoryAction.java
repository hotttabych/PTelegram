package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;

public class ClearSearchHistoryAction extends AccountAction {
    // Required for backward compatibility
    public boolean enabled = false;

    @Override
    public void execute() {
        MessagesStorage.getInstance(accountNum).getStorageQueue().postRunnable(() -> {
            try {
                MessagesStorage.getInstance(accountNum).getDatabase().executeFast("DELETE FROM hashtag_recent_v2 WHERE 1").stepThis().dispose();
            } catch (Exception ignored) {
            }
            try {
                MessagesStorage.getInstance(accountNum).getDatabase().executeFast("DELETE FROM search_recent WHERE 1").stepThis().dispose();
            } catch (Exception ignored) {
            }
            NotificationCenter.getInstance(accountNum).postNotificationName(NotificationCenter.searchCleared);
            MediaDataController.getInstance(accountNum).clearTopPeers();
        });
    }
}
