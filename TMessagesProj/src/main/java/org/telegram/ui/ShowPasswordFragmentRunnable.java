package org.telegram.ui;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;

public class ShowPasswordFragmentRunnable  implements Runnable {
    private BaseFragment parentFragment;
    private BaseFragment passwordFragment;
    private int delay;

    public ShowPasswordFragmentRunnable(BaseFragment parentFragment, BaseFragment passwordFragment, int delay) {
        this.parentFragment = parentFragment;
        this.passwordFragment = passwordFragment;
        this.delay = delay;
    }

    public void run() {
        AndroidUtilities.runOnUIThread(() -> {
            if (parentFragment.presentFragment(passwordFragment)) {
                passwordFragment = null;
            } else {
                Utilities.globalQueue.postRunnable(new ShowPasswordFragmentRunnable(parentFragment, passwordFragment, delay), delay);
            }
        });
    }
}
