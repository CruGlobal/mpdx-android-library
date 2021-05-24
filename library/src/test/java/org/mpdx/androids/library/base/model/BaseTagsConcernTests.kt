package org.mpdx.base.model

import io.realm.RealmList
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasSize
import org.junit.Test
import org.junit.runner.RunWith
import org.mpdx.androids.library.base.model.TagsConcern
import org.mpdx.androids.library.base.model.addTag

@RunWith(JUnitParamsRunner::class)
abstract class BaseTagsConcernTests {
    companion object {
        internal const val TAG1 = "tag1"
        internal const val TAG1_WS = "  tag1  "
        internal const val TAG2 = "tag2"
    }

    protected abstract fun model(): TagsConcern

    @Test
    fun verifyAddTagValid() {
        val model = model()
        model.addTag(TAG1)
        assertThat(model.tags, contains(TAG1))
    }

    @Test
    fun verifyAddTagDistinct() {
        val model = model()
        model.tags = RealmList(TAG2)
        model.addTag(TAG1)
        model.addTag(TAG2)
        assertThat(model.tags, allOf(hasSize(2), contains(TAG2, TAG1)))
    }

    @Test
    fun verifyAddTagTrim() {
        val model = model()
        model.tags = RealmList(TAG1)
        model.addTag(TAG1_WS)
        assertThat(model.tags, allOf(hasSize(1), contains(TAG1)))
    }

    @Test
    @Parameters("", "    ", " \n ")
    fun verifyAddTagInvalid(tag: String) {
        val model = model()
        model.addTag(tag)
        assertThat(model.tags, Matchers.empty())
    }
}
