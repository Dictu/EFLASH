// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import Foundation
import SwiftyJSON

class Disruption {

    var id: NSInteger = 0
    var title: String = ""
    var description: String = ""
    var dateTime: Date = Date(timeIntervalSinceReferenceDate: 0)
    var service: String = ""
    var location: String = ""
    var updates: [Update] = []
    var lastUpdatedDateTime: Date {
        return self.updates.map { $0.dateTime }.sorted(by: { $0 > $1 }).first ?? self.dateTime
    }
    
    init(dictionary: NSDictionary) {
        let json = JSON(dictionary)

        self.id = json["id"].intValue
        self.title = String(htmlEncodedString: json["title"].stringValue)
        self.description = json["body"].stringValue

        if let date = json["created"].string {
            if #available(iOS 10.0, *) {
                let dateFormatter = ISO8601DateFormatter()
                if let newdate = dateFormatter.date(from: date) {
                    self.dateTime = newdate
                }
            } else {
                let dateFormatter = DateFormatter()
                dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZZZ"
                if let newdate = dateFormatter.date(from: date) {
                    self.dateTime = newdate
                }
            }
        }
        
        let services: [JSON] = json["messagestreams"].arrayValue
        self.service = services.map { $0.dictionaryValue["name"] != nil ? $0.dictionaryValue["name"]!.stringValue : ""}.joined(separator: ", ")
        

        let locations: [JSON] = json["messagetypes"].arrayValue
        self.location = locations.map { $0.dictionaryValue["name"] != nil ? $0.dictionaryValue["name"]!.stringValue : ""}.joined(separator: ", ")

        if let updatesList = dictionary["field_updates"] {
            self.updates = Update.fromJSON(updatesList as Any)
        }
    }
    
    class func fromJSON(_ JSON: Any) -> [Disruption] {
        var disruptions: [Disruption] = []

        if let dictionaries = JSON as? [NSDictionary] {
            for dictionary in dictionaries {
                disruptions.append(Disruption(dictionary: dictionary))
            }
        }
        
        return disruptions
    }

}
