package org.mpdx.androids.library.base.timber

import timber.log.Timber

object ExceptionRaisingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t != null) throw t
    }
}
