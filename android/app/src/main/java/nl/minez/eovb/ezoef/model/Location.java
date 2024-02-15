/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Location implements Parcelable {

    public Long id = 0L;
    public String title = "";

    public Location(JSONObject jsonObject) {
        try {
            this.id = jsonObject.getLong("id");
            this.title = jsonObject.getString("name");
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Error parsing JSON: " + e);
        }
    }

    protected Location(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.title = in.readString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        final Location location = (Location) o;

        return this.id != null ? this.id.equals(location.id) : location.id == null;

    }

    @Override
    public int hashCode() {
        int result = this.id != null ? this.id.hashCode() : 0;
        result = 31 * result + (this.title != null ? this.title.hashCode() : 0);
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
    }

    public static List<Location> fromJSON(JSONArray jsonArray) {
        final ArrayList<Location> locations = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                locations.add(new Location(jsonArray.getJSONObject(i)));
            } catch (JSONException e) {
                Log.e(Update.class.getSimpleName(), "Error parsing JSON: " + e);
            }
        }
        return Collections.unmodifiableList(locations);
    }

    public static final Creator<Location> CREATOR = new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

}
