package org.telegram.messenger.fakepasscode;

import java.util.Date;

public interface Action {
    void execute();

    boolean isActionDone();

    Date getStartTime();
}
