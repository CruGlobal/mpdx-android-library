package org.mpdx.android.core.data.api.models;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;
import io.realm.RealmObject;

public class CRUCurrency extends RealmObject implements Comparable<CRUCurrency> {
    @SerializedName("code") private String code;
    @SerializedName("code_symbol_string") private String codeSymbolString;
    @SerializedName("name") private String name;
    @SerializedName("symbol") private String symbol;

    public String getCode() {
        return code;
    }

    public String getCodeSymbolString() {
        return TextUtils.isEmpty(codeSymbolString) ? "" : codeSymbolString;
    }

    @Override
    public int compareTo(@NonNull CRUCurrency o) {
        return codeSymbolString.compareTo(o.codeSymbolString);
    }
}
