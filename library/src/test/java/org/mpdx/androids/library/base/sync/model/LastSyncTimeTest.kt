package org.mpdx.androids.library.base.sync.model

import org.ccci.gto.android.common.base.TimeConstants.DAY_IN_MS
import org.ccci.gto.android.common.base.TimeConstants.MIN_IN_MS
import org.ccci.gto.android.common.base.TimeConstants.WEEK_IN_MS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

private const val STALE_TIME_SHORT = MIN_IN_MS
private const val STALE_TIME_MEDIUM = DAY_IN_MS
private const val STALE_TIME_LONG = WEEK_IN_MS

class LastSyncTimeTest {
    @Test
    fun testNeedsSyncNullLastSync() {
        val lastSyncTime = LastSyncTime().apply {
            lastSync = null
        }
        assertTrue(lastSyncTime.needsSync(STALE_TIME_SHORT))
        assertTrue(lastSyncTime.needsSync(STALE_TIME_MEDIUM))
        assertTrue(lastSyncTime.needsSync(STALE_TIME_LONG))
    }

    @Test
    fun testNeedsSyncHasLastSync() {
        val lastSyncTime = LastSyncTime().apply {
            lastSync = Date(System.currentTimeMillis() - STALE_TIME_MEDIUM)
        }
        assertTrue(lastSyncTime.needsSync(STALE_TIME_SHORT))
        assertFalse(lastSyncTime.needsSync(STALE_TIME_LONG))
    }

    @Test
    fun testNeedsSyncForced() {
        val lastSyncTime = LastSyncTime().apply {
            lastSync = Date()
        }
        assertTrue(lastSyncTime.needsSync(STALE_TIME_SHORT, true))
        assertTrue(lastSyncTime.needsSync(STALE_TIME_MEDIUM, true))
        assertTrue(lastSyncTime.needsSync(STALE_TIME_LONG, true))
    }
}
