// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class RegisterViewController: UIViewController {

    @IBOutlet var emailTextField: UITextField!
    @IBOutlet var registrationButton: UIButton!
    
    let gradientLayer: CAGradientLayer = {
        let layer = CAGradientLayer()
        layer.colors = [
            UIColor.themeGradientStartColor().cgColor,
            UIColor.themeGradientEndColor().cgColor
        ]
        return layer
    }()
    
    convenience init() {
        self.init(nibName: String(describing: RegisterViewController.self), bundle: Bundle.main)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
       
        view.layer.insertSublayer(gradientLayer, at: 0)
        gradientLayer.frame = view.bounds
        self.registrationButton.tintColor = UIColor.themeDarkTintColor()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        gradientLayer.frame = view.bounds
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        self.navigationController?.setNavigationBarHidden(true, animated: false)
        self.navigationController!.navigationBar.setBackgroundImage(
           self.navigationController!.navigationBar.themeBackgroundImage(),
            for: .default
        )
        
        if ApiConnector.sharedConnector.auth.isRegistered {
            // Show cancel button when already registered before
            self.navigationItem.leftBarButtonItem = UIBarButtonItem(barButtonSystemItem: .cancel, target: self, action: #selector(cancelButtonPressed))
        } else {
            self.navigationItem.leftBarButtonItem = nil
        }

        self.cleanUp()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated);
        AppAnalyticsUtil.log(event: .Scherm, segment: .RegistreerScherm)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        self.navigationController!.navigationBar.setBackgroundImage(nil, for: .default)
        
        super.viewWillDisappear(animated)
    }

    @IBAction func registerButtonPressed(_ sender: Any) {
        if self.emailTextField.canResignFirstResponder {
            self.emailTextField.resignFirstResponder()
        }
    }
    
    @objc func cancelButtonPressed() {
        self.emailTextField.delegate = nil
        self.emailTextField.resignFirstResponder()
        self.dismiss(animated: true, completion: nil)
    }
    
    fileprivate func cleanUp() {
        self.emailTextField.delegate = self
        self.emailTextField.text = ""
        self.emailTextField.becomeFirstResponder()
    }
}

extension RegisterViewController : UITextFieldDelegate {

    func textFieldShouldEndEditing(_ textField: UITextField) -> Bool {
        let emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        let emailTest = NSPredicate(format:"SELF MATCHES %@", emailRegEx)
        
        let valid = emailTest.evaluate(with: textField.text)
        if !valid {
            let alertController: UIAlertController = UIAlertController.errorAlertController(NSLocalizedString("Geen geldig email adres.", comment: ""))
            self.present(alertController, animated: true, completion: nil)
        }
        return valid
    }
    
    func textFieldDidEndEditing(_ textField: UITextField) {
        Profile.sharedProfile.invalidateAll()
        
        let progressView = ProgressHelper.showProgress(self.view, message: NSLocalizedString("Registreren...", comment: ""))
        
        ApiConnector.sharedConnector.auth.register(ApiConnector.sharedConnector, email: textField.text!) { (_, error) in
            if progressView != nil {
                DispatchQueue.main.async {
                    ProgressHelper.hideProgress(progressView)
                }
            }
            
            let alertController: UIAlertController
            if error == nil {
                AppAnalyticsUtil.log(event: .Event, segment: .RegistratieSuccesvol)
                
                alertController = UIAlertController(
                    title: NSLocalizedString("Registratie succesvol", comment: ""),
                    message: NSLocalizedString("Je hebt een activatie email ontvangen!\nNadat je deze email hebt geactiveerd kun je EZOEF gebruiken.", comment: ""),
                    preferredStyle: .alert
                )

                let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: { action in
                    self.dismiss(animated: true, completion: nil)
                })
                alertController.addAction(ok)
            } else {
                self.cleanUp()
                
                alertController = UIAlertController.authErrorAlertController(error!, registerHandler: nil)
            }
            self.present(alertController, animated: true, completion: nil)
        }
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        let shouldReturn = self.textFieldShouldEndEditing(textField)
        if shouldReturn {
            textField.resignFirstResponder()
        }
        return shouldReturn
    }

}
