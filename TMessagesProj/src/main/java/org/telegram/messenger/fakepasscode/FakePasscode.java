package org.telegram.messenger.fakepasscode;

import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.android.exoplayer2.util.Log;
import com.google.android.gms.common.util.Strings;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.NotificationsSettingsActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown=true)
public class FakePasscode {
    @JsonIgnore
    private final int CURRENT_PASSCODE_VERSION = 3;
    private int passcodeVersion = 0;

    @JsonProperty(value = "PLATFORM", access = JsonProperty.Access.READ_ONLY)
    public String getPlatform() {
        return "ANDROID";
    }

    public boolean allowLogin = true;
    public String name;
    @FakePasscodeSerializer.Ignore
    public String passcodeHash = "";
    public String activationMessage = "";
    public Integer badTriesToActivate;
    public boolean activateByFingerprint;
    public boolean clearAfterActivation;
    public boolean deleteOtherPasscodesAfterActivation;

    public ClearCacheAction clearCacheAction = new ClearCacheAction();
    public SmsAction smsAction = new SmsAction();
    public ClearProxiesAction clearProxiesAction = new ClearProxiesAction();

    @FakePasscodeSerializer.Ignore
    ActionsResult actionsResult = new ActionsResult();
    Integer activationDate = null;

    public List<AccountActions> accountActions = Collections.synchronizedList(new ArrayList<>());

    List<Action> actions()
    {
        List<Action> result = new ArrayList<>(Arrays.asList(clearCacheAction, smsAction));
        result.addAll(accountActions);
        result.add(clearProxiesAction);
        return result;
    }

    public AccountActions getAccountActions(int accountNum) {
        for (AccountActions actions : accountActions) {
            Integer actionsAccountNum = actions.getAccountNum();
            if (actionsAccountNum != null && actionsAccountNum == accountNum) {
                return actions;
            }
        }
        return null;
    }

    public AccountActions getOrCreateAccountActions(int accountNum) {
        for (AccountActions actions : accountActions) {
            Integer actionsAccountNum = actions.getAccountNum();
            if (actionsAccountNum != null && actionsAccountNum == accountNum) {
                return actions;
            }
        }
        AccountActions actions = new AccountActions();
        actions.setAccountNum(accountNum);
        accountActions.add(actions);
        return actions;
    }

    public List<AccountActions> getAllAccountActions() {
        return Collections.unmodifiableList(accountActions);
    }

    public List<AccountActions> getFilteredAccountActions() {
        return accountActions.stream().filter(a -> a.getAccountNum() != null).collect(Collectors.toList());
    }

    public void executeActions() {
        if (SharedConfig.fakePasscodeActivatedIndex == SharedConfig.fakePasscodes.indexOf(this)) {
            return;
        }
        activationDate = ConnectionsManager.getInstance(UserConfig.selectedAccount).getCurrentTime();
        actionsResult = new ActionsResult();
        AndroidUtilities.runOnUIThread(() -> {
            for (Action action : actions()) {
                try {
                    action.execute(this);
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) {
                        Log.e("FakePasscode", "Error", e);
                    }
                }
            }
            if (deleteOtherPasscodesAfterActivation) {
                SharedConfig.fakePasscodes = SharedConfig.fakePasscodes.stream()
                        .filter(passcode -> passcode == this).collect(Collectors.toList());
            }
            if (clearAfterActivation) {
                clear();
            }
        });
    }

    private void clear() {
        activationMessage = "";
        badTriesToActivate = null;
        clearAfterActivation = false;
        deleteOtherPasscodesAfterActivation = false;

        clearCacheAction = new ClearCacheAction();
        smsAction = new SmsAction();
        clearProxiesAction = new ClearProxiesAction();
        accountActions.clear();
        SharedConfig.saveConfig();
    }

    public void migrate() {
        actions().stream().forEach(Action::migrate);
        passcodeVersion = CURRENT_PASSCODE_VERSION;
    }

    public void onDelete() { }

    boolean needDeleteMessage(int accountNum, long dialogId) {
        RemoveChatsResult result = actionsResult.getRemoveChatsResult(accountNum);
        if (result != null && result.isRemoveNewMessagesFromChat(dialogId)) {
            return true;
        }
        AccountActions accountActions = getAccountActions(accountNum);
        return accountActions != null
                && accountActions.getRemoveChatsAction().isRemoveNewMessagesFromChat(dialogId);
    }

    public int getHideOrLogOutCount() {
        return (int)getFilteredAccountActions().stream().filter(AccountActions::isLogOutOrHideAccount).count();
    }

    public int getHideAccountCount() {
        return (int)getFilteredAccountActions().stream().filter(AccountActions::isHideAccount).count();
    }

    public boolean autoAddAccountHidings() {
        disableHidingForDeactivatedAccounts();
        checkSingleAccountHidden();

        int targetCount = UserConfig.getActivatedAccountsCount(true) - UserConfig.getFakePasscodeMaxAccountCount();
        if (targetCount > getHideOrLogOutCount()) {
            accountActions.stream().forEach(AccountActions::checkIdHash);
        }
        if (targetCount > getHideOrLogOutCount()) {
            List<Integer> configIds = new ArrayList<>();
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (UserConfig.getInstance(a).isClientActivated()) {
                    configIds.add(a);
                }
            }
            Collections.sort(configIds, (o1, o2) -> {
                long l1 = UserConfig.getInstance(o1).loginTime;
                long l2 = UserConfig.getInstance(o2).loginTime;
                if (l1 > l2) {
                    return 1;
                } else if (l1 < l2) {
                    return -1;
                }
                return 0;
            });
            for (int i = configIds.size() - 1; i >= 0; i--) {
                AccountActions actions = getAccountActions(configIds.get(i));
                if (actions == null) {
                    actions = getOrCreateAccountActions(configIds.get(i));
                    if (!FakePasscodeUtils.isHideAccount(i)) {
                        actions.toggleHideAccountAction();
                        if (targetCount <= getHideOrLogOutCount()) {
                            break;
                        }
                    }
                }
            }
            if (targetCount > getHideOrLogOutCount()) {
                for (int i = configIds.size() - 1; i >= 0; i--) {
                    AccountActions actions = getOrCreateAccountActions(configIds.get(i));
                    if (!FakePasscodeUtils.isHideAccount(i) && actions != null && !actions.isLogOut()) {
                        actions.toggleHideAccountAction();
                        if (targetCount <= getHideOrLogOutCount()) {
                            break;
                        }
                    }
                }
            }
            SharedConfig.saveConfig();
            return true;
        } else {
            return false;
        }
    }

    private void disableHidingForDeactivatedAccounts() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!AccountInstance.getInstance(a).getUserConfig().isClientActivated()) {
                AccountActions accountActions = getAccountActions(a);
                if (accountActions != null && accountActions.isHideAccount()) {
                    accountActions.toggleHideAccountAction();
                }
            }
        }
    }

    private void checkSingleAccountHidden() {
        if (UserConfig.getActivatedAccountsCount(true) == 1 && getHideAccountCount() == 1) {
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (AccountInstance.getInstance(a).getUserConfig().isClientActivated()) {
                    AccountActions accountActions = getAccountActions(a);
                    if (accountActions != null && accountActions.isHideAccount()) {
                        accountActions.toggleHideAccountAction();
                    }
                }
            }
        }
    }
}
