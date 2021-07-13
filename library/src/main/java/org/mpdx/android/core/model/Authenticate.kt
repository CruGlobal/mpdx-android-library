package org.mpdx.android.core.model

import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType

@JsonApiType("authenticate")
class Authenticate {
    @JsonApiAttribute("provider")
    var provider: String? = null

    @JsonApiAttribute("cas_ticket")
    var casTicket: String? = null

    @JsonApiAttribute("access_token")
    var accessToken: String? = null

    @JsonApiAttribute("json_web_token")
    var jsonWebToken: String? = null
}
