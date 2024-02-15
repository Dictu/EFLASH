// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import Foundation
import SwiftyJSON

class Service {
    
    var id: NSInteger = 0
    var title: String = ""
    var locations: [Location] = []

    init(id: NSInteger, title: String, locations: [Location]) {
        self.id = id
        self.title = title
        self.locations = locations
    }
    
    init(name: String, dictionary: NSDictionary) {
        let json = JSON(dictionary)
        self.id = json["id"].intValue
        self.title = String(htmlEncodedString:name)

        if let locationsList = dictionary["messagetypes"] {
            self.locations = Location.fromJSON(locationsList as Any)
        }
    }

    class func fromJSON(_ json: Any) -> [Service] {
        var services: [Service] = []

        if let dictionary: NSDictionary = json as? NSDictionary {
            if let allServices: [String] = dictionary.allKeys as? [String] {
                for service: String in allServices {
                    guard service.lowercased() != "alle diensten" else {
                        continue
                    }

                    if let location: NSDictionary = dictionary[service] as? NSDictionary {
                        services.append(Service(name: service, dictionary: location))
                    }
                }
            }
        }

        return services.sorted(by: { $0.title < $1.title })
    }

//    class func jsonFromCache(_ maxAgeDays: Int) -> Any? {
//        if let cachedJsonServicesDate: Date = UserDefaults.standard.object(forKey: "CachedJsonServicesDateKey") as? Date {
//            if (Calendar.current as NSCalendar).components(.day, from: cachedJsonServicesDate, to: Date(), options: NSCalendar.Options()).day! < maxAgeDays {
//                if let cachedJsonServices: Any = UserDefaults.standard.object(forKey: "CachedJsonServicesKey") {
//                    return cachedJsonServices
//                }
//            } else {
//                UserDefaults.standard.removeObject(forKey: "CachedJsonServicesKey")
//                UserDefaults.standard.synchronize()
//            }
//        }
//
//        return nil
//    }
//
//    class func fillJsonCache(_ dictionary: Any) {
//        UserDefaults.standard.set(Date(), forKey: "CachedJsonServicesDateKey")
//        UserDefaults.standard.set(dictionary, forKey: "CachedJsonServicesKey")
//        UserDefaults.standard.synchronize()
//    }
//
//    class func clearJsonCache() {
//        UserDefaults.standard.removeObject(forKey: "CachedJsonServicesDateKey")
//        UserDefaults.standard.removeObject(forKey: "CachedJsonServicesKey")
//        UserDefaults.standard.synchronize()
//    }
}

extension Service: CustomStringConvertible {
    
    var description: String {
        return self.title
    }

}

/// Cast NSDictionary to Service
extension Service {

    convenience init(_ dictionary: NSDictionary) {
        let locationDictionaries: [NSDictionary]? = dictionary["messagetypes"] as? [NSDictionary]

        self.init(
            id: (dictionary["id"] as! Int),
            title: dictionary["title"] as! String,
            locations: locationDictionaries == nil ? [] : locationDictionaries!.map { Location($0) }
        )
    }

}

/// Cast Service to NSDictionary
extension NSDictionary {
    
    convenience init(_ service: Service) {
        self.init(dictionary: ["id":service.id, "title":service.title, "messagetypes":service.locations.map { NSDictionary($0) }])
    }
    
}

/// Overload == operator
extension Service : Equatable {
}

func ==(lhs: Service, rhs: Service) -> Bool {
    return lhs.id == rhs.id
}
