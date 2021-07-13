package org.mpdx.android.features.base.fragments;

import android.app.Dialog;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.LayoutRes;
import butterknife.ButterKnife;

public abstract class BaseBottomSheetFragment extends BottomSheetDialogFragment {
    protected BottomSheetBehavior behavior;
    protected View dialogView;

    @Override
    public void setupDialog(Dialog dialog, int style) {
        dialogView = View.inflate(getContext(), layoutRes(), null);
        ButterKnife.bind(this, dialogView);
        dialog.setContentView(dialogView);
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            behavior = BottomSheetBehavior.from(bottomSheet);
            dialog.setOnShowListener(d -> behavior.setState(BottomSheetBehavior.STATE_EXPANDED));
        }
    }

    @LayoutRes
    protected abstract int layoutRes();

}
