package org.telegram.messenger.fakepasscode

import android.util.Base64
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.messenger.Utilities
import org.telegram.tgnet.TLRPC
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

class AccountActions(var accountNum: Int) : Action {
    var removeChatsAction = RemoveChatsAction()
        private set
    var telegramMessageAction = TelegramMessageAction()
        private set
    private var deleteContactsAction: DeleteContactsAction? = null
    private var deleteStickersAction: DeleteStickersAction? = null
    private var clearSearchHistoryAction: ClearSearchHistoryAction? = null
    private var clearBlackListAction: ClearBlackListAction? = null
    private var clearSavedChannelsAction: ClearSavedChannelsAction? = null
    var terminateOtherSessionsAction = TerminateOtherSessionsAction()
        private set
    private var logOutAction: LogOutAction? = null
    private var hideAccountAction: HideAccountAction? = null
    var fakePhone = ""
    var sessionsToHide = CheckedSessions()
        private set
    private var salt: String? = null
    private var idHash: String? = null

    override fun execute() {
        listOfNotNull(
            removeChatsAction, telegramMessageAction, deleteContactsAction,
            deleteStickersAction, clearSearchHistoryAction, clearBlackListAction,
            clearSavedChannelsAction, terminateOtherSessionsAction, logOutAction,
            hideAccountAction
        ).forEach { it.execute() }
    }

    private fun getSalt(): ByteArray {
        if (salt == null) {
            val saltBytes = ByteArray(16)
            Utilities.random.nextBytes(saltBytes)
            salt = Base64.encodeToString(saltBytes, Base64.DEFAULT)
        }
        return Base64.decode(salt, Base64.DEFAULT)
    }

    private fun calculateIdHash(user: TLRPC.User): String {
        val phoneDigits = user.phone.filter { it.isDigit() }
        if (phoneDigits.length < 4) {
            throw Exception("Can't calculate id hash: invalid phone")
        }
        val phoneId = phoneDigits.slice(phoneDigits.length - 4 until phoneDigits.length).toInt()
        val sum = (user.id % 10_000 + phoneId) % 10_000
        val sumBytes = sum.toString().encodeToByteArray()
        val bytes = ByteArray(32 + sumBytes.size)
        val salt = getSalt()
        System.arraycopy(salt, 0, bytes, 0, 16)
        System.arraycopy(sumBytes, 0, bytes, 16, sumBytes.size)
        System.arraycopy(salt, 0, bytes, sumBytes.size + 16, 16)
        return Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.size))
    }

    private fun getIdHash(): String {
        val userConfig = UserConfig.getInstance(accountNum)
        if (idHash == null && userConfig.isClientActivated) {
            idHash = calculateIdHash(userConfig.currentUser)
        }
        return idHash ?: ""
    }

    inline fun <reified T> reverse(action: T?): T? {
        if (action != null) {
            return null
        }
        return T::class.constructors.find { it.parameters.isEmpty() }?.call()
    }

    fun toggleDeleteContactsAction() { deleteContactsAction = reverse(deleteContactsAction) }
    fun toggleDeleteStickersAction() { deleteStickersAction = reverse(deleteStickersAction) }
    fun toggleClearSearchHistoryAction() { clearSearchHistoryAction = reverse(clearSearchHistoryAction) }
    fun toggleClearBlackListAction() { clearBlackListAction = reverse(clearBlackListAction) }
    fun toggleClearSavedChannelsAction() { clearSavedChannelsAction = reverse(clearSavedChannelsAction) }
    fun toggleLogOutAction() { logOutAction = reverse(logOutAction) }
    fun toggleHideAccountAction() { hideAccountAction = reverse(hideAccountAction) }

    fun setSessionsToHide(sessions: List<Long?>?) {
        sessionsToHide.sessions = sessions
        SharedConfig.saveConfig()
    }

    fun isDeleteContacts() = deleteContactsAction != null
    fun isDeleteStickers() = deleteStickersAction != null
    fun isClearSearchHistory() = clearSearchHistoryAction != null
    fun isClearBlackList() = clearBlackListAction != null
    fun isClearSavedChannels() = clearSavedChannelsAction != null
    fun isLogOut() = logOutAction != null
    fun isHideAccount() = hideAccountAction != null
    fun isLogOutOrHideAccount() = logOutAction != null || hideAccountAction != null

    fun getChatsToRemoveCount() = removeChatsAction.chatEntriesToRemove.size

    fun removeFakePhone() {
        fakePhone = ""
    }

    fun <T: AccountAction> setAction(action: T) {
        for (property in AccountActions::class.memberProperties) {
            if (property.returnType == action.javaClass && property is KMutableProperty<*>) {
                property.setter.call(action);
            }
        }
    }
}