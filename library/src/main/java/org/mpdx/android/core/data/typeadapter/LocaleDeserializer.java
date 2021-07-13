package org.mpdx.android.core.data.typeadapter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.ccci.gto.android.common.jsonapi.converter.TypeConverter;
import org.mpdx.android.core.data.models.CRULocale;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Singleton
public class LocaleDeserializer implements TypeConverter<CRULocaleList> {

    private Gson gson;

    @Inject
    LocaleDeserializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return CRULocaleList.class.equals(clazz);
    }

    @Nullable
    @Override
    public String toString(@Nullable CRULocaleList value) {
        return gson.toJson(value);
    }

    @Nullable
    @Override
    public CRULocaleList fromString(@Nullable String value) {
        Map<String, CRULocale> localesMap =
                gson.fromJson(value, new TypeToken<HashMap<String, CRULocale>>() { }.getType());
        if (localesMap == null) {
            return null;
        }
        CRULocaleList cruLocaleList = new CRULocaleList();
        for (String item : localesMap.keySet()) {
            CRULocale locale = localesMap.get(item);
            locale.setCode(item);
            cruLocaleList.add(locale);
        }

        return cruLocaleList;
    }
}
