// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import Foundation
import SwiftyJSON

class Update {

    var id: NSInteger = 0
    var dateTime: Date = Date(timeIntervalSince1970: 0)
    var description: String = ""

    init(dateTime: Date, description: String) {
        self.dateTime = dateTime
        self.description = description
    }

    init(dictionary: NSDictionary) {
        let json = JSON(dictionary)

        self.id = json["cid"].intValue
        self.description = json["update"].stringValue

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
    }

    class func fromJSON(_ JSON: Any) -> [Update] {
        var updates: [Update] = []

        if let dictionaries = JSON as? [NSDictionary] {
            for dictionary in dictionaries {
                updates.append(Update(dictionary: dictionary))
            }
        }

        return updates
    }
}
