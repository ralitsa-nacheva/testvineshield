package com.rncoding.testvineshield.core.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VineyardLinkInboxTest {
    @Test
    fun newerRequestForSameVineyardCannotBeConsumedByOldRequest() {
        val inbox = VineyardLinkInbox()
        inbox.offer(42)
        val first = assertNotNull(inbox.pending)
        inbox.offer(42)
        val second = assertNotNull(inbox.pending)

        assertFalse(inbox.consume(first))
        assertSame(second, inbox.pending)
        assertTrue(inbox.consume(second))
        assertFalse(inbox.consume(second))
        assertNull(inbox.pending)
    }

    @Test
    fun invalidIdsLeavePendingRequestIntact() {
        val inbox = VineyardLinkInbox()
        inbox.offer(42)
        val request = assertNotNull(inbox.pending)
        inbox.offer(0)
        inbox.offer(-1)
        assertSame(request, inbox.pending)
    }
}
