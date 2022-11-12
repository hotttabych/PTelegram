package org.telegram.ui.DialogBuilder;

import android.content.DialogInterface;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.Locale;

public class DialogButtonWithTimer {
    private static class Info {
        String text;
        public int timeout;
        public boolean isDialogDismissed = false;
    }

    public static void setButton(AlertDialog dialog, int buttonType, String text, int timeout, final DialogInterface.OnClickListener listener) {
        Info info = new Info();
        info.text = text;
        info.timeout = timeout;

        dialog.setButton(buttonType,text + "(" + timeout + ")", (dlg, which) -> {
            if (info.timeout == 0) {
                listener.onClick(dlg, which);
            }
        });
        dialog.setOnShowListener(dlg -> {
            TextView button = (TextView)dialog.getButton(buttonType);
            button.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
            button.setEnabled(false);
            TimeoutRunnable timeoutRunnable = new TimeoutRunnable(button, info);
            Utilities.globalQueue.postRunnable(timeoutRunnable, 1000);
        });
    }

    private static class TimeoutRunnable implements Runnable {
        TextView cancelButton;
        public Info info;

        public TimeoutRunnable(TextView cancelButton, Info info) {
            this.cancelButton = cancelButton;
            this.info = info;
        }

        @Override
        public void run() {
            if (!info.isDialogDismissed) {
                info.timeout--;
                AndroidUtilities.runOnUIThread(() -> {
                    if (info.timeout > 0) {
                        cancelButton.setText((info.text + " (" + info.timeout + ")").toUpperCase(Locale.ROOT));
                    } else {
                        cancelButton.setText(info.text.toUpperCase(Locale.ROOT));
                        cancelButton.setTextColor(Theme.getColor(Theme.key_dialogTextGray));
                        cancelButton.setEnabled(true);
                    }
                });
                if (info.timeout > 0) {
                    Utilities.globalQueue.postRunnable(this, 1000);
                }
            }
        }
    }
}
