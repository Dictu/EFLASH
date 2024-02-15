/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.profile;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import nl.minez.eovb.ezoef.model.Location;
import nl.minez.eovb.ezoef.model.Service;

public class ProfileUpdater {

    private final Profile profile;

    private ProfileUpdater(Profile profile) {
        this.profile = profile;
    }

    public static ProfileUpdater from(Profile profile) {
        return new ProfileUpdater(profile);
    }

    public void updateServices(final List<Service> newServices) {
        final ArrayList<Service> services = new ArrayList<>(this.profile.getServices());

        final ArrayList<Service> removedServices = new ArrayList<>(Collections2.filter(services, new Predicate<Service>() {
            @Override
            public boolean apply(@Nullable Service input) {
                return !newServices.contains(input);
            }
        }));
        Collections.reverse(removedServices);

        // Remove removed services
        for (Service removedService : removedServices) {
            services.remove(removedService);
        }

        // Assign the new locations and applications
        for (Service service : services) {
            final int idx = newServices.indexOf(service);
            if (0 <= idx) {
                final Service newService = newServices.get(idx);
                service.title = newService.title;
                service.locations = newService.locations;
            }
        }

        // Also update the profile locations and applications
        this.updateLocations(this.locationsForServices(services));
        this.profile.setServices(services);
    }

    private void updateLocations(final List<Location> newLocations) {
        final ArrayList<Location> locations = new ArrayList<>(this.profile.getLocations());

        final ArrayList<Location> removedLocations = new ArrayList<>(Collections2.filter(locations, new Predicate<Location>() {
            @Override
            public boolean apply(@Nullable Location input) {
                return !newLocations.contains(input);
            }
        }));
        Collections.reverse(removedLocations);

        // Remove removed locations
        for (Location removedLocation : removedLocations) {
            locations.remove(removedLocation);
        }

        this.profile.setLocations(locations);
    }

    private List<Location> locationsForServices(List<Service> selectedServices) {
        final ArrayList<Location> filteredLocations = new ArrayList<>();
        for (Service service : selectedServices) {
            filteredLocations.addAll(service.locations);
        }
        return Collections.unmodifiableList(filteredLocations);
    }

}