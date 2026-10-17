package com.sharebridge.app

/** Share target that forwards to Graph Messenger. */
class ToGraphActivity : BaseForwardActivity() {
    override val targetPackage = "ir.ilmili.telegraph"
    override val targetLabel = "Graph Messenger"
}
