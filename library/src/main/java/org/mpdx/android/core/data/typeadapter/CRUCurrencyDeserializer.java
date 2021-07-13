package org.mpdx.android.core.data.typeadapter;

import com.google.gson.Gson;

import org.ccci.gto.android.common.jsonapi.converter.TypeConverter;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Singleton
public class CRUCurrencyDeserializer implements TypeConverter<CRUCurrencyMap> {

    private Gson gson;

    @Inject
    public CRUCurrencyDeserializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return CRUCurrencyMap.class.equals(clazz);
    }

    @Nullable
    @Override
    public String toString(@Nullable CRUCurrencyMap value) {
        return gson.toJson(value);
    }

    @Nullable
    @Override
    public CRUCurrencyMap fromString(@Nullable String value) {
        return gson.fromJson(value, CRUCurrencyMap.class);
    }
}
