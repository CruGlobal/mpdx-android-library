package org.mpdx.base.databinding

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.chip.Chip
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.squareup.picasso.Picasso
import com.squareup.picasso.PicassoTestUtils
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Target
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mpdx.R
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ChipBindingAdapterTest {
    private lateinit var picasso: Picasso
    private lateinit var requestCreator: RequestCreator

    private lateinit var chip: Chip

    @Before
    fun setup() {
        requestCreator = spy()
        doNothing().whenever(requestCreator).into(any<Target>())

        picasso = mock()
        whenever(picasso.load(anyOrNull<String>())).thenReturn(requestCreator)
        Picasso.setSingletonInstance(picasso)

        chip = mock()
    }

    @After
    fun cleanup() {
        PicassoTestUtils.clearSingletonInstance()
    }

    @Test
    fun verifyPicassoRequestFilters() {
        chip.addIconFromUrl("https://mpdx.org")
        verify(requestCreator).placeholder(R.drawable.cru_icon_avatar)
        verify(requestCreator).error(R.drawable.cru_icon_avatar)
        verify(requestCreator).transform(any<CropCircleTransformation>())
    }

    @Test
    fun verifyPicassoChipIconTarget() {
        chip.addIconFromUrl("https://example.com")
        argumentCaptor<Target>().apply {
            verify(requestCreator).into(capture())
            assertThat(firstValue, instanceOf(ChipIconTarget::class.java))
            assertEquals(chip, (firstValue as ChipIconTarget).chip)
        }
    }

    @Test
    fun verifyUrlValid() {
        val url = "https://mpdx.org"
        chip.addIconFromUrl(url)
        verify(picasso).load(url)
    }

    @Test
    fun verifyUrlNull() {
        chip.addIconFromUrl(null)
        verify(picasso).load(null as String?)
    }

    @Test
    fun verifyUrlEmpty() {
        chip.addIconFromUrl("")
        verify(picasso).load(null as String?)
    }

    @Test
    fun verifyUrlBlank() {
        chip.addIconFromUrl("      ")
        verify(picasso).load(null as String?)
    }
}
