/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.VolleyError;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nl.minez.eovb.R;
import nl.minez.eovb.ezoef.api.ApiConnector;
import nl.minez.eovb.ezoef.model.Location;
import nl.minez.eovb.ezoef.model.Service;
import nl.minez.eovb.ezoef.util.JsonUtils;
import nl.minez.eovb.ezoef.util.PreferenceJsonUtils;

public class Profile {

    public interface SuccessHandler {
        void handleSuccess(String error);
    }

    private static final String PROFILE_PREFS_KEY = "ProfilePreferencesKey";
    private static final String PROFILE_SAVED_KEY = "ProfileSavedKey";
    private static final String PROFILE_ACTIVATED_KEY = "ProfileActivatedKey";

    private static final String NOTIFICATIONS_ON_KEY = "NotificationsOnKey";
    private static final String UPDATES_ON_KEY = "UpdatesOnKey";
    private static final String PUSH_TOKEN_KEY = "PushTokenKey";
    private static final String SERVICES_KEY = "ServicesKey";
    private static final String LOCATIONS_KEY = "LocationsKey";
    private static final String PUSH_TOKEN_IS_DIRTY_KEY = "PushTokenIsDirtyKey";

    private static final HashMap<Context, Profile> instances = new HashMap<>();

    private final PreferenceJsonUtils<Location[]> locationPreferenceJsonUtils = new PreferenceJsonUtils<>();
    private final PreferenceJsonUtils<Service[]> servicePreferenceJsonUtils = new PreferenceJsonUtils<>();
    private final Context context;

    private Profile(final Context context) {
        this.context = context;
    }

    public static synchronized Profile getInstance(final Context context) {
        Profile instance = instances.get(context);
        if (instance == null) {
            instance = new Profile(context);
            instances.put(context, instance);
        }
        return instance;
    }

    public boolean isProfileActivated() {
        return this.getProfilePreferences().getBoolean(PROFILE_ACTIVATED_KEY, false);
    }

    public void setProfileActivated(boolean profileSaved) {
        final SharedPreferences.Editor editor = this.getProfilePreferences().edit();
        editor.putBoolean(PROFILE_ACTIVATED_KEY, profileSaved);
        editor.apply();
    }

    public boolean isProfileSaved() {
        return this.getProfilePreferences().getBoolean(PROFILE_SAVED_KEY, false);
    }

    public void setProfileSaved(boolean profileSaved) {
        final SharedPreferences.Editor editor = this.getProfilePreferences().edit();
        editor.putBoolean(PROFILE_SAVED_KEY, profileSaved);
        editor.apply();
    }

    public boolean isNotificationsOn() {
        return this.getProfilePreferences().getBoolean(NOTIFICATIONS_ON_KEY, true);
    }

    public boolean isUpdatesOn() {
        // TODO : new code
        return  this.getProfilePreferences().getBoolean(UPDATES_ON_KEY, true);
        // originele code
        //return BuildConfig.TARGET_PUBLIC ? false : this.getProfilePreferences().getBoolean(UPDATES_ON_KEY, true);
    }

    public void setNotificationsOn(final boolean notificationsOn, final boolean updatesOn, final SuccessHandler successHandler) {
        if (this.isNotificationsOn() != notificationsOn || this.isUpdatesOn() != updatesOn) {
            this.changePushSettings(this.getPushToken(), notificationsOn, updatesOn, this.getServices(), this.getLocations(), new SuccessHandler() {
                @Override
                public void handleSuccess(String error) {
                    if (error == null) {
                        final SharedPreferences.Editor editor = getProfilePreferences().edit();
                        editor.putBoolean(NOTIFICATIONS_ON_KEY, notificationsOn);
                        editor.putBoolean(UPDATES_ON_KEY, updatesOn);
                        editor.apply();
                    }
                    successHandler.handleSuccess(error);
                }
            });
        } else {
            successHandler.handleSuccess(null);
        }
    }

    public String getPushToken() {
        return this.getProfilePreferences().getString(PUSH_TOKEN_KEY, "");
    }

