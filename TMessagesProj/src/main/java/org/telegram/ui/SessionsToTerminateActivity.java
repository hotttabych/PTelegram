package org.telegram.ui;

import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLObject;

import java.util.ArrayList;

public class SessionsToTerminateActivity extends CheckableSessionsActivity {
    @Override
    protected ArrayList<TLObject> loadCheckedSessions() {
        return SharedConfig.sessionsToTerminate;
    }

    @Override
    protected void saveCheckedSession(ArrayList<TLObject> checkedSessions) {
        SharedConfig.sessionsToTerminate = checkedSessions;
        SharedConfig.saveConfig();
    }
}
