package org.telegram.messenger.fakepasscode;

public interface Action {
    void execute();
    default void migrate() {}
}
