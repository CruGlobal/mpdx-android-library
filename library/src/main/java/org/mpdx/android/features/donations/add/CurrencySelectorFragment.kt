package org.mpdx.android.features.donations.add

import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import org.ccci.gto.android.common.androidx.fragment.app.findListener
import org.ccci.gto.android.common.androidx.lifecycle.orEmpty
import org.ccci.gto.android.common.androidx.lifecycle.switchCombineWith
import org.mpdx.android.R
import org.mpdx.android.base.lifecycle.RealmViewModel
import org.mpdx.android.base.realm.asLiveData
import org.mpdx.android.base.realm.filterByQuery
import org.mpdx.android.base.realm.firstAsLiveData
import org.mpdx.android.core.data.api.models.CRUCurrency
import org.mpdx.android.core.data.api.models.CRUCurrencyFields
import org.mpdx.android.features.constants.realm.getConstants
import org.mpdx.android.features.selector.BaseSelectorFragment
import org.mpdx.android.features.selector.SelectorFragmentDataModel

@AndroidEntryPoint
class CurrencySelectorFragment : BaseSelectorFragment<CRUCurrency>(
    R.string.donation_add_donation_details_currency_select, CRUCurrency::getCodeSymbolString, true
) {
    override val dataModel: CurrencySelectorFragmentDataModel by viewModels()

    override fun dispatchItemSelectedCallback(item: CRUCurrency?) {
        findListener<OnCurrencySelectedListener>()?.onCurrencyAccountSelected(item)
    }
}

class CurrencySelectorFragmentDataModel : RealmViewModel(), SelectorFragmentDataModel<CRUCurrency> {
    override val query = MutableLiveData("")
    override val items = realm.getConstants().firstAsLiveData().switchCombineWith(query) { constants, query ->
        constants?.currencyOptions?.where()?.filterByQuery(CRUCurrencyFields.CODE_SYMBOL_STRING, query)?.asLiveData()
            .orEmpty()
    }
}

interface OnCurrencySelectedListener {
    fun onCurrencyAccountSelected(currency: CRUCurrency?)
}
