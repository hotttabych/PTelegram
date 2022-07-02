package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;

import java.io.File;

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