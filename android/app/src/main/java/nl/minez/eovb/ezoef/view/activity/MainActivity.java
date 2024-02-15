/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.view.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.minez.eovb.BuildConfig;
import nl.minez.eovb.R;
import nl.minez.eovb.ezoef.api.ApiConnector;
import nl.minez.eovb.ezoef.api.Auth;
import nl.minez.eovb.ezoef.model.Location;
import nl.minez.eovb.ezoef.model.LocationsTree;
import nl.minez.eovb.ezoef.model.Service;
import nl.minez.eovb.ezoef.profile.Profile;
import nl.minez.eovb.ezoef.profile.ProfileUpdater;
import nl.minez.eovb.ezoef.service.EZOEFFirebaseMessagingService;
import nl.minez.eovb.ezoef.util.DialogUtils;
import nl.minez.eovb.ezoef.util.FragmentUtils;
import nl.minez.eovb.ezoef.util.LogUtil;
import nl.minez.eovb.ezoef.util.MetaUtils;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, FragmentManager.OnBackStackChangedListener {

    public static final String REFRESH_FILTER_BROADCAST = "RefreshFilterBroadcast";

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.toolbarImageView)
    ImageView toolbarImageView;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;

    @BindView(R.id.fab)
    FloatingActionButton fab;

    private ActionBarDrawerToggle drawerToggle;

    private List<Service> services = Collections.emptyList();
    private List<Location> locations = Collections.emptyList();
    private boolean emptyServiceAsked = false;
    private boolean emptyLocationAsked = false;
    private BroadcastReceiver remoteMessageFilterReceiver;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getResources().getBoolean(R.bool.is_tablet)) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        this.setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        this.setSupportActionBar(this.toolbar);

        this.navigationView.setNavigationItemSelectedListener(this);

        // NOTE: Header TextViews aren't available on OnCreate/OnResume, so refresh them when drawer is opening
        this.drawerToggle = new ActionBarDrawerToggle(this, this.drawer, this.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);

                if (newState == DrawerLayout.STATE_SETTLING) {
                    // Refresh text views in header
                    refreshHeaderView();
                }
            }
        };
        this.drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        this.drawer.setDrawerListener(this.drawerToggle);
        this.drawerToggle.syncState();

        this.fab.setVisibility(View.GONE);

        this.getSupportFragmentManager().addOnBackStackChangedListener(this);

        FragmentUtils.popBackStackToRootFragment(this, R.id.content_frame, MainTabsFragment.newInstance());

        setIcon();

        initFirebase();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // Cancel all active requests from the api connector
        ApiConnector.getInstance(this).cancelAll();

        // Dismiss any open error dialog
        DialogUtils.dismissErrorAlertDialogIfShown(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        ApiConnector.getInstance(this).unlistenForReachability();

        if (this.remoteMessageFilterReceiver != null) {
            this.unregisterReceiver(this.remoteMessageFilterReceiver);
            this.remoteMessageFilterReceiver = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ApiConnector.getInstance(this).listenForReachability();

        this.loadServices();

        // Refresh push token at the server when needed
        Profile.getInstance(this).refreshPushToken();

        if (this.remoteMessageFilterReceiver == null) {
            this.remoteMessageFilterReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final RemoteMessage remoteMessage = (RemoteMessage) intent.getExtras().get(EZOEFFirebaseMessagingService.REMOTE_MESSAGE_KEY);
                    if (remoteMessage != null) {
                        // Refresh items to show the new or closed disruption for this notification
                        sendBroadcast(new Intent(REFRESH_FILTER_BROADCAST));

                        DialogUtils.createNotificationAlertDialog(MainActivity.this, remoteMessage, null).show();
                    }
                }
            };
            this.registerReceiver(this.remoteMessageFilterReceiver, new IntentFilter(EZOEFFirebaseMessagingService.REMOTE_MESSAGE_FILTER_BROADCAST));
        }

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        this.filterChanged();

        this.refreshMenu();

        LogUtil.getInstance().logWithName(LogUtil.SCHERM, LogUtil.VERSTORINGEN_SCHERM);
    }

    @Override
    public void onBackPressed() {
        if (this.drawer.isDrawerOpen(GravityCompat.START)) {
            this.drawer.closeDrawer(GravityCompat.START);
        }

        if (this.getSupportFragmentManager().getBackStackEntryCount() > 0) {
            this.getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (this.getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    this.getSupportFragmentManager().popBackStack();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        final Fragment fragment;
        if (item.getItemId() == R.id.nav_disruptions) {
            fragment = MainTabsFragment.newInstance();
        } else {
            fragment = null;
        }

        if (fragment == null) {
            if (item.getItemId() == R.id.nav_edit_service) {
                this.changeService();
            } else if (item.getItemId() == R.id.nav_edit_location) {
                this.changeLocation(Profile.getInstance(this).getLocations());
            } else if (item.getItemId() == R.id.nav_notifications_on || item.getItemId() == R.id.nav_notifications_off) {
                this.changeNotifications();
            } else if (item.getItemId() == R.id.nav_info) {
                this.info();
            } else if (item.getItemId() == R.id.nav_privacy) {
                this.privacy();
            } else if (item.getItemId() == R.id.nav_checklist) {
                this.openToegankelijkheidsverklaring();
            }
        } else {
            FragmentUtils.popBackStackToRootFragment(this, R.id.content_frame, fragment);
            this.drawer.closeDrawer(GravityCompat.START);
        }

        return true;
    }

    @Override
    public void onBackStackChanged() {
        final boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (canGoBack) {
            //LET OP: de volgorde hier is heel belangrijk
            this.drawerToggle.setDrawerIndicatorEnabled(false);
            this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            this.toolbarImageView.setVisibility(View.GONE);
            this.tabLayout.setVisibility(View.GONE);
        } else {
            //LET OP: de volgorde hier is heel belangrijk
            this.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            this.drawerToggle.setDrawerIndicatorEnabled(true);
            this.tabLayout.setVisibility(View.VISIBLE);
            this.toolbarImageView.setVisibility(View.VISIBLE);
        }
    }

    private void loadServices() {
        final ProgressDialog progress = ProgressDialog.show(this, null, this.getString(R.string.fetch_services), true);

        ApiConnector.getInstance(this).locations(new ApiConnector.Listener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                progress.dismiss();

                updateFromJson(response);

                locations = locationsForServices(Profile.getInstance(MainActivity.this).getServices());
                filterChanged();
            }

            @Override
            public void onError(VolleyError error) {
                progress.dismiss();

                if (!DialogUtils.errorAlertDialogIsShowing(MainActivity.this)) {
                    DialogUtils.createErrorAlertDialog(MainActivity.this, String.format(getString(R.string.fetch_services_failed_reason_ps), error.getLocalizedMessage())).show();
                }
            }
        });
    }

    private void refreshHeaderView() {
        final TextView servicesTextView = (TextView) this.navigationView.findViewById(R.id.services_text_view);
        if (servicesTextView != null) {
            servicesTextView.setText(Profile.getInstance(this).getServicesText());
        }

        final TextView locationsTextView = (TextView) this.navigationView.findViewById(R.id.locations_text_view);
        if (locationsTextView != null) {
            locationsTextView.setText(Profile.getInstance(this).getLocationsText());
        }

        final TextView versionTextView = (TextView) this.navigationView.findViewById(R.id.versionTextView);
        if (versionTextView != null) {
            versionTextView.setText("V" + MetaUtils.getAppVersion(this));
        }
    }

    private void refreshMenu() {
        if (!ApiConnector.getInstance(this).getAuth().isRegistered()) {
            ApiConnector.getInstance(this).getAuth().register(ApiConnector.getInstance(this), new Auth.AuthHandler() {
                @Override
                public void accessToken(String accessToken, Auth.ErrorCode errorCode, String errorMessage) {
                    if (errorCode != null) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.oops)
                                .setMessage(R.string.no_connection)
                                .setPositiveButton(R.string.again, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        refreshMenu();
                                    }
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        refreshMenu();
                    }
                }
            });

            // Haal ook de voorzieningen op (oud ezoef)
            if (this.services.isEmpty()) {
                final JSONObject json = LocationsTree.jsonFromCache(this);
                if (json != null) {
                    final LocationsTree locationsTree = LocationsTree.fromJSON(json);

                    this.services = locationsTree.services;
                    this.locations = this.locationsForServices(Profile.getInstance(this).getServices());
                } else {
                    this.loadServices();
                }
                this.filterChanged();
                this.refreshMenuItems();
            }
            return;
        }
    }

    private void setIcon() {
        try {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            View hView = navigationView.getHeaderView(0);
            ImageView imageView = hView.findViewById(R.id.imageViewLauncher);
            Drawable d = getPackageManager().getApplicationIcon(getApplicationInfo());
            imageView.setImageDrawable(d);
        } catch (Exception e) {

        }
    }

    private void refreshMenuItems() {
        final boolean on = Profile.getInstance(this).isNotificationsOn();

        final Menu menu = this.navigationView.getMenu();
        final MenuItem notificationsOff = menu.findItem(R.id.nav_notifications_off);
        notificationsOff.setVisible(on);

        final MenuItem notificationsOn = menu.findItem(R.id.nav_notifications_on);
        notificationsOn.setVisible(!on);
    }

    private void updateFromJson(JSONObject json) {
        LocationsTree.fillJsonCache(this, json);

        final LocationsTree locationsTree = LocationsTree.fromJSON(json);
        this.services = locationsTree.services;

        final Profile profile = Profile.getInstance(this);
        ProfileUpdater.from(profile).updateServices(this.services);

        this.locations = this.locationsForServices(profile.getServices());
    }

    private void filterChanged() {
        // Update menu header
        this.refreshHeaderView();

        // Send refresh into the world
        this.sendBroadcast(new Intent(REFRESH_FILTER_BROADCAST));

        if (Profile.getInstance(this).isProfileSaved()) {
            // Don't show dialogs when profile was already saved for the first time
            return;
        }

        if (!this.services.isEmpty()) {
            // Ask for a service first
            if (Profile.getInstance(this).getServices().isEmpty() && !this.emptyServiceAsked) {
                this.changeService();
                this.emptyServiceAsked = true;
            } else if (!this.locations.isEmpty() && this.emptyServiceAsked && !this.emptyLocationAsked) {
                // After service, ask for user's location
                this.changeLocation(Profile.getInstance(this).getLocations());
                this.emptyLocationAsked = true;
            }
        }
    }

    private void changeService() {
        if (this.services.isEmpty()) {
            // Services should be filled, otherwise show an alert
            new AlertDialog.Builder(this)
                    .setTitle(R.string.change_services)
                    .setMessage(R.string.no_services_fetched)
                    .setPositiveButton(R.string.again, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadServices();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(false)
                    .show();
            return;
        }

        // Transform profile service to array of checked items
        final List<Service> profileServices = Profile.getInstance(this).getServices();
        final ArrayList<Boolean> checkedItems = new ArrayList<>(Lists.transform(this.services,
                new Function<Service, Boolean>() {
                    @Override
                    public Boolean apply(Service input) {
                        return profileServices.contains(input);
                    }
                }));

        // Transform service titles to array of strings
        final List<String> items = Lists.transform(this.services,
                new Function<Service, String>() {
                    @Override
                    public String apply(Service input) {
                        return input.title;
                    }
                });

        // Show checkboxes alert
        DialogUtils.createCheckListDialog(this, R.string.choose_one_or_more_services, items, checkedItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // checkedItems now contains the newly selected indexes
                        final ArrayList<Service> checkedServices = new ArrayList<>();
                        for (int i = 0; i < checkedItems.size(); i++) {
                            if (checkedItems.get(i)) {
                                checkedServices.add(services.get(i));
                            }
                        }
                        processSelectedProfileServices(checkedServices);
                    }
                }
        ).show();
    }

    private void changeLocation(List<Location> selectedLocations) {
        if (this.locations.isEmpty()) {
            // Locations should be filled, otherwise show an alert
            DialogUtils.createErrorAlertDialog(this, R.string.change_locations, R.string.no_service_selected).show();
            return;
        }

        final Profile profile = Profile.getInstance(this);

        final List<Service> profileServices = profile.getServices();
        final List<Location> locations = this.locationsForServices(profileServices);

        final List<String> checkBoxSectionTitles = Lists.transform(profileServices, new Function<Service, String>() {
            @Override
            public String apply(Service input) {
                return input.title;
            }
        });
        final List<List<String>> checkboxItems = this.locationsByServices(profileServices);
        final ArrayList<Integer> selectedCheckboxIndexes = new ArrayList<>(Lists.transform(selectedLocations, new Function<Location, Integer>() {
            @Override
            public Integer apply(Location input) {
                return locations.indexOf(input);
            }
        }));

        DialogUtils.createExpandableCheckListDialog(this, R.string.choose_one_or_more_locations, checkBoxSectionTitles, checkboxItems, selectedCheckboxIndexes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // selectedCheckboxIndexes now contains the newly selected indexes
                        final List<Location> newSelectedLocations = Lists.transform(selectedCheckboxIndexes, new Function<Integer, Location>() {
                            @Override
                            public Location apply(Integer input) {
                                return locations.get(input);
                            }
                        });

                        if (!hasAtLeastOneLocationForEachService(newSelectedLocations, profileServices)) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(R.string.change_locations)
                                    .setMessage(R.string.choose_one_or_more_locations_for_each_service)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            changeLocation(newSelectedLocations);
                                        }
                                    })
                                    .setCancelable(false)
                                    .show();
                            return;
                        }

                        final ProgressDialog progress = ProgressDialog.show(MainActivity.this, null, getString(R.string.processing), true);
                        profile.setLocations(newSelectedLocations, new Profile.SuccessHandler() {
                            @Override
                            public void handleSuccess(String error) {
                                progress.hide();

                                if (error == null) {
                                    profile.setProfileSaved(true);
                                    filterChanged();
                                } else if (!DialogUtils.errorAlertDialogIsShowing(MainActivity.this)) {
                                    DialogUtils.createErrorAlertDialog(MainActivity.this, String.format(getString(R.string.edit_locations_failed_reason_ps), error)).show();
                                }
                            }
                        });
                    }
                }
        ).show();
    }

    private boolean hasAtLeastOneLocationForEachService(List<Location> locations, List<Service> services) {
        for (Service service : services) {
            boolean serviceHasLocation = false;
            for (Location location : service.locations) {
                if (0 <= locations.indexOf(location)) {
                    serviceHasLocation = true;
                    break;
                }
            }

            if (!serviceHasLocation) {
                return false;
            }
        }
        return true;
    }

    private List<List<String>> locationsByServices(List<Service> services) {
        final ArrayList<List<String>> filteredLocations = new ArrayList<>();
        for (Service selectedService : services) {
            filteredLocations.add(Lists.transform(selectedService.locations, new Function<Location, String>() {
                @Override
                public String apply(Location input) {
                    return input.title;
                }
            }));
        }
        return Collections.unmodifiableList(filteredLocations);
    }

    private List<Location> locationsForServices(List<Service> selectedServices) {
        final ArrayList<Location> filteredLocations = new ArrayList<>();
        for (Service service : selectedServices) {
            filteredLocations.addAll(service.locations);
        }
        return Collections.unmodifiableList(filteredLocations);
    }

    private void processSelectedProfileServices(List<Service> services) {
        final Profile profile = Profile.getInstance(this);

        // Find newly added services
        final List<Service> profileServices = profile.getServices();
        final Collection<Service> newServices = Collections2.filter(services, new Predicate<Service>() {
            @Override
            public boolean apply(Service input) {
                return !profileServices.contains(input);
            }
        });

        // Get all locations for the selected services
        final List<Location> allLocations = this.locationsForServices(services);

        // Remove all profile locations that are not in allLocations
        final List<Location> profileLocations = profile.getLocations();
        final ArrayList<Location> newLocations = new ArrayList<>(
                Collections2.filter(profileLocations, new Predicate<Location>() {
                    @Override
                    public boolean apply(Location input) {
                        return allLocations.contains(input);
                    }
                })
        );

        // Auto select all locations for the newly added services
        for (Service service : newServices) {
            newLocations.addAll(service.locations);
        }

        // Set services and locations
        final ProgressDialog progress = ProgressDialog.show(this, null, this.getString(R.string.processing), true);
        profile.setServices(services, newLocations, new Profile.SuccessHandler() {
            @Override
            public void handleSuccess(String error) {
                progress.hide();

                if (error == null) {
                    locations = allLocations;

                    filterChanged();
                } else if (!DialogUtils.errorAlertDialogIsShowing(MainActivity.this)) {
                    DialogUtils.createErrorAlertDialog(MainActivity.this, String.format(getString(R.string.edit_services_failed_reason_ps), error)).show();
                }
            }
        });
    }

    private void changeNotifications() {
        final Profile profile = Profile.getInstance(this);

        final AtomicBoolean notificationsOn = new AtomicBoolean(profile.isNotificationsOn());
        final AtomicBoolean updatesOn = new AtomicBoolean(profile.isUpdatesOn());

        final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final ProgressDialog progress = ProgressDialog.show(MainActivity.this, null, getString(R.string.processing), true);

                // notificationsOn now contains the new switch status
                profile.setNotificationsOn(notificationsOn.get(), updatesOn.get(), new Profile.SuccessHandler() {
                    @Override
                    public void handleSuccess(String error) {
                        progress.hide();

                        if (error == null) {
                            // Refresh notificationsOn/off icon
                            refreshMenuItems();
                        } else if (!DialogUtils.errorAlertDialogIsShowing(MainActivity.this)) {
                            DialogUtils.createErrorAlertDialog(MainActivity.this, String.format(getString(R.string.edit_notifications_failed_reason_ps), error)).show();
                        }
                    }
                });
            }
        };

        DialogUtils.createNotificationUpdatesSwitchesDialog(this, R.string.push_notifications, R.string.receive_notifications, notificationsOn, R.string.receive_updates, updatesOn, onClickListener).show();

        LogUtil.getInstance().logWithName(LogUtil.SCHERM, LogUtil.CHANGE_NOTIFICATIES);
    }

    private void info() {
        final String info = String.format(this.getString(R.string.info_ps), this.getString(R.string.app_name), MetaUtils.getAppVersion(this) + (BuildConfig.DEBUG ? "D" : ""));
        DialogUtils.createInfoDialog(this, R.string.info, info, null).show();
        LogUtil.getInstance().logWithName(LogUtil.SCHERM, LogUtil.INFO_SCHERM);
    }

    private void privacy() {
        String info = this.getString(R.string.privacy_ps);
        DialogUtils.createInfoDialog(this, R.string.privacy, info, null).show();
        LogUtil.getInstance().logWithName(LogUtil.SCHERM, LogUtil.PRIVACY_SCHERM);
    }

    private void openToegankelijkheidsverklaring() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.accessibility_url)));
        startActivity(browserIntent);
    }

    public FloatingActionButton getFloatingActionButton() {
        return this.fab;
    }

    // Retrieve the current registration token
    private void initFirebase() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        LogUtil.getInstance().logWithName(LogUtil.FIREBASE, task.getException());
                        return;
                    }
                    Profile.getInstance(this).setPushToken(task.getResult().getToken());
                });
    }
}