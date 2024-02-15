// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension Date {

    var readableDateTime: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EE dd MMM' 'HH:mm'u'"
        formatter.locale = Locale(identifier: "nl-NL")
        return formatter.string(from: self)
    }
    
}
