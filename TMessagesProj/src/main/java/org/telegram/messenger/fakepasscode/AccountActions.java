package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.SharedConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AccountActions {
    public int accountNum;
    private FakePasscode fakePasscode;

    public TelegramMessageAction messageAction = null;
    public RemoveChatsAction removeChatsAction = null;
    public DeleteContactsAction deleteContactsAction = null;
    public TerminateOtherSessionsAction terminateOtherSessionsAction = null;
    public LogOutAction logOutAction = null;

    public ArrayList<Integer> getChatsToRemove() {
        if (removeChatsAction != null) {
            return removeChatsAction.chatsToRemove;
        }
        return new ArrayList<>();
    }

    public void setChatsToRemove(ArrayList<Integer> chats) {
        if (removeChatsAction != null) {
            removeChatsAction.chatsToRemove = chats;
        } else {
            removeChatsAction = new RemoveChatsAction(accountNum, chats);
            fakePasscode.removeChatsActions.add(removeChatsAction);
        }
        SharedConfig.saveConfig();
    }

    public void changeDeleteContactsState() {
        if (deleteContactsAction != null) {
            deleteContactsAction = null;
            fakePasscode.deleteContactsActions = fakePasscode.deleteContactsActions.stream()
                    .filter(a -> a.accountNum != accountNum).collect(Collectors.toCollection(ArrayList::new));
        } else {
            deleteContactsAction = new DeleteContactsAction(accountNum);
            fakePasscode.deleteContactsActions.add(deleteContactsAction);
        }
        SharedConfig.saveConfig();
    }

    public boolean isDeleteContacts() {
        return deleteContactsAction != null;
    }

    public void changeTerminateActionState() {
        if (terminateOtherSessionsAction != null) {
            terminateOtherSessionsAction = null;
            fakePasscode.terminateOtherSessionsActions = fakePasscode.terminateOtherSessionsActions.stream()
                    .filter(a -> a.accountNum != accountNum).collect(Collectors.toCollection(ArrayList::new));
        } else {
            terminateOtherSessionsAction = new TerminateOtherSessionsAction(accountNum);
            fakePasscode.terminateOtherSessionsActions.add(terminateOtherSessionsAction);
        }
        SharedConfig.saveConfig();
    }

    public boolean isTerminateOtherSessions() {
        return terminateOtherSessionsAction != null;
    }

    public void changeLogOutActionState() {
        if (logOutAction != null) {
            logOutAction = null;
            fakePasscode.logOutActions = fakePasscode.logOutActions.stream()
                    .filter(a -> a.accountNum != accountNum).collect(Collectors.toCollection(ArrayList::new));
        } else {
            logOutAction = new LogOutAction(accountNum);
            fakePasscode.logOutActions.add(logOutAction);
        }
        SharedConfig.saveConfig();
    }

    public boolean isLogOut() {
        return logOutAction != null;
    }

    public AccountActions(int accountNum, FakePasscode fakePasscode)
    {
        this.accountNum = accountNum;
        this.fakePasscode = fakePasscode;
    }
}
