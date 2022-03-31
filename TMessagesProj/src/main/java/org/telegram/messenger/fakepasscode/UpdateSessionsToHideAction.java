package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

public class UpdateSessionsToHideAction extends AccountAction {
    public int mode = 0;
    public List<Long> checkedSessions = new ArrayList<>();
    public List<Long> sessionsToHide = new ArrayList<>();

    @Override
    public void execute() {
        if (mode == 0) {
            sessionsToHide = checkedSessions;
        } else if (mode == 1) {
            sessionsToHide = new ArrayList<>();
            TLRPC.TL_account_getAuthorizations req = new TLRPC.TL_account_getAuthorizations();
            ConnectionsManager.getInstance(accountNum).sendRequest(req, (response, error) -> {
                if (error == null) {
                    TLRPC.TL_account_authorizations res = (TLRPC.TL_account_authorizations) response;
                    for (int a = 0, N = res.authorizations.size(); a < N; a++) {
                        TLRPC.TL_authorization authorization = res.authorizations.get(a);
                        if (!checkedSessions.contains(authorization.hash)) {
                            sessionsToHide.add(authorization.hash);
                        }
                    }
                }
            });
        }
    }
}
