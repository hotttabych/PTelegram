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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker implements NotificationCenter.NotificationCenterDelegate {
    public static class UpdateData {
        public int major;
        public int minor;
        public int patch;
        public long channelId;
        public int postId;

        public UpdateData(int major, int minor, int patch, long channelId, int postId) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.channelId = channelId;
            this.postId = postId;
        }
    }

    public interface UpdateCheckedDelegate {
        void onUpdateResult(boolean updateFounded, UpdateData data);
    }

    private final long CYBER_PARTISAN_SECURITY_TG_CHANNEL_ID = BuildVars.isAlphaApp() ? -1716369838 : -1164492294;  // For checking for updates
    private final String CYBER_PARTISAN_SECURITY_TG_CHANNEL_USERNAME = BuildVars.isAlphaApp() ? "ptg_update_test" : "cpartisans_security";
    private boolean partisanTgChannelLastMessageLoaded = false;
    private boolean appUpdatesChecked = false;
    private boolean partisanTgChannelUsernameResolved = false;
    private final int currentAccount;
    private final int classGuid;
    private final UpdateCheckedDelegate delegate;

    public UpdateChecker(int currentAccount, UpdateCheckedDelegate delegate) {
        this.currentAccount = currentAccount;
        classGuid = ConnectionsManager.generateClassGuid();
        this.delegate = delegate;
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
        int maxVersionMajor = 0;
        int maxVersionMinor = 0;
        int maxVersionPatch = 0;
        int maxVersionPostId = -1;
        MessageObject maxMessageObject = null;
        Pattern regex = Pattern.compile("PTelegram-v(\\d+)_(\\d+)_(\\d+)(_b)?\\.apk");
        for (MessageObject message : messages) {
            TLRPC.Document doc = message.getDocument();
            if (doc == null) {
                continue;
            }
            for (TLRPC.DocumentAttribute attribute : doc.attributes) {
                if (attribute instanceof TLRPC.TL_documentAttributeFilename) {
                    Matcher matcher = regex.matcher(attribute.file_name);
                    if (matcher.find()) {
                        int major = Integer.parseInt(matcher.group(1));
                        int minor = Integer.parseInt(matcher.group(2));
                        int patch = Integer.parseInt(matcher.group(3));
                        if (versionGreater(major, minor, patch, maxVersionMajor, maxVersionMinor, maxVersionPatch)
                                && (major < 3 || (major == 3 && minor == 0))) {
                            maxVersionMajor = major;
                            maxVersionMinor = minor;
                            maxVersionPatch = patch;
                            maxVersionPostId = message.getId();
                            maxMessageObject = message;
                        }
                    }
                }
            }
        }

        if (versionGreater(maxVersionMajor, maxVersionMinor, maxVersionPatch,
                SharedConfig.maxIgnoredVersionMajor, SharedConfig.maxIgnoredVersionMinor, SharedConfig.maxIgnoredVersionPatch)) {
            Matcher currentVersionMatcher = Pattern.compile("(\\d+).(\\d+).(\\d+)").matcher(BuildVars.PARTISAN_VERSION_STRING);
            if (currentVersionMatcher.find() && currentVersionMatcher.groupCount() == 3) {
                int major = Integer.parseInt(currentVersionMatcher.group(1));
                int minor = Integer.parseInt(currentVersionMatcher.group(2));
                int patch = Integer.parseInt(currentVersionMatcher.group(3));
                if (versionGreater(maxVersionMajor, maxVersionMinor, maxVersionPatch, major, minor, patch)) {
                    UpdateData data = new UpdateData(maxVersionMajor, maxVersionMinor, maxVersionPatch, getUpdateTgChannelId(), maxVersionPostId);
                    delegate.onUpdateResult(true, data);
                    return;
                }
            } else {
                UpdateData data = new UpdateData(maxVersionMajor, maxVersionMinor, maxVersionPatch, getUpdateTgChannelId(), maxVersionPostId);
                delegate.onUpdateResult(true, data);
                return;
            }
        }
        delegate.onUpdateResult(false, null);
    }

    private boolean versionGreater(int major, int minor, int patch, int otherMajor, int otherMinor, int otherPatch) {
        return major > otherMajor || major == otherMajor && minor > otherMinor
                || major == otherMajor && minor == otherMinor && patch > otherPatch;
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
