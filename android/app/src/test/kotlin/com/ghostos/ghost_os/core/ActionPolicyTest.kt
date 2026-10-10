package com.ghostos.ghost_os.core

import org.junit.Assert.*
import org.junit.Test

class ActionPolicyTest {
    @Test fun unlockedAlarmHasNoConfirmation() {
        assertEquals(ActionDisposition.EXECUTE, ActionPolicy.decide("alarm", false, true))
    }
    @Test fun lockedAlarmWaitsEvenIfLaunchCapabilityExists() {
        assertEquals(ActionDisposition.WAIT_FOR_UNLOCK, ActionPolicy.decide("alarm", true, true))
    }
    @Test fun backgroundWithoutLaunchPermissionWaits() {
        assertEquals(ActionDisposition.WAIT_FOR_FOREGROUND, ActionPolicy.decide("alarm", false, false))
    }
    @Test fun messageAlwaysRequiresReview() {
        assertEquals(ActionDisposition.REVIEW, ActionPolicy.decide("message", false, true))
        assertEquals(ActionDisposition.REVIEW, ActionPolicy.decide("message", true, true))
    }
    @Test fun phoneCallAlwaysRequiresReview() {
        assertEquals(ActionDisposition.REVIEW, ActionPolicy.decide("call", false, true))
        assertEquals(ActionDisposition.REVIEW, ActionPolicy.decide("call", true, true))
    }
    @Test fun notificationReplyAlwaysRequiresReview() {
        assertEquals(ActionDisposition.REVIEW, ActionPolicy.decide("notificationReply", false, true))
    }
}
