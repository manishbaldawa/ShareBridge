package com.sharebridge.app

/** Share target that forwards to the standard WhatsApp. */
class ToWhatsAppActivity : BaseForwardActivity() {
    override val targetPackage = "com.whatsapp"
    override val targetLabel = "WhatsApp"
}
