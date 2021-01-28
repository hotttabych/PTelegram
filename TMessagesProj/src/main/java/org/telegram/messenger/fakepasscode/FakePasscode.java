package org.telegram.messenger.fakepasscode;

import java.util.List;

public class FakePasscode {
    public boolean allowLogin;
    public String name;
    public String passcodeHash;
    public List<Action> actions;

    public void executeActions() {
        for (Action action : actions) {
            try {
                action.execute();
            } catch (Exception ignored) {
            }
        }
    }
}
