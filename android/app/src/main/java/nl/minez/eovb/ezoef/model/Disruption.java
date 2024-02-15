/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import nl.minez.eovb.ezoef.util.JsonUtils;

public class Disruption implements Parcelable {

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", new Locale("nl_NL"));

    public Long id = 0L;
    public String title = "";
    public String description = "";
    public Long dateTime = 0L;
    public String service = "";
    public String location = "";
    public List<Update> updates = Collections.emptyList();

    public Disruption(JSONObject jsonObject) {
        try {
            this.id = jsonObject.getLong("id");
            this.title = jsonObject.getString("title");
            this.description = jsonObject.getString("body");

            try {
                this.dateTime = dateFormatter.parse(jsonObject.getString("created")).getTime();
            } catch (ParseException e) {
                Log.e(this.getClass().getSimpleName(), "Error parsing time: " + e);
            }

            this.service = JsonUtils.combineWithSeparator(", ", JsonUtils.parseJsonPropertyValues(jsonObject.getJSONArray("messagestreams"), "name"));
            this.location = JsonUtils.combineWithSeparator(", ", JsonUtils.parseJsonPropertyValues(jsonObject.getJSONArray("messagetypes"), "name"));
            this.updates = Update.fromJSON(jsonObject.getJSONArray("field_updates"));
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Error parsing JSON: " + e);
        }
    }

    protected Disruption(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.title = in.readString();
        this.description = in.readString();
        this.dateTime = (Long) in.readValue(Long.class.getClassLoader());
        this.service = in.readString();
        this.location = in.readString();
        this.updates = new ArrayList<>();
        in.readList(this.updates, Update.class.getClassLoader());
    }

    public Long getLastUpdateDateTime() {
        if (this.updates.isEmpty()) {
            return this.dateTime;
        }

        final ArrayList<Long> updateDates = new ArrayList<>(Lists.transform(this.updates, new Function<Update, Long>() {
            @Override
            public Long apply(Update input) {
                return input.dateTime;
            }
        }));

        Collections.sort(updateDates, new Comparator<Long>() {
            @Override
            public int compare(Long value1, Long value2) {
                if (value1 == null || value2 == null) {
                    return 0;
                }
                return value2.compareTo(value1);
            }
        });

        return updateDates.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        final Disruption disruption = (Disruption) o;

        return this.id != null ? this.id.equals(disruption.id) : disruption.id == null;
    }

    @Override
    public int hashCode() {
        int result = this.id != null ? this.id.hashCode() : 0;
        result = 31 * result + (this.title != null ? this.title.hashCode() : 0);
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        result = 31 * result + (this.dateTime != null ? this.dateTime.hashCode() : 0);
        result = 31 * result + (this.service != null ? this.service.hashCode() : 0);
        result = 31 * result + (this.location != null ? this.location.hashCode() : 0);
        result = 31 * result + (this.updates != null ? this.updates.hashCode() : 0);
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
        dest.writeString(this.description);
        dest.writeValue(this.dateTime);
        dest.writeString(this.service);
        dest.writeString(this.location);
        dest.writeList(this.updates);
    }

    public static List<Disruption> fromJSON(JSONArray jsonArray) {
        final ArrayList<Disruption> disruptions = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                disruptions.add(new Disruption(jsonArray.getJSONObject(i)));
            } catch (JSONException e) {
                Log.e(Update.class.getSimpleName(), "Error parsing JSON: " + e);
            }
        }
        return Collections.unmodifiableList(disruptions);
    }

    public static final Creator<Disruption> CREATOR = new Creator<Disruption>() {
        @Override
        public Disruption createFromParcel(Parcel in) {
            return new Disruption(in);
        }

        @Override
        public Disruption[] newArray(int size) {
            return new Disruption[size];
        }
    };

}
