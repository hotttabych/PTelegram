package org.telegram.messenger.fakepasscode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.telegram.messenger.Emoji;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.StickersActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.telegram.messenger.MediaDataController.TYPE_EMOJI;
import static org.telegram.messenger.MediaDataController.TYPE_IMAGE;

public class DeleteStickersAction extends AccountAction implements NotificationCenter.NotificationCenterDelegate {
    @JsonIgnore
    private Set<Integer> loadedStickerTypes = new HashSet<>();
    @JsonIgnore
    private Set<Long> deletedStickerSets = new HashSet<>();

    @Override
    public void execute() {
        loadedStickerTypes.clear();
        deletedStickerSets.clear();
        NotificationCenter.getInstance(accountNum).addObserver(this, NotificationCenter.stickersDidLoad);
        MediaDataController.getInstance(accountNum).loadStickers(TYPE_IMAGE, true, false);
        //delete recent emoji
        Emoji.clearRecentEmoji();
        // delete recent gif
        for (TLRPC.Document document : MediaDataController.getInstance(accountNum).getRecentGifs()) {
            MediaDataController.getInstance(accountNum).removeRecentGif(document);
        }
        /*
        for (int i = 0; i <= TYPE_EMOJI; i++) {
            MediaDataController.getInstance(accountNum).loadStickers(i, true, false);
        }
        */
    }


    @Override
    public synchronized void didReceivedNotification(int id, int account, Object... args) {
        if (account != accountNum) {
            return;
        }
        int type = (int) args[0];
        if (!loadedStickerTypes.contains(type)) {
            loadedStickerTypes.add(type);
            MediaDataController controller = MediaDataController.getInstance(accountNum);
            List<TLRPC.TL_messages_stickerSet> stickerSets = new ArrayList<>(controller.getStickerSets(type));
            for (TLRPC.TL_messages_stickerSet stickerSet : stickerSets) {
                if (!deletedStickerSets.contains(stickerSet.set.id)) {
                    deletedStickerSets.add(stickerSet.set.id);
                    controller.toggleStickerSet(null, stickerSet, 0, null, false, false);
                }
            }
            for (TLRPC.Document document : controller.getRecentStickers(TYPE_IMAGE)) {
                controller.addRecentSticker(TYPE_IMAGE, null, document, 0, true, false);
            }
        }
    }
}
