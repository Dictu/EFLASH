/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class JsonUtils {

    public static Object[] parseJsonPropertyValues(final JSONArray jsonArray, final String propertyName) {
        final ArrayList<Object> values = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                final JSONObject jsonObject = jsonArray.getJSONObject(i);
                final Object value = jsonObject.get(propertyName);
                if (value != null) {
                    values.add(value);
                }
            } catch (JSONException e) {
                Log.e(JsonUtils.class.getSimpleName(), "Error parsing JSON: " + e);
            }
        }

        return values.toArray();
    }

    public static String combineWithSeparator(final String separator, final Object... objects) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (Object object : objects) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append(separator).append(object);
            } else {
                stringBuilder.append(object);
            }
        }
        return stringBuilder.toString();
    }

}
