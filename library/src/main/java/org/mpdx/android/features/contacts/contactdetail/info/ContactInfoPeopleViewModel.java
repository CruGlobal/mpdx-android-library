package org.mpdx.android.features.contacts.contactdetail.info;

import android.view.View;

import org.mpdx.android.features.contacts.model.EmailAddress;
import org.mpdx.android.features.contacts.model.PhoneNumber;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;

public class ContactInfoPeopleViewModel extends BaseObservable {
    @NonNull
    private List<EmailAddress> mEmailAddresses = Collections.emptyList();
    @NonNull
    private List<PhoneNumber> mPhoneNumbers = Collections.emptyList();

    @NonNull
    public List<EmailAddress> getEmailAddresses() {
        return mEmailAddresses;
    }

    public void setEmailAddresses(@NonNull List<EmailAddress> mEmailAddresses) {
        this.mEmailAddresses = mEmailAddresses;
    }

    @NonNull
    public List<PhoneNumber> getPhoneNumbers() {
        return mPhoneNumbers;
    }

    public void setPhoneNumbers(@NonNull List<PhoneNumber> mPhoneNumbers) {
        this.mPhoneNumbers = mPhoneNumbers;
    }

    public int getNoPhoneNumbersLabelVisibility() {
        if (!mPhoneNumbers.isEmpty()) {
            return View.INVISIBLE;
        }

        return View.VISIBLE;
    }

    public int getPhoneNumbersLabelVisibilty() {
        if (mPhoneNumbers.isEmpty()) {
            return View.INVISIBLE;
        }
         return View.VISIBLE;
    }

    public int getNoEmailAddressesLabelVisibility() {
        if (!mEmailAddresses.isEmpty()) {
            return View.INVISIBLE;
        }

        return View.VISIBLE;
    }

    public int getEmailAddressesLabelVisibility() {
        if (mEmailAddresses.isEmpty()) {
            return View.INVISIBLE;
        }

        return View.VISIBLE;
    }
}
