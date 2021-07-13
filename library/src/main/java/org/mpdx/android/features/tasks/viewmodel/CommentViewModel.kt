package org.mpdx.android.features.tasks.viewmodel

import androidx.databinding.Bindable
import org.mpdx.android.BR
import org.mpdx.android.base.lifecycle.ChangeAwareViewModel
import org.mpdx.android.features.contacts.viewmodel.PersonViewModel
import org.mpdx.android.features.tasks.model.Comment
import org.mpdx.android.utils.toZonedDateTime

class CommentViewModel : ChangeAwareViewModel<Comment>() {
    override fun createModel() = Comment()
    override fun updateRelated(model: Comment?) {
        lazyPerson.model = model?.person
    }

    // region Model Properties
    @get:Bindable
    var body by modelStringProperty(BR.body) { it::body }
    // endregion Model Properties

    // region Related Models
    private val lazyPerson = LazyViewModel { PersonViewModel() }
    val person get() = lazyPerson.viewModel
    // endregion Related Models

    // region Transformed Properties
    val commentTime get() = model?.createdAt?.toZonedDateTime()
    // endregion Transformed Properties
}
