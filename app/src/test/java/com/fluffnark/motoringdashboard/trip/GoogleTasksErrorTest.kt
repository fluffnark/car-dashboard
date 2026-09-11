package com.fluffnark.motoringdashboard.trip

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleTasksErrorTest {
    @Test fun explainsOAuthClientMismatch() {
        assertEquals("OAUTH CLIENT MISMATCH", googleTasksErrorLabel(ApiException(Status(10))))
    }

    @Test fun explainsUnverifiedTestUser() {
        assertEquals("ADD OAUTH TEST USER", googleTasksErrorLabel(
            IllegalStateException("403 access_denied: app has not completed the Google verification process")))
    }

    @Test fun explainsDisabledApi() {
        assertEquals("ENABLE TASKS API", googleTasksErrorLabel(
            IllegalStateException("Google Tasks API is disabled for this project")))
    }
}
