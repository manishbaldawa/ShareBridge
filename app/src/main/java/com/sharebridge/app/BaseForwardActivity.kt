package com.sharebridge.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import java.io.File
import java.util.UUID

/**
 * Receives a normal Android share (ACTION_SEND / ACTION_SEND_MULTIPLE) from any app,
 * copies the media into this app's cache (so the receiving app can always read it),
 * and re-sends it, together with the caption text, straight to [targetPackage].
 */
abstract class BaseForwardActivity : Activity() {

    abstract val targetPackage: String
    abstract val targetLabel: String

    private class Payload(val text: String?, val uris: List<Uri>, val mimes: List<String>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incoming = intent
        val action = incoming?.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            finish()
            return
        }
        toast("Sending to $targetLabel…")
        Thread {
            val result = runCatching { prepare(incoming) }
            runOnUiThread {
                result
                    .onSuccess { launchTarget(it) }
                    .onFailure { toast("Couldn't read the shared post: ${it.message}") }
                finish()
            }
        }.start()
    }

    // ---------- reading what was shared ----------

    private fun prepare(incoming: Intent): Payload {
        val text = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            ?: incoming.clipData?.let { cd ->
                (0 until cd.itemCount).firstNotNullOfOrNull { cd.getItemAt(it).text?.toString()?.takeIf { s -> s.isNotBlank() } }
            }

        val sources = ArrayList<Uri>()
        if (incoming.action == Intent.ACTION_SEND) {
            IntentCompat.getParcelableExtra(incoming, Intent.EXTRA_STREAM, Uri::class.java)?.let { sources.add(it) }
        } else {
            IntentCompat.getParcelableArrayListExtra(incoming, Intent.EXTRA_STREAM, Uri::class.java)?.let { sources.addAll(it) }
        }
        if (sources.isEmpty()) {
            incoming.clipData?.let { cd ->
                for (i in 0 until cd.itemCount) cd.getItemAt(i).uri?.let { sources.add(it) }
            }
        }

        val dir = File(cacheDir, "shared").apply { mkdirs() }
        cleanOldFiles(dir)

        val uris = ArrayList<Uri>()
        val mimes = ArrayList<String>()
        for (src in sources) {
            val (uri, mime) = copyToCache(src, dir)
            uris.add(uri)
            mimes.add(mime)
        }
        return Payload(text, uris, mimes)
    }

    private fun copyToCache(src: Uri, dir: File): Pair<Uri, String> {
        val name = displayName(src) ?: "file"
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dest = File(dir, "${UUID.randomUUID().toString().take(8)}_$safe")
        val input = contentResolver.openInputStream(src) ?: error("cannot open $src")
        input.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
        val mime = contentResolver.getType(src) ?: guessMime(safe)
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", dest) to mime
    }

    private fun displayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    private fun guessMime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun cleanOldFiles(dir: File) {
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }

    // ---------- re-sharing ----------

    private fun launchTarget(p: Payload) {
        val multi = p.uris.size > 1
        val send = Intent(if (multi) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND)
        send.setPackage(targetPackage)
        send.type = when {
            p.uris.isEmpty() -> "text/plain"
            !multi -> p.mimes[0]
            else -> commonType(p.mimes)
        }
        p.text?.let { send.putExtra(Intent.EXTRA_TEXT, it) }

        if (p.uris.isNotEmpty()) {
            if (multi) send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(p.uris))
            else send.putExtra(Intent.EXTRA_STREAM, p.uris[0])
            val clip = ClipData.newUri(contentResolver, "shared", p.uris[0])
            p.uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            send.clipData = clip
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Most apps ignore the caption when several files are sent at once, so keep it on the clipboard too.
        if (multi && p.text != null) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("caption", p.text))
            toast("Caption copied - paste it if it doesn't appear")
        }

        try {
            startActivity(send)
        } catch (e: ActivityNotFoundException) {
            toast("$targetLabel isn't installed or can't receive this share")
        }
    }

    private fun commonType(mimes: List<String>): String {
        if (mimes.all { it == mimes[0] }) return mimes[0]
        val top = mimes.map { it.substringBefore('/') }
        return if (top.all { it == top[0] }) "${top[0]}/*" else "*/*"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
