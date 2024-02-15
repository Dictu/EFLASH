// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension UIAlertController {
    
    var info: String {
        set {
            self.infoViewController.info = newValue
            self.infoViewController.preferredContentSize = CGSize(width: 372, height: self.infoViewController.preferredContentHeight())
        }
        get {
            return self.infoViewController.info
        }
    }

    fileprivate var infoViewController: InfoViewController! {
        get {
            if let infoViewController = self.value(forKey: "contentViewController") as? InfoViewController {
                return infoViewController
            }
            
            let infoViewController = InfoViewController()
            self.setValue(infoViewController, forKey: "contentViewController")
            
            return infoViewController
        }
    }
    
}
