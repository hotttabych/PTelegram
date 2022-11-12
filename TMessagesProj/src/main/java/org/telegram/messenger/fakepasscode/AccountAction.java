package org.telegram.messenger.fakepasscode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.telegram.messenger.UserConfig;

public abstract class AccountAction implements Action {
    @JsonIgnore
    protected int accountNum = 0;

    @JsonProperty(value = "accountNum", access = JsonProperty.Access.WRITE_ONLY)
    public void setAccountNum(int accountNum) {
        this.accountNum = accountNum;
    }

    protected UserConfig getUserConfig() {
        return UserConfig.getInstance(accountNum);
    }
}
