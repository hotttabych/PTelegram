package org.telegram.ui;

import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLObject;

import java.util.ArrayList;

public class SessionsToHideActivity extends CheckableSessionsActivity {
    @Override
    protected ArrayList<TLObject> loadCheckedSessions() {
        return SharedConfig.sessionsToHide;
    }

    @Override
    protected void saveCheckedSession(ArrayList<TLObject> checkedSessions) {
        SharedConfig.sessionsToHide = checkedSessions;
        SharedConfig.saveConfig();
    }
}
