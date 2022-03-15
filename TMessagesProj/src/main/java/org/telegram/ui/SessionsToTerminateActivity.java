package org.telegram.ui;

import org.telegram.messenger.SharedConfig;

import java.util.List;

public class SessionsToTerminateActivity extends CheckableSessionsActivity {
    @Override
    protected List<Long> loadCheckedSessions() {
        return SharedConfig.sessionsToTerminate;
    }

    @Override
    protected void saveCheckedSession(List<Long> checkedSessions) {
        SharedConfig.sessionsToTerminate = checkedSessions;
        SharedConfig.saveConfig();
    }
}
