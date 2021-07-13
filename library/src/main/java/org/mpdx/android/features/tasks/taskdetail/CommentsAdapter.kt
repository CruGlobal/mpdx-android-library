package org.mpdx.android.features.tasks.taskdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mpdx.android.CommentItemBinding
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.features.tasks.model.Comment
import org.mpdx.android.features.tasks.viewmodel.CommentViewModel

class CommentsAdapter : UniqueItemRealmDataBindingAdapter<Comment, CommentItemBinding>() {
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        CommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            comment = CommentViewModel()
        }

    override fun onBindViewDataBinding(binding: CommentItemBinding, position: Int) {
        binding.comment?.model = getItem(position)
    }
}
