// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import Foundation

/// Updates the profile by adding new locations and applications to the services; and removes unexisting services, locations and applications
extension Profile {
    
    func update(services newServices: [Service]) {
        var services = self.services

        // Remove removed services
        services.filter { !newServices.contains($0) }
            .compactMap { services.index(of: $0) }
            .reversed()
            .forEach { services.remove(at: $0) }

        // Assign the new locations and applications
        services.forEach { (service) in
            if let idx = newServices.index(of: service) {
                let newService = newServices[idx]
                service.locations = newService.locations
            }
        }

        // Also update the profile locations and applications
        self.update(locations: services.flatMap { $0.locations })
        self.services = services
    }

    fileprivate func update(locations newLocations: [Location]) {
        var locations = self.locations
        
        // Remove removed locations
        locations.filter { !newLocations.contains($0) }
            .compactMap { locations.index(of: $0) }
            .reversed()
            .forEach { locations.remove(at: $0) }

        self.locations = locations
    }
}
