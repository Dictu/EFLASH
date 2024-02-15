// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension UIAlertController {
    
    var onOffTitle: String {
        set {
            self.onOffViewController.onOffTitle = newValue
            self.onOffViewController.preferredContentSize = CGSize(width: 372, height: self.onOffViewController.preferredContentHeight())
        }
        get {
            return self.onOffViewController.onOffTitle
        }
    }
    
    var on: Bool {
        set {
            self.onOffViewController.on = newValue
        }
        get {
            return self.onOffViewController.on
        }
    }

    fileprivate var onOffViewController: OnOffViewController! {
        get {
            if let onOffViewController = self.value(forKey: "contentViewController") as? OnOffViewController {
                return onOffViewController
            }
            
            let onOffViewController = OnOffViewController()
            self.setValue(onOffViewController, forKey: "contentViewController")
            
            return onOffViewController
        }
    }

}
