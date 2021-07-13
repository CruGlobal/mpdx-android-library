package org.mpdx.android.features.donations.viewmodel

import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.features.donations.model.Donation
import org.mpdx.android.utils.toLocalDate

class DonationViewModel : ChangeAwareViewModel<Donation>() {
    // region Model Properties
    val amount by modelNullableProperty { it::amount }
    val currency by modelStringProperty { it::currency }
    val convertedAmount by modelNullableProperty { it::convertedAmount }
    val convertedCurrency by modelStringProperty { it::convertedCurrency }
    val convertedAppealAmount by modelNullableProperty { it::convertedAppealAmount }
    // endregion Model Properties

    // region Transformed Properties
    val donationDate get() = model?.donationDate?.toLocalDate()
    // endregion Transformed Properties
}
