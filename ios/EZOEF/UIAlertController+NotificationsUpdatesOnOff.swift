// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension UIAlertController {
    
    var notificationsOnOffTitle: String {
        set {
            self.onOffViewController.notificationsOnOffTitle = newValue
            self.onOffViewController.preferredContentSize = CGSize(width: 372, height: self.onOffViewController.preferredContentHeight())
        }
        get {
            return self.onOffViewController.notificationsOnOffTitle
        }
    }
    
    var notificationsOn: Bool {
        set {
            self.onOffViewController.notificationsOn = newValue
        }
        get {
            return self.onOffViewController.notificationsOn
        }
    }

    var updatesOnOffTitle: String {
        set {
            self.onOffViewController.updatesOnOffTitle = newValue
            self.onOffViewController.preferredContentSize = CGSize(width: 372, height: self.onOffViewController.preferredContentHeight())
        }
        get {
            return self.onOffViewController.updatesOnOffTitle
        }
    }
    
    var updatesOn: Bool {
        set {
            self.onOffViewController.updatesOn = newValue
        }
        get {
            return self.onOffViewController.updatesOn
        }
    }
    
    fileprivate var onOffViewController: NotificationUpdatesSwitchesViewController! {
        get {
            if let onOffViewController = self.value(forKey: "contentViewController") as? NotificationUpdatesSwitchesViewController {
                return onOffViewController
            }
            
            let onOffViewController = NotificationUpdatesSwitchesViewController()
            self.setValue(onOffViewController, forKey: "contentViewController")
            
            return onOffViewController
        }
    }

}
