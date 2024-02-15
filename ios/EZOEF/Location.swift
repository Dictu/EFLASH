// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import Foundation
import SwiftyJSON

class Location {
    
    var id: NSInteger = 0
    var title: String = ""

    init(id: NSInteger, title: String) {
        self.id = id
        self.title = title
    }

    init(dictionary: NSDictionary) {
        let json = JSON(dictionary)
        self.id = json["id"].intValue
        self.title = String(htmlEncodedString: json["name"].stringValue)
    }

    class func fromJSON(_ json: Any) -> [Location] {
        var locations: [Location] = []

        if let dictionaries = json as? [NSDictionary] {
            for dictionary in dictionaries {
                locations.append(Location(dictionary: dictionary))
            }
        }

        return locations.sorted(by: { $0.title < $1.title })
    }
}

extension Location: CustomStringConvertible {
    
    var description: String {
        return self.title
    }
    
}

/// Cast NSDictionary to Location
extension Location {
    
    convenience init(_ dictionary: NSDictionary) {
        self.init(id: (dictionary["id"] as! Int), title: dictionary["title"] as! String)
    }

}

/// Cast Location to NSDictionary
extension NSDictionary {
    
    convenience init(_ location: Location) {
        self.init(dictionary: ["id":location.id, "title":location.title])
    }
    
}

/// Overload == operator
extension Location : Equatable {
}

func ==(lhs: Location, rhs: Location) -> Bool {
    return lhs.id == rhs.id
}

