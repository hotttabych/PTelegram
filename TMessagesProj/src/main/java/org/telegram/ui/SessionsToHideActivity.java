package org.telegram.ui;

import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.fakepasscode.AccountActions;

import java.util.List;

public class SessionsToHideActivity extends CheckableSessionsActivity {
    private AccountActions actions;

    SessionsToHideActivity(AccountActions actions) {
        super();
        this.actions = actions;
    }

    @Override
    protected List<Long> loadCheckedSessions() {
        return actions.getFakePasscode().sessionsToHide;
    }

    @Override
    protected void saveCheckedSession(List<Long> checkedSessions) {
        actions.getFakePasscode().sessionsToHide = checkedSessions;
        SharedConfig.saveConfig();
    }
}
