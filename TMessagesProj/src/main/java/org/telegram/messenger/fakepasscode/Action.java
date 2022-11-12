package org.telegram.messenger.fakepasscode;

public interface Action {
    void execute(FakePasscode fakePasscode);
    default void migrate() {}
}
