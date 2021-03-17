package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.fakepasscode.SmsMessage;
import org.telegram.messenger.fakepasscode.TelegramMessageAction;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Adapters.SearchAdapterHelper;
import org.telegram.ui.Cells.GroupCreateUserCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.GroupCreateSpan;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

import androidx.annotation.Keep;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FakePasscodeTelegramMessagesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ScrollView scrollView;
    private SpansContainer spansContainer;
    private EditTextBoldCursor editText;
    private RecyclerListView listView;
    private EmptyTextProgressView emptyView;
    private GroupCreateAdapter adapter;
    private ImageView floatingButton;
    private boolean ignoreScrollEvent;

    private int containerHeight;

    TelegramMessageAction action;

    private boolean searchWas;
    private boolean searching;
    private GroupCreateSpan currentDeletingSpan;

    private int fieldY;

    private final static int done_button = 1;

    private static class ItemDecoration extends RecyclerView.ItemDecoration {

        private boolean single;

        public void setSingle(boolean value) {
            single = value;
        }

        @Override
        public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            int width = parent.getWidth();
            int top;
            int childCount = parent.getChildCount() - (single ? 0 : 1);
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                int position = parent.getChildAdapterPosition(child);
                if (position < 0) {
                    continue;
                }
                top = child.getBottom();
                canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(72), top, width - (LocaleController.isRTL ? AndroidUtilities.dp(72) : 0), top, Theme.dividerPaint);
            }
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.top = 1;
        }
    }

    private class SpansContainer extends ViewGroup {
        public SpansContainer(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int count = getChildCount();
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int maxWidth = width - AndroidUtilities.dp(26);
            int currentLineWidth = 0;
            int y = AndroidUtilities.dp(10);
            int allCurrentLineWidth = 0;
            int allY = AndroidUtilities.dp(10);
            int x;
            for (int a = 0; a < count; a++) {
                View child = getChildAt(a);
                if (!(child instanceof GroupCreateSpan)) {
                    continue;
                }
                child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY));
                if (currentLineWidth + child.getMeasuredWidth() > maxWidth) {
                    y += child.getMeasuredHeight() + AndroidUtilities.dp(8);
                    currentLineWidth = 0;
                }
                if (allCurrentLineWidth + child.getMeasuredWidth() > maxWidth) {
                    allY += child.getMeasuredHeight() + AndroidUtilities.dp(8);
                    allCurrentLineWidth = 0;
                }
                x = AndroidUtilities.dp(13) + currentLineWidth;
                child.setTranslationX(x);
                child.setTranslationY(y);
                currentLineWidth += child.getMeasuredWidth() + AndroidUtilities.dp(9);
                allCurrentLineWidth += child.getMeasuredWidth() + AndroidUtilities.dp(9);
            }
            int minWidth;
            if (AndroidUtilities.isTablet()) {
                minWidth = AndroidUtilities.dp(530 - 26 - 18 - 57 * 2) / 3;
            } else {
                minWidth = (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) - AndroidUtilities.dp(26 + 18 + 57 * 2)) / 3;
            }
            if (maxWidth - currentLineWidth < minWidth) {
                currentLineWidth = 0;
                y += AndroidUtilities.dp(32 + 8);
            }
            if (maxWidth - allCurrentLineWidth < minWidth) {
                allY += AndroidUtilities.dp(32 + 8);
            }
            editText.measure(MeasureSpec.makeMeasureSpec(maxWidth - currentLineWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY));
            int currentHeight = allY + AndroidUtilities.dp(32 + 10);
            int fieldX = currentLineWidth + AndroidUtilities.dp(16);
            fieldY = y;
            containerHeight = currentHeight;
            editText.setTranslationX(fieldX);
            editText.setTranslationY(fieldY);
            setMeasuredDimension(width, containerHeight);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int count = getChildCount();
            for (int a = 0; a < count; a++) {
                View child = getChildAt(a);
                child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
            }
        }
    }

    public FakePasscodeTelegramMessagesActivity(TelegramMessageAction action) {
        super();
        this.action = action;
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance(action.accountNum).addObserver(this, NotificationCenter.contactsDidLoad);
        NotificationCenter.getInstance(action.accountNum).addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance(action.accountNum).addObserver(this, NotificationCenter.chatDidCreated);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(action.accountNum).removeObserver(this, NotificationCenter.contactsDidLoad);
        NotificationCenter.getInstance(action.accountNum).removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance(action.accountNum).removeObserver(this, NotificationCenter.chatDidCreated);
    }

    @Override
    public View createView(Context context) {
        searching = false;
        searchWas = false;
        currentDeletingSpan = null;

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("TelegramMessages", R.string.TelegramMessages));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    onDonePressed();
                }
            }
        });

        fragmentView = new ViewGroup(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int height = MeasureSpec.getSize(heightMeasureSpec);
                setMeasuredDimension(width, height);
                int maxSize;
                if (AndroidUtilities.isTablet() || height > width) {
                    maxSize = AndroidUtilities.dp(144);
                } else {
                    maxSize = AndroidUtilities.dp(56);
                }

                scrollView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.AT_MOST));
                listView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height - scrollView.getMeasuredHeight(), MeasureSpec.EXACTLY));
                emptyView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height - scrollView.getMeasuredHeight(), MeasureSpec.EXACTLY));
                if (floatingButton != null) {
                    int w = AndroidUtilities.dp(Build.VERSION.SDK_INT >= 21 ? 56 : 60);
                    floatingButton.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY));
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                scrollView.layout(0, 0, scrollView.getMeasuredWidth(), scrollView.getMeasuredHeight());
                listView.layout(0, scrollView.getMeasuredHeight(), listView.getMeasuredWidth(), scrollView.getMeasuredHeight() + listView.getMeasuredHeight());
                emptyView.layout(0, scrollView.getMeasuredHeight(), emptyView.getMeasuredWidth(), scrollView.getMeasuredHeight() + emptyView.getMeasuredHeight());

                if (floatingButton != null) {
                    int l = LocaleController.isRTL ? AndroidUtilities.dp(14) : (right - left) - AndroidUtilities.dp(14) - floatingButton.getMeasuredWidth();
                    int t = bottom - top - AndroidUtilities.dp(14) - floatingButton.getMeasuredHeight();
                    floatingButton.layout(l, t, l + floatingButton.getMeasuredWidth(), t + floatingButton.getMeasuredHeight());
                }
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                boolean result = super.drawChild(canvas, child, drawingTime);
                if (child == listView || child == emptyView) {
                    parentLayout.drawHeaderShadow(canvas, scrollView.getMeasuredHeight());
                }
                return result;
            }
        };
        ViewGroup frameLayout = (ViewGroup) fragmentView;

        scrollView = new ScrollView(context) {
            @Override
            public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
                if (ignoreScrollEvent) {
                    ignoreScrollEvent = false;
                    return false;
                }
                rectangle.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());
                rectangle.top += fieldY + AndroidUtilities.dp(20);
                rectangle.bottom += fieldY + AndroidUtilities.dp(50);
                return super.requestChildRectangleOnScreen(child, rectangle, immediate);
            }
        };
        scrollView.setVerticalScrollBarEnabled(false);
        AndroidUtilities.setScrollViewEdgeEffectColor(scrollView, Theme.getColor(Theme.key_windowBackgroundWhite));
        frameLayout.addView(scrollView);

        spansContainer = new SpansContainer(context);
        scrollView.addView(spansContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        spansContainer.setOnClickListener(v -> {
            editText.clearFocus();
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        });

        editText = new EditTextBoldCursor(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (currentDeletingSpan != null) {
                    currentDeletingSpan.cancelDeleteAnimation();
                    currentDeletingSpan = null;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!AndroidUtilities.showKeyboard(this)) {
                        clearFocus();
                        requestFocus();
                    }
                }
                return super.onTouchEvent(event);
            }
        };
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setHintColor(Theme.getColor(Theme.key_groupcreate_hintText));
        editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorColor(Theme.getColor(Theme.key_groupcreate_cursor));
        editText.setCursorWidth(1.5f);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setSingleLine(true);
        editText.setBackgroundDrawable(null);
        editText.setVerticalScrollBarEnabled(false);
        editText.setHorizontalScrollBarEnabled(false);
        editText.setTextIsSelectable(false);
        editText.setPadding(0, 0, 0, 0);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        spansContainer.addView(editText);
        editText.setHintText(LocaleController.getString("SearchForPeopleAndGroups", R.string.SearchForPeopleAndGroups));

        editText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {

            }

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }
        });
        editText.setOnKeyListener(new View.OnKeyListener() {

            private boolean wasEmpty;

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        wasEmpty = editText.length() == 0;
                    } else if (event.getAction() == KeyEvent.ACTION_UP && wasEmpty) {
                        updateHint();
                        checkVisibleRows();
                        return true;
                    }
                }
                return false;
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editText.length() != 0) {
                    if (!adapter.searching) {
                        searching = true;
                        searchWas = true;
                        adapter.setSearching(true);
                        listView.setFastScrollVisible(false);
                        listView.setVerticalScrollBarEnabled(true);
                        emptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
                        emptyView.showProgress();
                    }
                    adapter.searchDialogs(editText.getText().toString());
                } else {
                    closeSearch();
                }
            }
        });

        emptyView = new EmptyTextProgressView(context);
        if (ContactsController.getInstance(action.accountNum).isLoadingContacts()) {
            emptyView.showProgress();
        } else {
            emptyView.showTextView();
        }
        emptyView.setShowAtCenter(true);
        emptyView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
        frameLayout.addView(emptyView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);

        listView = new RecyclerListView(context);
        listView.setFastScrollEnabled();
        listView.setEmptyView(emptyView);
        listView.setAdapter(adapter = new GroupCreateAdapter(context));
        listView.setLayoutManager(linearLayoutManager);
        listView.setVerticalScrollBarEnabled(false);
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? View.SCROLLBAR_POSITION_LEFT : View.SCROLLBAR_POSITION_RIGHT);
        listView.addItemDecoration(new ItemDecoration());
        frameLayout.addView(listView);
        listView.setOnItemClickListener((view, position) -> {
            if (view instanceof GroupCreateUserCell) {
                GroupCreateUserCell cell = (GroupCreateUserCell) view;
                Object object = cell.getObject();
                int id;
                if (object instanceof TLRPC.User) {
                    id = ((TLRPC.User) object).id;
                } else if (object instanceof TLRPC.Chat) {
                    id = -((TLRPC.Chat) object).id;
                } else {
                    return;
                }
                if (action.chatsToSendingMessages.containsKey(id)) {
                    FakePasscodeDialogBuilder.Template template = new FakePasscodeDialogBuilder.Template();
                    template.type = FakePasscodeDialogBuilder.DialogType.EDIT;
                    template.title = LocaleController.getString("ChangeMessage", R.string.ChangeMessage);
                    template.addEditTemplate(action.chatsToSendingMessages.get(id), LocaleController.getString("Message", R.string.Message), false);
                    template.positiveListener = edits -> {
                        String message = edits.get(0).getText().toString();
                        action.chatsToSendingMessages.put(id, message);
                        SharedConfig.saveConfig();
                        cell.setChecked(true, true);
                    };
                    template.negativeListener = (dlg, whichButton) -> {
                        action.chatsToSendingMessages.remove(id);
                        SharedConfig.saveConfig();
                        cell.setChecked(false, true);
                    };
                    AlertDialog dialog = FakePasscodeDialogBuilder.build(getParentActivity(), template);
                    showDialog(dialog);
                } else {
                    if (action.chatsToSendingMessages.size() >= 100) {
                        return;
                    }
                    if (object instanceof TLRPC.User) {
                        TLRPC.User user = (TLRPC.User) object;
                        MessagesController.getInstance(action.accountNum).putUser(user, !searching);
                    } else if (object instanceof TLRPC.Chat) {
                        TLRPC.Chat chat = (TLRPC.Chat) object;
                        MessagesController.getInstance(action.accountNum).putChat(chat, !searching);
                    }
                    FakePasscodeDialogBuilder.Template template = new FakePasscodeDialogBuilder.Template();
                    template.type = FakePasscodeDialogBuilder.DialogType.ADD;
                    template.title = LocaleController.getString("ChangeMessage", R.string.ChangeMessage);
                    template.addEditTemplate("", LocaleController.getString("Message", R.string.Message), false);
                    template.positiveListener = edits -> {
                        String message = edits.get(0).getText().toString();
                        action.chatsToSendingMessages.put(id, message);
                        SharedConfig.saveConfig();
                        cell.setChecked(true, true);
                    };
                    AlertDialog dialog = FakePasscodeDialogBuilder.build(getParentActivity(), template);
                    showDialog(dialog);
                }
                updateHint();
                if (searching || searchWas) {
                    AndroidUtilities.showKeyboard(editText);
                }
                if (editText.length() > 0) {
                    editText.setText(null);
                }
            }
        });
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(editText);
                }
            }
        });

        floatingButton = new ImageView(context);
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
        floatingButton.setImageResource(R.drawable.floating_check);
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
        frameLayout.addView(floatingButton);
        floatingButton.setOnClickListener(v -> onDonePressed());
        floatingButton.setContentDescription(LocaleController.getString("Next", R.string.Next));

        updateHint();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (editText != null) {
            editText.requestFocus();
        }
        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.contactsDidLoad) {
            if (emptyView != null) {
                emptyView.showTextView();
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } else if (id == NotificationCenter.updateInterfaces) {
            if (listView != null) {
                int mask = (Integer) args[0];
                int count = listView.getChildCount();
                if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                    for (int a = 0; a < count; a++) {
                        View child = listView.getChildAt(a);
                        if (child instanceof GroupCreateUserCell) {
                            ((GroupCreateUserCell) child).update(mask);
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.chatDidCreated) {
            removeSelfFromStack();
        }
    }

    @Keep
    public void setContainerHeight(int value) {
        containerHeight = value;
        if (spansContainer != null) {
            spansContainer.requestLayout();
        }
    }

    @Keep
    public int getContainerHeight() {
        return containerHeight;
    }

    private void checkVisibleRows() {
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = listView.getChildAt(a);
            if (child instanceof GroupCreateUserCell) {
                GroupCreateUserCell cell = (GroupCreateUserCell) child;
                Object object = cell.getObject();
                int id;
                if (object instanceof TLRPC.User) {
                    id = ((TLRPC.User) object).id;
                } else if (object instanceof TLRPC.Chat) {
                    id = -((TLRPC.Chat) object).id;
                } else {
                    id = 0;
                }
                if (id != 0) {
                    cell.setChecked(action.chatsToSendingMessages.containsKey(id), true);
                    cell.setCheckBoxEnabled(true);
                }
            }
        }
    }

    private void onDonePressed() {
        finishFragment();
    }

    private void closeSearch() {
        searching = false;
        searchWas = false;
        adapter.setSearching(false);
        adapter.searchDialogs(null);
        listView.setFastScrollVisible(true);
        listView.setVerticalScrollBarEnabled(false);
        emptyView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
    }

    private void updateHint() {
        if (action.chatsToSendingMessages.size() == 0) {
            actionBar.setSubtitle(LocaleController.formatString("MembersCountZero", R.string.MembersCountZero, LocaleController.formatPluralString("Chats", 100)));
        } else {
            actionBar.setSubtitle(String.format(LocaleController.getPluralString("MembersCountSelected", action.chatsToSendingMessages.size()), action.chatsToSendingMessages.size(), 100));
        }
    }

    public class GroupCreateAdapter extends RecyclerListView.FastScrollAdapter {

        private Context context;
        private ArrayList<TLObject> searchResult = new ArrayList<>();
        private ArrayList<CharSequence> searchResultNames = new ArrayList<>();
        private SearchAdapterHelper searchAdapterHelper;
        private Runnable searchRunnable;
        private boolean searching;
        private ArrayList<TLObject> contacts = new ArrayList<>();

        public GroupCreateAdapter(Context ctx) {
            context = ctx;

            boolean hasSelf = false;
            ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getAllDialogs();
            for (int a = 0, N = dialogs.size(); a < N; a++) {
                TLRPC.Dialog dialog = dialogs.get(a);
                int lowerId = (int) dialog.id;
                if (lowerId == 0) {
                    continue;
                }
                if (lowerId > 0) {
                    TLRPC.User user = getMessagesController().getUser(lowerId);
                    if (user != null) {
                        contacts.add(user);
                        if (UserObject.isUserSelf(user)) {
                            hasSelf = true;
                        }
                    }
                } else {
                    TLRPC.Chat chat = getMessagesController().getChat(-lowerId);
                    if (chat != null) {
                        if (!chat.broadcast || (chat.admin_rights != null && chat.admin_rights.post_messages)) {
                            contacts.add(chat);
                        }
                    }
                }
            }
            if (!hasSelf) {
                TLRPC.User user = getMessagesController().getUser(getUserConfig().clientUserId);
                contacts.add(0, user);
            }

            searchAdapterHelper = new SearchAdapterHelper(false);
            searchAdapterHelper.setAllowGlobalResults(false);
            searchAdapterHelper.setDelegate((searchId) -> {
                if (searchRunnable == null && !searchAdapterHelper.isSearchInProgress()) {
                    emptyView.showTextView();
                }
                notifyDataSetChanged();
            });
        }

        public void setSearching(boolean value) {
            if (searching == value) {
                return;
            }
            searching = value;
            notifyDataSetChanged();
        }

        @Override
        public String getLetter(int position) {
            return null;
        }

        @Override
        public int getItemCount() {
            if (searching) {
                int count = searchResult.size();
                int localServerCount = searchAdapterHelper.getLocalServerSearch().size();
                int globalCount = searchAdapterHelper.getGlobalSearch().size();
                count += localServerCount + globalCount;
                return count;
            } else {
                return contacts.size();
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 1:
                default:
                    view = new GroupCreateUserCell(context, true, 0, true);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    GroupCreateUserCell cell = (GroupCreateUserCell) holder.itemView;
                    Object object;
                    CharSequence username = null;
                    CharSequence name = null;
                    if (searching) {
                        int localCount = searchResult.size();
                        int globalCount = searchAdapterHelper.getGlobalSearch().size();
                        int localServerCount = searchAdapterHelper.getLocalServerSearch().size();

                        if (position >= 0 && position < localCount) {
                            object = searchResult.get(position);
                        } else if (position >= localCount && position < localServerCount + localCount) {
                            object = searchAdapterHelper.getLocalServerSearch().get(position - localCount);
                        } else if (position > localCount + localServerCount && position < globalCount + localCount + localServerCount) {
                            object = searchAdapterHelper.getGlobalSearch().get(position - localCount - localServerCount);
                        } else {
                            object = null;
                        }
                        if (object != null) {
                            String objectUserName;
                            if (object instanceof TLRPC.User) {
                                objectUserName = ((TLRPC.User) object).username;
                            } else {
                                objectUserName = ((TLRPC.Chat) object).username;
                            }
                            if (position < localCount) {
                                name = searchResultNames.get(position);
                                if (name != null && !TextUtils.isEmpty(objectUserName)) {
                                    if (name.toString().startsWith("@" + objectUserName)) {
                                        username = name;
                                        name = null;
                                    }
                                }
                            } else if (position > localCount && !TextUtils.isEmpty(objectUserName)) {
                                String foundUserName = searchAdapterHelper.getLastFoundUsername();
                                if (foundUserName.startsWith("@")) {
                                    foundUserName = foundUserName.substring(1);
                                }
                                try {
                                    int index;
                                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                                    spannableStringBuilder.append("@");
                                    spannableStringBuilder.append(objectUserName);
                                    if ((index = AndroidUtilities.indexOfIgnoreCase(objectUserName, foundUserName)) != -1) {
                                        int len = foundUserName.length();
                                        if (index == 0) {
                                            len++;
                                        } else {
                                            index++;
                                        }
                                        spannableStringBuilder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4)), index, index + len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }
                                    username = spannableStringBuilder;
                                } catch (Exception e) {
                                    username = objectUserName;
                                }
                            }
                        }
                    } else {
                        if (position < 0) {
                            return;
                        }
                        object = contacts.get(position);
                    }
                    int id;
                    if (object instanceof TLRPC.User) {
                        id = ((TLRPC.User) object).id;
                    } else if (object instanceof TLRPC.Chat) {
                        id = -((TLRPC.Chat) object).id;
                    } else {
                        id = 0;
                    }
                    if (!searching) {
                        StringBuilder builder = new StringBuilder();
                        ArrayList<MessagesController.DialogFilter> filters = getMessagesController().dialogFilters;
                        for (int a = 0, N = filters.size(); a < N; a++) {
                            MessagesController.DialogFilter filter = filters.get(a);
                            if (filter.includesDialog(getAccountInstance(), id)) {
                                if (builder.length() > 0) {
                                    builder.append(", ");
                                }
                                builder.append(filter.name);
                            }
                        }
                        username = builder;
                    }
                    cell.setObject(object, name, username);
                    if (id != 0) {
                        cell.setChecked(action.chatsToSendingMessages.containsKey(id), false);
                        cell.setCheckBoxEnabled(true);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return 1;
        }

        @Override
        public int getPositionForScrollProgress(float progress) {
            return (int) (getItemCount() * progress);
        }

        @Override
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            if (holder.itemView instanceof GroupCreateUserCell) {
                ((GroupCreateUserCell) holder.itemView).recycle();
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == 1;
        }

        public void searchDialogs(final String query) {
            if (searchRunnable != null) {
                Utilities.searchQueue.cancelRunnable(searchRunnable);
                searchRunnable = null;
            }
            if (query == null) {
                searchResult.clear();
                searchResultNames.clear();
                searchAdapterHelper.mergeResults(null);
                searchAdapterHelper.queryServerSearch(null, true, true, false, false, false, 0, false, 0, 0);
                notifyDataSetChanged();
            } else {
                Utilities.searchQueue.postRunnable(searchRunnable = () -> AndroidUtilities.runOnUIThread(() -> {
                    searchAdapterHelper.queryServerSearch(query, true, true, true, true, false, 0, false, 0, 0);
                    Utilities.searchQueue.postRunnable(searchRunnable = () -> {
                        String search1 = query.trim().toLowerCase();
                        if (search1.length() == 0) {
                            updateSearchResults(new ArrayList<>(), new ArrayList<>());
                            return;
                        }
                        String search2 = LocaleController.getInstance().getTranslitString(search1);
                        if (search1.equals(search2) || search2.length() == 0) {
                            search2 = null;
                        }
                        String[] search = new String[1 + (search2 != null ? 1 : 0)];
                        search[0] = search1;
                        if (search2 != null) {
                            search[1] = search2;
                        }

                        ArrayList<TLObject> resultArray = new ArrayList<>();
                        ArrayList<CharSequence> resultArrayNames = new ArrayList<>();

                        for (int a = 0; a < contacts.size(); a++) {
                            TLObject object = contacts.get(a);

                            String username;
                            final String[] names = new String[3];

                            if (object instanceof TLRPC.User) {
                                TLRPC.User user = (TLRPC.User) object;
                                names[0] = ContactsController.formatName(user.first_name, user.last_name).toLowerCase();
                                username = user.username;
                                if (UserObject.isReplyUser(user)) {
                                    names[2] = LocaleController.getString("RepliesTitle", R.string.RepliesTitle).toLowerCase();
                                } else if (user.self) {
                                    names[2] = LocaleController.getString("SavedMessages", R.string.SavedMessages).toLowerCase();
                                }
                            } else {
                                TLRPC.Chat chat = (TLRPC.Chat) object;
                                names[0] = chat.title.toLowerCase();
                                username = chat.username;
                            }
                            names[1] = LocaleController.getInstance().getTranslitString(names[0]);
                            if (names[0].equals(names[1])) {
                                names[1] = null;
                            }

                            int found = 0;
                            for (String q : search) {
                                for (int i = 0; i < names.length; i++) {
                                    final String name = names[i];
                                    if (name != null && (name.startsWith(q) || name.contains(" " + q))) {
                                        found = 1;
                                        break;
                                    }
                                }
                                if (found == 0 && username != null && username.toLowerCase().startsWith(q)) {
                                    found = 2;
                                }

                                if (found != 0) {
                                    if (found == 1) {
                                        if (object instanceof TLRPC.User) {
                                            TLRPC.User user = (TLRPC.User) object;
                                            resultArrayNames.add(AndroidUtilities.generateSearchName(user.first_name, user.last_name, q));
                                        } else {
                                            TLRPC.Chat chat = (TLRPC.Chat) object;
                                            resultArrayNames.add(AndroidUtilities.generateSearchName(chat.title, null, q));
                                        }
                                    } else {
                                        resultArrayNames.add(AndroidUtilities.generateSearchName("@" + username, null, "@" + q));
                                    }
                                    resultArray.add(object);
                                    break;
                                }
                            }
                        }
                        updateSearchResults(resultArray, resultArrayNames);
                    });
                }), 300);
            }
        }

        private void updateSearchResults(final ArrayList<TLObject> users, final ArrayList<CharSequence> names) {
            AndroidUtilities.runOnUIThread(() -> {
                if (!searching) {
                    return;
                }
                searchRunnable = null;
                searchResult = users;
                searchResultNames = names;
                searchAdapterHelper.mergeResults(searchResult);
                if (searching && !searchAdapterHelper.isSearchInProgress()) {
                    emptyView.showTextView();
                }
                notifyDataSetChanged();
            });
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        ThemeDescription.ThemeDescriptionDelegate cellDelegate = () -> {
            if (listView != null) {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = listView.getChildAt(a);
                    if (child instanceof GroupCreateUserCell) {
                        ((GroupCreateUserCell) child).update(0);
                    }
                }
            }
        };

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(scrollView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_FASTSCROLL, null, null, null, null, Theme.key_fastScrollActive));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_FASTSCROLL, null, null, null, null, Theme.key_fastScrollInactive));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_FASTSCROLL, null, null, null, null, Theme.key_fastScrollText));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder));
        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle));

        themeDescriptions.add(new ThemeDescription(editText, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(editText, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_groupcreate_hintText));
        themeDescriptions.add(new ThemeDescription(editText, ThemeDescription.FLAG_CURSORCOLOR, null, null, null, null, Theme.key_groupcreate_cursor));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{GroupCreateUserCell.class}, new String[]{"textView"}, null, null, null, Theme.key_groupcreate_sectionText));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{GroupCreateUserCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkbox));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{GroupCreateUserCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkboxDisabled));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{GroupCreateUserCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkboxCheck));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{GroupCreateUserCell.class}, new String[]{"statusTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{GroupCreateUserCell.class}, new String[]{"statusTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{GroupCreateUserCell.class}, null, Theme.avatarDrawables, null, Theme.key_avatar_text));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundRed));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundOrange));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundViolet));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundGreen));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundCyan));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundBlue));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundPink));

        themeDescriptions.add(new ThemeDescription(spansContainer, 0, new Class[]{GroupCreateSpan.class}, null, null, null, Theme.key_groupcreate_spanBackground));
        themeDescriptions.add(new ThemeDescription(spansContainer, 0, new Class[]{GroupCreateSpan.class}, null, null, null, Theme.key_groupcreate_spanText));
        themeDescriptions.add(new ThemeDescription(spansContainer, 0, new Class[]{GroupCreateSpan.class}, null, null, null, Theme.key_groupcreate_spanDelete));
        themeDescriptions.add(new ThemeDescription(spansContainer, 0, new Class[]{GroupCreateSpan.class}, null, null, null, Theme.key_avatar_backgroundBlue));

        return themeDescriptions;
    }
}
