package org.mpdx.androids.library.base.sync.model

import androidx.annotation.VisibleForTesting
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.ccci.gto.android.common.base.TimeConstants.DAY_IN_MS
import org.mpdx.utils.toBpInstant
import org.threeten.bp.Instant
import java.util.Date

open class LastSyncTime(@PrimaryKey var id: String = "") : RealmObject() {
    @VisibleForTesting
    internal var lastSync: Date? = null
    private var lastFullSync: Date? = null

    fun needsSync(staleTime: Long, forced: Boolean = false) =
        forced || lastSync?.let { it.time < System.currentTimeMillis() - staleTime } != false

    fun needsFullSync(staleTime: Long, forced: Boolean = false) =
        forced || lastFullSync?.let { it.time < System.currentTimeMillis() - staleTime } != false

    fun trackSync(fullSync: Boolean = true) {
        lastSync = Date()
        if (fullSync) lastFullSync = lastSync
    }

    fun getSinceLastSyncRange(
        timeSkew: Long = DAY_IN_MS,
        defaultStart: Instant = Instant.ofEpochMilli(0)
    ): ClosedRange<Instant> =
        (lastSync?.toBpInstant()?.minusMillis(timeSkew) ?: defaultStart)
            .rangeTo(Instant.now().plusMillis(timeSkew))
}
