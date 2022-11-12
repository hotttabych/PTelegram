package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.SharedConfig;

@FakePasscodeSerializer.EnabledSerialization
public class ClearProxiesAction implements Action {
    public boolean enabled = false;

    @Override
    public void execute(FakePasscode fakePasscode) {
        if (enabled) {
            while (SharedConfig.proxyList.size() > 0) {
                SharedConfig.deleteProxy(SharedConfig.proxyList.get(0));
            }
        }
    }
}