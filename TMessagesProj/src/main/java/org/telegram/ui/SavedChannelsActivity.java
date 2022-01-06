/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Property;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScrollerCustom;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FilesMigrationService;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.XiaomiUtilities;
import org.telegram.messenger.fakepasscode.FakePasscode;
import org.telegram.messenger.fakepasscode.RemoveAsReadMessages;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Adapters.DrawerLayoutAdapter;
import org.telegram.ui.Adapters.SavedChannelsAdapter;
import org.telegram.ui.Cells.AccountSelectCell;
import org.telegram.ui.Cells.ArchiveHintInnerCell;
import org.telegram.ui.Cells.DialogsEmptyCell;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DrawerAddCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.DrawerUserCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HashtagSearchCell;
import org.telegram.ui.Cells.HintDialogCell;
import org.telegram.ui.Cells.LoadingCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.SavedChannelCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.DialogsItemAnimator;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.JoinGroupAlert;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MediaActionDrawable;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.PacmanAnimation;
import org.telegram.ui.Components.PullForegroundDrawable;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RadialProgress2;
import org.telegram.ui.Components.RecyclerAnimationScrollHelper;
import org.telegram.ui.Components.RecyclerItemsEnterAnimator;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.StickersAlert;
import org.telegram.ui.Components.UndoView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SavedChannelsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private FrameLayout viewPage;

    private DialogsRecyclerView listView;
    private LinearLayoutManager layoutManager;
    private SavedChannelsAdapter dialogsAdapter;
    private ItemTouchHelper itemTouchhelper;
    private SwipeController swipeController;
    private PullForegroundDrawable pullForegroundDrawable;
    private RecyclerAnimationScrollHelper scrollHelper;
    private int dialogsType;
    private int archivePullViewState;
    private FlickerLoadingView progressView;
    private int lastItemsCount;
    private DialogsItemAnimator dialogsItemAnimator;
    private RecyclerItemsEnterAnimator recyclerItemsEnterAnimator;

    public boolean isDefaultDialogType() {
        return dialogsType == 0 || dialogsType == 7 || dialogsType == 8;
    }

    private ActionBarMenuItem doneItem;
    private RLottieImageView floatingButton;
    private FrameLayout floatingButtonContainer;
    private ChatAvatarContainer avatarContainer;
    private UndoView[] undoView = new UndoView[2];
    private boolean askingForPermissions;

    private View blurredView;

    private int initialDialogsType;

    private boolean checkingImportDialog;

    private int messagesCount;
    private int hasPoll;
    private boolean hasInvoice;

    private PacmanAnimation pacmanAnimation;

    private SavedChannelCell slidingView;
    private SavedChannelCell movingView;
    private boolean allowMoving;
    private boolean movingWas;
    private boolean waitingForScrollFinished;
    private boolean updatePullAfterScroll;

    private Paint actionBarDefaultPaint = new Paint();

    private NumberTextView selectedDialogsCountTextView;
    private ArrayList<View> actionModeViews = new ArrayList<>();
    private ActionBarMenuItem deleteItem;

    private float additionalFloatingTranslation;
    private float additionalFloatingTranslation2;
    private float floatingButtonTranslation;
    private float floatingButtonHideProgress;

    private RecyclerView sideMenu;
    private ChatActivityEnterView commentView;
    private ActionBarMenuItem switchItem;

    private FragmentContextView fragmentLocationContextView;
    private FragmentContextView fragmentContextView;

    private ArrayList<TLRPC.Chat> frozenChatsList;
    private boolean dialogsListFrozen;
    private int dialogRemoveFinished;
    private int dialogInsertFinished;
    private int dialogChangeFinished;

    private AlertDialog permissionDialog;
    private boolean askAboutContacts = true;

    private int prevPosition;
    private int prevTop;
    private boolean scrollUpdated;
    private boolean floatingHidden;
    private boolean floatingForceVisible;
    private final AccelerateDecelerateInterpolator floatingInterpolator = new AccelerateDecelerateInterpolator();

    private boolean checkPermission = true;

    private int currentConnectionState;

    private String selectAlertString;
    private String selectAlertStringGroup;
    private String addToGroupAlertString;
    private boolean resetDelegate = true;

    public static boolean[] dialogsLoaded = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private boolean onlySelect;
    private long openedDialogId;
    private boolean cantSendToChannels;
    private boolean allowSwitchAccount;
    private boolean checkCanWrite;
    private boolean afterSignup;
    private boolean showSetPasswordConfirm;

    private FrameLayout updateLayout;
    private AnimatorSet updateLayoutAnimator;
    private RadialProgress2 updateLayoutIcon;
    private TextView updateTextView;

    private SavedChannelsActivityDelegate delegate;

    private ArrayList<Long> selectedDialogs = new ArrayList<>();

    private boolean canDeletePsaSelected;

    private int topPadding;

    private final static int delete = 102;

    private final static int ARCHIVE_ITEM_STATE_PINNED = 0;
    private final static int ARCHIVE_ITEM_STATE_SHOWED = 1;
    private final static int ARCHIVE_ITEM_STATE_HIDDEN = 2;

    private long startArchivePullingTime;
    private boolean scrollingManually;
    private boolean canShowHiddenArchive;

    private int animationIndex = -1;
    private float progressToActionMode;
    private ValueAnimator actionBarColorAnimator;

    private float tabsYOffset;
    private float scrollAdditionalOffset;

    private int debugLastUpdateAction = -1;

    public BaseFragment passwordFragment = null;

    private final long PARTISAN_TG_CHANNEL_ID = -1164492294;
    private boolean partisanTgChannelLastMessageLoaded = false;
    private boolean appUpdatesChecked = false;

    ArrayList<TLRPC.Chat> chats = new ArrayList<>();
    private boolean chatLoading = false;

    public final Property<SavedChannelsActivity, Float> SCROLL_Y = new AnimationProperties.FloatProperty<SavedChannelsActivity>("animationValue") {
        @Override
        public void setValue(SavedChannelsActivity object, float value) {
            object.setScrollY(value);
        }

        @Override
        public Float get(SavedChannelsActivity object) {
            return actionBar.getTranslationY();
        }
    };

    private class ContentView extends SizeNotifierFrameLayout {

        private Paint actionBarSearchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint windowBackgroundPaint = new Paint();
        private int inputFieldHeight;

        public ContentView(Context context) {
            super(context);
        }

        @Override
        public void setPadding(int left, int top, int right, int bottom) {
            topPadding = top;
            updateContextViewPosition();
            requestLayout();
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            if (child == fragmentContextView && fragmentContextView.isCallStyle()) {
                return true;
            }
            if (child == blurredView) {
                return true;
            }
            boolean result;
            if (child == viewPage || child == fragmentContextView || child == fragmentLocationContextView) {
                canvas.save();
                canvas.clipRect(0, -getY() + actionBar.getY() + actionBar.getHeight(), getMeasuredWidth(), getMeasuredHeight());
                if (slideFragmentProgress != 1f) {
                    float s = 1f - 0.05f * (1f - slideFragmentProgress);
                    canvas.translate((isDrawerTransition ? AndroidUtilities.dp(4) : -AndroidUtilities.dp(4)) * (1f - slideFragmentProgress), 0);
                    canvas.scale(s, s, isDrawerTransition ? getMeasuredWidth() : 0, -getY() + actionBar.getY() + actionBar.getHeight());
                }

                result = super.drawChild(canvas, child, drawingTime);
                canvas.restore();
            } else if (child == actionBar && slideFragmentProgress != 1f) {
                canvas.save();
                float s = 1f - 0.05f * (1f - slideFragmentProgress);
                canvas.translate((isDrawerTransition ? AndroidUtilities.dp(4) : -AndroidUtilities.dp(4)) * (1f - slideFragmentProgress), 0);
                canvas.scale(s, s, isDrawerTransition ? getMeasuredWidth() : 0, (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2f);
                result = super.drawChild(canvas, child, drawingTime);
                canvas.restore();
            } else {
                result = super.drawChild(canvas, child, drawingTime);
            }
            if (child == actionBar && parentLayout != null) {
                int y = (int) (actionBar.getY() + actionBar.getHeight());
                parentLayout.drawHeaderShadow(canvas, 255, y);
            }
            return result;
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            int actionBarHeight = actionBar.getHeight();
            int top;
            if (inPreviewMode) {
                top = AndroidUtilities.statusBarHeight;
            } else {
                top = (int) (-getY() + actionBar.getY());
            }
            if (!inPreviewMode) {
                if (progressToActionMode > 0) {
                    actionBarSearchPaint.setColor(ColorUtils.blendARGB(Theme.getColor(Theme.key_actionBarDefault), Theme.getColor(Theme.key_windowBackgroundWhite), progressToActionMode));
                    canvas.drawRect(0, top, getMeasuredWidth(), top + actionBarHeight, actionBarSearchPaint);
                } else {
                    canvas.drawRect(0, top, getMeasuredWidth(), top + actionBarHeight, actionBarDefaultPaint);
                }
            }
            tabsYOffset = 0;
            updateContextViewPosition();
            super.dispatchDraw(canvas);
            if (fragmentContextView != null && fragmentContextView.isCallStyle()) {
                canvas.save();
                canvas.translate(fragmentContextView.getX(), fragmentContextView.getY());
                if (slideFragmentProgress != 1f) {
                    float s = 1f - 0.05f * (1f - slideFragmentProgress);
                    canvas.translate((isDrawerTransition ? AndroidUtilities.dp(4) : -AndroidUtilities.dp(4)) * (1f - slideFragmentProgress), 0);
                    canvas.scale(s, 1f, isDrawerTransition ? getMeasuredWidth() : 0, fragmentContextView.getY());
                }
                fragmentContextView.setDrawOverlay(true);
                fragmentContextView.draw(canvas);
                fragmentContextView.setDrawOverlay(false);
                canvas.restore();
            }
            if (blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
                if (blurredView.getAlpha() != 1f) {
                    if (blurredView.getAlpha() != 0) {
                        canvas.saveLayerAlpha(blurredView.getLeft(), blurredView.getTop(), blurredView.getRight(), blurredView.getBottom(), (int) (255 * blurredView.getAlpha()), Canvas.ALL_SAVE_FLAG);
                        canvas.translate(blurredView.getLeft(), blurredView.getTop());
                        blurredView.draw(canvas);
                        canvas.restore();
                    }
                } else {
                    blurredView.draw(canvas);
                }
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

            setMeasuredDimension(widthSize, heightSize);
            heightSize -= getPaddingTop();

            if (doneItem != null) {
                LayoutParams layoutParams = (LayoutParams) doneItem.getLayoutParams();
                layoutParams.topMargin = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
                layoutParams.height = ActionBar.getCurrentActionBarHeight();
            }

            measureChildWithMargins(actionBar, widthMeasureSpec, 0, heightMeasureSpec, 0);

            int keyboardSize = measureKeyboardHeight();
            int childCount = getChildCount();

            if (commentView != null) {
                measureChildWithMargins(commentView, widthMeasureSpec, 0, heightMeasureSpec, 0);
                Object tag = commentView.getTag();
                if (tag != null && tag.equals(2)) {
                    if (keyboardSize <= AndroidUtilities.dp(20) && !AndroidUtilities.isInMultiwindow) {
                        heightSize -= commentView.getEmojiPadding();
                    }
                    inputFieldHeight = commentView.getMeasuredHeight();
                } else {
                    inputFieldHeight = 0;
                }

                if (SharedConfig.smoothKeyboard && commentView.isPopupShowing()) {
                    fragmentView.setTranslationY(0);
                    if (viewPage != null) {
                        viewPage.setTranslationY(0);
                    }
                    if (!onlySelect) {
                        actionBar.setTranslationY(0);
                    }
                }
            }

            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child == null || child.getVisibility() == GONE || child == commentView || child == actionBar) {
                    continue;
                }
                if (child instanceof DatabaseMigrationHint) {
                    int contentWidthSpec = View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY);
                    int h = View.MeasureSpec.getSize(heightMeasureSpec) + keyboardSize;
                    int contentHeightSpec = View.MeasureSpec.makeMeasureSpec(Math.max(AndroidUtilities.dp(10), h - inputFieldHeight + AndroidUtilities.dp(2) - actionBar.getMeasuredHeight()), View.MeasureSpec.EXACTLY);
                    child.measure(contentWidthSpec, contentHeightSpec);
                } else if (child == viewPage) {
                    int contentWidthSpec = View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY);
                    int h = heightSize - inputFieldHeight + AndroidUtilities.dp(2) - (onlySelect ? 0 : actionBar.getMeasuredHeight()) - topPadding;

                    child.setTranslationY(0);
                    int transitionPadding = (isSlideBackTransition || isDrawerTransition) ? (int) (h * 0.05f) : 0;
                    h += transitionPadding;
                    child.setPadding(child.getPaddingLeft(), child.getPaddingTop(), child.getPaddingRight(), transitionPadding);
                    child.measure(contentWidthSpec, View.MeasureSpec.makeMeasureSpec(Math.max(AndroidUtilities.dp(10), h), View.MeasureSpec.EXACTLY));
                    child.setPivotX(child.getMeasuredWidth() / 2);
                } else if (commentView != null && commentView.isPopupView(child)) {
                    if (AndroidUtilities.isInMultiwindow) {
                        if (AndroidUtilities.isTablet()) {
                            child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(Math.min(AndroidUtilities.dp(320), heightSize - inputFieldHeight - AndroidUtilities.statusBarHeight + getPaddingTop()), View.MeasureSpec.EXACTLY));
                        } else {
                            child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(heightSize - inputFieldHeight - AndroidUtilities.statusBarHeight + getPaddingTop(), View.MeasureSpec.EXACTLY));
                        }
                    } else {
                        child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, View.MeasureSpec.EXACTLY));
                    }
                } else {
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            final int count = getChildCount();

            int paddingBottom;
            Object tag = commentView != null ? commentView.getTag() : null;
            int keyboardSize = measureKeyboardHeight();
            if (tag != null && tag.equals(2)) {
                paddingBottom = keyboardSize <= AndroidUtilities.dp(20) && !AndroidUtilities.isInMultiwindow ? commentView.getEmojiPadding() : 0;
            } else {
                paddingBottom = 0;
            }
            setBottomClip(paddingBottom);

            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = Gravity.TOP | Gravity.LEFT;
                }

                final int absoluteGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = (r - l - width) / 2 + lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        childLeft = r - width - lp.rightMargin;
                        break;
                    case Gravity.LEFT:
                    default:
                        childLeft = lp.leftMargin;
                }

                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = lp.topMargin + getPaddingTop();
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = ((b - paddingBottom) - t - height) / 2 + lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = ((b - paddingBottom) - t) - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = lp.topMargin;
                }

                if (commentView != null && commentView.isPopupView(child)) {
                    if (AndroidUtilities.isInMultiwindow) {
                        childTop = commentView.getTop() - child.getMeasuredHeight() + AndroidUtilities.dp(1);
                    } else {
                        childTop = commentView.getBottom();
                    }
                } else if (child instanceof DatabaseMigrationHint) {
                    childTop = actionBar.getMeasuredHeight();
                } else if (child == viewPage) {
                    if (!onlySelect) {
                        childTop = actionBar.getMeasuredHeight();
                    }
                    childTop += topPadding;
                } else if (child instanceof FragmentContextView) {
                    childTop += actionBar.getMeasuredHeight();
                }
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }

            notifyHeightChanged();
            updateContextViewPosition();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (actionBar.isActionModeShowed()) {
                    allowMoving = true;
                }
            }
            return onTouchEvent(ev);
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }

    }

    public static float viewOffset = 0.0f;

    public class DialogsRecyclerView extends RecyclerListView {

        private boolean firstLayout = true;
        private boolean ignoreLayout;
        private int lastListPadding;

        Paint paint = new Paint();
        RectF rectF = new RectF();

        public DialogsRecyclerView(Context context) {
            super(context);
        }

        @Override
        protected boolean updateEmptyViewAnimated() {
            return true;
        }

        public void setViewsOffset(float viewOffset) {
            SavedChannelsActivity.viewOffset = viewOffset;
            int n = getChildCount();
            for (int i = 0; i < n; i++) {
                getChildAt(i).setTranslationY(viewOffset);
            }

            if (selectorPosition != NO_POSITION) {
                View v = getLayoutManager().findViewByPosition(selectorPosition);
                if (v != null) {
                    selectorRect.set(v.getLeft(), (int) (v.getTop() + viewOffset), v.getRight(), (int) (v.getBottom() + viewOffset));
                    selectorDrawable.setBounds(selectorRect);
                }
            }
            invalidate();
        }

        public float getViewOffset() {
            return viewOffset;
        }

        @Override
        public void addView(View child, int index, ViewGroup.LayoutParams params) {
            super.addView(child, index, params);
            child.setTranslationY(viewOffset);
            child.setTranslationX(0);
            child.setAlpha(1f);
        }

        @Override
        public void removeView(View view) {
            super.removeView(view);
            view.setTranslationY(0);
            view.setTranslationX(0);
            view.setAlpha(1f);
        }

        @Override
        public void onDraw(Canvas canvas) {
            if (pullForegroundDrawable != null && viewOffset != 0) {
                int pTop = getPaddingTop();
                if (pTop != 0) {
                    canvas.save();
                    canvas.translate(0, pTop);
                }
                pullForegroundDrawable.drawOverScroll(canvas);
                if (pTop != 0) {
                    canvas.restore();
                }
            }
            super.onDraw(canvas);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (drawMovingViewsOverlayed()) {
                paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                for (int i = 0; i < getChildCount(); i++) {
                    View view = getChildAt(i);

                    if ((view instanceof SavedChannelCell && ((SavedChannelCell) view).isMoving()) || (view instanceof SavedChannelsAdapter.LastEmptyView && ((SavedChannelsAdapter.LastEmptyView) view).moving)) {
                        if (view.getAlpha() != 1f) {
                            rectF.set(view.getX(), view.getY(), view.getX() + view.getMeasuredWidth(), view.getY() +  view.getMeasuredHeight());
                            canvas.saveLayerAlpha(rectF, (int) (255 * view.getAlpha()), Canvas.ALL_SAVE_FLAG);
                        } else {
                            canvas.save();
                        }
                        canvas.translate(view.getX(), view.getY());
                        canvas.drawRect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight(), paint);
                        view.draw(canvas);
                        canvas.restore();
                    }
                }
                invalidate();
            }
            if (slidingView != null && pacmanAnimation != null) {
                pacmanAnimation.draw(canvas, slidingView.getTop() + slidingView.getMeasuredHeight() / 2);
            }
        }

        private boolean drawMovingViewsOverlayed() {
            return getItemAnimator() != null && getItemAnimator().isRunning() && (dialogRemoveFinished != 0 || dialogInsertFinished != 0 || dialogChangeFinished != 0);
        }

        @Override
        public boolean drawChild(Canvas canvas, View child, long drawingTime) {
            if (drawMovingViewsOverlayed() && child instanceof SavedChannelCell && ((SavedChannelCell) child).isMoving()) {
                return true;
            }
            return super.drawChild(canvas, child, drawingTime);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
        }

        @Override
        public void setAdapter(RecyclerView.Adapter adapter) {
            super.setAdapter(adapter);
            firstLayout = true;
        }

        private void checkIfAdapterValid() {
            RecyclerView.Adapter adapter = getAdapter();
            if (lastItemsCount != adapter.getItemCount() && !dialogsListFrozen) {
                ignoreLayout = true;
                adapter.notifyDataSetChanged();
                ignoreLayout = false;
            }
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int t = 0;
            if (!onlySelect) {
                t = actionBar.getMeasuredHeight();
            }

            int pos = layoutManager.findFirstVisibleItemPosition();
            if (pos != RecyclerView.NO_POSITION && !dialogsListFrozen && itemTouchhelper.isIdle()) {
                RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(pos);
                if (holder != null) {
                    int top = holder.itemView.getTop();

                    ignoreLayout = true;
                    layoutManager.scrollToPositionWithOffset(pos, (int) (top - lastListPadding + scrollAdditionalOffset));
                    ignoreLayout = false;
                }
            }
            if (!onlySelect) {
                ignoreLayout = true;
                t = inPreviewMode && Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0;
                setTopGlowOffset(t);
                setPadding(0, t, 0, 0);
                progressView.setPaddingTop(t);
                ignoreLayout = false;
            }

            if (firstLayout && getMessagesController().dialogsLoaded) {
                if (dialogsType == 0 && hasHiddenArchive()) {
                    ignoreLayout = true;
                    LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                    layoutManager.scrollToPositionWithOffset(1, (int) actionBar.getTranslationY());
                    ignoreLayout = false;
                }
                firstLayout = false;
            }
            checkIfAdapterValid();
            super.onMeasure(widthSpec, heightSpec);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            lastListPadding = getPaddingTop();
            scrollAdditionalOffset = 0;

            if ((dialogRemoveFinished != 0 || dialogInsertFinished != 0 || dialogChangeFinished != 0) && !dialogsItemAnimator.isRunning()) {
                onDialogAnimationFinished();
            }
        }

        @Override
        public void requestLayout() {
            if (ignoreLayout) {
                return;
            }
            super.requestLayout();
        }

        private void toggleArchiveHidden(boolean action, SavedChannelCell dialogCell) {
            SharedConfig.toggleArchiveHidden();
            if (SharedConfig.archiveHidden) {
                if (dialogCell != null) {
                    waitingForScrollFinished = true;
                    smoothScrollBy(0, (dialogCell.getMeasuredHeight() + (dialogCell.getTop() - getPaddingTop())), CubicBezierInterpolator.EASE_OUT);
                    if (action) {
                        updatePullAfterScroll = true;
                    } else {
                        updatePullState();
                    }
                }
                getUndoView().showWithAction(0, UndoView.ACTION_ARCHIVE_HIDDEN, null, null);
            } else {
                getUndoView().showWithAction(0, UndoView.ACTION_ARCHIVE_PINNED, null, null);
                updatePullState();
                if (action && dialogCell != null) {
                    dialogCell.resetPinnedArchiveState();
                    dialogCell.invalidate();
                }
            }
        }

        private void updatePullState() {
            archivePullViewState = SharedConfig.archiveHidden ? ARCHIVE_ITEM_STATE_HIDDEN : ARCHIVE_ITEM_STATE_PINNED;
            if (pullForegroundDrawable != null) {
                pullForegroundDrawable.setWillDraw(archivePullViewState != ARCHIVE_ITEM_STATE_PINNED);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (fastScrollAnimationRunning || waitingForScrollFinished || dialogRemoveFinished != 0 || dialogInsertFinished != 0 || dialogChangeFinished != 0) {
                return false;
            }
            int action = e.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                setOverScrollMode(View.OVER_SCROLL_ALWAYS);
            }
            boolean result = super.onTouchEvent(e);
            if (dialogsType == 0 && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN && hasHiddenArchive()) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                int currentPosition = layoutManager.findFirstVisibleItemPosition();
                if (currentPosition == 0) {
                    int pTop = getPaddingTop();
                    View view = layoutManager.findViewByPosition(currentPosition);
                    int height = (int) (AndroidUtilities.dp(SharedConfig.useThreeLinesLayout ? 78 : 72) * PullForegroundDrawable.SNAP_HEIGHT);
                    int diff = (view.getTop() - pTop) + view.getMeasuredHeight();
                    if (view != null) {
                        long pullingTime = System.currentTimeMillis() - startArchivePullingTime;
                        if (diff < height || pullingTime < PullForegroundDrawable.minPullingTime) {
                            smoothScrollBy(0, diff, CubicBezierInterpolator.EASE_OUT_QUINT);
                            archivePullViewState = ARCHIVE_ITEM_STATE_HIDDEN;
                        } else {
                            if (archivePullViewState != ARCHIVE_ITEM_STATE_SHOWED) {
                                if (getViewOffset() == 0) {
                                    smoothScrollBy(0, (view.getTop() - pTop), CubicBezierInterpolator.EASE_OUT_QUINT);
                                }
                                if (!canShowHiddenArchive) {
                                    canShowHiddenArchive = true;
                                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                                    if (pullForegroundDrawable != null) {
                                        pullForegroundDrawable.colorize(true);
                                    }
                                }
                                ((SavedChannelCell) view).startOutAnimation();
                                archivePullViewState = ARCHIVE_ITEM_STATE_SHOWED;
                            }
                        }

                        if (getViewOffset() != 0) {
                            ValueAnimator valueAnimator = ValueAnimator.ofFloat(getViewOffset(), 0f);
                            valueAnimator.addUpdateListener(animation -> setViewsOffset((float) animation.getAnimatedValue()));

                            valueAnimator.setDuration(Math.max(100, (long) (350f - 120f * (getViewOffset() / PullForegroundDrawable.getMaxOverscroll()))));
                            valueAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
                            setScrollEnabled(false);
                            valueAnimator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    setScrollEnabled(true);
                                }
                            });
                            valueAnimator.start();
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
            if (fastScrollAnimationRunning || waitingForScrollFinished || dialogRemoveFinished != 0 || dialogInsertFinished != 0 || dialogChangeFinished != 0) {
                return false;
            }
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                checkIfAdapterValid();
            }
            return super.onInterceptTouchEvent(e);
        }
    }

    private class SwipeController extends ItemTouchHelper.Callback {
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (waitingForDialogsAnimationEnd() || parentLayout != null && parentLayout.isInPreviewMode()) {
                return 0;
            }
            if (!onlySelect && isDefaultDialogType() && slidingView == null && viewHolder.itemView instanceof SavedChannelCell) {
                SavedChannelCell dialogCell = (SavedChannelCell) viewHolder.itemView;
                long dialogId = dialogCell.getDialogId();
                if (actionBar.isActionModeShowed(null)) {
                    TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
                    if (!allowMoving || dialog == null || DialogObject.isFolderDialogId(dialogId)) {
                        return 0;
                    }
                    movingView = (SavedChannelCell) viewHolder.itemView;
                    movingView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
                } else {
                    dialogCell.setSliding(true);
                    return makeMovementFlags(0, ItemTouchHelper.LEFT);
                }
            }
            return 0;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            if (!(target.itemView instanceof SavedChannelCell)) {
                return false;
            }
            SavedChannelCell dialogCell = (SavedChannelCell) target.itemView;
            long dialogId = dialogCell.getDialogId();
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
            if (dialog == null || DialogObject.isFolderDialogId(dialogId)) {
                return false;
            }
            int fromIndex = source.getAdapterPosition();
            int toIndex = target.getAdapterPosition();
            dialogsAdapter.notifyItemMoved(fromIndex, toIndex);
            updateDialogIndices();
            if (dialogsType == 7 || dialogsType == 8) {
                MessagesController.DialogFilter filter = getMessagesController().selectedDialogFilter[dialogsType == 8 ? 1 : 0];
            } else {
                movingWas = true;
            }
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            if (viewHolder != null) {
                SavedChannelCell dialogCell = (SavedChannelCell) viewHolder.itemView;
                long dialogId = dialogCell.getDialogId();
                if (DialogObject.isFolderDialogId(dialogId)) {
                    listView.toggleArchiveHidden(false, dialogCell);
                    return;
                }
                TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
                if (dialog == null) {
                    return;
                }

                slidingView = dialogCell;
                int position = viewHolder.getAdapterPosition();
                int count = dialogsAdapter.getItemCount();
                Runnable finishRunnable = () -> {
                    if (frozenChatsList == null) {
                        return;
                    }
                    frozenChatsList.remove(dialog);
                    int pinnedNum = dialog.pinnedNum;
                    slidingView = null;
                    listView.invalidate();
                    int lastItemPosition = layoutManager.findLastVisibleItemPosition();
                    if (lastItemPosition == count - 1) {
                        layoutManager.findViewByPosition(lastItemPosition).requestLayout();
                    }
                    if (getMessagesController().isPromoDialog(dialog.id, false)) {
                        getMessagesController().hidePromoDialog();
                        dialogsItemAnimator.prepareForRemove();
                        lastItemsCount--;
                        dialogsAdapter.notifyItemRemoved(position);
                        dialogRemoveFinished = 2;
                    } else {
                        int added = getMessagesController().addDialogToFolder(dialog.id, 1, -1, 0);
                        if (added != 2 || position != 0) {
                            dialogsItemAnimator.prepareForRemove();
                            lastItemsCount--;
                            dialogsAdapter.notifyItemRemoved(position);
                            dialogRemoveFinished = 2;
                        }
                        if (added == 2) {
                            dialogsItemAnimator.prepareForRemove();
                            if (position == 0) {
                                dialogChangeFinished = 2;
                                setDialogsListFrozen(true);
                                dialogsAdapter.notifyItemChanged(0);
                            } else {
                                lastItemsCount++;
                                dialogsAdapter.notifyItemInserted(0);
                                if (!SharedConfig.archiveHidden && layoutManager.findFirstVisibleItemPosition() == 0) {
                                    listView.smoothScrollBy(0, -AndroidUtilities.dp(SharedConfig.useThreeLinesLayout ? 78 : 72));
                                }
                            }
                            ArrayList<TLRPC.Chat> chats = getChatsArray(currentAccount, dialogsType, 0, false);
                            frozenChatsList.add(0, chats.get(0));
                        } else if (added == 1) {
                            RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(0);
                            if (holder != null && holder.itemView instanceof SavedChannelCell) {
                                SavedChannelCell cell = (SavedChannelCell) holder.itemView;
                                cell.checkCurrentDialogIndex(true);
                                cell.animateArchiveAvatar();
                            }
                        }
                        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                        boolean hintShowed = preferences.getBoolean("archivehint_l", false) || SharedConfig.archiveHidden;
                        if (!hintShowed) {
                            preferences.edit().putBoolean("archivehint_l", true).commit();
                        }
                        getUndoView().showWithAction(dialog.id, hintShowed ? UndoView.ACTION_ARCHIVE : UndoView.ACTION_ARCHIVE_HINT, null, () -> {
                                dialogsListFrozen = true;
                                getMessagesController().addDialogToFolder(dialog.id, 0, pinnedNum, 0);
                                dialogsListFrozen = false;
                                ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(0);
                                int index = dialogs.indexOf(dialog);
                                if (index >= 0) {
                                    ArrayList<TLRPC.Dialog> archivedDialogs = getMessagesController().getDialogs(1);
                                    if (!archivedDialogs.isEmpty() || index != 1) {
                                        dialogInsertFinished = 2;
                                        setDialogsListFrozen(true);
                                        dialogsItemAnimator.prepareForRemove();
                                        lastItemsCount++;
                                        dialogsAdapter.notifyItemInserted(index);
                                    }
                                    if (archivedDialogs.isEmpty()) {
                                        dialogs.remove(0);
                                        if (index == 1) {
                                            dialogChangeFinished = 2;
                                            setDialogsListFrozen(true);
                                            dialogsAdapter.notifyItemChanged(0);
                                        } else {
                                            frozenChatsList.remove(0);
                                            dialogsItemAnimator.prepareForRemove();
                                            lastItemsCount--;
                                            dialogsAdapter.notifyItemRemoved(0);
                                        }
                                    }
                                } else {
                                    dialogsAdapter.notifyDataSetChanged();
                                }
                            });
                    }
                };
                setDialogsListFrozen(true);
                if (Utilities.random.nextInt(1000) == 1) {
                    if (pacmanAnimation == null) {
                        pacmanAnimation = new PacmanAnimation(listView);
                    }
                    pacmanAnimation.setFinishRunnable(finishRunnable);
                    pacmanAnimation.start();
                } else {
                    finishRunnable.run();
                }
            } else {
                slidingView = null;
            }
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder != null) {
                listView.hideSelector(false);
            }
            if (viewHolder != null && viewHolder.itemView instanceof SavedChannelCell) {
                ((SavedChannelCell) viewHolder.itemView).swipeCanceled = false;
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public long getAnimationDuration(RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
            if (animationType == ItemTouchHelper.ANIMATION_TYPE_SWIPE_CANCEL) {
                return 200;
            } else if (animationType == ItemTouchHelper.ANIMATION_TYPE_DRAG) {
                if (movingView != null) {
                    View view = movingView;
                    AndroidUtilities.runOnUIThread(() -> view.setBackgroundDrawable(null), dialogsItemAnimator.getMoveDuration());
                    movingView = null;
                }
            }
            return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
        }

        @Override
        public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
            return 0.45f;
        }

        @Override
        public float getSwipeEscapeVelocity(float defaultValue) {
            return 3500;
        }

        @Override
        public float getSwipeVelocityThreshold(float defaultValue) {
            return Float.MAX_VALUE;
        }
    }

    public interface SavedChannelsActivityDelegate {
        void didSelectDialogs(SavedChannelsActivity fragment, ArrayList<Long> dids, CharSequence message, boolean param);
    }

    public SavedChannelsActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        if (getArguments() != null) {
            onlySelect = arguments.getBoolean("onlySelect", false);
            cantSendToChannels = arguments.getBoolean("cantSendToChannels", false);
            initialDialogsType = arguments.getInt("dialogsType", 0);
            selectAlertString = arguments.getString("selectAlertString");
            selectAlertStringGroup = arguments.getString("selectAlertStringGroup");
            addToGroupAlertString = arguments.getString("addToGroupAlertString");
            allowSwitchAccount = arguments.getBoolean("allowSwitchAccount");
            checkCanWrite = arguments.getBoolean("checkCanWrite", true);
            afterSignup = arguments.getBoolean("afterSignup", false);
            resetDelegate = arguments.getBoolean("resetDelegate", true);
            messagesCount = arguments.getInt("messagesCount", 0);
            hasPoll = arguments.getInt("hasPoll", 0);
            hasInvoice = arguments.getBoolean("hasInvoice", false);
            showSetPasswordConfirm = arguments.getBoolean("showSetPasswordConfirm", showSetPasswordConfirm);
        }

        if (initialDialogsType == 0) {
            askAboutContacts = MessagesController.getGlobalNotificationsSettings().getBoolean("askAboutContacts", true);
            SharedConfig.loadProxyList();
        }

        currentConnectionState = getConnectionsManager().getConnectionState();

        getNotificationCenter().addObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);
        if (!onlySelect) {
            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.closeSearchByActiveAction);
        }
        getNotificationCenter().addObserver(this, NotificationCenter.updateInterfaces);
        getNotificationCenter().addObserver(this, NotificationCenter.encryptedChatUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.contactsDidLoad);
        getNotificationCenter().addObserver(this, NotificationCenter.appDidLogout);
        getNotificationCenter().addObserver(this, NotificationCenter.openedChatChanged);
        getNotificationCenter().addObserver(this, NotificationCenter.notificationsSettingsUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.messageReceivedByAck);
        getNotificationCenter().addObserver(this, NotificationCenter.messageReceivedByServer);
        getNotificationCenter().addObserver(this, NotificationCenter.messageSendError);
        getNotificationCenter().addObserver(this, NotificationCenter.replyMessagesDidLoad);
        getNotificationCenter().addObserver(this, NotificationCenter.didUpdateConnectionState);
        getNotificationCenter().addObserver(this, NotificationCenter.needDeleteDialog);
        getNotificationCenter().addObserver(this, NotificationCenter.newSuggestionsAvailable);
        if (SharedConfig.showUpdates) {
            getNotificationCenter().addObserver(this, NotificationCenter.messagesDidLoad);
        }

        getNotificationCenter().addObserver(this, NotificationCenter.onDatabaseMigration);
        getNotificationCenter().addObserver(this, NotificationCenter.didClearDatabase);

        loadDialogs(getAccountInstance());
        getMessagesController().loadPinnedDialogs(0, 0, null);
        if (databaseMigrationHint != null && !getMessagesStorage().isDatabaseMigrationInProgress()) {
            View localView = databaseMigrationHint;
            if (localView.getParent() != null) {
                ((ViewGroup) localView.getParent()).removeView(localView);
            }
            databaseMigrationHint = null;
        }
        FakePasscode.checkPendingRemovalChats();
        return true;
    }

    public static void loadDialogs(AccountInstance accountInstance) {
        int currentAccount = accountInstance.getCurrentAccount();
        if (!dialogsLoaded[currentAccount]) {
            MessagesController messagesController = accountInstance.getMessagesController();
            messagesController.loadGlobalNotificationsSettings();
            for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                if (!dialogsLoaded[i]) {
                    MessagesController controller = AccountInstance.getInstance(i).getMessagesController();
                    controller.loadDialogs(0, 0, 100, true);
                }
            }
            messagesController.loadHintDialogs();
            messagesController.loadUserInfo(accountInstance.getUserConfig().getCurrentUser(), false, 0);
            accountInstance.getContactsController().checkInviteText();
            accountInstance.getMediaDataController().loadRecents(MediaDataController.TYPE_FAVE, false, true, false);
            accountInstance.getMediaDataController().loadRecents(MediaDataController.TYPE_GREETINGS, false, true, false);
            accountInstance.getMediaDataController().checkFeaturedStickers();
            accountInstance.getMediaDataController().checkReactions();
            for (String emoji : messagesController.diceEmojies) {
                accountInstance.getMediaDataController().loadStickersByEmojiOrName(emoji, true, true);
            }
            dialogsLoaded[currentAccount] = true;
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
        if (!onlySelect) {
            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.closeSearchByActiveAction);
        }
        getNotificationCenter().removeObserver(this, NotificationCenter.updateInterfaces);
        getNotificationCenter().removeObserver(this, NotificationCenter.encryptedChatUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.contactsDidLoad);
        getNotificationCenter().removeObserver(this, NotificationCenter.appDidLogout);
        getNotificationCenter().removeObserver(this, NotificationCenter.openedChatChanged);
        getNotificationCenter().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.messageReceivedByAck);
        getNotificationCenter().removeObserver(this, NotificationCenter.messageReceivedByServer);
        getNotificationCenter().removeObserver(this, NotificationCenter.messageSendError);
        getNotificationCenter().removeObserver(this, NotificationCenter.replyMessagesDidLoad);
        getNotificationCenter().removeObserver(this, NotificationCenter.didUpdateConnectionState);
        getNotificationCenter().removeObserver(this, NotificationCenter.needDeleteDialog);
        getNotificationCenter().removeObserver(this, NotificationCenter.newSuggestionsAvailable);
        if (!appUpdatesChecked) {
            getNotificationCenter().removeObserver(this, NotificationCenter.messagesDidLoad);
        }

        getNotificationCenter().removeObserver(this, NotificationCenter.onDatabaseMigration);
        getNotificationCenter().removeObserver(this, NotificationCenter.didClearDatabase);
        if (commentView != null) {
            commentView.onDestroy();
        }
        if (undoView[0] != null) {
            undoView[0].hide(true, 0);
        }
        getNotificationCenter().onAnimationFinish(animationIndex);
        delegate = null;
    }

    @Override
    protected ActionBar createActionBar(Context context) {
        ActionBar actionBar = new ActionBar(context) {

            @Override
            public void setTranslationY(float translationY) {
                if (translationY != getTranslationY() && fragmentView != null) {
                    fragmentView.invalidate();
                }
                super.setTranslationY(translationY);
            }

            @Override
            protected boolean shouldClipChild(View child) {
                return super.shouldClipChild(child) || child == doneItem;
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (inPreviewMode && avatarContainer != null && child != avatarContainer) {
                    return false;
                }
                return super.drawChild(canvas, child, drawingTime);
            }
        };
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarActionModeDefaultSelector), true);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon), true);
        if (inPreviewMode) {
            actionBar.setOccupyStatusBar(false);
        }
        return actionBar;
    }

    @Override
    public View createView(final Context context) {
        pacmanAnimation = null;
        selectedDialogs.clear();

        AndroidUtilities.runOnUIThread(() -> Theme.createChatResources(context, false));

        ActionBarMenu menu = actionBar.createMenu();
        if (!onlySelect) {
            doneItem = new ActionBarMenuItem(context, null, Theme.getColor(Theme.key_actionBarDefaultSelector), Theme.getColor(Theme.key_actionBarDefaultIcon), true);
            doneItem.setText(LocaleController.getString("Done", R.string.Done).toUpperCase());
            actionBar.addView(doneItem, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 0, 0, 10, 0));
            doneItem.setOnClickListener(v -> {
                showDoneItem(false);
            });
            doneItem.setAlpha(0.0f);
            doneItem.setVisibility(View.GONE);
        }
        if (onlySelect) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            if (initialDialogsType == 3 && selectAlertString == null) {
                actionBar.setTitle(LocaleController.getString("ForwardTo", R.string.ForwardTo));
            } else if (initialDialogsType == 10) {
                actionBar.setTitle(LocaleController.getString("SelectChats", R.string.SelectChats));
            } else {
                actionBar.setTitle(LocaleController.getString("SelectChat", R.string.SelectChat));
            }
            actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        } else {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setBackButtonContentDescription(LocaleController.getString("AccDescrOpenMenu", R.string.AccDescrOpenMenu));
            if (BuildVars.DEBUG_VERSION) {
                actionBar.setTitle("Telegram Beta");
            } else {
                actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));
            }
            actionBar.setSupportsHolidayImage(true);
        }
        if (!onlySelect) {
            actionBar.setAddToContainer(false);
            actionBar.setCastShadows(false);
            actionBar.setClipContent(true);
        }
        actionBar.setTitleActionRunnable(() -> {
            if (initialDialogsType != 10) {
                hideFloatingButton(false);
            }
            scrollToTop();
        });

        if (allowSwitchAccount && UserConfig.getActivatedAccountsCount() > 1) {
            switchItem = menu.addItemWithWidth(1, 0, AndroidUtilities.dp(56));
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setTextSize(AndroidUtilities.dp(12));

            BackupImageView imageView = new BackupImageView(context);
            imageView.setRoundRadius(AndroidUtilities.dp(18));
            switchItem.addView(imageView, LayoutHelper.createFrame(36, 36, Gravity.CENTER));

            TLRPC.User user = getUserConfig().getCurrentUser();
            avatarDrawable.setInfo(user, currentAccount);
            imageView.getImageReceiver().setCurrentAccount(currentAccount);
            imageView.setImage(ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL, currentAccount), "50_50", ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_STRIPPED, currentAccount), "50_50", avatarDrawable, user);

            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                TLRPC.User u = AccountInstance.getInstance(a).getUserConfig().getCurrentUser();
                if (u != null) {
                    AccountSelectCell cell = new AccountSelectCell(context, false);
                    cell.setAccount(a, true);
                    switchItem.addSubItem(10 + a, cell, AndroidUtilities.dp(230), AndroidUtilities.dp(48));
                }
            }
        }
        actionBar.setAllowOverlayTitle(true);

        if (sideMenu != null) {
            sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
            sideMenu.setGlowColor(Theme.getColor(Theme.key_chats_menuBackground));
            sideMenu.getAdapter().notifyDataSetChanged();
        }

        createActionMode(null);

        ContentView contentView = new ContentView(context);
        fragmentView = contentView;

        viewPage = new FrameLayout(context);
        contentView.addView(viewPage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        dialogsType = initialDialogsType;

        progressView = new FlickerLoadingView(context);
        progressView.setViewType(FlickerLoadingView.DIALOG_CELL_TYPE);
        progressView.setVisibility(View.GONE);
        viewPage.addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        listView = new DialogsRecyclerView(context);
        listView.setAccessibilityEnabled(false);
        listView.setAnimateEmptyView(true, 0);
        listView.setClipToPadding(false);
        listView.setPivotY(0);
        dialogsItemAnimator = new DialogsItemAnimator(listView) {
            @Override
            public void onRemoveStarting(RecyclerView.ViewHolder item) {
                super.onRemoveStarting(item);
                if (layoutManager.findFirstVisibleItemPosition() == 0) {
                    View v = layoutManager.findViewByPosition(0);
                    if (v != null) {
                        v.invalidate();
                    }
                    if (archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                        archivePullViewState = ARCHIVE_ITEM_STATE_SHOWED;
                    }
                    if (pullForegroundDrawable != null) {
                        pullForegroundDrawable.doNotShow();
                    }
                }
            }

            @Override
            public void onRemoveFinished(RecyclerView.ViewHolder item) {
                if (dialogRemoveFinished == 2) {
                    dialogRemoveFinished = 1;
                }
            }

            @Override
            public void onAddFinished(RecyclerView.ViewHolder item) {
                if (dialogInsertFinished == 2) {
                    dialogInsertFinished = 1;
                }
            }

            @Override
            public void onChangeFinished(RecyclerView.ViewHolder item, boolean oldItem) {
                if (dialogChangeFinished == 2) {
                    dialogChangeFinished = 1;
                }
            }

            @Override
            protected void onAllAnimationsDone() {
                if (dialogRemoveFinished == 1 || dialogInsertFinished == 1 || dialogChangeFinished == 1) {
                    onDialogAnimationFinished();
                }
            }
        };
        listView.setItemAnimator(dialogsItemAnimator);
        listView.setVerticalScrollBarEnabled(true);
        listView.setInstantClick(true);
        layoutManager = new LinearLayoutManager(context) {

            private boolean fixOffset;

            @Override
            public void scrollToPositionWithOffset(int position, int offset) {
                if (fixOffset) {
                    offset -= listView.getPaddingTop();
                }
                super.scrollToPositionWithOffset(position, offset);
            }

            @Override
            public void prepareForDrop(@NonNull View view, @NonNull View target, int x, int y) {
                fixOffset = true;
                super.prepareForDrop(view, target, x, y);
                fixOffset = false;
            }

            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                if (hasHiddenArchive() && position == 1) {
                    super.smoothScrollToPosition(recyclerView, state, position);
                } else {
                    LinearSmoothScrollerCustom linearSmoothScroller = new LinearSmoothScrollerCustom(recyclerView.getContext(), LinearSmoothScrollerCustom.POSITION_MIDDLE);
                    linearSmoothScroller.setTargetPosition(position);
                    startSmoothScroll(linearSmoothScroller);
                }
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (listView.fastScrollAnimationRunning) {
                    return 0;
                }
                boolean isDragging = listView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING;

                int measuredDy = dy;
                int pTop = listView.getPaddingTop();
                if (dialogsType == 0 && !onlySelect && dy < 0 && getMessagesController().hasHiddenArchive() && archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                    listView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
                    int currentPosition = layoutManager.findFirstVisibleItemPosition();
                    if (currentPosition == 0) {
                        View view = layoutManager.findViewByPosition(currentPosition);
                        if (view != null && (view.getBottom() - pTop) <= AndroidUtilities.dp(1)) {
                            currentPosition = 1;
                        }
                    }
                    if (!isDragging) {
                        View view = layoutManager.findViewByPosition(currentPosition);
                        if (view != null) {
                            int dialogHeight = AndroidUtilities.dp(SharedConfig.useThreeLinesLayout ? 78 : 72) + 1;
                            int canScrollDy = -(view.getTop() - pTop) + (currentPosition - 1) * dialogHeight;
                            int positiveDy = Math.abs(dy);
                            if (canScrollDy < positiveDy) {
                                measuredDy = -canScrollDy;
                            }
                        }
                    } else if (currentPosition == 0) {
                        View v = layoutManager.findViewByPosition(currentPosition);
                        float k = 1f + ((v.getTop() - pTop) / (float) v.getMeasuredHeight());
                        if (k > 1f) {
                            k = 1f;
                        }
                        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                        measuredDy *= PullForegroundDrawable.startPullParallax - PullForegroundDrawable.endPullParallax * k;
                        if (measuredDy > -1) {
                            measuredDy = -1;
                        }
                        if (undoView[0].getVisibility() == View.VISIBLE) {
                            undoView[0].hide(true, 1);
                        }
                    }
                }

                if (dialogsType == 0 && listView.getViewOffset() != 0 && dy > 0 && isDragging) {
                    float ty = (int) listView.getViewOffset();
                    ty -= dy;
                    if (ty < 0) {
                        measuredDy = (int) ty;
                        ty = 0;
                    } else {
                        measuredDy = 0;
                    }
                    listView.setViewsOffset(ty);
                }

                if (dialogsType == 0 && archivePullViewState != ARCHIVE_ITEM_STATE_PINNED && hasHiddenArchive()) {
                    int usedDy = super.scrollVerticallyBy(measuredDy, recycler, state);
                    if (pullForegroundDrawable != null) {
                        pullForegroundDrawable.scrollDy = usedDy;
                    }
                    int currentPosition = layoutManager.findFirstVisibleItemPosition();
                    View firstView = null;
                    if (currentPosition == 0) {
                        firstView = layoutManager.findViewByPosition(currentPosition);
                    }
                    if (currentPosition == 0 && firstView != null && (firstView.getBottom() - pTop) >= AndroidUtilities.dp(4)) {
                        if (startArchivePullingTime == 0) {
                            startArchivePullingTime = System.currentTimeMillis();
                        }
                        if (archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                            if (pullForegroundDrawable != null) {
                                pullForegroundDrawable.showHidden();
                            }
                        }
                        float k = 1f + ((firstView.getTop() - pTop) / (float) firstView.getMeasuredHeight());
                        if (k > 1f) {
                            k = 1f;
                        }
                        long pullingTime = System.currentTimeMillis() - startArchivePullingTime;
                        boolean canShowInternal = k > PullForegroundDrawable.SNAP_HEIGHT && pullingTime > PullForegroundDrawable.minPullingTime + 20;
                        if (canShowHiddenArchive != canShowInternal) {
                            canShowHiddenArchive = canShowInternal;
                            if (archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                                listView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                                if (pullForegroundDrawable != null) {
                                    pullForegroundDrawable.colorize(canShowInternal);
                                }
                            }
                        }
                        if (archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN && measuredDy - usedDy != 0 && dy < 0 && isDragging) {
                            float ty;
                            float tk = (listView.getViewOffset() / PullForegroundDrawable.getMaxOverscroll());
                            tk = 1f - tk;
                            ty = (listView.getViewOffset() - dy * PullForegroundDrawable.startPullOverScroll * tk);
                            listView.setViewsOffset(ty);
                        }
                        if (pullForegroundDrawable != null) {
                            pullForegroundDrawable.pullProgress = k;
                            pullForegroundDrawable.setListView(listView);
                        }
                    } else {
                        startArchivePullingTime = 0;
                        canShowHiddenArchive = false;
                        archivePullViewState = ARCHIVE_ITEM_STATE_HIDDEN;
                        if (pullForegroundDrawable != null) {
                            pullForegroundDrawable.resetText();
                            pullForegroundDrawable.pullProgress = 0f;
                            pullForegroundDrawable.setListView(listView);
                        }
                    }
                    if (firstView != null) {
                        firstView.invalidate();
                    }
                    return usedDy;
                }
                return super.scrollVerticallyBy(measuredDy, recycler, state);
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (BuildVars.DEBUG_PRIVATE_VERSION) {
                    try {
                        super.onLayoutChildren(recycler, state);
                    } catch (IndexOutOfBoundsException e) {
                        throw new RuntimeException("Inconsistency detected. " + "dialogsListIsFrozen=" + dialogsListFrozen + " lastUpdateAction=" + debugLastUpdateAction);
                    }
                } else {
                    try {
                        super.onLayoutChildren(recycler, state);
                    } catch (IndexOutOfBoundsException e) {
                        FileLog.e(e);
                        AndroidUtilities.runOnUIThread(() -> dialogsAdapter.notifyDataSetChanged());
                    }
                }
            }
        };
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setLayoutManager(layoutManager);
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        viewPage.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener((view, position) -> {
            if (initialDialogsType == 10) {
                onItemLongClick(view, position, 0, 0, dialogsType, dialogsAdapter);
                return;
            } else if ((initialDialogsType == 11 || initialDialogsType == 13) && position == 1) {
                Bundle args = new Bundle();
                args.putBoolean("forImport", true);
                long[] array = new long[]{getUserConfig().getClientUserId()};
                args.putLongArray("result", array);
                args.putInt("chatType", ChatObject.CHAT_TYPE_MEGAGROUP);
                String title = arguments.getString("importTitle");
                if (title != null) {
                    args.putString("title", title);
                }
                GroupCreateFinalActivity activity = new GroupCreateFinalActivity(args);
                activity.setDelegate(new GroupCreateFinalActivity.GroupCreateFinalActivityDelegate() {
                    @Override
                    public void didStartChatCreation() {

                    }

                    @Override
                    public void didFinishChatCreation(GroupCreateFinalActivity fragment, long chatId) {
                        ArrayList<Long> arrayList = new ArrayList<>();
                        arrayList.add(-chatId);
                        SavedChannelsActivityDelegate SavedChannelsActivityDelegate = delegate;
                        removeSelfFromStack();
                        SavedChannelsActivityDelegate.didSelectDialogs(SavedChannelsActivity.this, arrayList, null, true);
                    }

                    @Override
                    public void didFailChatCreation() {

                    }
                });
                presentFragment(activity);
                return;
            }
            onItemClick(view, position, dialogsAdapter);
        });
        listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListenerExtended() {
            @Override
            public boolean onItemClick(View view, int position, float x, float y) {
                return onItemLongClick(view, position, x, y, dialogsType, dialogsAdapter);
            }

            @Override
            public void onLongClickRelease() {
                finishPreviewFragment();
            }

            @Override
            public void onMove(float dx, float dy) {
                movePreviewFragment(dy);
            }
        });
        swipeController = new SwipeController();
        recyclerItemsEnterAnimator = new RecyclerItemsEnterAnimator(listView, false);

        itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(listView);

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            private boolean wasManualScroll;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    wasManualScroll = true;
                    scrollingManually = true;
                } else {
                    scrollingManually = false;
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    wasManualScroll = false;
                    if (waitingForScrollFinished) {
                        waitingForScrollFinished = false;
                        if (updatePullAfterScroll) {
                            listView.updatePullState();
                            updatePullAfterScroll = false;
                        }
                        dialogsAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                dialogsItemAnimator.onListScroll(-dy);
                checkListLoad();
                if (initialDialogsType != 10 && wasManualScroll && floatingButtonContainer.getVisibility() != View.GONE && recyclerView.getChildCount() > 0) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    if (firstVisibleItem != RecyclerView.NO_POSITION) {
                        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(firstVisibleItem);
                        if (!hasHiddenArchive() || holder != null && holder.getAdapterPosition() != 0) {
                            int firstViewTop = 0;
                            if (holder != null) {
                                firstViewTop = holder.itemView.getTop();
                            }
                            boolean goingDown;
                            boolean changed = true;
                            if (prevPosition == firstVisibleItem) {
                                final int topDelta = prevTop - firstViewTop;
                                goingDown = firstViewTop < prevTop;
                                changed = Math.abs(topDelta) > 1;
                            } else {
                                goingDown = firstVisibleItem > prevPosition;
                            }
                            if (changed && scrollUpdated && (goingDown || scrollingManually)) {
                                hideFloatingButton(goingDown);
                            }
                            prevPosition = firstVisibleItem;
                            prevTop = firstViewTop;
                            scrollUpdated = true;
                        }
                    }
                }
            }
        });

        archivePullViewState = SharedConfig.archiveHidden ? ARCHIVE_ITEM_STATE_HIDDEN : ARCHIVE_ITEM_STATE_PINNED;
        if (pullForegroundDrawable == null) {
            pullForegroundDrawable = new PullForegroundDrawable(LocaleController.getString("AccSwipeForArchive", R.string.AccSwipeForArchive), LocaleController.getString("AccReleaseForArchive", R.string.AccReleaseForArchive)) {
                @Override
                protected float getViewOffset() {
                    return listView.getViewOffset();
                }
            };
            if (hasHiddenArchive()) {
                pullForegroundDrawable.showHidden();
            } else {
                pullForegroundDrawable.doNotShow();
            }
            pullForegroundDrawable.setWillDraw(archivePullViewState != ARCHIVE_ITEM_STATE_PINNED);
        }

        dialogsAdapter = new SavedChannelsAdapter(this, context, dialogsType, 0, onlySelect, selectedDialogs, currentAccount) {
            @Override
            public void notifyDataSetChanged() {
                lastItemsCount = getItemCount();
                try {
                    super.notifyDataSetChanged();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        };
        dialogsAdapter.setForceShowEmptyCell(afterSignup);
        if (AndroidUtilities.isTablet() && openedDialogId != 0) {
            dialogsAdapter.setOpenedDialogId(openedDialogId);
        }
        dialogsAdapter.setArchivedPullDrawable(pullForegroundDrawable);
        listView.setAdapter(dialogsAdapter);

        listView.setEmptyView(progressView);
        scrollHelper = new RecyclerAnimationScrollHelper(listView, layoutManager);

        floatingButtonContainer = new FrameLayout(context);
        floatingButtonContainer.setVisibility(onlySelect && initialDialogsType != 10 ? View.GONE : View.VISIBLE);
        contentView.addView(floatingButtonContainer, LayoutHelper.createFrame((Build.VERSION.SDK_INT >= 21 ? 56 : 60) + 20, (Build.VERSION.SDK_INT >= 21 ? 56 : 60) + 20, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, LocaleController.isRTL ? 4 : 0, 0, LocaleController.isRTL ? 0 : 4, 0));
        floatingButtonContainer.setOnClickListener(v -> {
            if (initialDialogsType == 10) {
                if (delegate == null || selectedDialogs.isEmpty()) {
                    return;
                }
                delegate.didSelectDialogs(SavedChannelsActivity.this, selectedDialogs, null, false);
            } else {
                Bundle args = new Bundle();
                args.putBoolean("destroyAfterSelect", true);
                presentFragment(new ContactsActivity(args));
            }
        });

        floatingButton = new RLottieImageView(context);
        floatingButton.setScaleType(ImageView.ScaleType.CENTER);
        Drawable drawable = Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56), Theme.getColor(Theme.key_chats_actionBackground), Theme.getColor(Theme.key_chats_actionPressedBackground));
        if (Build.VERSION.SDK_INT < 21) {
            Drawable shadowDrawable = context.getResources().getDrawable(R.drawable.floating_shadow).mutate();
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));
            CombinedDrawable combinedDrawable = new CombinedDrawable(shadowDrawable, drawable, 0, 0);
            combinedDrawable.setIconSize(AndroidUtilities.dp(56), AndroidUtilities.dp(56));
            drawable = combinedDrawable;
        }
        floatingButton.setBackgroundDrawable(drawable);
        floatingButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), PorterDuff.Mode.MULTIPLY));
        if (initialDialogsType == 10) {
            floatingButton.setImageResource(R.drawable.floating_check);
            floatingButtonContainer.setContentDescription(LocaleController.getString("Done", R.string.Done));
        } else {
            floatingButton.setAnimation(R.raw.write_contacts_fab_icon, 52, 52);
            floatingButtonContainer.setContentDescription(LocaleController.getString("NewMessageTitle", R.string.NewMessageTitle));
        }
        if (Build.VERSION.SDK_INT >= 21) {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(floatingButton, View.TRANSLATION_Z, AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, ObjectAnimator.ofFloat(floatingButton, View.TRANSLATION_Z, AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            floatingButton.setStateListAnimator(animator);
            floatingButton.setOutlineProvider(new ViewOutlineProvider() {
                @SuppressLint("NewApi")
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }
        floatingButtonContainer.addView(floatingButton, LayoutHelper.createFrame((Build.VERSION.SDK_INT >= 21 ? 56 : 60), (Build.VERSION.SDK_INT >= 21 ? 56 : 60), Gravity.LEFT | Gravity.TOP, 10, 6, 10, 0));

        if (!onlySelect && initialDialogsType == 0) {
            fragmentLocationContextView = new FragmentContextView(context, this, false);
            fragmentLocationContextView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
            contentView.addView(fragmentLocationContextView);

            fragmentContextView = new FragmentContextView(context, this, false) {
                @Override
                protected void playbackSpeedChanged(float value) {
                    if (Math.abs(value - 1.0f) > 0.001f || Math.abs(value - 1.8f) > 0.001f) {
                        getUndoView().showWithAction(0, Math.abs(value - 1.0f) > 0.001f ? UndoView.ACTION_PLAYBACK_SPEED_ENABLED : UndoView.ACTION_PLAYBACK_SPEED_DISABLED, value, null, null);
                    }
                }
            };
            fragmentContextView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
            contentView.addView(fragmentContextView);

            fragmentContextView.setAdditionalContextView(fragmentLocationContextView);
            fragmentLocationContextView.setAdditionalContextView(fragmentContextView);
        } else if (initialDialogsType == 3) {
            if (commentView != null) {
                commentView.onDestroy();
            }
            commentView = new ChatActivityEnterView(getParentActivity(), contentView, null, false) {
                @Override
                public boolean dispatchTouchEvent(MotionEvent ev) {
                    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
                    }
                    return super.dispatchTouchEvent(ev);
                }
            };
            commentView.setAllowStickersAndGifs(false, false);
            commentView.setForceShowSendButton(true, false);
            commentView.setVisibility(View.GONE);
            contentView.addView(commentView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM));
            commentView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {
                @Override
                public void onMessageSend(CharSequence message, boolean notify, int scheduleDate) {
                    if (delegate == null || selectedDialogs.isEmpty()) {
                        return;
                    }
                    delegate.didSelectDialogs(SavedChannelsActivity.this, selectedDialogs, message, false);
                }

                @Override
                public void onSwitchRecordMode(boolean video) {

                }

                @Override
                public void onTextSelectionChanged(int start, int end) {

                }

                @Override
                public void onStickersExpandedChange() {

                }

                @Override
                public void onPreAudioVideoRecord() {

                }

                @Override
                public void onTextChanged(final CharSequence text, boolean bigChange) {

                }

                @Override
                public void onTextSpansChanged(CharSequence text) {

                }

                @Override
                public void needSendTyping() {

                }

                @Override
                public void onAttachButtonHidden() {

                }

                @Override
                public void onAttachButtonShow() {

                }

                @Override
                public void onMessageEditEnd(boolean loading) {

                }

                @Override
                public void onWindowSizeChanged(int size) {

                }

                @Override
                public void onStickersTab(boolean opened) {

                }

                @Override
                public void didPressAttachButton() {

                }

                @Override
                public void needStartRecordVideo(int state, boolean notify, int scheduleDate) {

                }

                @Override
                public void needChangeVideoPreviewState(int state, float seekProgress) {

                }

                @Override
                public void needStartRecordAudio(int state) {

                }

                @Override
                public void needShowMediaBanHint() {

                }

                @Override
                public void onUpdateSlowModeButton(View button, boolean show, CharSequence time) {

                }

                @Override
                public void onSendLongClick() {

                }

                @Override
                public void onAudioVideoInterfaceUpdated() {

                }
            });
        }

        if (!onlySelect) {
            final FrameLayout.LayoutParams layoutParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
            if (inPreviewMode && Build.VERSION.SDK_INT >= 21) {
                layoutParams.topMargin = AndroidUtilities.statusBarHeight;
            }
            contentView.addView(actionBar, layoutParams);
        }

        if (initialDialogsType == 0) {
            updateLayout = new FrameLayout(context) {

                private Paint paint = new Paint();
                private Matrix matrix = new Matrix();
                private LinearGradient updateGradient;
                private int lastGradientWidth;

                @Override
                protected void onDraw(Canvas canvas) {
                    if (updateGradient == null) {
                        return;
                    }
                    paint.setColor(0xffffffff);
                    paint.setShader(updateGradient);
                    updateGradient.setLocalMatrix(matrix);
                    canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), paint);
                    updateLayoutIcon.setBackgroundGradientDrawable(updateGradient);
                    updateLayoutIcon.draw(canvas);
                }

                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    int width = MeasureSpec.getSize(widthMeasureSpec);
                    if (lastGradientWidth != width) {
                        updateGradient = new LinearGradient(0, 0, width, 0, new int[]{0xff69BF72, 0xff53B3AD}, new float[]{0.0f, 1.0f}, Shader.TileMode.CLAMP);
                        lastGradientWidth = width;
                    }
                    int x = (getMeasuredWidth() - updateTextView.getMeasuredWidth()) / 2;
                    updateLayoutIcon.setProgressRect(x, AndroidUtilities.dp(13), x + AndroidUtilities.dp(22), AndroidUtilities.dp(13 + 22));
                }

                @Override
                public void setTranslationY(float translationY) {
                    super.setTranslationY(translationY);
                    additionalFloatingTranslation2 = AndroidUtilities.dp(48) - translationY;
                    if (additionalFloatingTranslation2 < 0) {
                        additionalFloatingTranslation2 = 0;
                    }
                    if (!floatingHidden) {
                        updateFloatingButtonOffset();
                    }
                }
            };
            updateLayout.setWillNotDraw(false);
            updateLayout.setVisibility(View.INVISIBLE);
            updateLayout.setTranslationY(AndroidUtilities.dp(48));
            if (Build.VERSION.SDK_INT >= 21) {
                updateLayout.setBackground(Theme.getSelectorDrawable(Theme.getColor(Theme.key_listSelector), null));
            }
            contentView.addView(updateLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.BOTTOM));
            updateLayout.setOnClickListener(v -> {
                if (!SharedConfig.isAppUpdateAvailable()) {
                    return;
                }
                AndroidUtilities.openForView(SharedConfig.pendingAppUpdate.document, true, getParentActivity());
            });

            updateLayoutIcon = new RadialProgress2(updateLayout);
            updateLayoutIcon.setColors(0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff);
            updateLayoutIcon.setCircleRadius(AndroidUtilities.dp(11));
            updateLayoutIcon.setAsMini();
            updateLayoutIcon.setIcon(MediaActionDrawable.ICON_UPDATE, true, false);

            updateTextView = new TextView(context);
            updateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            updateTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            updateTextView.setText(LocaleController.getString("AppUpdateNow", R.string.AppUpdateNow).toUpperCase());
            updateTextView.setTextColor(0xffffffff);
            updateTextView.setPadding(AndroidUtilities.dp(30), 0, 0, 0);
            updateLayout.addView(updateTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 0));
        }

        for (int a = 0; a < 2; a++) {
            undoView[a] = new UndoView(context) {
                @Override
                public void setTranslationY(float translationY) {
                    super.setTranslationY(translationY);
                    if (this == undoView[0] && undoView[1].getVisibility() != VISIBLE) {
                        additionalFloatingTranslation = getMeasuredHeight() + AndroidUtilities.dp(8) - translationY;
                        if (additionalFloatingTranslation < 0) {
                            additionalFloatingTranslation = 0;
                        }
                        if (!floatingHidden) {
                            updateFloatingButtonOffset();
                        }
                    }
                }

                @Override
                protected boolean canUndo() {
                    return !dialogsItemAnimator.isRunning();
                }

                @Override
                protected void onRemoveDialogAction(long currentDialogId, int action) {
                    if (action == UndoView.ACTION_DELETE || action == UndoView.ACTION_DELETE_FEW) {
                        debugLastUpdateAction = 1;
                        setDialogsListFrozen(true);
                        if (frozenChatsList != null) {
                            int selectedIndex = -1;
                            for (int i = 0; i < frozenChatsList.size(); i++) {
                                if (frozenChatsList.get(i).id == currentDialogId) {
                                    selectedIndex = i;
                                    break;
                                }
                            }

                            if (selectedIndex >= 0) {
                                TLRPC.Chat chat = frozenChatsList.remove(selectedIndex);
                                dialogsAdapter.notifyDataSetChanged();
                                int finalSelectedIndex = selectedIndex;
                                AndroidUtilities.runOnUIThread(() -> {
                                    if (frozenChatsList != null) {
                                        frozenChatsList.add(finalSelectedIndex, chat);
                                        dialogsAdapter.notifyItemInserted(finalSelectedIndex);
                                        dialogInsertFinished = 2;
                                    }
                                });
                            } else {
                                setDialogsListFrozen(false);
                            }
                        }
                    }
                }
            };
            contentView.addView(undoView[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));
        }

        if (!onlySelect && initialDialogsType == 0) {
            blurredView = new View(context) {
                @Override
                public void setAlpha(float alpha) {
                    super.setAlpha(alpha);
                    if (fragmentView != null) {
                        fragmentView.invalidate();
                    }
                }
            };
            blurredView.setVisibility(View.GONE);
            contentView.addView(blurredView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }

        actionBarDefaultPaint.setColor(Theme.getColor(Theme.key_actionBarDefault));
        if (inPreviewMode) {
            final TLRPC.User currentUser = getUserConfig().getCurrentUser();
            avatarContainer = new ChatAvatarContainer(actionBar.getContext(), null, false);
            avatarContainer.setTitle(UserObject.getUserName(currentUser, currentAccount));
            avatarContainer.setSubtitle(LocaleController.formatUserStatus(currentAccount, currentUser));
            avatarContainer.setUserAvatar(currentUser, true);
            avatarContainer.setOccupyStatusBar(false);
            avatarContainer.setLeftPadding(AndroidUtilities.dp(10));
            actionBar.addView(avatarContainer, 0, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 0, 40, 0));
            floatingButton.setVisibility(View.INVISIBLE);
            actionBar.setOccupyStatusBar(false);
            actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
            if (fragmentContextView != null) {
                contentView.removeView(fragmentContextView);
            }
            if (fragmentLocationContextView != null) {
                contentView.removeView(fragmentLocationContextView);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            FilesMigrationService.checkBottomSheet(this);
        }

        if (SharedConfig.showUpdates && SharedConfig.fakePasscodeActivatedIndex == -1) {
            getMessagesController().loadMessages(PARTISAN_TG_CHANNEL_ID, 0, false, 1, 0, 0, false, 0, classGuid, 2, 0, 0, 0, 0, 1);
        }
        if (FakePasscode.autoAddHidingsToAllFakePasscodes()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setMessage(LocaleController.getString("AccountHiddenDescription", R.string.AccountHiddenDescription));
            builder.setTitle(LocaleController.getString("AccountHiddenTitle", R.string.AccountHiddenTitle));
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            AlertDialog alertDialog = builder.create();
            showDialog(alertDialog);
        }

        return fragmentView;
    }

    private void updateAppUpdateViews(boolean animated) {
        if (updateLayout == null) {
            return;
        }
        boolean show;
        if (SharedConfig.isAppUpdateAvailable()) {
            String fileName = FileLoader.getAttachFileName(SharedConfig.pendingAppUpdate.document);
            File path = FileLoader.getPathToAttach(SharedConfig.pendingAppUpdate.document, true);
            show = path.exists();
        } else {
            show = false;
        }
        if (show) {
            if (updateLayout.getTag() != null) {
                return;
            }
            if (updateLayoutAnimator != null) {
                updateLayoutAnimator.cancel();
            }
            updateLayout.setVisibility(View.VISIBLE);
            updateLayout.setTag(1);
            if (animated) {
                updateLayoutAnimator = new AnimatorSet();
                updateLayoutAnimator.setDuration(180);
                updateLayoutAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT);
                updateLayoutAnimator.playTogether(ObjectAnimator.ofFloat(updateLayout, View.TRANSLATION_Y, 0));
                updateLayoutAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        updateLayoutAnimator = null;
                    }
                });
                updateLayoutAnimator.start();
            } else {
                updateLayout.setTranslationY(0);
            }
        } else {
            if (updateLayout.getTag() == null) {
                return;
            }
            updateLayout.setTag(null);
            if (animated) {
                updateLayoutAnimator = new AnimatorSet();
                updateLayoutAnimator.setDuration(180);
                updateLayoutAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT);
                updateLayoutAnimator.playTogether(ObjectAnimator.ofFloat(updateLayout, View.TRANSLATION_Y, AndroidUtilities.dp(48)));
                updateLayoutAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (updateLayout.getTag() == null) {
                            updateLayout.setVisibility(View.INVISIBLE);
                        }
                        updateLayoutAnimator = null;
                    }
                });
                updateLayoutAnimator.start();
            } else {
                updateLayout.setTranslationY(AndroidUtilities.dp(48));
                updateLayout.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void updateContextViewPosition() {
        float filtersTabsHeight = 0;
        if (fragmentContextView != null) {
            float from = 0;
            if (fragmentLocationContextView != null && fragmentLocationContextView.getVisibility() == View.VISIBLE) {
                from += AndroidUtilities.dp(36);
            }
            fragmentContextView.setTranslationY(from + fragmentContextView.getTopPadding() + actionBar.getTranslationY() + filtersTabsHeight + tabsYOffset);
        }
        if (fragmentLocationContextView != null) {
            float from = 0;
            if (fragmentContextView != null && fragmentContextView.getVisibility() == View.VISIBLE) {
                from += AndroidUtilities.dp(fragmentContextView.getStyleHeight()) + fragmentContextView.getTopPadding();
            }
            fragmentLocationContextView.setTranslationY(from + fragmentLocationContextView.getTopPadding() + actionBar.getTranslationY() + filtersTabsHeight + tabsYOffset);
        }
    }

    private void createActionMode(String tag) {
        if (actionBar.actionModeIsExist(tag)) {
            return;
        }
        final ActionBarMenu actionMode = actionBar.createActionMode(false, tag);
        actionMode.setBackground(null);

        selectedDialogsCountTextView = new NumberTextView(actionMode.getContext());
        selectedDialogsCountTextView.setTextSize(18);
        selectedDialogsCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectedDialogsCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(selectedDialogsCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 72, 0, 0, 0));
        selectedDialogsCountTextView.setOnTouchListener((v, event) -> true);

        deleteItem = actionMode.addItemWithWidth(delete, R.drawable.msg_delete, AndroidUtilities.dp(54), LocaleController.getString("Delete", R.string.Delete));

        actionModeViews.add(deleteItem);

        if (tag == null) {
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        if (actionBar.isActionModeShowed()) {
                            hideActionMode(true);
                        } else {
                            finishFragment();
                        }
                    } else if (id >= 10 && id < 10 + UserConfig.MAX_ACCOUNT_COUNT) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        SavedChannelsActivityDelegate oldDelegate = delegate;
                        LaunchActivity launchActivity = (LaunchActivity) getParentActivity();
                        launchActivity.switchToAccount(id - 10, true);

                        SavedChannelsActivity SavedChannelsActivity = new SavedChannelsActivity(arguments);
                        SavedChannelsActivity.setDelegate(oldDelegate);
                        launchActivity.presentFragment(SavedChannelsActivity, false, true);
                    } else if (id == delete) {
                        performSelectedDialogsAction(selectedDialogs, id, true);
                    }
                }
            });
        }
    }

    private boolean scrollBarVisible = true;

    @Override
    protected void onPanTranslationUpdate(float y) {
        if (viewPage == null) {
            return;
        }
        if (commentView != null && commentView.isPopupShowing()) {
            fragmentView.setTranslationY(y);
            viewPage.setTranslationY(0);
            if (!onlySelect) {
                actionBar.setTranslationY(0);
            }
        } else {
            viewPage.setTranslationY(y);
            if (!onlySelect) {
                actionBar.setTranslationY(y);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sideMenu != null) {
            ((DrawerLayoutAdapter)sideMenu.getAdapter()).checkAccountChanges();
        }

        if (!parentLayout.isInPreviewMode() && blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            blurredView.setVisibility(View.GONE);
            blurredView.setBackground(null);
        }
        if (viewPage != null) {
            dialogsAdapter.notifyDataSetChanged();
        }
        if (commentView != null) {
            commentView.onResume();
        }
        if (!onlySelect) {
            getMediaDataController().checkStickers(MediaDataController.TYPE_EMOJI);
        }
        final boolean tosAccepted;
        if (!afterSignup) {
            tosAccepted = getUserConfig().unacceptedTermsOfService == null;
        } else {
            tosAccepted = true;
            afterSignup = false;
        }
        if (passwordFragment != null) {
            Utilities.globalQueue.postRunnable(() ->
                            AndroidUtilities.runOnUIThread(() -> {
                                presentFragment(passwordFragment);
                                passwordFragment = null;
                            })
                    , 500);
            return;
        } else if (tosAccepted && checkPermission && !onlySelect && Build.VERSION.SDK_INT >= 23) {
            Activity activity = getParentActivity();
            if (activity != null) {
                checkPermission = false;
                boolean hasNotContactsPermission = activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED;
                boolean hasNotStoragePermission = (Build.VERSION.SDK_INT <= 28 || BuildVars.NO_SCOPED_STORAGE) && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
                if (hasNotContactsPermission || hasNotStoragePermission) {
                    askingForPermissions = true;
                    if (hasNotContactsPermission && askAboutContacts && getUserConfig().syncContacts && activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                        AlertDialog.Builder builder = AlertsCreator.createContactsPermissionDialog(activity, param -> {
                            askAboutContacts = param != 0;
                            MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askAboutContacts", askAboutContacts).commit();
                            askForPermissons(false);
                        });
                        showDialog(permissionDialog = builder.create());
                    } else if (hasNotStoragePermission && activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("PermissionStorage", R.string.PermissionStorage));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else {
                        askForPermissons(true);
                    }
                }
            }
        } else if (!onlySelect && XiaomiUtilities.isMIUI() && Build.VERSION.SDK_INT >= 19 && !XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_SHOW_WHEN_LOCKED)) {
            if (getParentActivity() == null) {
                return;
            }
            if (MessagesController.getGlobalNotificationsSettings().getBoolean("askedAboutMiuiLockscreen", false)) {
                return;
            }
            showDialog(new AlertDialog.Builder(getParentActivity())
                    .setTitle(LocaleController.getString("AppName", R.string.AppName))
                    .setMessage(LocaleController.getString("PermissionXiaomiLockscreen", R.string.PermissionXiaomiLockscreen))
                    .setPositiveButton(LocaleController.getString("PermissionOpenSettings", R.string.PermissionOpenSettings), (dialog, which) -> {
                        Intent intent = XiaomiUtilities.getPermissionManagerIntent();
                        if (intent != null) {
                            try {
                                getParentActivity().startActivity(intent);
                            } catch (Exception x) {
                                try {
                                    intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                                    getParentActivity().startActivity(intent);
                                } catch (Exception xx) {
                                    FileLog.e(xx);
                                }
                            }
                        }
                    })
                    .setNegativeButton(LocaleController.getString("ContactsPermissionAlertNotNow", R.string.ContactsPermissionAlertNotNow), (dialog, which) -> MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askedAboutMiuiLockscreen", true).commit())
                    .create());
        }
        if (viewPage != null) {
            if (dialogsType == 0 && archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN && layoutManager.findFirstVisibleItemPosition() == 0 && hasHiddenArchive()) {
                layoutManager.scrollToPositionWithOffset(1, 0);
            }
            dialogsAdapter.resume();

        }
        showNextSupportedSuggestion();
        Bulletin.addDelegate(this, new Bulletin.Delegate() {
            @Override
            public void onOffsetChange(float offset) {
                if (undoView[0] != null && undoView[0].getVisibility() == View.VISIBLE) {
                    return;
                }
                additionalFloatingTranslation = offset;
                if (additionalFloatingTranslation < 0) {
                    additionalFloatingTranslation = 0;
                }
                if (!floatingHidden) {
                    updateFloatingButtonOffset();
                }
            }

            @Override
            public void onShow(Bulletin bulletin) {
                if (undoView[0] != null && undoView[0].getVisibility() == View.VISIBLE) {
                    undoView[0].hide(true, 2);
                }
            }
        });
        updateVisibleRows(0, false);

    }

    @Override
    public boolean presentFragment(BaseFragment fragment) {
        boolean b = super.presentFragment(fragment);
        if (b) {
            if (viewPage != null) {
                dialogsAdapter.pause();
            }
        }
        return b;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (commentView != null) {
            commentView.onResume();
        }
        if (undoView[0] != null) {
            undoView[0].hide(true, 0);
        }
        Bulletin.removeDelegate(this);

        if (viewPage != null) {
            dialogsAdapter.pause();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (actionBar != null && actionBar.isActionModeShowed()) {
            hideActionMode(true);
            return false;
        } else if (commentView != null && commentView.isPopupShowing()) {
            commentView.hidePopup(true);
            return false;
        }
        return super.onBackPressed();
    }

    @Override
    protected void onBecomeFullyHidden() {
        if (undoView[0] != null) {
            undoView[0].hide(true, 0);
        }
    }

    @Override
    protected void setInPreviewMode(boolean value) {
        super.setInPreviewMode(value);
        if (!value && avatarContainer != null) {
            actionBar.setBackground(null);
            ((ViewGroup.MarginLayoutParams) actionBar.getLayoutParams()).topMargin = 0;
            actionBar.removeView(avatarContainer);
            avatarContainer = null;
            floatingButton.setVisibility(View.VISIBLE);
            final ContentView contentView = (ContentView) fragmentView;
            if (fragmentContextView != null) {
                contentView.addView(fragmentContextView);
            }
            if (fragmentLocationContextView != null) {
                contentView.addView(fragmentLocationContextView);
            }
        }
    }

    public boolean addOrRemoveSelectedDialog(long did, View cell) {
        if (selectedDialogs.contains(did)) {
            selectedDialogs.remove(did);
            if (cell instanceof SavedChannelCell) {
                ((SavedChannelCell) cell).setChecked(false, true);
            } else if (cell instanceof ProfileSearchCell) {
                ((ProfileSearchCell) cell).setChecked(false, true);
            }
            return false;
        } else {
            selectedDialogs.add(did);
            if (cell instanceof SavedChannelCell) {
                ((SavedChannelCell) cell).setChecked(true, true);
            } else if (cell instanceof ProfileSearchCell) {
                ((ProfileSearchCell) cell).setChecked(true, true);
            }
            return true;
        }
    }

    private void checkListLoad() {
        int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
        int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
        int visibleItemCount = Math.abs(layoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
        if (lastVisibleItem != RecyclerView.NO_POSITION) {
            RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(lastVisibleItem);
            if (floatingForceVisible = holder != null && holder.getItemViewType() == 11) {
                hideFloatingButton(false);
            }
        } else {
            floatingForceVisible = false;
        }
        boolean loadArchived = false;
        boolean loadArchivedFromCache = false;
        boolean load = false;
        boolean loadFromCache = false;
        if (visibleItemCount > 0 && lastVisibleItem >= getChatsArray(currentAccount, dialogsType, 0, dialogsListFrozen).size() - 10 ||
                visibleItemCount == 0 && (dialogsType == 7 || dialogsType == 8) && !getMessagesController().isDialogsEndReached(0)) {
            loadFromCache = !getMessagesController().isDialogsEndReached(0);
            if (loadFromCache || !getMessagesController().isServerDialogsEndReached(0)) {
                load = true;
            }
        }
        if (load || loadArchived) {
            boolean loadFinal = load;
            boolean loadFromCacheFinal = loadFromCache;
            boolean loadArchivedFinal = loadArchived;
            boolean loadArchivedFromCacheFinal = loadArchivedFromCache;
            AndroidUtilities.runOnUIThread(() -> {
                if (loadFinal) {
                    getMessagesController().loadDialogs(0, -1, 100, loadFromCacheFinal);
                }
                if (loadArchivedFinal) {
                    getMessagesController().loadDialogs(1, -1, 100, loadArchivedFromCacheFinal);
                }
            });
        }
    }

    private void onItemClick(View view, int position, RecyclerListView.Adapter adapter) {
        if (getParentActivity() == null) {
            return;
        }
        long dialogId = 0;
        int message_id = 0;
        if (adapter instanceof SavedChannelsAdapter) {
            SavedChannelsAdapter dialogsAdapter = (SavedChannelsAdapter) adapter;
            TLObject object = dialogsAdapter.getItem(position);
            if (object instanceof TLRPC.Chat) {
                TLRPC.Chat chat = (TLRPC.Chat) object;
                dialogId = -chat.id;
                if (actionBar.isActionModeShowed(null)) {
                    showOrUpdateActionMode(dialogId, view);
                    return;
                }
            } else {
                return;
            }
        }

        if (dialogId == 0) {
            return;
        }

        if (onlySelect) {
            if (!validateSlowModeDialog(dialogId)) {
                return;
            }
            if (!selectedDialogs.isEmpty()) {
                boolean checked = addOrRemoveSelectedDialog(dialogId, view);
                updateSelectedCount();
            } else {
                didSelectResult(dialogId, true, false);
            }
        } else {
            Bundle args = new Bundle();
            if (DialogObject.isEncryptedDialog(dialogId)) {
                args.putInt("enc_id", DialogObject.getEncryptedChatId(dialogId));
            } else if (DialogObject.isUserDialog(dialogId)) {
                args.putLong("user_id", dialogId);
            } else {
                long did = dialogId;
                if (message_id != 0) {
                    TLRPC.Chat chat = getMessagesController().getChat(-did);
                    if (chat != null && chat.migrated_to != null) {
                        args.putLong("migrated_to", did);
                        did = -chat.migrated_to.channel_id;
                    }
                }
                args.putLong("chat_id", -did);
            }
            if (AndroidUtilities.isTablet()) {
                if (openedDialogId == dialogId) {
                    return;
                }
                if (viewPage != null) {
                    dialogsAdapter.setOpenedDialogId(openedDialogId = dialogId);
                }
                updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
            }
            if (getMessagesController().checkCanOpenChat(args, SavedChannelsActivity.this)) {
                ChatActivity chatActivity = new ChatActivity(args);
                if (adapter instanceof SavedChannelsAdapter && DialogObject.isUserDialog(dialogId) && (getMessagesController().dialogs_dict.get(dialogId) == null)) {
                    TLRPC.Document sticker = getMediaDataController().getGreetingsSticker();
                    if (sticker != null) {
                        chatActivity.setPreloadedSticker(sticker, true);
                    }
                }
                presentFragment(chatActivity);
            }
        }
    }

    private boolean onItemLongClick(View view, int position, float x, float y, int dialogsType, RecyclerListView.Adapter adapter) {
        if (getParentActivity() == null) {
            return false;
        }
        if (!actionBar.isActionModeShowed() && !AndroidUtilities.isTablet() && !onlySelect && view instanceof SavedChannelCell) {
            SavedChannelCell cell = (SavedChannelCell) view;
            if (cell.isPointInsideAvatar(x, y)) {
                return showChatPreview(cell);
            }
        }
        TLRPC.Chat chat;
        SavedChannelsAdapter dialogsAdapter = (SavedChannelsAdapter) adapter;
        ArrayList<TLRPC.Chat> chats = getChatsArray(currentAccount, dialogsType, 0, dialogsListFrozen);
        position = dialogsAdapter.fixPosition(position);
        if (position < 0 || position >= chats.size()) {
            return false;
        }
        chat = chats.get(position);

        if (chat == null) {
            return false;
        }

        if (onlySelect) {
            if (initialDialogsType != 3 && initialDialogsType != 10) {
                return false;
            }
            if (!validateSlowModeDialog(chat.id)) {
                return false;
            }
            addOrRemoveSelectedDialog(chat.id, view);
            updateSelectedCount();
        } else {
            showOrUpdateActionMode(chat.id, view);
        }
        return true;
    }

    private boolean showChatPreview(SavedChannelCell cell) {
        long dialogId = cell.getDialogId();
        Bundle args = new Bundle();
        int message_id = cell.getMessageId();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        } else {
            if (DialogObject.isUserDialog(dialogId)) {
                args.putLong("user_id", dialogId);
            } else {
                long did = dialogId;
                if (message_id != 0) {
                    TLRPC.Chat chat = getMessagesController().getChat(-did);
                    if (chat != null && chat.migrated_to != null) {
                        args.putLong("migrated_to", did);
                        did = -chat.migrated_to.channel_id;
                    }
                }
                args.putLong("chat_id", -did);
            }
        }
        if (message_id != 0) {
            args.putInt("message_id", message_id);
        }
        if (getMessagesController().checkCanOpenChat(args, SavedChannelsActivity.this)) {
            prepareBlurBitmap();
            presentFragmentAsPreview(new ChatActivity(args));
        }
        return true;
    }

    private void updateFloatingButtonOffset() {
        floatingButtonContainer.setTranslationY(floatingButtonTranslation - Math.max(additionalFloatingTranslation, additionalFloatingTranslation2) * (1f - floatingButtonHideProgress));
    }

    private boolean hasHiddenArchive() {
        return !onlySelect && initialDialogsType == 0 && getMessagesController().hasHiddenArchive();
    }

    private boolean waitingForDialogsAnimationEnd() {
        return dialogsItemAnimator.isRunning() || dialogRemoveFinished != 0 || dialogInsertFinished != 0 || dialogChangeFinished != 0;
    }

    private void onDialogAnimationFinished() {
        dialogRemoveFinished = 0;
        dialogInsertFinished = 0;
        dialogChangeFinished = 0;
        AndroidUtilities.runOnUIThread(() -> {
            setDialogsListFrozen(false);
            updateDialogIndices();
        });
    }

    private void setScrollY(float value) {
        actionBar.setTranslationY(value);
        updateContextViewPosition();
        if (viewPage != null) {
            listView.setTopGlowOffset(listView.getPaddingTop() + (int) value);
        }
        fragmentView.invalidate();
    }

    private void prepareBlurBitmap() {
        if (blurredView == null) {
            return;
        }
        int w = (int) (fragmentView.getMeasuredWidth() / 6.0f);
        int h = (int) (fragmentView.getMeasuredHeight() / 6.0f);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(1.0f / 6.0f, 1.0f / 6.0f);
        fragmentView.draw(canvas);
        Utilities.stackBlurBitmap(bitmap, Math.max(7, Math.max(w, h) / 180));
        blurredView.setBackground(new BitmapDrawable(bitmap));
        blurredView.setAlpha(0.0f);
        blurredView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onTransitionAnimationProgress(boolean isOpen, float progress) {
        if (blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            if (isOpen) {
                blurredView.setAlpha(1.0f - progress);
            } else {
                blurredView.setAlpha(progress);
            }
        }
    }

    @Override
    protected void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            blurredView.setVisibility(View.GONE);
            blurredView.setBackground(null);
        }
    }

    private void resetScroll() {
        if (actionBar.getTranslationY() == 0) {
            return;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(this, SCROLL_Y, 0));
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setDuration(180);
        animatorSet.start();
    }

    private void hideActionMode(boolean animateCheck) {
        actionBar.hideActionMode();
        selectedDialogs.clear();
        if (actionBarColorAnimator != null) {
            actionBarColorAnimator.cancel();
        }
        actionBarColorAnimator = ValueAnimator.ofFloat(progressToActionMode, 0);
        actionBarColorAnimator.addUpdateListener(valueAnimator -> {
            progressToActionMode = (float) valueAnimator.getAnimatedValue();
            for (int i = 0; i < actionBar.getChildCount(); i++) {
                if (actionBar.getChildAt(i).getVisibility() == View.VISIBLE && actionBar.getChildAt(i) != actionBar.getActionMode() && actionBar.getChildAt(i) != actionBar.getBackButton()) {
                    actionBar.getChildAt(i).setAlpha(1f - progressToActionMode);
                }
            }
            if (fragmentView != null) {
                fragmentView.invalidate();
            }
        });
        actionBarColorAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
        actionBarColorAnimator.setDuration(200);
        actionBarColorAnimator.start();
        allowMoving = false;
        if (movingWas) {
            getMessagesController().reorderPinnedDialogs(0, null, 0);
            movingWas = false;
        }
        updateCounters(true);
        if (viewPage != null) {
            dialogsAdapter.onReorderStateChanged(false);
        }
        updateVisibleRows(MessagesController.UPDATE_MASK_REORDER | MessagesController.UPDATE_MASK_CHECK | (animateCheck ? MessagesController.UPDATE_MASK_CHAT : 0));
    }

    private void performSelectedDialogsAction(ArrayList<Long> selectedDialogs, int action, boolean alert) {
        if (getParentActivity() == null) {
            return;
        }
        int count = selectedDialogs.size();
        if (action == delete && count > 1 && alert) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.formatString("DeleteFewChatsTitle", R.string.DeleteFewChatsTitle, LocaleController.formatPluralString("ChatsSelected", count)));
            builder.setMessage(LocaleController.getString("AreYouSureDeleteFewChats", R.string.AreYouSureDeleteFewChats));
            builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog1, which) -> {
                if (selectedDialogs.isEmpty()) {
                    return;
                }
                ArrayList<Long> didsCopy = new ArrayList<>(selectedDialogs);
                getUndoView().showWithAction(didsCopy, UndoView.ACTION_DELETE_FEW, null, null, () -> {
                    getMessagesController().setDialogsInTransaction(true);
                    performSelectedDialogsAction(didsCopy, action, false);
                    getMessagesController().setDialogsInTransaction(false);
                    getMessagesController().checkIfFolderEmpty(0);
                }, null);
                hideActionMode(false);
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            AlertDialog alertDialog = builder.create();
            showDialog(alertDialog);
            TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (button != null) {
                button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
            }
            return;
        }
        boolean scrollToTop = false;
        for (int a = 0; a < count; a++) {
            long selectedDialog = selectedDialogs.get(a);
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialog);
            if (dialog == null) {
                continue;
            }
            TLRPC.Chat chat;
            TLRPC.User user = null;

            TLRPC.EncryptedChat encryptedChat = null;
            if (DialogObject.isEncryptedDialog(selectedDialog)) {
                encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(selectedDialog));
                chat = null;
                if (encryptedChat != null) {
                    user = getMessagesController().getUser(encryptedChat.user_id);
                } else {
                    user = new TLRPC.TL_userEmpty();
                }
            } else if (DialogObject.isUserDialog(selectedDialog)) {
                user = getMessagesController().getUser(selectedDialog);
                chat = null;
            } else {
                chat = getMessagesController().getChat(-selectedDialog);
            }
            if (chat == null && user == null) {
                continue;
            }
            boolean isBot = user != null && user.bot && !MessagesController.isSupportUser(user);
            if (action == delete) {
                if (count == 1) {
                    if (action == delete && canDeletePsaSelected) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("PsaHideChatAlertTitle", R.string.PsaHideChatAlertTitle));
                        builder.setMessage(LocaleController.getString("PsaHideChatAlertText", R.string.PsaHideChatAlertText));
                        builder.setPositiveButton(LocaleController.getString("PsaHide", R.string.PsaHide), (dialog1, which) -> {
                            getMessagesController().hidePromoDialog();
                            hideActionMode(false);
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                    } else {
                        AlertsCreator.createClearOrDeleteDialogAlert(SavedChannelsActivity.this, false, chat, user, DialogObject.isEncryptedDialog(dialog.id), action == delete, (param) -> {
                            hideActionMode(false);
                            debugLastUpdateAction = 3;
                            int selectedDialogIndex = -1;
                            if (action == delete) {
                                setDialogsListFrozen(true);
                                if (frozenChatsList != null) {
                                    for (int i = 0; i < frozenChatsList.size(); i++) {
                                        if (frozenChatsList.get(i).id == selectedDialog) {
                                            selectedDialogIndex = i;
                                            break;
                                        }
                                    }
                                }
                            }

                            getUndoView().showWithAction(selectedDialog, UndoView.ACTION_DELETE, () -> performDeleteDialogAction(action, selectedDialog, chat, isBot, param));

                            ArrayList<TLRPC.Chat> currentChats = new ArrayList<>(getChatsArray(currentAccount, dialogsType, 0, false));
                            int currentDialogIndex = -1;
                            for (int i = 0; i < currentChats.size(); i++) {
                                if (currentChats.get(i).id == selectedDialog) {
                                    currentDialogIndex = i;
                                    break;
                                }
                            }

                            if (action == delete) {
                                if (selectedDialogIndex >= 0 && currentDialogIndex < 0 && frozenChatsList != null) {
                                    frozenChatsList.remove(selectedDialogIndex);
                                    dialogsItemAnimator.prepareForRemove();
                                    dialogsAdapter.notifyItemRemoved(selectedDialogIndex);
                                    dialogRemoveFinished = 2;
                                } else {
                                    setDialogsListFrozen(false);
                                }
                            }
                        });
                    }
                    return;
                } else {
                    if (getMessagesController().isPromoDialog(selectedDialog, true)) {
                        getMessagesController().hidePromoDialog();
                    } else {
                        performDeleteDialogAction(action, selectedDialog, chat, isBot, false);
                    }
                }
            }
        }

        if (scrollToTop) {
            if (initialDialogsType != 10) {
                hideFloatingButton(false);
            }
            scrollToTop();
        }
        hideActionMode(action != delete);
    }

    private void performDeleteDialogAction(int action, long selectedDialog, TLRPC.Chat chat, boolean isBot, boolean revoke) {
        if (chat != null) {
            if (ChatObject.isNotInChat(chat)) {
                getMessagesController().deleteDialog(selectedDialog, 0, revoke);
            } else {
                TLRPC.User currentUser = getMessagesController().getUser(getUserConfig().getClientUserId());
                getMessagesController().deleteParticipantFromChat((int) -selectedDialog, currentUser, null, null, revoke, false);
            }
        } else {
            getMessagesController().deleteDialog(selectedDialog, 0, revoke);
            if (isBot) {
                getMessagesController().blockPeer((int) selectedDialog);
            }
        }
        if (AndroidUtilities.isTablet()) {
            getNotificationCenter().postNotificationName(NotificationCenter.closeChats, selectedDialog);
        }
        getMessagesController().checkIfFolderEmpty(0);
    }

    private void pinDialog(long selectedDialog, boolean pin, MessagesController.DialogFilter filter, int minPinnedNum, boolean animated) {

        int selectedDialogIndex = -1;
        int currentDialogIndex = -1;

        int scrollToPosition = dialogsType == 0 && hasHiddenArchive() ? 1 : 0;
        int currentPosition = layoutManager.findFirstVisibleItemPosition();

        if (filter != null) {
            int index = filter.pinnedDialogs.get(selectedDialog, Integer.MIN_VALUE);
            if (!pin && index == Integer.MIN_VALUE) {
                return;
            }

        }

        debugLastUpdateAction = pin ? 4 : 5;
        boolean needScroll = false;
        if (currentPosition > scrollToPosition || !animated) {
            needScroll = true;
        } else {
            setDialogsListFrozen(true);
            if (frozenChatsList != null) {
                for (int i = 0; i < frozenChatsList.size(); i++) {
                    if (frozenChatsList.get(i).id == selectedDialog) {
                        selectedDialogIndex = i;
                        break;
                    }
                }
            }
        }

        boolean updated;
        if (filter != null) {
            if (pin) {
                filter.pinnedDialogs.put(selectedDialog, minPinnedNum);
            } else {
                filter.pinnedDialogs.delete(selectedDialog);
            }

            if (animated) {
                getMessagesController().onFilterUpdate(filter);
            }
            updated = true;
        } else {
            updated = getMessagesController().pinDialog(selectedDialog, pin, null, -1);
        }


        if (updated) {
            if (needScroll) {
                if (initialDialogsType != 10) {
                    hideFloatingButton(false);
                }
                scrollToTop();
            } else {
                ArrayList<TLRPC.Chat> currentChats = getChatsArray(currentAccount, dialogsType, 0, false);
                for (int i = 0; i < currentChats.size(); i++) {
                    if (currentChats.get(i).id == selectedDialog) {
                        currentDialogIndex = i;
                        break;
                    }
                }
            }
        }

        if (!needScroll) {
            boolean animate = false;
            if (selectedDialogIndex >= 0) {
                if (frozenChatsList != null && currentDialogIndex >= 0 && selectedDialogIndex != currentDialogIndex) {
                    frozenChatsList.add(currentDialogIndex, frozenChatsList.remove(selectedDialogIndex));
                    dialogsItemAnimator.prepareForRemove();
                    dialogsAdapter.notifyItemRemoved(selectedDialogIndex);
                    dialogsAdapter.notifyItemInserted(currentDialogIndex);
                    dialogRemoveFinished = 2;
                    dialogInsertFinished = 2;

                    layoutManager.scrollToPositionWithOffset(dialogsType == 0 && hasHiddenArchive() ? 1 : 0, (int) actionBar.getTranslationY());

                    animate = true;
                } else if (currentDialogIndex >= 0 && selectedDialogIndex == currentDialogIndex) {
                    animate = true;
                    AndroidUtilities.runOnUIThread(() -> setDialogsListFrozen(false), 200);
                }
            }
            if (!animate) {
                setDialogsListFrozen(false);
            }
        }
    }

    private void scrollToTop() {
        int scrollDistance = layoutManager.findFirstVisibleItemPosition() * AndroidUtilities.dp(SharedConfig.useThreeLinesLayout ? 78 : 72);
        int position = dialogsType == 0 && hasHiddenArchive() ? 1 : 0;
//        if (animator != null) {
//            animator.endAnimations();
//        }
        if (scrollDistance >= listView.getMeasuredHeight() * 1.2f) {
            scrollHelper.setScrollDirection(RecyclerAnimationScrollHelper.SCROLL_DIRECTION_UP);
            scrollHelper.scrollToPosition(position, 0, false, true);
            resetScroll();
        } else {
            listView.smoothScrollToPosition(position);
        }
    }

    private void updateCounters(boolean hide) {
        int canDeleteCount = 0;
        canDeletePsaSelected = false;
        if (hide) {
            return;
        }
        int count = selectedDialogs.size();
        for (int a = 0; a < count; a++) {
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialogs.get(a));
            if (dialog == null) {
                continue;
            }

            if (DialogObject.isChannel(dialog)) {
                if (getMessagesController().isPromoDialog(dialog.id, true)) {
                    if (getMessagesController().promoDialogType == MessagesController.PROMO_TYPE_PSA) {
                        canDeleteCount++;
                        canDeletePsaSelected = true;
                    }
                } else {
                    canDeleteCount++;
                }
            } else {
                canDeleteCount++;
            }
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            TransitionSet transition = new TransitionSet();
//            transition.addTransition(new Visibility() {
//                @Override
//                public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
//                    AnimatorSet set = new AnimatorSet();
//                    set.playTogether(
//                            ObjectAnimator.ofFloat(view, View.ALPHA, 0, 1f),
//                            ObjectAnimator.ofFloat(view, View.SCALE_X, 0.5f, 1f),
//                            ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.5f, 1f)
//                    );
//                    set.setInterpolator(CubicBezierInterpolator.DEFAULT);
//                    return set;
//                }
//
//                @Override
//                public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
//                    AnimatorSet set = new AnimatorSet();
//                    set.playTogether(
//                            ObjectAnimator.ofFloat(view, View.ALPHA, view.getAlpha(), 0f),
//                            ObjectAnimator.ofFloat(view, View.SCALE_X, view.getScaleX(), 0.5f),
//                            ObjectAnimator.ofFloat(view, View.SCALE_Y, view.getScaleX(), 0.5f)
//                    );
//                    set.setInterpolator(CubicBezierInterpolator.DEFAULT);
//                    return set;
//                }
//            }).addTransition(new ChangeBounds());
//            transition.setOrdering(TransitionSet.ORDERING_TOGETHER);
//            transition.setInterpolator(CubicBezierInterpolator.EASE_OUT);
//            transition.setDuration(150);
//            TransitionManager.beginDelayedTransition(actionBar.getActionMode(), transition);
//        }
        if (canDeleteCount != count) {
            deleteItem.setVisibility(View.GONE);
        } else {
            deleteItem.setVisibility(View.VISIBLE);
        }
    }

    private boolean validateSlowModeDialog(long dialogId) {
        if (messagesCount <= 1 && (commentView == null || commentView.getVisibility() != View.VISIBLE || TextUtils.isEmpty(commentView.getFieldText()))) {
            return true;
        }
        if (!DialogObject.isChatDialog(dialogId)) {
            return true;
        }
        TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
        if (chat != null && !ChatObject.hasAdminRights(chat) && chat.slowmode_enabled) {
            AlertsCreator.showSimpleAlert(SavedChannelsActivity.this, LocaleController.getString("Slowmode", R.string.Slowmode), LocaleController.getString("SlowmodeSendError", R.string.SlowmodeSendError));
            return false;
        }
        return true;
    }

    private void showOrUpdateActionMode(long dialogId, View cell) {
        addOrRemoveSelectedDialog(dialogId, cell);
        boolean updateAnimated = false;
        if (actionBar.isActionModeShowed()) {
            if (selectedDialogs.isEmpty()) {
                hideActionMode(true);
                return;
            }
            updateAnimated = true;
        } else {
            createActionMode(null);
            AndroidUtilities.hideKeyboard(fragmentView.findFocus());
            actionBar.setActionModeOverrideColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            actionBar.showActionMode();
            resetScroll();

            AnimatorSet animatorSet = new AnimatorSet();
            ArrayList<Animator> animators = new ArrayList<>();
            for (int a = 0; a < actionModeViews.size(); a++) {
                View view = actionModeViews.get(a);
                view.setPivotY(ActionBar.getCurrentActionBarHeight() / 2);
                AndroidUtilities.clearDrawableAnimation(view);
                animators.add(ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.1f, 1.0f));
            }
            animatorSet.playTogether(animators);
            animatorSet.setDuration(200);
            animatorSet.start();

            if (actionBarColorAnimator != null) {
                actionBarColorAnimator.cancel();
            }
            actionBarColorAnimator = ValueAnimator.ofFloat(progressToActionMode, 1f);
            actionBarColorAnimator.addUpdateListener(valueAnimator -> {
                progressToActionMode = (float) valueAnimator.getAnimatedValue();
                for (int i = 0; i < actionBar.getChildCount(); i++) {
                    if (actionBar.getChildAt(i).getVisibility() == View.VISIBLE && actionBar.getChildAt(i) != actionBar.getActionMode() && actionBar.getChildAt(i) != actionBar.getBackButton()) {
                        actionBar.getChildAt(i).setAlpha(1f - progressToActionMode);
                    }
                }
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
            });
            actionBarColorAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
            actionBarColorAnimator.setDuration(200);
            actionBarColorAnimator.start();
        }
        updateCounters(false);
        selectedDialogsCountTextView.setNumber(selectedDialogs.size(), updateAnimated);
    }

    public UndoView getUndoView() {
        if (undoView[0].getVisibility() == View.VISIBLE) {
            UndoView old = undoView[0];
            undoView[0] = undoView[1];
            undoView[1] = old;
            old.hide(true, 2);
            ContentView contentView = (ContentView) fragmentView;
            contentView.removeView(undoView[0]);
            contentView.addView(undoView[0]);
        }
        return undoView[0];
    }

    private AnimatorSet doneItemAnimator;
    private void showDoneItem(boolean show) {
        if (doneItem == null) {
            return;
        }
        if (doneItemAnimator != null) {
            doneItemAnimator.cancel();
            doneItemAnimator = null;
        }
        doneItemAnimator = new AnimatorSet();
        doneItemAnimator.setDuration(180);
        if (show) {
            doneItem.setVisibility(View.VISIBLE);
        } else {
            doneItem.setSelected(false);
            Drawable background = doneItem.getBackground();
            if (background != null) {
                background.setState(StateSet.NOTHING);
                background.jumpToCurrentState();
            }
        }
        ArrayList<Animator> arrayList = new ArrayList<>();
        arrayList.add(ObjectAnimator.ofFloat(doneItem, View.ALPHA, show ? 1.0f : 0.0f));
        doneItemAnimator.playTogether(arrayList);
        doneItemAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                doneItemAnimator = null;
                if (doneItem != null) {
                    doneItem.setVisibility(View.GONE);
                }
            }
        });
        doneItemAnimator.start();
    }

    private void updateSelectedCount() {
        if (commentView != null) {
            if (selectedDialogs.isEmpty()) {
                if (initialDialogsType == 3 && selectAlertString == null) {
                    actionBar.setTitle(LocaleController.getString("ForwardTo", R.string.ForwardTo));
                } else {
                    actionBar.setTitle(LocaleController.getString("SelectChat", R.string.SelectChat));
                }
                if (commentView.getTag() != null) {
                    commentView.hidePopup(false);
                    commentView.closeKeyboard();
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(ObjectAnimator.ofFloat(commentView, View.TRANSLATION_Y, 0, commentView.getMeasuredHeight()));
                    animatorSet.setDuration(180);
                    animatorSet.setInterpolator(new DecelerateInterpolator());
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            commentView.setVisibility(View.GONE);
                        }
                    });
                    animatorSet.start();
                    commentView.setTag(null);
                    fragmentView.requestLayout();
                }
            } else {
                if (commentView.getTag() == null) {
                    commentView.setFieldText("");
                    commentView.setVisibility(View.VISIBLE);
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(ObjectAnimator.ofFloat(commentView, View.TRANSLATION_Y, commentView.getMeasuredHeight(), 0));
                    animatorSet.setDuration(180);
                    animatorSet.setInterpolator(new DecelerateInterpolator());
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            commentView.setTag(2);
                            commentView.requestLayout();
                        }
                    });
                    animatorSet.start();
                    commentView.setTag(1);
                }
                actionBar.setTitle(LocaleController.formatPluralString("Recipient", selectedDialogs.size()));
            }
        } else if (initialDialogsType == 10) {
            hideFloatingButton(selectedDialogs.isEmpty());
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForPermissons(boolean alert) {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        ArrayList<String> permissons = new ArrayList<>();
        if (getUserConfig().syncContacts && askAboutContacts && activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            if (alert) {
                AlertDialog.Builder builder = AlertsCreator.createContactsPermissionDialog(activity, param -> {
                    askAboutContacts = param != 0;
                    MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askAboutContacts", askAboutContacts).commit();
                    askForPermissons(false);
                });
                showDialog(permissionDialog = builder.create());
                return;
            }
            permissons.add(Manifest.permission.READ_CONTACTS);
            permissons.add(Manifest.permission.WRITE_CONTACTS);
            permissons.add(Manifest.permission.GET_ACCOUNTS);
        }
        if ((Build.VERSION.SDK_INT <= 28 || BuildVars.NO_SCOPED_STORAGE) && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissons.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissons.isEmpty()) {
            if (askingForPermissions) {
                askingForPermissions = false;
            }
            return;
        }
        String[] items = permissons.toArray(new String[0]);
        try {
            activity.requestPermissions(items, 1);
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        super.onDialogDismiss(dialog);
        if (permissionDialog != null && dialog == permissionDialog && getParentActivity() != null) {
            askForPermissons(false);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!onlySelect && floatingButtonContainer != null) {
            floatingButtonContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    floatingButtonTranslation = floatingHidden ? AndroidUtilities.dp(100) : 0;
                    updateFloatingButtonOffset();
                    floatingButtonContainer.setClickable(!floatingHidden);
                    if (floatingButtonContainer != null) {
                        floatingButtonContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            for (int a = 0; a < permissions.length; a++) {
                if (grantResults.length <= a) {
                    continue;
                }
                switch (permissions[a]) {
                    case Manifest.permission.READ_CONTACTS:
                        if (grantResults[a] == PackageManager.PERMISSION_GRANTED) {
                            getContactsController().forceImportContacts();
                        } else {
                            MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askAboutContacts", askAboutContacts = false).commit();
                        }
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        if (grantResults[a] == PackageManager.PERMISSION_GRANTED) {
                            ImageLoader.getInstance().checkMediaPaths();
                        }
                        break;
                }
            }
            if (askingForPermissions) {
                askingForPermissions = false;
            }
        } else if (requestCode == 4) {
            boolean allGranted = true;
            for (int a = 0; a < grantResults.length; a++) {
                if (grantResults[a] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && FilesMigrationService.filesMigrationBottomSheet != null) {
                FilesMigrationService.filesMigrationBottomSheet.migrateOldFolder();
            }

        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload) {
            if (viewPage == null || dialogsListFrozen) {
                return;
            }

            if (viewPage.getVisibility() == View.VISIBLE) {
                int oldItemCount = dialogsAdapter.getCurrentCount();

                if (dialogsType == 0 && hasHiddenArchive() && listView.getChildCount() == 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) listView.getLayoutManager();
                    layoutManager.scrollToPositionWithOffset(1, 0);
                }

                if (dialogsAdapter.isDataSetChanged() || args.length > 0) {
                    dialogsAdapter.notifyDataSetChanged();
                    int newItemCount = dialogsAdapter.getItemCount();
                    if (newItemCount > oldItemCount && initialDialogsType != 11 && initialDialogsType != 12 && initialDialogsType != 13) {
                        recyclerItemsEnterAnimator.showItemsAnimated(oldItemCount);
                    }
                } else {
                    updateVisibleRows(MessagesController.UPDATE_MASK_NEW_MESSAGE);
                    int newItemCount = dialogsAdapter.getItemCount();
                    if (newItemCount > oldItemCount && initialDialogsType != 11 && initialDialogsType != 12 && initialDialogsType != 13) {
                        recyclerItemsEnterAnimator.showItemsAnimated(oldItemCount);
                    }
                }
                try {
                    listView.setEmptyView(progressView);
                } catch (Exception e) {
                    FileLog.e(e);
                }
                checkListLoad();
            }
        } else if (id == NotificationCenter.emojiLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.updateInterfaces) {
            Integer mask = (Integer) args[0];
            updateVisibleRows(mask);
            if (viewPage != null) {
                if ((mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                    dialogsAdapter.sortOnlineContacts(true);
                }
            }
        } else if (id == NotificationCenter.appDidLogout) {
            dialogsLoaded[currentAccount] = false;
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.contactsDidLoad) {
            if (viewPage == null || dialogsListFrozen) {
                return;
            }
            boolean updateVisibleRows = false;
            if (isDefaultDialogType() && getMessagesController().getDialogs(0).size() <= 10) {
                dialogsAdapter.notifyDataSetChanged();
            } else {
                updateVisibleRows = true;
            }
            if (updateVisibleRows) {
                updateVisibleRows(0);
            }
        } else if (id == NotificationCenter.openedChatChanged) {
            if (viewPage == null) {
                return;
            }
            if (isDefaultDialogType() && AndroidUtilities.isTablet()) {
                boolean close = (Boolean) args[1];
                long dialog_id = (Long) args[0];
                if (close) {
                    if (dialog_id == openedDialogId) {
                        openedDialogId = 0;
                    }
                } else {
                    openedDialogId = dialog_id;
                }
                dialogsAdapter.setOpenedDialogId(openedDialogId);
            }
            updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
        } else if (id == NotificationCenter.notificationsSettingsUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.messageReceivedByAck || id == NotificationCenter.messageReceivedByServer || id == NotificationCenter.messageSendError) {
            updateVisibleRows(MessagesController.UPDATE_MASK_SEND_STATE);

            if (id == NotificationCenter.messageReceivedByServer) {
                Integer msgId = (Integer) args[0];
                Integer newMsgId = (Integer) args[1];
                TLRPC.Message newMsgObj = (TLRPC.Message) args[2];
                RemoveAsReadMessages.load();
                RemoveAsReadMessages.messagesToRemoveAsRead.putIfAbsent("" + currentAccount, new HashMap<>());
                if (newMsgObj != null && RemoveAsReadMessages.messagesToRemoveAsRead.get("" + currentAccount).containsKey("" + newMsgObj.dialog_id)) {
                    for (RemoveAsReadMessages.RemoveAsReadMessage message : RemoveAsReadMessages.messagesToRemoveAsRead.get("" + currentAccount).get("" + newMsgObj.dialog_id)) {
                        if (message.getId() == msgId) {
                            message.setId(newMsgId);
                            break;
                        }
                    }
                }
                RemoveAsReadMessages.save();
            }
        } else if (id == NotificationCenter.replyMessagesDidLoad) {
            updateVisibleRows(MessagesController.UPDATE_MASK_MESSAGE_TEXT);
        } else if (id == NotificationCenter.didUpdateConnectionState) {
            int state = AccountInstance.getInstance(account).getConnectionsManager().getConnectionState();
            if (currentConnectionState != state) {
                currentConnectionState = state;
            }
        } else if (id == NotificationCenter.needDeleteDialog) {
            if (fragmentView == null || isPaused) {
                return;
            }
            long dialogId = (Long) args[0];
            TLRPC.User user = (TLRPC.User) args[1];
            TLRPC.Chat chat = (TLRPC.Chat) args[2];
            boolean revoke = (Boolean) args[3];
            Runnable deleteRunnable = () -> {
                if (chat != null) {
                    if (ChatObject.isNotInChat(chat)) {
                        getMessagesController().deleteDialog(dialogId, 0, revoke);
                    } else {
                        getMessagesController().deleteParticipantFromChat(-dialogId, getMessagesController().getUser(getUserConfig().getClientUserId()), null, null, revoke, revoke);
                    }
                } else {
                    getMessagesController().deleteDialog(dialogId, 0, revoke);
                    if (user != null && user.bot) {
                        getMessagesController().blockPeer(user.id);
                    }
                }
                getMessagesController().checkIfFolderEmpty(0);
            };
            if (undoView[0] != null) {
                getUndoView().showWithAction(dialogId, UndoView.ACTION_DELETE, deleteRunnable);
            } else {
                deleteRunnable.run();
            }
        } else if (id == NotificationCenter.newSuggestionsAvailable) {
            showNextSupportedSuggestion();
        } else if (id == NotificationCenter.didClearDatabase) {
            if (viewPage != null) {
                dialogsAdapter.didDatabaseCleared();
            }
        } else if (id == NotificationCenter.messagesDidLoad) {
            if (SharedConfig.showUpdates && SharedConfig.fakePasscodeActivatedIndex == -1 && (Long)args[0] == PARTISAN_TG_CHANNEL_ID) {
                if (!partisanTgChannelLastMessageLoaded) {
                    partisanTgChannelLastMessageLoaded = true;
                    getMessagesController().loadMessages(PARTISAN_TG_CHANNEL_ID, 0, false, 50, 0, 0, false, 0, classGuid, 2, (int)args[5], 0, 0, 0, 1);
                } else {
                    appUpdatesChecked = true;
                    getNotificationCenter().removeObserver(this, NotificationCenter.messagesDidLoad);
                    processPartisanTgChannelMessages((ArrayList<MessageObject>)args[2]);
                }
            }
        } else if (id == NotificationCenter.onDatabaseMigration) {
            boolean startMigration = (boolean) args[0];
            if (fragmentView != null) {
                if (startMigration) {
                    if (databaseMigrationHint == null) {
                        databaseMigrationHint = new DatabaseMigrationHint(fragmentView.getContext(), currentAccount);
                        databaseMigrationHint.setAlpha(0f);
                        ((ContentView) fragmentView).addView(databaseMigrationHint);
                        databaseMigrationHint.animate().alpha(1).setDuration(300).setStartDelay(1000).start();
                    }
                    databaseMigrationHint.setTag(1);
                } else {
                    if (databaseMigrationHint != null && databaseMigrationHint.getTag() != null) {
                        View localView = databaseMigrationHint;
                        localView.animate().setListener(null).cancel();
                        localView.animate().setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (localView.getParent() != null) {
                                    ((ViewGroup) localView.getParent()).removeView(localView);
                                }
                                databaseMigrationHint = null;
                            }
                        }).alpha(0f).setStartDelay(0).setDuration(150).start();
                        databaseMigrationHint.setTag(null);
                    }
                }
            }
        }
    }

    View databaseMigrationHint;

    private void processPartisanTgChannelMessages(ArrayList<MessageObject> messages) {
        int maxVersionMajor = 0;
        int maxVersionMinor = 0;
        int maxVersionPatch = 0;
        int maxVersionPostId = -1;
        Pattern regex = Pattern.compile("PTelegram-v(\\d+)_(\\d+)_(\\d+)\\.apk");
        for (MessageObject message : messages) {
            TLRPC.Document doc = message.getDocument();
            if (doc == null) {
                continue;
            }
            for (TLRPC.DocumentAttribute attribute : doc.attributes) {
                if (attribute instanceof TLRPC.TL_documentAttributeFilename) {
                    Matcher matcher = regex.matcher(attribute.file_name);
                    if (matcher.find() && matcher.groupCount() == 3) {
                        int major = Integer.parseInt(matcher.group(1));
                        int minor = Integer.parseInt(matcher.group(2));
                        int patch = Integer.parseInt(matcher.group(3));
                        if (versionGreater(major, minor, patch, maxVersionMajor, maxVersionMinor, maxVersionPatch)) {
                            maxVersionMajor = major;
                            maxVersionMinor = minor;
                            maxVersionPatch = patch;
                            maxVersionPostId = message.getId();
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
                    showUpdateDialog(maxVersionMajor, maxVersionMinor, maxVersionPatch, maxVersionPostId);
                }
            } else {
                showUpdateDialog(maxVersionMajor, maxVersionMinor, maxVersionPatch, maxVersionPostId);
            }
        }
    }

    private void showUpdateDialog(int major, int minor, int patch, int postId) {
        if (!SharedConfig.showUpdates || SharedConfig.fakePasscodeActivatedIndex != -1) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
        builder.setMessage(AndroidUtilities.replaceTags(LocaleController.formatString("NewVersionAlert", R.string.NewVersionAlert, major, minor, patch)));
        builder.setNeutralButton(LocaleController.getString("DoNotShowAgain", R.string.DoNotShowAgain), (dialog, which) -> {
            SharedConfig.toggleShowUpdates();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, which) -> {
            SharedConfig.setVersionIgnored(major, minor, patch);
        });
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            SharedConfig.setVersionIgnored(major, minor, patch);
            Bundle args = new Bundle();
            args.putLong("chat_id", -PARTISAN_TG_CHANNEL_ID);
            args.putInt("message_id", postId);
            presentFragment(new ChatActivity(args));
        });
        showDialog(builder.create());
    }

    private boolean versionGreater(int major, int minor, int patch, int otherMajor, int otherMinor, int otherPatch) {
        return major > otherMajor || major == otherMajor && minor > otherMinor
                || major == otherMajor && minor == otherMinor && patch > otherPatch;
    }

    private String showingSuggestion;
    private void showNextSupportedSuggestion() {
        if (showingSuggestion != null) {
            return;
        }
        for (String suggestion : getMessagesController().pendingSuggestions) {
            if (showSuggestion(suggestion)) {
                showingSuggestion = suggestion;
                return;
            }
        }
    }

    private void onSuggestionDismiss() {
        if (showingSuggestion == null) {
            return;
        }
        getMessagesController().removeSuggestion(0, showingSuggestion);
        showingSuggestion = null;
        showNextSupportedSuggestion();
    }

    private boolean showSuggestion(String suggestion) {
        if ("AUTOARCHIVE_POPULAR".equals(suggestion)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("HideNewChatsAlertTitle", R.string.HideNewChatsAlertTitle));
            builder.setMessage(AndroidUtilities.replaceTags(LocaleController.getString("HideNewChatsAlertText", R.string.HideNewChatsAlertText)));
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            builder.setPositiveButton(LocaleController.getString("GoToSettings", R.string.GoToSettings), (dialog, which) -> {
                presentFragment(new PrivacySettingsActivity());
                AndroidUtilities.scrollToFragmentRow(parentLayout, "newChatsRow");
            });
            showDialog(builder.create(), dialog -> onSuggestionDismiss());
            return true;
        }
        return false;
    }

    private void setDialogsListFrozen(boolean frozen, boolean notify) {
        if (dialogsListFrozen == frozen) {
            return;
        }
        if (frozen) {
            frozenChatsList = new ArrayList<>(getChatsArray(currentAccount, dialogsType, 0, false));
        } else {
            frozenChatsList = null;
        }
        dialogsListFrozen = frozen;
        dialogsAdapter.setDialogsListFrozen(frozen);
        if (!frozen && notify) {
            dialogsAdapter.notifyDataSetChanged();
        }
    }

    private void setDialogsListFrozen(boolean frozen) {
        setDialogsListFrozen(frozen, true);
    }

    @NonNull
    public synchronized ArrayList<TLRPC.Chat> getChatsArray(int currentAccount, int dialogsType, int folderId, boolean frozen) {
        Set<Long> ids = chats.stream().map(c -> c.id).collect(Collectors.toSet());
        Set<String> names = new HashSet<>(UserConfig.getInstance(currentAccount).savedChannels);
        Set<String> existedNames = new HashSet<>();
        MessagesController controller = MessagesController.getInstance(currentAccount);
        for (String name : names) {
            TLObject obj = controller.getUserOrChat(name);
            if (obj instanceof TLRPC.Chat) {
                TLRPC.Chat chat = (TLRPC.Chat)obj;
                if (!ids.contains(chat.id)) {
                    chats.add(chat);
                }
                existedNames.add(name);
            }
        }
        names.removeAll(existedNames);
        if (!names.isEmpty() && !chatLoading) {
            chatLoading = true;
            String username = names.iterator().next();
            Utilities.globalQueue.postRunnable(() -> resolveUsername(username), 1000);
        }
        return chats;
    }

    public void setSideMenu(RecyclerView recyclerView) {
        sideMenu = recyclerView;
        sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
        sideMenu.setGlowColor(Theme.getColor(Theme.key_chats_menuBackground));
    }

    private void hideFloatingButton(boolean hide) {
        if (floatingHidden == hide || hide && floatingForceVisible) {
            return;
        }
        floatingHidden = hide;
        AnimatorSet animatorSet = new AnimatorSet();
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(floatingButtonHideProgress,floatingHidden ? 1f : 0f);
        valueAnimator.addUpdateListener(animation -> {
            floatingButtonHideProgress = (float) animation.getAnimatedValue();
            floatingButtonTranslation = AndroidUtilities.dp(100) * floatingButtonHideProgress;
            updateFloatingButtonOffset();
        });
        animatorSet.playTogether(valueAnimator);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(floatingInterpolator);
        floatingButtonContainer.setClickable(!hide);
        animatorSet.start();
    }

    private void updateDialogIndices() {
        if (viewPage == null) {
            return;
        }
        if (viewPage.getVisibility() == View.VISIBLE) {
            ArrayList<TLRPC.Chat> chats = getChatsArray(currentAccount, dialogsType, 0, false);
            int count = listView.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = listView.getChildAt(a);
                if (child instanceof SavedChannelCell) {
                    SavedChannelCell dialogCell = (SavedChannelCell) child;
                    TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogCell.getDialogId());
                    if (dialog == null) {
                        continue;
                    }
                    int index = chats.indexOf(dialog);
                    if (index < 0) {
                        continue;
                    }
                    dialogCell.setDialogIndex(index);
                }
            }
        }
    }

    private void updateVisibleRows(int mask) {
        updateVisibleRows(mask, true);
    }
    private void updateVisibleRows(int mask, boolean animated) {
        if ((dialogsListFrozen && (mask & MessagesController.UPDATE_MASK_REORDER) == 0) || isPaused) {
            return;
        }
        if (listView != null) {
            int count = listView.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = listView.getChildAt(a);
                if (child instanceof SavedChannelCell) {
                    SavedChannelCell cell = (SavedChannelCell) child;
                    if ((mask & MessagesController.UPDATE_MASK_REORDER) != 0) {
                        cell.onReorderStateChanged(actionBar.isActionModeShowed(), true);
                        if (dialogsListFrozen) {
                            continue;
                        }
                    }
                    if ((mask & MessagesController.UPDATE_MASK_CHECK) != 0) {
                        cell.setChecked(false, (mask & MessagesController.UPDATE_MASK_CHAT) != 0);
                    } else {
                        if ((mask & MessagesController.UPDATE_MASK_NEW_MESSAGE) != 0) {
                            cell.checkCurrentDialogIndex(dialogsListFrozen);
                            if (isDefaultDialogType() && AndroidUtilities.isTablet()) {
                                cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                            }
                        } else if ((mask & MessagesController.UPDATE_MASK_SELECT_DIALOG) != 0) {
                            if (isDefaultDialogType() && AndroidUtilities.isTablet()) {
                                cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                            }
                        } else {
                            cell.update(mask, animated);
                        }
                        if (selectedDialogs != null) {
                            cell.setChecked(selectedDialogs.contains(cell.getDialogId()), false);
                        }
                    }
                }


                if (child instanceof UserCell) {
                    ((UserCell) child).update(mask);
                } else if (child instanceof ProfileSearchCell) {
                    ProfileSearchCell cell = (ProfileSearchCell) child;
                    cell.update(mask);
                    if (selectedDialogs != null) {
                        cell.setChecked(selectedDialogs.contains(cell.getDialogId()), false);
                    }
                }
                if (dialogsListFrozen) {
                    continue;
                }
                if (child instanceof RecyclerListView) {
                    RecyclerListView innerListView = (RecyclerListView) child;
                    int count2 = innerListView.getChildCount();
                    for (int b = 0; b < count2; b++) {
                        View child2 = innerListView.getChildAt(b);
                        if (child2 instanceof HintDialogCell) {
                            ((HintDialogCell) child2).update(mask);
                        }
                    }
                }
            }
        }
    }

    public void setDelegate(SavedChannelsActivityDelegate SavedChannelsActivityDelegate) {
        delegate = SavedChannelsActivityDelegate;
    }

    public boolean isMainDialogList() {
        return delegate == null;
    }

    private void didSelectResult(final long dialogId, boolean useAlert, final boolean param) {
        if (addToGroupAlertString == null && checkCanWrite) {
            if (DialogObject.isChatDialog(dialogId)) {
                TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                if (ChatObject.isChannel(chat) && !chat.megagroup && ((cantSendToChannels || !ChatObject.isCanWriteToChannel(-dialogId, currentAccount)) || hasPoll == 2)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("SendMessageTitle", R.string.SendMessageTitle));
                    if (hasPoll == 2) {
                        builder.setMessage(LocaleController.getString("PublicPollCantForward", R.string.PublicPollCantForward));
                    } else {
                        builder.setMessage(LocaleController.getString("ChannelCantSendMessage", R.string.ChannelCantSendMessage));
                    }
                    builder.setNegativeButton(LocaleController.getString("OK", R.string.OK), null);
                    showDialog(builder.create());
                    return;
                }
            } else if (DialogObject.isEncryptedDialog(dialogId) && (hasPoll != 0 || hasInvoice)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("SendMessageTitle", R.string.SendMessageTitle));
                if (hasPoll != 0) {
                    builder.setMessage(LocaleController.getString("PollCantForwardSecretChat", R.string.PollCantForwardSecretChat));
                } else {
                    builder.setMessage(LocaleController.getString("InvoiceCantForwardSecretChat", R.string.InvoiceCantForwardSecretChat));
                }
                builder.setNegativeButton(LocaleController.getString("OK", R.string.OK), null);
                showDialog(builder.create());
                return;
            }
        }
        if (initialDialogsType == 11 || initialDialogsType == 12 || initialDialogsType == 13) {
            if (checkingImportDialog) {
                return;
            }
            TLRPC.User user;
            TLRPC.Chat chat;
            if (DialogObject.isUserDialog(dialogId)) {
                user = getMessagesController().getUser(dialogId);
                chat = null;
                if (!user.mutual_contact) {
                    getUndoView().showWithAction(dialogId, UndoView.ACTION_IMPORT_NOT_MUTUAL, null);
                    return;
                }
            } else {
                user = null;
                chat = getMessagesController().getChat(-dialogId);
                if (!ChatObject.hasAdminRights(chat) || !ChatObject.canChangeChatInfo(chat)) {
                    getUndoView().showWithAction(dialogId, UndoView.ACTION_IMPORT_GROUP_NOT_ADMIN, null);
                    return;
                }
            }
            final AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
            TLRPC.TL_messages_checkHistoryImportPeer req = new TLRPC.TL_messages_checkHistoryImportPeer();
            req.peer = getMessagesController().getInputPeer(dialogId);
            getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                try {
                    progressDialog.dismiss();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                checkingImportDialog = false;
                if (response != null) {
                    TLRPC.TL_messages_checkedHistoryImportPeer res = (TLRPC.TL_messages_checkedHistoryImportPeer) response;
                    AlertsCreator.createImportDialogAlert(this, arguments.getString("importTitle"), res.confirm_text, user, chat, () -> {
                        setDialogsListFrozen(true);
                        ArrayList<Long> dids = new ArrayList<>();
                        dids.add(dialogId);
                        delegate.didSelectDialogs(SavedChannelsActivity.this, dids, null, param);
                    });
                } else {
                    AlertsCreator.processError(currentAccount, error, this, req);
                    getNotificationCenter().postNotificationName(NotificationCenter.historyImportProgressChanged, dialogId, req, error);
                }
            }));
            try {
                progressDialog.showDelayed(300);
            } catch (Exception ignore) {

            }
        } else if (useAlert && (selectAlertString != null && selectAlertStringGroup != null || addToGroupAlertString != null)) {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            String title;
            String message;
            String buttonText;
            if (DialogObject.isEncryptedDialog(dialogId)) {
                TLRPC.EncryptedChat chat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
                TLRPC.User user = getMessagesController().getUser(chat.user_id);
                if (user == null) {
                    return;
                }
                title = LocaleController.getString("SendMessageTitle", R.string.SendMessageTitle);
                message = LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user));
                buttonText = LocaleController.getString("Send", R.string.Send);
            } else if (DialogObject.isUserDialog(dialogId)) {
                if (dialogId == getUserConfig().getClientUserId()) {
                    title = LocaleController.getString("SendMessageTitle", R.string.SendMessageTitle);
                    message = LocaleController.formatStringSimple(selectAlertStringGroup, LocaleController.getString("SavedMessages", R.string.SavedMessages));
                    buttonText = LocaleController.getString("Send", R.string.Send);
                } else {
                    TLRPC.User user = getMessagesController().getUser(dialogId);
                    if (user == null || selectAlertString == null) {
                        return;
                    }
                    title = LocaleController.getString("SendMessageTitle", R.string.SendMessageTitle);
                    message = LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user, currentAccount));
                    buttonText = LocaleController.getString("Send", R.string.Send);
                }
            } else {
                TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                if (chat == null) {
                    return;
                }
                String chatTitle = UserConfig.getChatTitleOverride(currentAccount, (int)chat.id);
                if (chatTitle == null) {
                    chatTitle = chat.title;
                }
                if (addToGroupAlertString != null) {
                    title = LocaleController.getString("AddToTheGroupAlertTitle", R.string.AddToTheGroupAlertTitle);
                    message = LocaleController.formatStringSimple(addToGroupAlertString, chatTitle);
                    buttonText = LocaleController.getString("Add", R.string.Add);
                } else {
                    title = LocaleController.getString("SendMessageTitle", R.string.SendMessageTitle);
                    message = LocaleController.formatStringSimple(selectAlertStringGroup, chatTitle);
                    buttonText = LocaleController.getString("Send", R.string.Send);
                }
            }
            builder.setTitle(title);
            builder.setMessage(AndroidUtilities.replaceTags(message));
            builder.setPositiveButton(buttonText, (dialogInterface, i) -> didSelectResult(dialogId, false, false));
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        } else {
            if (delegate != null) {
                ArrayList<Long> dids = new ArrayList<>();
                dids.add(dialogId);
                delegate.didSelectDialogs(SavedChannelsActivity.this, dids, null, param);
                if (resetDelegate) {
                    delegate = null;
                }
            } else {
                finishFragment();
            }
        }
    }

    public RLottieImageView getFloatingButton() {
        return floatingButton;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ThemeDescription.ThemeDescriptionDelegate cellDelegate = () -> {
            if (listView != null) {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = listView.getChildAt(a);
                    if (child instanceof ProfileSearchCell) {
                        ((ProfileSearchCell) child).update(0);
                    } else if (child instanceof SavedChannelCell) {
                        ((SavedChannelCell) child).update(0);
                    } else if (child instanceof UserCell) {
                        ((UserCell) child).update(0);
                    }
                }
            }
            if (sideMenu != null) {
                View child = sideMenu.getChildAt(0);
                if (child instanceof DrawerProfileCell) {
                    DrawerProfileCell profileCell = (DrawerProfileCell) child;
                    profileCell.applyBackground(true);
                    profileCell.updateColors();
                }
            }
            if (pullForegroundDrawable != null) {
                pullForegroundDrawable.updateColors();
            }
            if (actionBar != null) {
                actionBar.setPopupBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), true);
                actionBar.setPopupItemsColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem), false, true);
                actionBar.setPopupItemsColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon), true, true);
                actionBar.setPopupItemsSelectorColor(Theme.getColor(Theme.key_dialogButtonSelector), true);
            }

            if (doneItem != null) {
                doneItem.setIconColor(Theme.getColor(Theme.key_actionBarDefaultIcon));
            }
            if (commentView != null) {
                commentView.updateColors();
            }
        };

        ArrayList<ThemeDescription> arrayList = new ArrayList<>();

        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));

        if (movingView != null) {
            arrayList.add(new ThemeDescription(movingView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        }

        if (doneItem != null) {
            arrayList.add(new ThemeDescription(doneItem, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_actionBarDefaultSelector));
        }

        if (onlySelect) {
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        }
        arrayList.add(new ThemeDescription(fragmentView, 0, null, actionBarDefaultPaint, null, null, Theme.key_actionBarDefault));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, cellDelegate, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, new Drawable[]{Theme.dialogs_holidayDrawable}, null, Theme.key_actionBarDefaultTitle));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder));

        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarActionModeDefaultIcon));
        //arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_BACKGROUND, null, null, null, null, Theme.key_actionBarActionModeDefault));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_TOPBACKGROUND, null, null, null, null, Theme.key_actionBarActionModeDefaultTop));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarActionModeDefaultSelector));
        arrayList.add(new ThemeDescription(selectedDialogsCountTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_actionBarActionModeDefaultIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_actionBarDefaultSubmenuBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_actionBarDefaultSubmenuItem));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_actionBarDefaultSubmenuItemIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_dialogButtonSelector));

        arrayList.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionIcon));
        arrayList.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionBackground));
        arrayList.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionPressedBackground));

        if (listView != null) {
            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class, ProfileSearchCell.class}, null, Theme.avatarDrawables, null, Theme.key_avatar_text));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_countPaint, null, null, Theme.key_chats_unreadCounter));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_countGrayPaint, null, null, Theme.key_chats_unreadCounterMuted));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_countTextPaint, null, null, Theme.key_chats_unreadCounterText));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_lockDrawable}, null, Theme.key_chats_secretIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_groupDrawable, Theme.dialogs_broadcastDrawable, Theme.dialogs_botDrawable}, null, Theme.key_chats_nameIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_scamDrawable, Theme.dialogs_fakeDrawable}, null, Theme.key_chats_draft));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, new Drawable[]{Theme.dialogs_pinnedDrawable, Theme.dialogs_reorderDrawable}, null, Theme.key_chats_pinnedIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class, ProfileSearchCell.class}, null, new Paint[]{Theme.dialogs_namePaint[0], Theme.dialogs_namePaint[1], Theme.dialogs_searchNamePaint}, null, null, Theme.key_chats_name));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class, ProfileSearchCell.class}, null, new Paint[]{Theme.dialogs_nameEncryptedPaint[0], Theme.dialogs_nameEncryptedPaint[1], Theme.dialogs_searchNameEncryptedPaint}, null, null, Theme.key_chats_secretName));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_messagePaint[1], null, null, Theme.key_chats_message_threeLines));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_messagePaint[0], null, null, Theme.key_chats_message));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_messageNamePaint, null, null, Theme.key_chats_nameMessage_threeLines));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, null, null, Theme.key_chats_draft));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, Theme.dialogs_messagePrintingPaint, null, null, Theme.key_chats_actionMessage));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_timePaint, null, null, Theme.key_chats_date));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_pinnedPaint, null, null, Theme.key_chats_pinnedOverlay));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_tabletSeletedPaint, null, null, Theme.key_chats_tabletSelectedOverlay));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, new Drawable[]{Theme.dialogs_checkDrawable}, null, Theme.key_chats_sentCheck));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, new Drawable[]{Theme.dialogs_checkReadDrawable, Theme.dialogs_halfCheckDrawable}, null, Theme.key_chats_sentReadCheck));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, new Drawable[]{Theme.dialogs_clockDrawable}, null, Theme.key_chats_sentClock));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, Theme.dialogs_errorPaint, null, null, Theme.key_chats_sentError));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, new Drawable[]{Theme.dialogs_errorDrawable}, null, Theme.key_chats_sentErrorIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedCheckDrawable}, null, Theme.key_chats_verifiedCheck));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedDrawable}, null, Theme.key_chats_verifiedBackground));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, new Drawable[]{Theme.dialogs_muteDrawable}, null, Theme.key_chats_muteIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, new Drawable[]{Theme.dialogs_mentionDrawable}, null, Theme.key_chats_mentionIcon));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, null, null, Theme.key_chats_archivePinBackground));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, null, null, Theme.key_chats_archiveBackground));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, null, null, Theme.key_chats_onlineCircle));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKBOX, new Class[]{SavedChannelCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_windowBackgroundWhite));
            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKBOXCHECK, new Class[]{SavedChannelCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkboxCheck));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{LoadingCell.class}, new String[]{"progressBar"}, null, null, null, Theme.key_progressCircle));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_offlinePaint, null, null, Theme.key_windowBackgroundWhiteGrayText3));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_onlinePaint, null, null, Theme.key_windowBackgroundWhiteBlueText3));

            GraySectionCell.createThemeDescriptions(arrayList, listView);

            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{HashtagSearchCell.class}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGray));

            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGray));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText2));
        }

        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundRed));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundOrange));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundViolet));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundGreen));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundCyan));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundBlue));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundPink));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundSaved));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundArchived));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundArchivedHidden));

        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_nameMessage));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_draft));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_attachMessage));

        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_nameArchived));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_nameMessageArchived));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_nameMessageArchived_threeLines));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_messageArchived));

        if (viewPage != null) {
            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefaultArchived));

            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DialogsEmptyCell.class}, new String[]{"emptyTextView1"}, null, null, null, Theme.key_chats_nameMessage_threeLines));
            arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DialogsEmptyCell.class}, new String[]{"emptyTextView2"}, null, null, null, Theme.key_chats_message));

            if (SharedConfig.archiveHidden) {
                arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Arrow1", Theme.key_avatar_backgroundArchivedHidden));
                arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Arrow2", Theme.key_avatar_backgroundArchivedHidden));
            } else {
                arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Arrow1", Theme.key_avatar_backgroundArchived));
                arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Arrow2", Theme.key_avatar_backgroundArchived));
            }
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Box2", Theme.key_avatar_text));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Box1", Theme.key_avatar_text));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_pinArchiveDrawable}, "Arrow", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_pinArchiveDrawable}, "Line", Theme.key_chats_archiveIcon));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_unpinArchiveDrawable}, "Arrow", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_unpinArchiveDrawable}, "Line", Theme.key_chats_archiveIcon));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_archiveDrawable}, "Arrow", Theme.key_chats_archiveBackground));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_archiveDrawable}, "Box2", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_archiveDrawable}, "Box1", Theme.key_chats_archiveIcon));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_hidePsaDrawable}, "Line 1", Theme.key_chats_archiveBackground));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_hidePsaDrawable}, "Line 2", Theme.key_chats_archiveBackground));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_hidePsaDrawable}, "Line 3", Theme.key_chats_archiveBackground));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_hidePsaDrawable}, "Cup Red", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_hidePsaDrawable}, "Box", Theme.key_chats_archiveIcon));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_unarchiveDrawable}, "Arrow1", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_unarchiveDrawable}, "Arrow2", Theme.key_chats_archivePinBackground));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_unarchiveDrawable}, "Box2", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{SavedChannelCell.class}, new RLottieDrawable[]{Theme.dialogs_unarchiveDrawable}, "Box1", Theme.key_chats_archiveIcon));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"statusColor"}, null, null, cellDelegate, Theme.key_windowBackgroundWhiteGrayText));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"statusOnlineColor"}, null, null, cellDelegate, Theme.key_windowBackgroundWhiteBlueText));

            arrayList.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText4));
            arrayList.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText4));

            arrayList.add(new ThemeDescription(progressView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle));

            ViewPager pager = dialogsAdapter.getArchiveHintCellPager();
            arrayList.add(new ThemeDescription(pager, 0, new Class[]{ArchiveHintInnerCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_chats_nameMessage_threeLines));
            arrayList.add(new ThemeDescription(pager, 0, new Class[]{ArchiveHintInnerCell.class}, new String[]{"imageView2"}, null, null, null, Theme.key_chats_unreadCounter));
            arrayList.add(new ThemeDescription(pager, 0, new Class[]{ArchiveHintInnerCell.class}, new String[]{"headerTextView"}, null, null, null, Theme.key_chats_nameMessage_threeLines));
            arrayList.add(new ThemeDescription(pager, 0, new Class[]{ArchiveHintInnerCell.class}, new String[]{"messageTextView"}, null, null, null, Theme.key_chats_message));
            arrayList.add(new ThemeDescription(pager, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefaultArchived));
        }

        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_archivePullDownBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_archivePullDownBackgroundActive));

        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_chats_menuBackground));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuName));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhone));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhoneCats));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuCloudBackgroundCats));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chat_serviceBackground));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuTopShadow));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuTopShadowCats));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{DrawerProfileCell.class}, new String[]{"darkThemeView"}, null, null, null, Theme.key_chats_menuName));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{DrawerProfileCell.class}, null, null, cellDelegate, Theme.key_chats_menuTopBackgroundCats));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{DrawerProfileCell.class}, null, null, cellDelegate, Theme.key_chats_menuTopBackground));

        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{DrawerActionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemIcon));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerActionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemText));

        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerUserCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemText));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DrawerUserCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_chats_unreadCounterText));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DrawerUserCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_chats_unreadCounter));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DrawerUserCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_chats_menuBackground));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{DrawerAddCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemIcon));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerAddCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemText));

        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DividerCell.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_inappPlayerBackground));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{FragmentContextView.class}, new String[]{"playButton"}, null, null, null, Theme.key_inappPlayerPlayPause));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerTitle));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_FASTSCROLL, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerPerformer));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{FragmentContextView.class}, new String[]{"closeButton"}, null, null, null, Theme.key_inappPlayerClose));

        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_returnToCallBackground));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_returnToCallText));

        for (int a = 0; a < undoView.length; a++) {
            arrayList.add(new ThemeDescription(undoView[a], ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_undo_background));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"undoImageView"}, null, null, null, Theme.key_undo_cancelColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"undoTextView"}, null, null, null, Theme.key_undo_cancelColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"infoTextView"}, null, null, null, Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"subinfoTextView"}, null, null, null, Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"textPaint"}, null, null, null, Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"progressPaint"}, null, null, null, Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "info1", Theme.key_undo_background));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "info2", Theme.key_undo_background));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc12", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc11", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc10", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc9", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc8", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc7", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc6", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc5", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc4", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc3", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc2", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc1", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "Oval", Theme.key_undo_infoColor));
        }

        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBackgroundGray));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlack));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextLink));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLinkSelection));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue3));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue4));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextRed));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextRed2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray3));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray4));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRedIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextHint));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogInputField));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogInputFieldActivated));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareCheck));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareUnchecked));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareDisabled));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRadioBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRadioBackgroundChecked));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogProgressCircle));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogButton));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogButtonSelector));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogScrollGlow));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRoundCheckBox));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRoundCheckBoxCheck));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBadgeBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBadgeText));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLineProgress));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLineProgressBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogGrayLine));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialog_inlineProgressBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialog_inlineProgress));

        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogSearchBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogSearchHint));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogSearchIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogSearchText));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogFloatingButton));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogFloatingIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogShadowLine));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_sheet_scrollUp));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_sheet_other));

        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBar));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarSelector));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarTitle));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarTop));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarSubtitle));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarItems));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_background));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_time));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_progressBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_progressCachedBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_progress));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_button));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_buttonActive));

        if (commentView != null) {
            arrayList.add(new ThemeDescription(commentView, 0, null, Theme.chat_composeBackgroundPaint, null, null, Theme.key_chat_messagePanelBackground));
            arrayList.add(new ThemeDescription(commentView, 0, null, null, new Drawable[]{Theme.chat_composeShadowDrawable}, null, Theme.key_chat_messagePanelShadow));
            arrayList.add(new ThemeDescription(commentView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{ChatActivityEnterView.class}, new String[]{"messageEditText"}, null, null, null, Theme.key_chat_messagePanelText));
            arrayList.add(new ThemeDescription(commentView, ThemeDescription.FLAG_CURSORCOLOR, new Class[]{ChatActivityEnterView.class}, new String[]{"messageEditText"}, null, null, null, Theme.key_chat_messagePanelCursor));
            arrayList.add(new ThemeDescription(commentView, ThemeDescription.FLAG_HINTTEXTCOLOR, new Class[]{ChatActivityEnterView.class}, new String[]{"messageEditText"}, null, null, null, Theme.key_chat_messagePanelHint));
            arrayList.add(new ThemeDescription(commentView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{ChatActivityEnterView.class}, new String[]{"sendButton"}, null, null, null, Theme.key_chat_messagePanelSend));
        }

        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_actionBarTipBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_player_time));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chat_messagePanelCursor));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_actionBarIconBlue));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_groupcreate_spanBackground));

        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayGreen1));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayGreen2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayBlue1));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayBlue2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_topPanelGreen1));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_topPanelGreen2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_topPanelBlue1));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_topPanelBlue2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_topPanelGray));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayAlertGradientMuted));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayAlertGradientMuted2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayAlertGradientUnmuted));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayAlertGradientUnmuted2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_mutedByAdminGradient));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_mutedByAdminGradient2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_mutedByAdminGradient3));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayAlertMutedByAdmin));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_voipgroup_overlayAlertMutedByAdmin2));

        return arrayList;
    }

    float slideFragmentProgress = 1f;
    boolean isSlideBackTransition;
    boolean isDrawerTransition;
    ValueAnimator slideBackTransitionAnimator;

    @Override
    protected Animator getCustomSlideTransition(boolean topFragment, boolean backAnimation, float distanceToMove) {
        if (backAnimation) {
            slideBackTransitionAnimator = ValueAnimator.ofFloat(slideFragmentProgress, 1f);
            return slideBackTransitionAnimator;
        }
        int duration = (int) (Math.max((int) (200.0f / getLayoutContainer().getMeasuredWidth() * distanceToMove), 80) * 1.2f);
        slideBackTransitionAnimator = ValueAnimator.ofFloat(slideFragmentProgress, 1f);
        slideBackTransitionAnimator.addUpdateListener(valueAnimator -> setSlideTransitionProgress((float) valueAnimator.getAnimatedValue()));
        slideBackTransitionAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT);
        slideBackTransitionAnimator.setDuration(duration);
        slideBackTransitionAnimator.start();
        return slideBackTransitionAnimator;
    }

    @Override
    protected void prepareFragmentToSlide(boolean topFragment, boolean beginSlide) {
        if (!topFragment && beginSlide) {
            isSlideBackTransition = true;
            setFragmentIsSliding(true);
        } else {
            slideBackTransitionAnimator = null;
            isSlideBackTransition = false;
            setFragmentIsSliding(false);
            setSlideTransitionProgress(1f);
        }
    }

    private void setFragmentIsSliding(boolean sliding) {
        if (SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW) {
            return;
        }
        if (sliding) {
            if (viewPage != null) {
                viewPage.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                viewPage.setClipChildren(false);
                viewPage.setClipToPadding(false);
                listView.setClipChildren(false);
            }

            if (actionBar != null) {
                actionBar.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            if (fragmentView != null) {
                ((ViewGroup) fragmentView).setClipChildren(false);
                fragmentView.requestLayout();
            }
        } else {
            if (viewPage != null) {
                viewPage.setLayerType(View.LAYER_TYPE_NONE, null);
                viewPage.setClipChildren(true);
                viewPage.setClipToPadding(true);
                listView.setClipChildren(true);
            }
            if (actionBar != null) {
                actionBar.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            if (fragmentView != null) {
                ((ViewGroup) fragmentView).setClipChildren(true);
                fragmentView.requestLayout();
            }
        }
    }

    @Override
    protected void onSlideProgress(boolean isOpen, float progress) {
        if (SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW) {
            return;
        }
        if (isSlideBackTransition && slideBackTransitionAnimator == null) {
            setSlideTransitionProgress(progress);
        }
    }

    private void setSlideTransitionProgress(float progress) {
        if (SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW) {
            return;
        }
        slideFragmentProgress = progress;
        if (fragmentView != null) {
            fragmentView.invalidate();
        }
    }

    @Override
    public void setProgressToDrawerOpened(float progress) {
        if (SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW || isSlideBackTransition) {
            return;
        }
        boolean drawerTransition = progress > 0;
        if (drawerTransition != isDrawerTransition) {
            isDrawerTransition = drawerTransition;
            if (isDrawerTransition) {
                setFragmentIsSliding(true);
            } else {
                setFragmentIsSliding(false);
            }
            if (fragmentView != null) {
                fragmentView.requestLayout();
            }
        }
        setSlideTransitionProgress(1f - progress);
    }

    private void resolveUsername(String username) {
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = username;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            chatLoading = false;
            if (response != null) {
                AndroidUtilities.runOnUIThread(() -> {
                    TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                    MessagesController.getInstance(currentAccount).putUsers(res.users, false);
                    MessagesController.getInstance(currentAccount).putChats(res.chats, false);
                    MessagesStorage.getInstance(currentAccount).putUsersAndChats(res.users, res.chats, true, true);


                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    getUserConfig().savedChannels.remove(username);
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                });
            }
        });
    }
}

