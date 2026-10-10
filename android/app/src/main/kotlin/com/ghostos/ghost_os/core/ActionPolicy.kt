package com.ghostos.ghost_os.core

enum class ActionDisposition { EXECUTE, WAIT_FOR_UNLOCK, WAIT_FOR_FOREGROUND, REVIEW }

object ActionPolicy {
    fun requiresReview(kind: String) = kind in setOf("message", "call", "notificationReply", "delete")
    fun decide(kind: String, locked: Boolean, launchAllowed: Boolean): ActionDisposition = when {
        requiresReview(kind) -> ActionDisposition.REVIEW
        locked -> ActionDisposition.WAIT_FOR_UNLOCK
        launchAllowed -> ActionDisposition.EXECUTE
        else -> ActionDisposition.WAIT_FOR_FOREGROUND
    }
}
