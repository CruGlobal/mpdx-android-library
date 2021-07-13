package org.mpdx.android.core.data.typeadapter;

import com.google.gson.Gson;

import org.ccci.gto.android.common.jsonapi.converter.TypeConverter;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CRUListDeserializer implements TypeConverter<CRUListMap> {

    private Gson gson;

    @Inject
    public CRUListDeserializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return CRUListMap.class.equals(clazz);
    }

    @Nullable
    @Override
    public String toString(@Nullable CRUListMap value) {
        return gson.toJson(value);
    }

    @Nullable
    @Override
    public CRUListMap fromString(@Nullable String value) {
        return gson.fromJson(value, CRUListMap.class);
    }
}