    public void setPushToken(final String pushToken) {
        if (!this.getPushToken().equals(pushToken)) {
            final SharedPreferences.Editor editor = this.getProfilePreferences().edit();
            editor.putString(PUSH_TOKEN_KEY, pushToken);
            editor.putBoolean(PUSH_TOKEN_IS_DIRTY_KEY, true);
            editor.apply();
        }
    }

    public void refreshPushToken() {
        if (this.pushTokenIsDirty()) {
            // Push token has changed, notify the server
            this.changePushSettings(this.getPushToken(), this.isNotificationsOn(), this.isUpdatesOn(), this.getServices(), this.getLocations(), new SuccessHandler() {
                @Override
                public void handleSuccess(String error) {

                }
            });
        }
    }

    public List<Service> getServices() {
        final Service[] services = this.servicePreferenceJsonUtils.read(Service[].class, getProfilePreferences(), SERVICES_KEY);
        // Migrating data , make sure old data won't lead to crashing the app
        for (Service service : services) {
            if (service.locations == null) {
                service.locations = Collections.emptyList();
            }
        }
        return Collections.unmodifiableList(Arrays.asList(services));
    }

    public void setServices(final List<Service> services) {
        final SharedPreferences profilePreferences = getProfilePreferences();
        this.servicePreferenceJsonUtils.write(profilePreferences, SERVICES_KEY, services.toArray(new Service[]{}));
    }

    public void setServices(final List<Service> services, final List<Location> locations, final SuccessHandler successHandler) {
        if (!this.getServices().equals(services)) {
            this.changePushSettings(this.getPushToken(), this.isNotificationsOn(), this.isUpdatesOn(), services, locations, new SuccessHandler() {
                @Override
                public void handleSuccess(String error) {
                    if (error == null) {
                        final SharedPreferences profilePreferences = getProfilePreferences();
                        servicePreferenceJsonUtils.write(profilePreferences, SERVICES_KEY, services.toArray(new Service[]{}));
                        locationPreferenceJsonUtils.write(profilePreferences, LOCATIONS_KEY, locations.toArray(new Location[]{}));
                    }
                    successHandler.handleSuccess(error);
                }
            });
        } else {
            successHandler.handleSuccess(null);
        }
    }

    public List<Long> getServiceIds() {
        return Lists.transform(this.getServices(), new Function<Service, Long>() {
            @Override
            public Long apply(Service input) {
                return input.id;
            }
        });
    }

    public String getServicesText() {
        final String servicesText = JsonUtils.combineWithSeparator(", ", Lists.transform(this.getServices(), new Function<Service, String>() {
            @Override
            public String apply(Service input) {
                return input.title;
            }
        }).toArray());
        return servicesText.isEmpty() ? this.context.getString(R.string.no_services) : servicesText;
    }

    public List<Location> getLocations() {
        final Location[] locations = locationPreferenceJsonUtils.read(Location[].class, getProfilePreferences(), LOCATIONS_KEY);
        return Collections.unmodifiableList(Arrays.asList(locations));
    }

    public void setLocations(final List<Location> locations, final SuccessHandler successHandler) {
        if (!this.getLocations().equals(locations)) {
            this.changePushSettings(this.getPushToken(), this.isNotificationsOn(), this.isUpdatesOn(), this.getServices(), locations, new SuccessHandler() {
                @Override
                public void handleSuccess(String error) {
                    if (error == null) {
                        locationPreferenceJsonUtils.write(getProfilePreferences(), LOCATIONS_KEY, locations.toArray(new Location[]{}));
                    }
                    successHandler.handleSuccess(error);
                }
            });
        } else {
            successHandler.handleSuccess(null);
        }
    }

    public void setLocations(List<Location> locations) {
        this.locationPreferenceJsonUtils.write(getProfilePreferences(), LOCATIONS_KEY, locations.toArray(new Location[]{}));
    }

    public List<Long> getLocationIds() {
        return Lists.transform(this.getLocations(), new Function<Location, Long>() {
            @Override
            public Long apply(Location input) {
                return input.id;
            }
        });
    }

