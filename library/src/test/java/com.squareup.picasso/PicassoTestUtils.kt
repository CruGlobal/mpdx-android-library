package com.squareup.picasso

object PicassoTestUtils {
    fun clearSingletonInstance() {
        Picasso.singleton = null
    }
}
