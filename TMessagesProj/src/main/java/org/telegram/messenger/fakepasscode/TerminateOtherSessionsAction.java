package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TerminateOtherSessionsAction extends AccountAction {
    public int mode = 0;
    public List<Long> sessions = Collections.synchronizedList(new ArrayList<>());

    public TerminateOtherSessionsAction() {}

    public TerminateOtherSessionsAction(int accountNum) {
        this.accountNum = accountNum;
    }

    @Override
    public void execute() {
        FakePasscode fakePasscode = SharedConfig.getActivatedFakePasscode();
        if (fakePasscode != null) {
            List<Long> sessionsToTerminate = sessions;
            if (mode == 1) {
                for (Long session : sessionsToTerminate) {
                    TLRPC.TL_account_resetAuthorization req = new TLRPC.TL_account_resetAuthorization();
                    req.hash = session;
                    ConnectionsManager.getInstance(accountNum).sendRequest(req, (response, error) -> {
                    });
                }
            } else if (mode == 2) {
                TLRPC.TL_account_getAuthorizations req = new TLRPC.TL_account_getAuthorizations();
                ConnectionsManager.getInstance(accountNum).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        TLRPC.TL_account_authorizations res = (TLRPC.TL_account_authorizations) response;
                        for (int a = 0, N = res.authorizations.size(); a < N; a++) {
                            TLRPC.TL_authorization authorization = res.authorizations.get(a);
                            if ((authorization.flags & 1) == 0 && !sessions.contains(authorization.hash)) {
                                TLRPC.TL_account_resetAuthorization terminateReq = new TLRPC.TL_account_resetAuthorization();
                                terminateReq.hash = authorization.hash;
                                ConnectionsManager.getInstance(accountNum).sendRequest(terminateReq, (tResponse, tError) -> {
                                });
                            }
                        }
                    }
                }));

            }
        }
    }
}
