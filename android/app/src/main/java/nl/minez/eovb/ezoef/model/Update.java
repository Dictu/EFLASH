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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Update implements Parcelable {

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", new Locale("nl_NL"));

    public Long id = 0L;
    public Long dateTime = 0L;
    public String description = "";

    public Update(JSONObject jsonObject) {
        try {
            this.id = jsonObject.getLong("cid");
            this.description = jsonObject.getString("update");

            try {
                this.dateTime = dateFormatter.parse(jsonObject.getString("created")).getTime();
            } catch (ParseException e) {
            }
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Error parsing JSON: " + e);
        }
    }

    protected Update(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.dateTime = (Long) in.readValue(Long.class.getClassLoader());
        this.description = in.readString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        final Update update = (Update) o;

        return this.id != null ? this.id.equals(update.id) : update.id == null;

    }

    @Override
    public int hashCode() {
        int result = this.id != null ? this.id.hashCode() : 0;
        result = 31 * result + (this.dateTime != null ? this.dateTime.hashCode() : 0);
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeValue(this.dateTime);
        dest.writeString(this.description);
    }

    public static List<Update> fromJSON(JSONArray jsonArray) {
        final ArrayList<Update> updates = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                updates.add(new Update(jsonArray.getJSONObject(i)));
            } catch (JSONException e) {
                Log.e(Update.class.getSimpleName(), "Error parsing JSON: " + e);
            }
        }

        return Collections.unmodifiableList(updates);
    }

    public static final Creator<Update> CREATOR = new Creator<Update>() {
        @Override
        public Update createFromParcel(Parcel in) {
            return new Update(in);
        }

        @Override
        public Update[] newArray(int size) {
            return new Update[size];
        }
    };
}