    public String getLocationsText() {
        final String locationsText = JsonUtils.combineWithSeparator(", ", Lists.transform(this.getLocations(), new Function<Location, String>() {
            @Override
            public String apply(Location input) {
                return input.title;
            }
        }).toArray());
        return locationsText.isEmpty() ? this.context.getString(R.string.no_locations) : locationsText;
    }

    public void invalidateAll() {
        final SharedPreferences.Editor edit = this.getProfilePreferences().edit();
        edit.remove(PROFILE_SAVED_KEY);
        edit.remove(LOCATIONS_KEY);
        edit.remove(SERVICES_KEY);
        edit.remove(PUSH_TOKEN_IS_DIRTY_KEY);
        edit.apply();
    }

    private SharedPreferences getProfilePreferences() {
        return this.context.getSharedPreferences(PROFILE_PREFS_KEY, Context.MODE_PRIVATE);
    }

    private void changePushSettings(String pushToken, final boolean notificationsOn, final boolean updatesOn, final List<Service> services, final List<Location> locations, final SuccessHandler successHandler) {
        if (pushToken.isEmpty()) {
            successHandler.handleSuccess(null);
            return;
        }

        final ApiConnector.Listener<JSONObject> listener = new ApiConnector.Listener<JSONObject>() {
            @NonNull
            private String createLogMessage(final List<Service> services, final List<Location> locations) {
                final String servicesText = JsonUtils.combineWithSeparator(", ", Lists.transform(services, new Function<Service, String>() {
                    @Override
                    public String apply(Service input) {
                        return input.title;
                    }
                }).toArray());

                final String locationsText = JsonUtils.combineWithSeparator(", ", Lists.transform(locations, new Function<Location, String>() {
                    @Override
                    public String apply(Location input) {
                        return input.title;
                    }
                }).toArray());

                return "Push settings changed succesfully for Services: " + servicesText + " Locations: " + locationsText;
            }

            @Override
            public void onSuccess(JSONObject response) {
                try {
                    clearPushTokenIsDirty();

                    if (response.has("success") && response.getInt("success") != 0) {
                        Log.i(this.getClass().getSimpleName(), createLogMessage(services, locations));
                        successHandler.handleSuccess(null);
                    } else if (response.has("message")) {
                        successHandler.handleSuccess(response.getString("message"));
                    } else {
                        successHandler.handleSuccess(null);
                    }
                } catch (JSONException e) {
                    Log.e(this.getClass().getSimpleName(), "Error parsing JSON: " + e);
                    successHandler.handleSuccess(e.getLocalizedMessage());
                }
            }

            @Override
            public void onError(VolleyError error) {
                successHandler.handleSuccess(error.getLocalizedMessage());
            }
        };

        if (notificationsOn) {
            ApiConnector.getInstance(this.context).registerForPushNotifications(pushToken, updatesOn, this.getAllLocationIds(services, locations), listener);
        } else {
            ApiConnector.getInstance(this.context).unregisterForPushNotifications(pushToken, listener);
        }
    }

    @NonNull
    private ArrayList<Long> getAllLocationIds(List<Service> services, List<Location> locations) {
        final ArrayList<Long> allLocations = new ArrayList<>(Lists.transform(services, new Function<Service, Long>() {
            @Override
            public Long apply(Service input) {
                return input.id;
            }
        }));

        allLocations.addAll(Lists.transform(locations, new Function<Location, Long>() {
            @Override
            public Long apply(Location input) {
                return input.id;
            }
        }));
        return allLocations;
    }

    private boolean pushTokenIsDirty() {
        return this.getProfilePreferences().getBoolean(PUSH_TOKEN_IS_DIRTY_KEY, false);
    }

    private void clearPushTokenIsDirty() {
        final SharedPreferences.Editor editor = this.getProfilePreferences().edit();
        editor.remove(PUSH_TOKEN_IS_DIRTY_KEY);
        editor.apply();
    }

}