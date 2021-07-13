package org.mpdx.android.features.coaching;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import org.mpdx.android.CoachingViewBinding;
import org.mpdx.android.R;
import org.mpdx.android.core.model.AccountList;
import org.mpdx.android.features.coaching.viewmodel.AccountListViewModel;

import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;

public class CoachingView extends FrameLayout {
    private CoachingViewBinding binding;
    private final AccountListViewModel mAccountList = new AccountListViewModel();

    public CoachingView(Context context) {
        super(context);
        init();
    }

    public CoachingView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public CoachingView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CoachingView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.view_coaching, this, true);
        binding.setAccountList(mAccountList);
    }

    public void setListener(CoachingSelectedListener listener) {
        setOnClickListener(v -> {
            final AccountList list = mAccountList.getModel();
            if (list != null) {
                listener.onCoachingSelected(list.getId());
            }
        });
    }

    public void setAccountList(AccountList account) {
        mAccountList.setModel(account);
        binding.executePendingBindings();
    }

    public void setTitle(int titleRes) {
        binding.coachingTitle.setText(titleRes);
    }

    public interface CoachingSelectedListener {
        void onCoachingSelected(String id);
    }
}
