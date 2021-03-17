package org.telegram.messenger.fakepasscode;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesController;

public class DeleteContactsAction implements Action {
    public int accountNum = 0;

    public DeleteContactsAction() {}

    public DeleteContactsAction(int accountNum) {
        this.accountNum = accountNum;
    }

    @Override
    public void execute() {
        ContactsController.getInstance(accountNum).deleteAllContacts(() -> {});
    }
}
