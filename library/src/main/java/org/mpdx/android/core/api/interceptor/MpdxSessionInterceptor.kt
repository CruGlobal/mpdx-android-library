package org.mpdx.android.core.api.interceptor

import android.content.Context
import android.content.SharedPreferences
import java.io.IOException
import java.net.HttpURLConnection
import okhttp3.Request
import okhttp3.Response
import org.ccci.gto.android.common.api.TheKeySession
import org.ccci.gto.android.common.api.okhttp3.interceptor.SessionInterceptor
import org.mpdx.android.base.AuthenticationListener
import org.mpdx.android.core.AuthApi
import org.mpdx.android.core.model.Authenticate

private const val HTTP_HEADER_AUTHORIZATION = "Authorization"

// TODO: Remove Key Reference
class MpdxSessionInterceptor(
    context: Context,
    private val authenticationListener: AuthenticationListener,
    private val authApi: AuthApi
) :
    SessionInterceptor<TheKeySession>(context) {

    override fun attachSession(request: Request, session: TheKeySession): Request {
        if (!session.isValid) return request

        return request.newBuilder()
            .addHeader(HTTP_HEADER_AUTHORIZATION, "Bearer ${session.id}")
            .build()
    }

    override fun loadSession(prefs: SharedPreferences) = TheKeySession(prefs, authenticationListener.getSessionGuid())

    @Throws(IOException::class)
    override fun establishSession(): TheKeySession? {
        val token = authenticationListener.getAccessToken() ?: return null
        val guid = authenticationListener.getSessionGuid() ?: return null

        val request = Authenticate().apply {
            provider = "okta"
            accessToken = token
        }
        return authApi.authenticate(request).execute().takeIf { it.isSuccessful }
            ?.body()?.takeUnless { it.hasErrors() }
            ?.dataSingle
            ?.let { TheKeySession(it.jsonWebToken, guid) }
    }

    @Throws(IOException::class)
    override fun isSessionInvalid(response: Response) = response.code == HttpURLConnection.HTTP_UNAUTHORIZED
}
