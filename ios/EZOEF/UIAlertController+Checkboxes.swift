// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension UIAlertController {

    var checkboxSectionTitles: [String] {
        set {
            self.checkboxesViewController.sectionTitles = newValue
            self.checkboxesViewController.preferredContentSize = CGSize(width: 372, height: self.checkboxesViewController.preferredContentHeight())
        }
        get {
            return self.checkboxesViewController.sectionTitles
        }
    }

    var checkboxItems: [[String]] {
        set {
            self.checkboxesViewController.items = newValue
            self.checkboxesViewController.preferredContentSize = CGSize(width: 372, height: self.checkboxesViewController.preferredContentHeight())
        }
        get {
            return self.checkboxesViewController.items
        }
    }

    var selectedCheckboxItems: [String] {
        set {
            self.checkboxesViewController.selectedItems = newValue
        }
        get {
            return self.checkboxesViewController.selectedItems
        }
    }

    var selectedCheckboxIndexes: [Int] {
        set {
            self.checkboxesViewController.selectedIndexes = newValue
        }
        get {
            return self.checkboxesViewController.selectedIndexes
        }
    }
    
    fileprivate var checkboxesViewController: CheckboxesViewController! {
        get {
            if let checkboxesViewController = self.value(forKey: "contentViewController") as? CheckboxesViewController {
                return checkboxesViewController
            }

            let checkboxesViewController = CheckboxesViewController()
            self.setValue(checkboxesViewController, forKey: "contentViewController")

            return checkboxesViewController
        }
    }
}
