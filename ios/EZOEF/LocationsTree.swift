// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import Foundation
import SwiftyJSON

class LocationsTree {
    
    var services: [Service] = []

    init(services: [Service]) {
        self.services = services
    }
    
    class func fromJSON(_ json: Any) -> LocationsTree {
        var services: [Service] = []

        if let dictionary = json as? NSDictionary {
            if let allServices: [String] = dictionary.allKeys as? [String] {
                for service: String in allServices {
                    guard service.lowercased() != "alle diensten" else {
                        continue
                    }
                    
                    if let dictionary: NSDictionary = dictionary[service] as? NSDictionary {
                        services.append(Service(name: service, dictionary: dictionary))
                    }
                }
            }
        }
        return LocationsTree(services: services)
    }

    class func jsonFromCache() -> Any? {
        return UserDefaults.standard.object(forKey: "CachedJsonLocationsTreeKey")
    }
    
    class func fillJsonCache(_ dictionary: Any) {
        UserDefaults.standard.set(dictionary, forKey: "CachedJsonLocationsTreeKey")
        UserDefaults.standard.synchronize()
    }
    
    class func clearJsonCache() {
        UserDefaults.standard.removeObject(forKey: "CachedJsonLocationsTreeKey")
        UserDefaults.standard.synchronize()
    }

}
