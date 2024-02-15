// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension UIAlertController {

    class func authErrorAlertController(_ error: Error, registerHandler: ((UIAlertAction) -> Void)?) -> UIAlertController {
            let showRegisterButton = false

        let alertController: UIAlertController = UIAlertController(
            title: NSLocalizedString("Toegang", comment: ""),
            message: error.localizedDescription,
            preferredStyle: .alert
        )

        if registerHandler != nil && showRegisterButton {
            let registerAgain = UIAlertAction(title: NSLocalizedString("Registreer", comment: ""), style: .default, handler: registerHandler)
            alertController.addAction(registerAgain)
        }

        let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: nil)
        alertController.addAction(ok)

        return alertController
    }

}
