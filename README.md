## Partisan-Telegram messenger for Android

A special version of Telegram that protects peaceful protesters in Belarus (can be used in other countries with authoritarian regimes as well). 
P-Telegram has two passcodes instead of one - the real passcode and a false passcode.
If a user enters the false passcode a series of pre-defined actions is performed such as:
- Sending a custom SOS message to family and a trusted contact using SMS
- Delete chats and channels that can be used against the user
- Log-out of the account on this device
- Delete all other sessions besides the current one
- And others

Usage of P-Telegram can be used against the user in Belarus to justify torture and jail, therefore P-Telegram looks and feels like the original Telegram as much as possible.

Stay safe.

### Compilation Guide

**Note**: In order to support [reproducible builds](https://core.telegram.org/reproducible-builds), this repo contains dummy release.keystore,  google-services.json and filled variables inside BuildVars.java. Before publishing your own APKs please make sure to replace all these files with your own.

You will require Android Studio 3.4, Android NDK rev. 20 and Android SDK 8.1

1. Download the Telegram source code from https://github.com/wrwrabbit/Partisan-Telegram-Android ( git clone https://github.com/wrwrabbit/Partisan-Telegram-Android.git )
2. Copy your release.keystore into TMessagesProj/config
3. Fill out RELEASE_KEY_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_STORE_PASSWORD in gradle.properties to access your release.keystore
4.  Go to https://console.firebase.google.com/, create two android apps with application IDs org.telegram.messenger and org.telegram.messenger.beta, turn on firebase messaging and download google-services.json, which should be copied to the same folder as TMessagesProj.
5. Open the project in the Studio (note that it should be opened, NOT imported).
6. Fill out values in TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java – there’s a link for each of the variables showing where and which data to obtain.
7. You are ready to compile Telegram.

