/*
 * This is the source code of Telegram for Android v. 5.x.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.fakepasscode.FakePasscode;
import org.telegram.messenger.fakepasscode.RemoveChatsAction;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FakePasscodeActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private TextView titleTextView;
    private EditTextBoldCursor passwordEditText;
    private TextView dropDown;
    private ActionBarMenuItem dropDownContainer;
    private Drawable dropDownDrawable;

    TextSettingsCell changeSosPhoneNumberCell;
    TextSettingsCell changeSosMessageCell;
    TextCheckCell sosMessageCell;
    TextSettingsCell changeFakePasscodeCell;

    private int type;
    private int currentPasswordType = 0;
    private int passcodeSetStep = 0;
    private String firstPassword;

    private int rowCount;

    private int fakePasscodeRow;
    private int changeFakePasscodeRow;
    private int allowFakePasscodeLoginRow;
    private int sosMessageRow;
    private int changeSosPhoneNumberRow;
    private int changeSosMessageRow;
    private int clearTelegramCacheRow;
    private int changeChatsToRemoveRow;
    private int fakePasscodeDetailRow;

    private boolean creating;
    private FakePasscode fakePasscode;

    private final static int done_button = 1;
    private final static int pin_item = 2;
    private final static int password_item = 3;

    public FakePasscodeActivity(int type, FakePasscode fakePasscode, boolean creating) {
        super();
        this.type = type;
        this.fakePasscode = fakePasscode;
        this.creating = creating;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        if (type == 0) {
            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetPasscode);
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (type == 0) {
            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetPasscode);
        }
    }

    @Override
    public View createView(Context context) {
        if (type != 3) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        }
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    if (passcodeSetStep == 0) {
                        processNext();
                    } else if (passcodeSetStep == 1) {
                        processDone();
                    }
                } else if (id == pin_item) {
                    currentPasswordType = 0;
                    updateDropDownTextView();
                } else if (id == password_item) {
                    currentPasswordType = 1;
                    updateDropDownTextView();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        if (type != 0) {
            ActionBarMenu menu = actionBar.createMenu();
            menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56), LocaleController.getString("Done", R.string.Done));

            titleTextView = new TextView(context);
            titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6));
            if (type == 1) {
                if (SharedConfig.passcodeHash.length() != 0) {
                    titleTextView.setText(LocaleController.getString("EnterNewPasscode", R.string.EnterNewPasscode));
                } else {
                    titleTextView.setText(LocaleController.getString("EnterNewFirstPasscode", R.string.EnterNewFirstPasscode));
                }
            } else {
                titleTextView.setText(LocaleController.getString("EnterCurrentPasscode", R.string.EnterCurrentPasscode));
            }
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            frameLayout.addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 38, 0, 0));

            passwordEditText = new EditTextBoldCursor(context);
            passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            passwordEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            passwordEditText.setBackgroundDrawable(Theme.createEditTextDrawable(context, false));
            passwordEditText.setMaxLines(1);
            passwordEditText.setLines(1);
            passwordEditText.setGravity(Gravity.CENTER_HORIZONTAL);
            passwordEditText.setSingleLine(true);
            if (type == 1) {
                passcodeSetStep = 0;
                passwordEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            } else {
                passcodeSetStep = 1;
                passwordEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            }
            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            passwordEditText.setTypeface(Typeface.DEFAULT);
            passwordEditText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            passwordEditText.setCursorSize(AndroidUtilities.dp(20));
            passwordEditText.setCursorWidth(1.5f);
            frameLayout.addView(passwordEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36, Gravity.TOP | Gravity.LEFT, 40, 90, 40, 0));
            passwordEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
                if (passcodeSetStep == 0) {
                    processNext();
                    return true;
                } else if (passcodeSetStep == 1) {
                    processDone();
                    return true;
                }
                return false;
            });
            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (passwordEditText.length() == 4) {
                        if (type == 1 && currentPasswordType == 0) {
                            if (passcodeSetStep == 0) {
                                processNext();
                            } else if (passcodeSetStep == 1) {
                                processDone();
                            }
                        }
                    }
                }
            });

            passwordEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
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

            if (type == 1) {
                frameLayout.setTag(Theme.key_windowBackgroundWhite);
                dropDownContainer = new ActionBarMenuItem(context, menu, 0, 0);
                dropDownContainer.setSubMenuOpenSide(1);
                dropDownContainer.addSubItem(pin_item, LocaleController.getString("PasscodePIN", R.string.PasscodePIN));
                dropDownContainer.addSubItem(password_item, LocaleController.getString("PasscodePassword", R.string.PasscodePassword));
                actionBar.addView(dropDownContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, AndroidUtilities.isTablet() ? 64 : 56, 0, 40, 0));
                dropDownContainer.setOnClickListener(view -> dropDownContainer.toggleSubMenu());

                dropDown = new TextView(context);
                dropDown.setGravity(Gravity.LEFT);
                dropDown.setSingleLine(true);
                dropDown.setLines(1);
                dropDown.setMaxLines(1);
                dropDown.setEllipsize(TextUtils.TruncateAt.END);
                dropDown.setTextColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
                dropDown.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                dropDownDrawable = context.getResources().getDrawable(R.drawable.ic_arrow_drop_down).mutate();
                dropDownDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultTitle), PorterDuff.Mode.MULTIPLY));
                dropDown.setCompoundDrawablesWithIntrinsicBounds(null, null, dropDownDrawable, null);
                dropDown.setCompoundDrawablePadding(AndroidUtilities.dp(4));
                dropDown.setPadding(0, 0, AndroidUtilities.dp(10), 0);
                dropDownContainer.addView(dropDown, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 16, 0, 0, 1));
            } else {
                actionBar.setTitle(LocaleController.getString("Passcode", R.string.Passcode));
            }

            updateDropDownTextView();
        } else {
            actionBar.setTitle(LocaleController.getString("Passcode", R.string.Passcode));
            frameLayout.setTag(Theme.key_windowBackgroundGray);
            frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
            listView = new RecyclerListView(context);
            listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
                @Override
                public boolean supportsPredictiveItemAnimations() {
                    return false;
                }
            });
            listView.setVerticalScrollBarEnabled(false);
            listView.setItemAnimator(null);
            listView.setLayoutAnimation(null);
            frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            listView.setAdapter(listAdapter = new ListAdapter(context));
            listView.setOnItemClickListener((view, position) -> {
                if (!view.isEnabled()) {
                    return;
                }
                if (position == changeFakePasscodeRow) {
                    presentFragment(new FakePasscodeActivity(1, fakePasscode, false));
                } else if (position == fakePasscodeRow) {
                    TextCheckCell cell = (TextCheckCell) view;
                    if (fakePasscode.passcodeHash.length() != 0) {
                        fakePasscode.passcodeHash = "";
                        SharedConfig.saveConfig();
                        changeFakePasscodeCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText7));
                        cell.setChecked(fakePasscode.passcodeHash.length() != 0);
                        updateRows();
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    } else {
                        presentFragment(new FakePasscodeActivity(1, fakePasscode, false));
                    }
                } else if (position == allowFakePasscodeLoginRow) {
                    TextCheckCell cell = (TextCheckCell) view;
                    fakePasscode.allowLogin = !fakePasscode.allowLogin;
                    SharedConfig.saveConfig();
                    cell.setChecked(fakePasscode.allowLogin);
                } else if (position == sosMessageRow) {
                    Activity parentActivity = getParentActivity();
                    if (!fakePasscode.sosMessageAction.enabled && (ContextCompat.checkSelfPermission(parentActivity, Manifest.permission.SEND_SMS)) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(parentActivity, new String[]{Manifest.permission.SEND_SMS}, 1000);
                    } else {
                        TextCheckCell cell = (TextCheckCell) view;
                        fakePasscode.sosMessageAction.enabled = !fakePasscode.sosMessageAction.enabled;
                        cell.setChecked(fakePasscode.sosMessageAction.enabled);
                        updateRows();
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                } else if (position == changeSosPhoneNumberRow) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getParentActivity());
                    final EditText edittext = new EditText(getParentActivity());
                    edittext.setText(fakePasscode.sosMessageAction.phoneNumber);
                    alert.setTitle(LocaleController.getString("ChangeSosPhoneNumber", R.string.ChangeSosPhoneNumber));
                    alert.setView(edittext);
                    alert.setPositiveButton(LocaleController.getString("Done", R.string.Done), (dialog, whichButton) -> {
                        fakePasscode.sosMessageAction.phoneNumber = edittext.getText().toString();
                        SharedConfig.saveConfig();
                        changeSosPhoneNumberCell.setTextAndValue(LocaleController.getString("ChangeSosPhoneNumber", R.string.ChangeSosPhoneNumber),
                                fakePasscode.sosMessageAction.phoneNumber, true);
                    });

                    alert.show();
                } else if (position == changeSosMessageRow) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getParentActivity());
                    final EditText edittext = new EditText(getParentActivity());
                    edittext.setText(fakePasscode.sosMessageAction.message);
                    alert.setTitle(LocaleController.getString("ChangeSosMessage", R.string.ChangeSosMessage));
                    alert.setView(edittext);
                    alert.setPositiveButton(LocaleController.getString("Done", R.string.Done), (dialog, whichButton) -> {
                        fakePasscode.sosMessageAction.message = edittext.getText().toString();
                        SharedConfig.saveConfig();
                        changeSosMessageCell.setTextAndValue(LocaleController.getString("ChangeSosMessage", R.string.ChangeSosMessage),
                                fakePasscode.sosMessageAction.message, true);
                    });

                    alert.show();
                } else if (position == clearTelegramCacheRow) {
                    TextCheckCell cell = (TextCheckCell) view;
                    fakePasscode.clearCacheAction.enabled = !fakePasscode.clearCacheAction.enabled;
                    cell.setChecked(fakePasscode.clearCacheAction.enabled);
                    updateRows();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                } else if (position == changeChatsToRemoveRow) {
                    ArrayList<Integer> chats = fakePasscode.findChatsToRemove(currentAccount);
                    FilterUsersActivity fragment = new FilterUsersActivity(null, chats, 0);
                    fragment.setDelegate((ids, flags) -> {
                        RemoveChatsAction action = fakePasscode.findRemoveChatsAction(currentAccount);
                        if (action == null) {
                            action = new RemoveChatsAction();
                            action.accountNum = currentAccount;
                            fakePasscode.removeChatsActions.add(action);
                        }
                        action.chatsToRemove = ids;
                        SharedConfig.saveConfig();
                        updateRows();
                    });
                    presentFragment(fragment);
                }
            });
        }

        return fragmentView;
    }

    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1000 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fakePasscode.sosMessageAction.enabled = !fakePasscode.sosMessageAction.enabled;
            sosMessageCell.setChecked(fakePasscode.sosMessageAction.enabled);
            updateRows();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        if (type != 0) {
            AndroidUtilities.runOnUIThread(() -> {
                if (passwordEditText != null) {
                    passwordEditText.requestFocus();
                    AndroidUtilities.showKeyboard(passwordEditText);
                }
            }, 200);
        }
        fixLayoutInternal();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didSetPasscode) {
            if (type == 0) {
                updateRows();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void updateRows() {
        allowFakePasscodeLoginRow = -1;
        sosMessageRow = -1;
        changeSosPhoneNumberRow = -1;
        changeSosMessageRow = -1;
        changeChatsToRemoveRow = -1;
        clearTelegramCacheRow = -1;

        rowCount = 0;
        if (SharedConfig.passcodeHash.length() > 0) {
            fakePasscodeRow = rowCount++;
            changeFakePasscodeRow = rowCount++;
            if (fakePasscode.passcodeHash.length() > 0) {
                allowFakePasscodeLoginRow = rowCount++;
                sosMessageRow = rowCount++;
                if (fakePasscode.sosMessageAction.enabled) {
                    changeSosPhoneNumberRow = rowCount++;
                    changeSosMessageRow = rowCount++;
                }
                changeChatsToRemoveRow = rowCount++;
                clearTelegramCacheRow = rowCount++;
            }
            fakePasscodeDetailRow = rowCount++;
        } else {
            fakePasscodeRow = -1;
            fakePasscodeDetailRow = -1;
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (listView != null) {
            ViewTreeObserver obs = listView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    listView.getViewTreeObserver().removeOnPreDrawListener(this);
                    fixLayoutInternal();
                    return true;
                }
            });
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && type != 0) {
            AndroidUtilities.showKeyboard(passwordEditText);
        }
    }

    private void updateDropDownTextView() {
        if (dropDown != null) {
            if (currentPasswordType == 0) {
                dropDown.setText(LocaleController.getString("PasscodePIN", R.string.PasscodePIN));
            } else if (currentPasswordType == 1) {
                dropDown.setText(LocaleController.getString("PasscodePassword", R.string.PasscodePassword));
            }
        }
        if (type == 1 && currentPasswordType == 0) {
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(4);
            passwordEditText.setFilters(filterArray);
            passwordEditText.setInputType(InputType.TYPE_CLASS_PHONE);
            passwordEditText.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
        } else if (type == 1 && currentPasswordType == 1) {
            passwordEditText.setFilters(new InputFilter[0]);
            passwordEditText.setKeyListener(null);
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
    }

    private void processNext() {
        if (passwordEditText.getText().length() == 0 || currentPasswordType == 0 && passwordEditText.getText().length() != 4) {
            onPasscodeError();
            return;
        }
        if (currentPasswordType == 0) {
            actionBar.setTitle(LocaleController.getString("PasscodePIN", R.string.PasscodePIN));
        } else {
            actionBar.setTitle(LocaleController.getString("PasscodePassword", R.string.PasscodePassword));
        }
        dropDownContainer.setVisibility(View.GONE);
        titleTextView.setText(LocaleController.getString("ReEnterYourPasscode", R.string.ReEnterYourPasscode));
        firstPassword = passwordEditText.getText().toString();
        passwordEditText.setText("");
        passcodeSetStep = 1;
    }

    private void processDone() {
        if (passwordEditText.getText().length() == 0) {
            onPasscodeError();
            return;
        }
        if (type == 1) {
            if (!firstPassword.equals(passwordEditText.getText().toString())) {
                try {
                    Toast.makeText(getParentActivity(), LocaleController.getString("PasscodeDoNotMatch", R.string.PasscodeDoNotMatch), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                AndroidUtilities.shakeView(titleTextView, 2, 0);
                passwordEditText.setText("");
                return;
            }

            try {
                byte[] passcodeBytes = firstPassword.getBytes("UTF-8");
                byte[] bytes = new byte[32 + passcodeBytes.length];
                System.arraycopy(SharedConfig.passcodeSalt, 0, bytes, 0, 16);
                System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
                System.arraycopy(SharedConfig.passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
                fakePasscode.passcodeHash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
            } catch (Exception e) {
                FileLog.e(e);
            }

            SharedConfig.allowScreenCapture = true;
            SharedConfig.passcodeType = currentPasswordType;
            SharedConfig.saveConfig();
            getMediaDataController().buildShortcuts();
            presentFragment(new FakePasscodeActivity(0, fakePasscode, false), true);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetPasscode);
            passwordEditText.clearFocus();
            AndroidUtilities.hideKeyboard(passwordEditText);
        }
    }

    private void onPasscodeError() {
        if (getParentActivity() == null) {
            return;
        }
        Vibrator v = (Vibrator) getParentActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(200);
        }
        AndroidUtilities.shakeView(titleTextView, 2, 0);
    }

    private void fixLayoutInternal() {
        if (dropDownContainer != null) {
            if (!AndroidUtilities.isTablet()) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) dropDownContainer.getLayoutParams();
                layoutParams.topMargin = (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
                dropDownContainer.setLayoutParams(layoutParams);
            }
            if (!AndroidUtilities.isTablet() && ApplicationLoader.applicationContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                dropDown.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            } else {
                dropDown.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            }
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == changeFakePasscodeRow || position == fakePasscodeRow || position == allowFakePasscodeLoginRow
                    || position == sosMessageRow || position == changeSosPhoneNumberRow || position == changeSosMessageRow
                    || position == changeChatsToRemoveRow || position == clearTelegramCacheRow;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                default:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == fakePasscodeRow) {
                        textCell.setTextAndCheck(LocaleController.getString("FakePasscode", R.string.FakePasscode),  fakePasscode.passcodeHash.length() > 0, true);
                    } else if (position == allowFakePasscodeLoginRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AllowFakePasscodeLogin", R.string.AllowFakePasscodeLogin), fakePasscode.allowLogin, true);
                    } else if (position == sosMessageRow) {
                        sosMessageCell = textCell;
                        textCell.setTextAndCheck(LocaleController.getString("SosMessage", R.string.SosMessage), fakePasscode.sosMessageAction.enabled, true);
                    } else if (position == clearTelegramCacheRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ClearTelegramCacheOnFakeLogin", R.string.ClearTelegramCacheOnFakeLogin), fakePasscode.clearCacheAction.enabled, true);
                    }
                    break;
                }
                case 1: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == changeFakePasscodeRow) {
                        changeFakePasscodeCell = textCell;
                        textCell.setText(LocaleController.getString("ChangeFakePasscode", R.string.ChangeFakePasscode), true);
                        if (fakePasscode.passcodeHash.length() == 0) {
                            textCell.setTag(Theme.key_windowBackgroundWhiteGrayText7);
                            textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText7));
                        } else {
                            textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                            textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                        }
                    } else if (position == changeSosPhoneNumberRow) {
                        changeSosPhoneNumberCell = textCell;
                        textCell.setTextAndValue(LocaleController.getString("ChangeSosPhoneNumber", R.string.ChangeSosPhoneNumber),
                                fakePasscode.sosMessageAction.phoneNumber, true);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    } else if (position == changeSosMessageRow) {
                        changeSosMessageCell = textCell;
                        textCell.setTextAndValue(LocaleController.getString("ChangeSosMessage", R.string.ChangeSosMessage),
                                fakePasscode.sosMessageAction.message, true);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    } else if (position == changeChatsToRemoveRow) {
                        textCell.setTextAndValue(LocaleController.getString("ChatsToRemove", R.string.ChatsToRemove),
                                String.valueOf(fakePasscode.findChatsToRemove(currentAccount).size()), true);
                        textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    }
                    break;
                }
                case 2: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == fakePasscodeDetailRow) {
                        cell.setText(LocaleController.getString("ChangeFakePasscodeInfo", R.string.ChangeFakePasscodeInfo));
                        cell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == fakePasscodeRow || position == allowFakePasscodeLoginRow || position == sosMessageRow
                    || position == clearTelegramCacheRow) {
                return 0;
            } else if (position == changeFakePasscodeRow || position == changeSosPhoneNumberRow
                    || position == changeSosMessageRow || position == changeChatsToRemoveRow) {
                return 1;
            } else if (position == fakePasscodeDetailRow) {
                return 2;
            }
            return 0;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextCheckCell.class, TextSettingsCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM | ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_actionBarDefaultSubmenuItemIcon));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(titleTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText6));
        themeDescriptions.add(new ThemeDescription(passwordEditText, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(passwordEditText, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(passwordEditText, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));
        themeDescriptions.add(new ThemeDescription(dropDown, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(dropDown, 0, null, null, new Drawable[]{dropDownDrawable}, null, Theme.key_actionBarDefaultTitle));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText7));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        return themeDescriptions;
    }
}
