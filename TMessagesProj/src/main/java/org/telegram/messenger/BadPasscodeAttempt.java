package org.telegram.messenger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public class BadPasscodeAttempt {
    public static final int AppUnlockType = 0;
    public static final int PasscodeSettingsType = 1;
    public int type;
    public boolean isFakePasscode;
    public LocalDateTime date;

    public BadPasscodeAttempt() {}
    public BadPasscodeAttempt(int type, boolean isFakePasscode) {
        this.type = type;
        this.isFakePasscode = isFakePasscode;
        this.date = LocalDateTime.now();
    }

    @JsonIgnore
    public String getTypeString() {
        switch (type) {
            case AppUnlockType: return LocaleController.getString("AppUnlock", R.string.AppUnlock);
            default:
            case PasscodeSettingsType: return LocaleController.getString("EnterPasswordSettings", R.string.EnterPasswordSettings);
        }
    }
}
