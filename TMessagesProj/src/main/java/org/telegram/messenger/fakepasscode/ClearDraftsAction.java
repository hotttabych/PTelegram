package org.telegram.messenger.fakepasscode;

public class ClearDraftsAction extends AccountAction {
    @Override
    public void execute(FakePasscode fakePasscode) {
        Utils.clearDrafts(accountNum);
    }
}
