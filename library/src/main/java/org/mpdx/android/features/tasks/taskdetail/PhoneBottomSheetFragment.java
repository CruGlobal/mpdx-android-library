package org.mpdx.android.features.tasks.taskdetail;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.databinding.PhoneNumberItemBinding;
import org.mpdx.android.features.base.fragments.BaseBottomSheetFragment;
import org.mpdx.android.features.contacts.model.PhoneNumber;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PhoneBottomSheetFragment extends BaseBottomSheetFragment implements View.OnClickListener {

    @SuppressWarnings("HardCodedStringLiteral")
    private static final String PHONE_NUMBERS = "PHONE_NUMBERS";
    @SuppressWarnings("HardCodedStringLiteral")
    private static final String ACTIVITY_TYPE = "ACTIVITY_TYPE";

    @BindView(R2.id.phone_number_container) LinearLayout container;

    private List<PhoneNumber> phoneNumbers;
    private AllowedActivityTypes activityType;

    public static PhoneBottomSheetFragment newInstance(List<PhoneNumber> phoneNumbers, AllowedActivityTypes activity) {
        PhoneBottomSheetFragment fragment = new PhoneBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ACTIVITY_TYPE, activity);

        ArrayList<PhoneNumber> array = new ArrayList<>();
        array.addAll(phoneNumbers);
        args.putParcelableArrayList(PHONE_NUMBERS, array);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.phoneNumbers = this.getArguments().getParcelableArrayList(PHONE_NUMBERS);
        this.activityType = (AllowedActivityTypes) this.getArguments().getSerializable(ACTIVITY_TYPE);
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        for (PhoneNumber phoneNumber : phoneNumbers) {
            addView(container, phoneNumber);
        }
        behavior.setBottomSheetCallback(bottomSheetBehaviorCallback);
    }

    @Override
    protected int layoutRes() {
        return R.layout.bottom_sheet_phone_numbers;
    }

    private void addView(ViewGroup viewGroup, PhoneNumber phoneNumber) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        PhoneNumberItemBinding binding = PhoneNumberItemBinding.inflate(inflater, viewGroup, true);
        binding.phoneNumberItemContainer.setOnClickListener(this);
        binding.setNameAndPhoneNumber(phoneNumber.getNumber());
        binding.setLocation(phoneNumber.getLocation());
        binding.notifyChange();
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public void onClick(View view) {
        int index = container.indexOfChild(view);
        PhoneNumber phoneNumber = phoneNumbers.get(index);
        if (activityType == AllowedActivityTypes.CALL) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber.getNumber()));
            dismiss();
            startActivity(intent);
        } else if (activityType == AllowedActivityTypes.TEXT_MESSAGE) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("sms:" + phoneNumber.getNumber()));
            dismiss();
            startActivity(intent);
        }
    }

    private BottomSheetBehavior.BottomSheetCallback bottomSheetBehaviorCallback =
            new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dismiss();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                }
            };
}
