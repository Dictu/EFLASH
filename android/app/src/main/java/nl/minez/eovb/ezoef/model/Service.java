/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Service implements Parcelable {

    private static final String SERVICES_PREFS_KEY = "ServicesPreferencesKey";
    private static final String CACHED_JSON_SERVICES_DATE_KEY = "CachedJsonServicesDateKey";
    private static final String CACHED_JSON_SERVICES_KEY = "CachedJsonServicesKey";

    public Long id = 0L;
    public String title = "";
    public List<Location> locations = Collections.emptyList();

    public Service(Long id, String title, List<Location> locations) {
        this.id = id;
        this.title = title;
        this.locations = locations;
    }

    public Service(String name, JSONObject jsonObject) {
        try {
            this.id = jsonObject.getLong("id");
            this.title = name;

            final JSONArray locations = jsonObject.getJSONArray("messagetypes");
            if (locations != null && locations.length() > 0) {
                this.locations = Location.fromJSON(locations);
            }
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Error parsing JSON: " + e);
        }
    }

    public Service(JSONObject jsonObject) {
        try {
            this.id = jsonObject.getLong("id");
            this.title = jsonObject.getString("name");
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Error parsing JSON: " + e);
        }
    }

    protected Service(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.title = in.readString();
        this.locations = new ArrayList<>();
        in.readList(this.locations, Location.class.getClassLoader());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        final Service service = (Service) o;

        return this.id != null ? this.id.equals(service.id) : service.id == null;

    }

    @Override
    public int hashCode() {
        int result = this.id != null ? this.id.hashCode() : 0;
        result = 31 * result + (this.title != null ? this.title.hashCode() : 0);
        result = 31 * result + (this.locations != null ? this.locations.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeString(this.title);
        dest.writeList(this.locations);
    }

    public static List<Service> fromJSON(JSONObject jsonObject) {
        final ArrayList<Service> services = new ArrayList<>();

        if (jsonObject.keys() != null) {
            final Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                final String service = iterator.next();
                if ("alle diensten".equals(service.toLowerCase())) {
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
        return Collections.unmodifiableList(services);
    }

    private static SharedPreferences getServicePreferences(Context context) {
        return context.getSharedPreferences(SERVICES_PREFS_KEY, Context.MODE_PRIVATE);
    }

    public static void fillJsonCache(Context context, JSONObject response) {
        final SharedPreferences.Editor editor = getServicePreferences(context).edit();
        editor.putLong(CACHED_JSON_SERVICES_DATE_KEY, new Date().getTime());
        editor.putString(CACHED_JSON_SERVICES_KEY, response.toString());
        editor.commit();
    }

    public static void clearJsonCache(Context context) {
        final SharedPreferences.Editor editor = getServicePreferences(context).edit();
        editor.remove(CACHED_JSON_SERVICES_DATE_KEY);
        editor.remove(CACHED_JSON_SERVICES_KEY);
        editor.commit();
    }

    public static JSONObject jsonFromCache(Context context) {
        final SharedPreferences servicePreferences = getServicePreferences(context);

                final String cachedJsonServices = servicePreferences.getString(CACHED_JSON_SERVICES_KEY, null);
                if (cachedJsonServices != null) {
                    try {
                        return new JSONObject(cachedJsonServices);
                    } catch (JSONException e) {
                        Log.i(Service.class.getSimpleName(), "Error parsing JSON cache", e);
                    }
                }
        return null;
    }

    public static final Creator<Service> CREATOR = new Creator<Service>() {
        @Override
        public Service createFromParcel(Parcel in) {
            return new Service(in);
        }

        @Override
        public Service[] newArray(int size) {
            return new Service[size];
        }
    };

}
