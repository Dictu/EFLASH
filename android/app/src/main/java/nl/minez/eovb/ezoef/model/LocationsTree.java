/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class LocationsTree {

    private static final String LOCATIONS_TREE_PREFS_KEY = "LocationsTreeKey";
    private static final String CACHED_JSON_LOCATIONS_TREE_KEY = "CachedJsonLocationsTreeKey";
    private static final String JSON_KEY_ALLE_DIENSTEN = "alle diensten";

    public List<Service> services = Collections.emptyList();

    public LocationsTree(List<Service> services) {
        this.services = services;
    }

    public static LocationsTree fromJSON(JSONObject jsonObject) {
        final ArrayList<Service> services = new ArrayList<>();

        if (jsonObject.keys() != null) {
            final Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                final String service = iterator.next();
                if (JSON_KEY_ALLE_DIENSTEN.equals(service.toLowerCase())) {
                    continue;
                }

                try {
                    JSONObject location = (JSONObject) jsonObject.get(service);
                    services.add(new Service(service, location));
                } catch (JSONException e) {
                    Log.e(Update.class.getSimpleName(), "Error parsing JSON: " + e);
                }
            }
        }

        Collections.sort(services, new Comparator<Service>() {
            @Override
            public int compare(Service service1, Service service2) {
                if (service1.title != null && service2.title != null) {
                    return service1.title.compareTo(service2.title);
                }
                return 0;
            }
        });
        return new LocationsTree(services);
    }


    private static SharedPreferences getLocationTreePreferences(Context context) {
        return context.getSharedPreferences(LOCATIONS_TREE_PREFS_KEY, Context.MODE_PRIVATE);
    }

    public static void fillJsonCache(Context context, JSONObject response) {
        final SharedPreferences.Editor editor = getLocationTreePreferences(context).edit();
        editor.putString(CACHED_JSON_LOCATIONS_TREE_KEY, response.toString());
        editor.commit();
    }

    public static void clearJsonCache(Context context) {
        final SharedPreferences.Editor editor = getLocationTreePreferences(context).edit();
        editor.remove(CACHED_JSON_LOCATIONS_TREE_KEY);
        editor.commit();
    }

    public static JSONObject jsonFromCache(Context context) {
        final SharedPreferences servicePreferences = getLocationTreePreferences(context);
        final String cachedJsonServices = servicePreferences.getString(CACHED_JSON_LOCATIONS_TREE_KEY, null);
        if (cachedJsonServices != null) {
            try {
                return new JSONObject(cachedJsonServices);
            } catch (JSONException e) {
                //#IFDEF 'debug'
                Log.i(LocationsTree.class.getSimpleName(), "Error parsing JSON cache", e);
                //#ENDIF
            }
        }
        return null;
    }

}
