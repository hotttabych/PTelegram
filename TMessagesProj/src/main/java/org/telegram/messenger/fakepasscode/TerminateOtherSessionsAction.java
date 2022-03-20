package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.List;

public class TerminateOtherSessionsAction extends AccountAction {
    public TerminateOtherSessionsAction() {}

    public TerminateOtherSessionsAction(int accountNum) {
        this.accountNum = accountNum;
    }

    @Override
    public void execute() {
        FakePasscode fakePasscode = SharedConfig.getActivatedFakePasscode();
        if (fakePasscode != null) {
            List<Long> sessionsToTerminate = fakePasscode.sessionsToTerminate;
            for (Long session : sessionsToTerminate) {
                TLRPC.TL_account_resetAuthorization req = new TLRPC.TL_account_resetAuthorization();
                req.hash = session;
                ConnectionsManager.getInstance(accountNum).sendRequest(req, (response, error) -> {
                });
            }
        }
    }
}
