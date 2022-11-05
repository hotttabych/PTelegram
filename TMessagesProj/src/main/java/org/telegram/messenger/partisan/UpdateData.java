package org.telegram.messenger.partisan;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class UpdateData {
    public AppVersion version;
    public long channelId;
    public int postId;
    public boolean canNotSkip;
    public String text;
    public TLRPC.Document document;
    public TLRPC.Document sticker;
    public ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
    public String url;
}
