package org.mpdx.android.core.api.interceptor

import androidx.annotation.VisibleForTesting
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.mpdx.android.base.api.ApiConstants.FILTER_ACCOUNT_LIST_ID
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.utils.NetUtils
import timber.log.Timber

private const val TAG = "MpdxRequestInterceptor"

@VisibleForTesting
internal const val HTTP_HEADER_ACCEPT_LANGUAGE = "Accept-Language"
@VisibleForTesting
internal const val HTTP_HEADER_CONTENT_TYPE = "Content-Type"

class MpdxRequestInterceptor(private val appPrefs: AppPrefs) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val method = chain.request().method
        var url = chain.request().url

        // populate the accountListId filter if it hasn't been populated already
        val accountListId = appPrefs.accountListId
        if (!accountListId.isNullOrEmpty() && needsAccountListFilter(method, url)) {
            // This error will be escalated with time as we start addressing the issue
            Timber.tag(TAG).w("account_list_id filter being injected into request: %s %s", method, url.encodedPath)
            Timber.tag(TAG).e(UnsupportedOperationException("automated filtering"))

            url = url.newBuilder()
                .addQueryParameter(FILTER_ACCOUNT_LIST_ID, accountListId)
                .build()
        }

        // build request to send
        val request = chain.request().newBuilder()
            .url(url)
            .header(HTTP_HEADER_ACCEPT_LANGUAGE, NetUtils.getPreferredLanaguage())
            .header(HTTP_HEADER_CONTENT_TYPE, JsonApiObject.MEDIA_TYPE)
            .build()
        return chain.proceed(request)
    }

    private fun needsAccountListFilter(method: String, url: HttpUrl) = when {
        url.encodedPath.contains("""/account_lists$""".toRegex()) -> false
        url.encodedPath.contains("""/account_lists/""".toRegex()) -> false
        url.encodedPath.contains("""/appeals/[0-9a-fA-F\\-]{36}$""".toRegex()) -> false
        url.encodedPath.contains("""/appeals/[0-9a-fA-F\\-]{36}/appeal_contacts""".toRegex()) -> false
        url.encodedPath.contains("""/appeals/[0-9a-fA-F\\-]{36}/excluded_appeal_contacts""".toRegex()) -> false
        url.encodedPath.contains("""/constants$""".toRegex()) -> false
        url.encodedPath.contains("""/contacts/[0-9a-fA-F\\-]{36}$""".toRegex()) -> false
        url.encodedPath.contains("""/contacts/[0-9a-fA-F\\-]{36}/addresses""".toRegex()) -> false
        url.encodedPath.contains("""/contacts/[0-9a-fA-F\\-]{36}/people""".toRegex()) -> false
        url.encodedPath.contains("""/deleted_records$""".toRegex()) -> false
        url.encodedPath.contains("""/tasks$""".toRegex()) -> false
        url.encodedPath.contains("""/tasks/""".toRegex()) -> false
        url.encodedPath.contains("""/user$""".toRegex()) -> false
        url.encodedPath.contains("""/user/account_list_coaches$""".toRegex()) -> false
        url.encodedPath.contains("""/user/devices$""".toRegex()) -> false
        url.encodedPath.contains("""/user/notifications""".toRegex()) -> false
        url.encodedPath.contains("""/user/authenticate""".toRegex()) -> false
        method == "POST" -> false
        else -> url.queryParameter(FILTER_ACCOUNT_LIST_ID) == null
    }
}
