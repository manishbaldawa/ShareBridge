# ShareBridge

Forward a post (media + caption) between **Graph Messenger** (`ir.ilmili.telegraph`) and **WhatsApp** (`com.whatsapp`) with the normal Android share button.

ShareBridge adds two entries to the Android share sheet:

- **To WhatsApp** - opens the standard WhatsApp with the media and caption ready
- **To Graph Messenger** - opens Graph Messenger with the media and caption ready

Media is copied to ShareBridge's cache and handed over through a FileProvider, so the receiving app can always read it. Caption text (`EXTRA_TEXT`) is passed along unchanged. No internet permission, no accounts, nothing leaves your phone.

## Build the APK

### Option A - GitHub Actions (no setup)
1. Create a new GitHub repository and upload everything in this folder (keep `.github/workflows/build.yml`).
2. Open the **Actions** tab, run **Build APK** (it also runs on every push to `main`).
3. Download the `ShareBridge-apk` artifact, unzip it and install `app-debug.apk`.

### Option B - Android Studio
Open this folder, wait for Gradle sync, then **Build > Build APK(s)**. The file lands in `app/build/outputs/apk/debug/`.

### Option C - command line
With the Android SDK and Gradle 8.7+ installed: `gradle assembleDebug`

## Use it
- **Graph -> WhatsApp:** open the post, tap Share, choose **To WhatsApp**, pick the chat in WhatsApp.
- **WhatsApp -> Graph:** share the post from WhatsApp, choose **To Graph Messenger**, pick the chat.
- Long-press the two ShareBridge entries in the share sheet to pin them to the top.

## Known limits
- Whether the caption travels depends on what the sending app puts in the share. If the sending app doesn't include it, there is nothing for ShareBridge to forward.
- With several files in one share, WhatsApp/Telegram often ignore the caption; ShareBridge copies it to the clipboard so you can paste it.
- Telegram-style apps may open their own in-app chat picker for the floating share button instead of the Android share sheet. If that happens, use the Share option in the media viewer or the message menu, which hands over to the system share sheet.
