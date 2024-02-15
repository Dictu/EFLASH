// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension Date {
    
    func yearsFrom(_ date: Date) -> Int {
        return (Calendar.current as NSCalendar).components(.year, from: date, to: self, options: NSCalendar.Options()).year!
    }
    
    func monthsFrom(_ date: Date) -> Int {
        return (Calendar.current as NSCalendar).components(.month, from: date, to: self, options: NSCalendar.Options()).month!
    }
    
    func weeksFrom(_ date: Date) -> Int {
        return (Calendar.current as NSCalendar).components(.weekOfYear, from: date, to: self, options: NSCalendar.Options()).weekOfYear!
    }
    
    func daysFrom(_ date: Date) -> Int {
        return (Calendar.current as NSCalendar).components(.day, from: date, to: self, options: NSCalendar.Options()).day!
    }
    
    func hoursFrom(_ date: Date) -> Int {
        return (Calendar.current as NSCalendar).components(.hour, from: date, to: self, options: NSCalendar.Options()).hour!
    }
    
    func minutesFrom(_ date: Date) -> Int {
        return (Calendar.current as NSCalendar).components(.minute, from: date, to: self, options: NSCalendar.Options()).minute!
    }
    
    func secondsFrom(_ date: Date) -> Int {
        return (Calendar.current as NSCalendar).components(.second, from: date, to: self, options: NSCalendar.Options()).second!
    }
    
    var relativeTime: String {
        let now = Date()
        let vandaag = Calendar.current.isDateInToday(self)
        
        if !vandaag && now.yearsFrom(self) > 0 {
            return "\(now.yearsFrom(self)) " + { return now.yearsFrom(self) > 1 ? NSLocalizedString("years ago", comment: "") : NSLocalizedString("year ago", comment: "") }()
        }
        if !vandaag && now.monthsFrom(self) > 0 {
            return "\(now.monthsFrom(self)) " + { return now.monthsFrom(self) > 1 ? NSLocalizedString("months ago", comment: "") : NSLocalizedString("month ago", comment: "") }()
        }
        if !vandaag && now.weeksFrom(self) > 0 {
            return "\(now.weeksFrom(self).description) " + { return now.weeksFrom(self) > 1 ? NSLocalizedString("weeks ago", comment: "") : NSLocalizedString("week ago", comment: "") }()
        }
        if !vandaag {
            if  Calendar.current.isDateInYesterday(self) {
                return NSLocalizedString("Yesterday", comment: "")
            }
            if  now.daysFrom(self) > 0 {
                let compareDate = (Calendar.current as NSCalendar).date(bySettingHour: 12, minute: 0, second: 0, of: self, options: [])
                return "\(now.daysFrom(compareDate!)) " + NSLocalizedString("days ago", comment: "")
            }
        }
        if now.hoursFrom(self) > 0 {
            return "\(now.hoursFrom(self)) " + { return now.hoursFrom(self) > 1 ? NSLocalizedString("hours ago", comment: "") : NSLocalizedString("hour ago", comment: "") }()
        }
        if now.minutesFrom(self) > 0 {
            return "\(now.minutesFrom(self)) " + { return now.minutesFrom(self) > 1 ? NSLocalizedString("minutes ago", comment: "") : NSLocalizedString("minute ago", comment: "") }()
        }
        if now.secondsFrom(self) >= 0 {
            if now.secondsFrom(self) < 15 { return NSLocalizedString("Just now", comment: "") }
            return "\(now.secondsFrom(self)) " + NSLocalizedString("seconds ago", comment: "")
        }
        return ""
    }
    
    var relativeTimeNew: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EE d MMM HH:mm"
        formatter.locale = Locale(identifier: "nl-NL")
        return formatter.string(from: self)
    }

    var relativeTimeToday: String {
        let now = Date()
        
        if now.daysFrom(self) >= 1 {
            let dateFormatter = DateFormatter()
            dateFormatter.locale = Locale(identifier: "nl_NL")
            dateFormatter.dateFormat = "E dd MMM"
            return dateFormatter.string(from: self)
        }
        if now.hoursFrom(self) > 0 {
            return "\(now.hoursFrom(self)) " + { return now.hoursFrom(self) > 1 ? NSLocalizedString("hours ago", comment: "") : NSLocalizedString("hour ago", comment: "") }()
        }
        if now.minutesFrom(self) > 0 {
            return "\(now.minutesFrom(self)) " + { return now.minutesFrom(self) > 1 ? NSLocalizedString("minutes ago", comment: "") : NSLocalizedString("minute ago", comment: "") }()
        }
        if now.secondsFrom(self) >= 0 {
            if now.secondsFrom(self) < 15 { return NSLocalizedString("Just now", comment: "") }
            return "\(now.secondsFrom(self)) " + NSLocalizedString("seconds ago", comment: "")
        }
        return ""
    }
    
}
