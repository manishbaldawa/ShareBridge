package com.sharebridge.app

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

/** Tiny info screen: shows whether both apps are installed and how to use ShareBridge. */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun installed(pkg: String) = runCatching { packageManager.getPackageInfo(pkg, 0) }.isSuccess
        fun mark(ok: Boolean) = if (ok) "installed" else "NOT FOUND"

        val body = """
            ShareBridge

            WhatsApp (com.whatsapp): ${mark(installed("com.whatsapp"))}
            Graph Messenger (ir.ilmili.telegraph): ${mark(installed("ir.ilmili.telegraph"))}

            How to use
            1. In Graph Messenger, open the post and tap Share (or use the share button in the media viewer).
            2. Choose "To WhatsApp" from the share sheet.
            3. WhatsApp opens with the media and caption ready - pick the chat and send.

            The other way round: in WhatsApp, share the post and choose "To Graph Messenger".

            Tip: if the share sheet is crowded, long-press "To WhatsApp" / "To Graph Messenger" and pin them to the top.

            Note: when several files are shared together, WhatsApp/Telegram may drop the caption. ShareBridge copies the caption to the clipboard in that case, so you can paste it.
        """.trimIndent()

        val tv = TextView(this).apply {
            text = body
            textSize = 16f
            setPadding(48, 64, 48, 48)
        }
        setContentView(ScrollView(this).apply { addView(tv) })
    }
}
