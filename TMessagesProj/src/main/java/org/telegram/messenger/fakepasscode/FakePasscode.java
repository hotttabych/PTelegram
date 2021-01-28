package org.telegram.messenger.fakepasscode;

import java.util.List;

public class FakePasscode {
    public boolean allowLogin;
    public String name;
    public List<Action> actions;

    void login() {
        for (Action action : actions) {
            try {
                action.execute();
            } catch (Exception ignored) {
            }
        }
    }
}
