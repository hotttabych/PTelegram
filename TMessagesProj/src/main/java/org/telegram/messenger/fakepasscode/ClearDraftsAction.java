package org.telegram.messenger.fakepasscode;

@FakePasscodeSerializer.ToggleSerialization
public class ClearDraftsAction extends AccountAction {
    @Override
    public void execute(FakePasscode fakePasscode) {
        Utils.clearDrafts(accountNum);
    }
}
