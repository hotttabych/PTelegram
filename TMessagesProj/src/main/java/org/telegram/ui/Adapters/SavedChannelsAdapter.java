/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SavedChannelCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.SavedChannelsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedChannelsAdapter extends RecyclerListView.SelectionAdapter {

    private final Context mContext;
    private int dialogsCount;
    private long openedDialogId;
    private int currentCount;
    private final ArrayList<Long> selectedDialogs;
    private final int currentAccount;

    private final SavedChannelsActivity parentFragment;

    public SavedChannelsAdapter(SavedChannelsActivity fragment, Context context, ArrayList<Long> selected, int account) {
        mContext = context;
        parentFragment = fragment;
        selectedDialogs = selected;
        currentAccount = account;
    }

    public void setOpenedDialogId(long id) {
        openedDialogId = id;
    }

    public boolean isDataSetChanged() {
        int current = currentCount;
        return current != getItemCount() || current == 1;
    }

    @Override
    public int getItemCount() {
        List<TLRPC.Chat> list = parentFragment.getChatsArray(currentAccount);
        dialogsCount = list.size();
        if (dialogsCount == 0 && !parentFragment.isChatsEndReached()) {
            return (currentCount = 0);
        }
        int count = dialogsCount;
        if (!parentFragment.isChatsEndReached() || dialogsCount == 0) {
            count++;
        }
        if (dialogsCount != 0) {
            count++;
            if (dialogsCount > 10) {
                count++;
            }
        }
        currentCount = count;

        return count;
    }

    public TLObject getItem(int i) {
        List<TLRPC.Chat> list = parentFragment.getChatsArray(currentAccount);
        if (i < 0 || i >= list.size()) {
            return null;
        }
        return list.get(i);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        if (holder.itemView instanceof SavedChannelCell) {
            SavedChannelCell dialogCell = (SavedChannelCell) holder.itemView;
            dialogCell.onReorderStateChanged(false, false);
            dialogCell.setDialogIndex(holder.getAdapterPosition());
            dialogCell.checkCurrentDialogIndex();
            dialogCell.setChecked(selectedDialogs.contains(dialogCell.getChatId()), false);
        }
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        int viewType = holder.getItemViewType();
        return viewType != 1 && viewType != 5 && viewType != 3 && viewType != 8 && viewType != 7 && viewType != 9 && viewType != 10 && viewType != 11;
    }

    @Override
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = new SavedChannelCell(parentFragment, mContext, true, false, currentAccount, null);
                break;
            case 1:
                FlickerLoadingView flickerLoadingView = new FlickerLoadingView(mContext);
                flickerLoadingView.setIsSingleCell(true);
                flickerLoadingView.setViewType(FlickerLoadingView.DIALOG_CELL_TYPE);
                view = flickerLoadingView;
                break;
            case 10: {
                view = new LastEmptyView(mContext);
                break;
            }
            case 11: {
                view = new TextInfoPrivacyCell(mContext);
                Drawable drawable = Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow);
                CombinedDrawable combinedDrawable = new CombinedDrawable(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray)), drawable);
                combinedDrawable.setFullsize(true);
                view.setBackgroundDrawable(combinedDrawable);
                break;
            }
            default: {
                view = new TextCell(mContext);
            }
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, viewType == 5 ? RecyclerView.LayoutParams.MATCH_PARENT : RecyclerView.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int i) {
        if (holder.getItemViewType() == 0) {
            SavedChannelCell cell = (SavedChannelCell) holder.itemView;
            TLRPC.Chat chat = (TLRPC.Chat) getItem(i);
            TLRPC.Chat nextChat = (TLRPC.Chat) getItem(i + 1);
            cell.useSeparator = nextChat != null;
            cell.fullSeparator = false;
            if (AndroidUtilities.isTablet()) {
                cell.setDialogSelected(chat.id == openedDialogId);
            }
            cell.setChecked(selectedDialogs.contains(chat.id), false);
            cell.setChat(chat);
        }
    }

    @Override
    public int getItemViewType(int i) {
        if (dialogsCount > 10 && i == currentCount - 2) {
            return 11;
        }
        int size = parentFragment.getChatsArray(currentAccount).size();
        if (i == size) {
            if (!parentFragment.isChatsEndReached()) {
                return 1;
            } else {
                return 10;
            }
        } else if (i > size) {
            return 10;
        }
        return 0;
    }

    @Override
    public void notifyItemMoved(int fromPosition, int toPosition) {
        List<TLRPC.Chat> dialogs = parentFragment.getChatsArray(currentAccount);
        Collections.swap(dialogs, fromPosition, toPosition);
        super.notifyItemMoved(fromPosition, toPosition);
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public class LastEmptyView extends View {

        public boolean moving;

        public LastEmptyView(Context context) {
            super(context);
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int size = parentFragment.getChatsArray(currentAccount).size();
            View parent = (View) getParent();
            int height;
            int paddingTop = parent.getPaddingTop();
            if (size == 0 || paddingTop == 0) {
                height = 0;
            } else {
                height = MeasureSpec.getSize(heightMeasureSpec);
                if (height == 0) {
                    height = parent.getMeasuredHeight();
                }
                if (height == 0) {
                    height = AndroidUtilities.displaySize.y - ActionBar.getCurrentActionBarHeight() - (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
                }
                int cellHeight = AndroidUtilities.dp(SharedConfig.useThreeLinesLayout ? 78 : 72);
                int dialogsHeight = size * cellHeight + (size - 1);
                int archiveHeight = (0);
                if (dialogsHeight < height) {
                    height = height - dialogsHeight + archiveHeight;
                    height -= AndroidUtilities.statusBarHeight;
                    if (height < 0) {
                        height = 0;
                    }
                } else if (dialogsHeight - height < archiveHeight) {
                    height = archiveHeight - (dialogsHeight - height);
                    height -= AndroidUtilities.statusBarHeight;
                    if (height < 0) {
                        height = 0;
                    }
                } else {
                    height = 0;
                }
            }
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
        }

    }
}
