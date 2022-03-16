package org.telegram.ui;

import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.fakepasscode.AccountActions;

import java.util.List;

public class SessionsToTerminateActivity extends CheckableSessionsActivity {
    private AccountActions actions;

    SessionsToTerminateActivity(AccountActions actions) {
        super();
        this.actions = actions;
    }

    @Override
    protected List<Long> loadCheckedSessions() {
        return actions.getFakePasscode().sessionsToTerminate;
    }

    @Override
    protected void saveCheckedSession(List<Long> checkedSessions) {
        actions.getFakePasscode().sessionsToTerminate = checkedSessions;
        SharedConfig.saveConfig();
    }
}
