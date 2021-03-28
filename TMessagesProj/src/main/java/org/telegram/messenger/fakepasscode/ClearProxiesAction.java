package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;

import java.io.File;

public class ClearProxiesAction implements Action {
    public boolean enabled = true;

    @Override
    public void execute() {
        for (SharedConfig.ProxyInfo proxyInfo : SharedConfig.proxyList) {
            SharedConfig.deleteProxy(proxyInfo);
        }
    }
}