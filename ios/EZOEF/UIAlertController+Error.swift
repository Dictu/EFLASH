// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension UIAlertController {
    
    class func errorAlertController(_ title: String, message: String) -> UIAlertController {
        let alertController: UIAlertController = UIAlertController(
            title: title,
            message: message,
            preferredStyle: .alert
        )
        
        let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .destructive, handler: nil)
        alertController.addAction(ok)
        
        return alertController
    }
    
    class func errorAlertController(_ message: String) -> UIAlertController {
        return errorAlertController(NSLocalizedString("Oops!", comment: ""), message: message)
    }
    
}
