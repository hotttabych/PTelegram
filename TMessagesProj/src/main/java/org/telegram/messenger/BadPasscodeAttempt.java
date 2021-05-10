package org.telegram.messenger;

import android.content.Context;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.telegram.messenger.camera.HiddenCameraManager;

import java.io.File;
import java.time.LocalDateTime;

public class BadPasscodeAttempt {
    public static final int AppUnlockType = 0;
    public static final int PasscodeSettingsType = 1;
    public int type;
    public boolean isFakePasscode;
    public LocalDateTime date;
    public String frontPhotoPath;
    public String backPhotoPath;

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

    public void takePhoto(Context context) {
        if (SharedConfig.takePhotoWithBadPasscodeFront) {
            (new HiddenCameraManager(context)).takePhoto(true, path -> {
                frontPhotoPath = path;
                SharedConfig.saveConfig();
                if (SharedConfig.takePhotoWithBadPasscodeBack) {
                    (new HiddenCameraManager(context)).takePhoto(false, backPath -> {
                        backPhotoPath = backPath;
                        SharedConfig.saveConfig();
                    });
                }
            });
        } else if (SharedConfig.takePhotoWithBadPasscodeBack) {
            (new HiddenCameraManager(context)).takePhoto(false, path -> {
                backPhotoPath = path;
                SharedConfig.saveConfig();
            });
        }
    }

    public void clear() {
        if (frontPhotoPath != null) {
            new File(frontPhotoPath).delete();
            frontPhotoPath = null;
        } else if (backPhotoPath != null) {
            new File(backPhotoPath).delete();
            backPhotoPath = null;
        }
    }
}
