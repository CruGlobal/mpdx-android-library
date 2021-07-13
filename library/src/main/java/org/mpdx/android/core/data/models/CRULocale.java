package org.mpdx.android.core.data.models;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class CRULocale extends RealmObject implements Comparable<CRULocale> {
    @PrimaryKey
    private String code;
    @SerializedName("english_name")
    private String name;
    @SerializedName("native_name")
    private String nativeName;

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public String getCode() {
        return code;
    }

    public CRULocale setCode(String code) {
        this.code = code;
        return this;
    }

    public CRULocale setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public int compareTo(@NonNull CRULocale o) {
        return name.compareTo(o.name);
    }
}
