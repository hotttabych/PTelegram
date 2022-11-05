package org.telegram.messenger.partisan;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

public class UpdateChecker implements NotificationCenter.NotificationCenterDelegate {
    public interface UpdateCheckedDelegate {
        void onUpdateResult(boolean updateFounded, UpdateData data);
    }

    private final long CYBER_PARTISAN_SECURITY_TG_CHANNEL_ID = BuildVars.isAlphaApp() ? -1716369838 : -1164492294;  // For checking for updates
    private final String CYBER_PARTISAN_SECURITY_TG_CHANNEL_USERNAME = BuildVars.isAlphaApp() ? "ptg_update_test" : "cpartisans_security";
    private final String CAN_NOT_SKIP_PREFIX = "IMPORTANT\n";

    private boolean partisanTgChannelLastMessageLoaded = false;
    private boolean appUpdatesChecked = false;
    private boolean partisanTgChannelUsernameResolved = false;
    private int currentAccount;
    private UpdateCheckedDelegate delegate;
    private final int classGuid;

    public UpdateChecker(int currentAccount, UpdateCheckedDelegate delegate) {
        this.currentAccount = currentAccount;
        this.delegate = delegate;
        classGuid = ConnectionsManager.generateClassGuid();
    }

    private UpdateChecker() {
        classGuid = ConnectionsManager.generateClassGuid();
    }

    public static void checkUpdate(int currentAccount, UpdateCheckedDelegate delegate) {
        UpdateChecker checker = new UpdateChecker();
        checker.currentAccount = currentAccount;
        checker.delegate = (updateFounded, data) -> {
            checker.removeObservers();
            delegate.onUpdateResult(updateFounded, data);
        };
        checker.checkUpdate();
    }

    public void checkUpdate() {
        appUpdatesChecked = false;
        getNotificationCenter().addObserver(this, NotificationCenter.messagesDidLoad);
        getNotificationCenter().addObserver(this, NotificationCenter.loadingMessagesFailed);
        getMessagesController().loadMessages(getUpdateTgChannelId(), 0, false, 1, 0, 0, false, 0, classGuid, 2, 0, 0, 0, 0, 1);
    }

    public void removeObservers() {
        if (!appUpdatesChecked) {
            getNotificationCenter().removeObserver(this, NotificationCenter.messagesDidLoad);
            getNotificationCenter().removeObserver(this, NotificationCenter.loadingMessagesFailed);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagesDidLoad) {
            if (SharedConfig.showUpdates && SharedConfig.fakePasscodeActivatedIndex == -1) {
                if ((Long)args[0] == getUpdateTgChannelId()) {
                    if (!partisanTgChannelLastMessageLoaded) {
                        partisanTgChannelLastMessageLoaded = true;
                        getMessagesController().loadMessages(getUpdateTgChannelId(), 0, false, 50, 0, 0, false, 0, classGuid, 2, (int)args[5], 0, 0, 0, 1);
                    } else {
                        appUpdatesChecked = true;
                        getNotificationCenter().removeObserver(this, NotificationCenter.messagesDidLoad);
                        getNotificationCenter().removeObserver(this, NotificationCenter.loadingMessagesFailed);
                        processPartisanTgChannelMessages((ArrayList<MessageObject>)args[2]);
                    }
                }
            }
        } else if (id == NotificationCenter.loadingMessagesFailed) {
            if (args.length > 1 && args[1] instanceof TLRPC.TL_messages_getPeerDialogs) {
                TLRPC.TL_messages_getPeerDialogs oldReq = (TLRPC.TL_messages_getPeerDialogs)args[1];
                TLRPC.InputPeer peer = null;
                if (!oldReq.peers.isEmpty() && oldReq.peers.get(0) instanceof TLRPC.TL_inputDialogPeer) {
                    peer = ((TLRPC.TL_inputDialogPeer)oldReq.peers.get(0)).peer;
                }
                if (!partisanTgChannelUsernameResolved && SharedConfig.showUpdates && SharedConfig.fakePasscodeActivatedIndex == -1
                        && (int)args[0] == classGuid && peer != null
                        && (peer.channel_id == getUpdateTgChannelId() || peer.chat_id == getUpdateTgChannelId()
                        || peer.channel_id == -getUpdateTgChannelId() || peer.chat_id == -getUpdateTgChannelId())) {
                    TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
                    req.username = getUpdateTgChannelUsername();
                    ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                        partisanTgChannelUsernameResolved = true;
                        AndroidUtilities.runOnUIThread(() -> {
                            getNotificationCenter().removeObserver(this, NotificationCenter.loadingMessagesFailed);
                            if (response != null) {
                                TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                                MessagesController.getInstance(currentAccount).putUsers(res.users, false);
                                MessagesController.getInstance(currentAccount).putChats(res.chats, false);
                                MessagesStorage.getInstance(currentAccount).putUsersAndChats(res.users, res.chats, true, true);
                                getMessagesController().loadMessages(getUpdateTgChannelId(), 0, false, 1, 0, 0, false, 0, classGuid, 2, 0, 0, 0, 0, 1);
                            } else {
                                getNotificationCenter().removeObserver(this, NotificationCenter.messagesDidLoad);
                            }
                        });
                    });
                }
            }
        }
    }

    private void processPartisanTgChannelMessages(ArrayList<MessageObject> messages) {
        UpdateData data = new UpdateData();
        Pattern regex = Pattern.compile("PTelegram-v(\\d+)_(\\d+)_(\\d+)(_b)?\\.apk");
        for (MessageObject message : messages) {
            AppVersion version = getAppVersionFromMessage(message, regex);
            if (version != null && (data.version == null || version.greater(data.version))) {
                data.version = version;
                data.postId = message.getId();
                data.document = message.getDocument();
                data.url = "https://google.com";
                if (message.caption != null) {
                    String caption = message.caption.toString();
                    data.canNotSkip = caption.startsWith(CAN_NOT_SKIP_PREFIX);
                    data.text = data.canNotSkip
                            ? caption.substring(CAN_NOT_SKIP_PREFIX.length())
                            : caption;
                }
            }
        }

        if (data.version != null && (AppVersion.getCurrentVersion() == null || data.version.greater(AppVersion.getCurrentVersion()))) {
            delegate.onUpdateResult(true, data);
        } else {
            delegate.onUpdateResult(false, null);
        }
    }

    private static AppVersion getAppVersionFromMessage(MessageObject message, Pattern regex) {
        TLRPC.Document doc = message.getDocument();
        if (doc == null) {
            return null;
        }
        for (TLRPC.DocumentAttribute attribute : doc.attributes) {
            if (attribute instanceof TLRPC.TL_documentAttributeFilename) {
                return AppVersion.parseVersion(attribute.file_name, regex);
            }
        }
        return null;
    }

    private long getUpdateTgChannelId() {
        if (SharedConfig.updateChannelIdOverride != 0) {
            return SharedConfig.updateChannelIdOverride;
        } else {
            return CYBER_PARTISAN_SECURITY_TG_CHANNEL_ID;
        }
    }

    private String getUpdateTgChannelUsername() {
        if (!Objects.equals(SharedConfig.updateChannelUsernameOverride, "")) {
            return SharedConfig.updateChannelUsernameOverride;
        } else {
            return CYBER_PARTISAN_SECURITY_TG_CHANNEL_USERNAME;
        }
    }

    private AccountInstance getAccountInstance() {
        return AccountInstance.getInstance(currentAccount);
    }

    private NotificationCenter getNotificationCenter() {
        return getAccountInstance().getNotificationCenter();
    }

    private MessagesController getMessagesController() {
        return getAccountInstance().getMessagesController();
    }
}
