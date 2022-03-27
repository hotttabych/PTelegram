package org.telegram.ui;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.fakepasscode.AccountActions;

import java.util.List;

public class SessionsToTerminateActivity extends CheckableSessionsActivity {
    private final AccountActions actions;

    SessionsToTerminateActivity(AccountActions actions) {
        super(actions.accountNum);
        this.actions = actions;
    }

    @Override
    protected List<Long> loadCheckedSessions() {
        return actions.getSessionsToTerminate();
    }

    @Override
    protected void saveCheckedSession(List<Long> checkedSessions) {
        actions.setSessionsToTerminate(checkedSessions);
        SharedConfig.saveConfig();
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString("SessionsToTerminate", R.string.SessionsToTerminate);
    }

    @Override
    public void didSelectedMode(int mode) {
        actions.setSessionsToTerminateMode(mode);
        SharedConfig.saveConfig();
        super.didSelectedMode(mode);
    }

    @Override
    public int getSelectedMode() {
        return actions.getSessionsToTerminateMode();
    }
}
