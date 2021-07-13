package org.mpdx.android.core.data.typeadapter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.ccci.gto.android.common.jsonapi.converter.TypeConverter;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Singleton
public class MapWrapperDeserializer implements TypeConverter<StringMapWrapper> {

    private Gson gson;

    @Inject
    public MapWrapperDeserializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return clazz.equals(StringMapWrapper.class);
    }

    @Nullable
    @Override
    public String toString(@Nullable StringMapWrapper value) {
        return gson.toJson(value.getMap());
    }

    @Nullable
    @Override
    public StringMapWrapper fromString(@Nullable String value) {
        Map<String, String> map = gson.fromJson(value, new TypeToken<HashMap<String, String>>() { }.getType());
        if (map == null) {
            return null;
        }
        StringMapWrapper mapper = new StringMapWrapper();
        mapper.setMap(map);
        return mapper;
    }
}
