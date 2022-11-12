package org.telegram.messenger.fakepasscode;

@FakePasscodeSerializer.EnabledSerialization
public class ClearCacheAction implements Action {
    public boolean enabled = false;

    @Override
    public void execute(FakePasscode fakePasscode) {
        if (enabled) {
            Utils.clearCache(null);
        }
    }
}
