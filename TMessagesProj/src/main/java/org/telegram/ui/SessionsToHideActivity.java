package org.telegram.ui;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.fakepasscode.AccountActions;

import java.util.List;

public class SessionsToHideActivity extends CheckableSessionsActivity {
    private final AccountActions actions;

    SessionsToHideActivity(AccountActions actions) {
        super(actions.accountNum);
        this.actions = actions;
    }

    @Override
    protected List<Long> loadCheckedSessions() {
        return actions.getCheckedSessionsToHide();
    }

    @Override
    protected void saveCheckedSession(List<Long> checkedSessions) {
        actions.setSessionsToHide(checkedSessions);
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString("SessionsToHide", R.string.SessionsToHide);
    }

    @Override
    public void didSelectedMode(int mode) {
        actions.setSessionsToHideMode(mode);
        super.didSelectedMode(mode);
    }

    @Override
    public int getSelectedMode() {
        return actions.getSessionsToHideMode();
    }
}
