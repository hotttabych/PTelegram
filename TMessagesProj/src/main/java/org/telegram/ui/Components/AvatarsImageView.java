package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.tgnet.TLObject;

public class AvatarsImageView extends View {

    public final AvatarsDarawable avatarsDarawable;

    public AvatarsImageView(@NonNull Context context, boolean inCall) {
        super(context);
        avatarsDarawable = new AvatarsDarawable(this, inCall);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        avatarsDarawable.width = getMeasuredWidth();
        avatarsDarawable.height = getMeasuredHeight();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarsDarawable.onAttachedToWindow();
    }

    public void setObject(int index, int account, TLObject object) {
        animatingStates[index].id = 0;
        animatingStates[index].participant = null;
        if (object == null) {
            animatingStates[index].imageReceiver.setImageBitmap((Drawable) null);
            invalidate();
            return;
        }
        TLRPC.User currentUser = null;
        TLRPC.Chat currentChat = null;
        animatingStates[index].lastSpeakTime = -1;
        if (object instanceof TLRPC.TL_groupCallParticipant) {
            TLRPC.TL_groupCallParticipant participant = (TLRPC.TL_groupCallParticipant) object;
            animatingStates[index].participant = participant;
            long id = MessageObject.getPeerId(participant.peer);
            if (DialogObject.isUserDialog(id)) {
                currentUser = MessagesController.getInstance(account).getUser(id);
                animatingStates[index].avatarDrawable.setInfo(currentUser);
            } else {
                currentChat = MessagesController.getInstance(account).getChat(-id);
                animatingStates[index].avatarDrawable.setInfo(currentChat, account);
            }
            if (currentStyle == 4) {
                if (id == AccountInstance.getInstance(account).getUserConfig().getClientUserId()) {
                    animatingStates[index].lastSpeakTime = 0;
                } else {
                    if (isInCall) {
                        animatingStates[index].lastSpeakTime = participant.lastActiveDate;
                    } else {
                        animatingStates[index].lastSpeakTime = participant.active_date;
                    }
                }
            } else {
                animatingStates[index].lastSpeakTime = participant.active_date;
            }
            animatingStates[index].id = id;
        } else if (object instanceof TLRPC.User) {
            currentUser = (TLRPC.User) object;
            animatingStates[index].avatarDrawable.setInfo(currentUser);
            animatingStates[index].id = currentUser.id;
        } else {
            currentChat = (TLRPC.Chat) object;
            animatingStates[index].avatarDrawable.setInfo(currentChat, account);
            animatingStates[index].id = -currentChat.id;
        }
        if (currentUser != null) {
            animatingStates[index].imageReceiver.setForUserOrChat(currentUser, animatingStates[index].avatarDrawable);
        } else {
            animatingStates[index].imageReceiver.setForUserOrChat(currentChat, animatingStates[index].avatarDrawable);
        }
        boolean bigAvatars = currentStyle == 4 || currentStyle == STYLE_GROUP_CALL_TOOLTIP;
        animatingStates[index].imageReceiver.setRoundRadius(AndroidUtilities.dp(bigAvatars ? 16 : 12));
        int size = AndroidUtilities.dp(bigAvatars ? 32 : 24);
        animatingStates[index].imageReceiver.setImageCoords(0, 0, size, size);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        avatarsDarawable.onDraw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avatarsDarawable.onDetachedFromWindow();
    }


    public void setStyle(int style) {
        avatarsDarawable.setStyle(style);
    }

    public void setDelegate(Runnable delegate) {
        avatarsDarawable.setDelegate(delegate);
    }

    public void setObject(int a, int currentAccount, TLObject object) {
        avatarsDarawable.setObject(a, currentAccount, object);
    }

    public void reset() {
        avatarsDarawable.reset();
    }

    public void setCount(int usersCount) {
        avatarsDarawable.setCount(usersCount);
    }

    public void commitTransition(boolean animated) {
        avatarsDarawable.commitTransition(animated);
    }

    public void updateAfterTransitionEnd() {
        avatarsDarawable.updateAfterTransitionEnd();
    }

    public void setCentered(boolean centered) {
        avatarsDarawable.setCentered(centered);
    }
}
