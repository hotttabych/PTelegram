package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.fakepasscode.AccountActions;

import java.util.List;

public class SessionsToTerminateActivity extends CheckableSessionsActivity {
    private final AccountActions actions;

    SessionsToTerminateActivity(AccountActions actions) {
        super(actions.accountNum);
        this.actions = actions;
    }


    @Override
    public View createView(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("TerminateOtherSessionsWarningTitle", R.string.TerminateOtherSessionsWarningTitle));
        builder.setMessage(LocaleController.getString("TerminateOtherSessionsWarningMessage", R.string.TerminateOtherSessionsWarningMessage));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        showDialog(builder.create());
        return super.createView(context);
    }

    @Override
    protected List<Long> loadCheckedSessions() {
        return actions.getSessionsToTerminate();
    }

    @Override
    protected void saveCheckedSession(List<Long> checkedSessions) {
        actions.setSessionsToTerminate(checkedSessions);
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString("SessionsToTerminate", R.string.SessionsToTerminate);
    }

    @Override
    public void didSelectedMode(int mode) {
        actions.setSessionsToTerminateMode(mode);
        super.didSelectedMode(mode);
    }

    @Override
    public int getSelectedMode() {
        return actions.getSessionsToTerminateMode();
    }
}
