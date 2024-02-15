/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.util;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Array;

public class PreferenceJsonUtils<T> {

    public void write(SharedPreferences preferences, String key, T value) {
        final Gson gson = new GsonBuilder().create();
        final String json = gson.toJson(value);

        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, json);
        editor.commit();
    }

    public T read(Class<T> clazz, SharedPreferences preferences, String key) {
        final String json = preferences.getString(key, null);
        if (json != null) {
            final Gson gson = new GsonBuilder().create();
            return gson.fromJson(json, clazz);
        }

        if (clazz.isArray()) {
            return (T) Array.newInstance(clazz.getComponentType(), 0);
        } else {
            return null;
        }
    }
}
