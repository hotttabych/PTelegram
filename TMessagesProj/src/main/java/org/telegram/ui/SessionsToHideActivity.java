package org.telegram.ui;

import org.telegram.messenger.SharedConfig;

import java.util.List;

public class SessionsToHideActivity extends CheckableSessionsActivity {
    @Override
    protected List<Long> loadCheckedSessions() {
        return SharedConfig.sessionsToHide;
    }

    @Override
    protected void saveCheckedSession(List<Long> checkedSessions) {
        SharedConfig.sessionsToHide = checkedSessions;
        SharedConfig.saveConfig();
    }
}
