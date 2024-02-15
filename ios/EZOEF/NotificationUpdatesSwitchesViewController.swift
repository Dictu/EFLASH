// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class NotificationUpdatesSwitchesViewController: UIViewController {

    @IBOutlet var notificationsTextLabel: UILabel?
    @IBOutlet var notificationsOnOffSwitch: UISwitch?
    @IBOutlet var updatesTextLabel: UILabel?
    @IBOutlet var updatesOnOffSwitch: UISwitch?

    var notificationsOnOffTitle: String = "" {
        didSet {
            if let notificationsTextLabel = self.notificationsTextLabel {
                notificationsTextLabel.text = notificationsOnOffTitle
            }
        }
    }
    var notificationsOn: Bool = false {
        didSet {
            if let notificationsOnOffSwitch = self.notificationsOnOffSwitch {
                notificationsOnOffSwitch.isOn = notificationsOn
            }
        }
    }
    
    var updatesOnOffTitle: String = "" {
        didSet {
            if let updatesTextLabel = self.updatesTextLabel {
                updatesTextLabel.text = updatesOnOffTitle
            }
        }
    }
    var updatesOn: Bool = false {
        didSet {
            if let updatesOnOffSwitch = self.updatesOnOffSwitch {
                updatesOnOffSwitch.isOn = updatesOn
            }
        }
    }

    convenience init() {
        self.init(nibName: String(describing: NotificationUpdatesSwitchesViewController.self), bundle: Bundle.main)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        if let updatesTextLabel = self.updatesTextLabel {
            updatesTextLabel.textColor = UIColor(white: 0, alpha: 0xac / 255)
            updatesTextLabel.font = UIFont.themeRegularFont(updatesTextLabel.font.pointSize)
            updatesTextLabel.textAlignment = NSTextAlignment.left
            updatesTextLabel.text = self.updatesOnOffTitle
        }

        if let updatesOnOffSwitch = self.updatesOnOffSwitch {
            updatesOnOffSwitch.isOn = self.updatesOn
        }
        
        if let notificationsTextLabel = self.notificationsTextLabel {
            notificationsTextLabel.textColor = UIColor(white: 0, alpha: 0xac / 255)
            notificationsTextLabel.font = UIFont.themeRegularFont(notificationsTextLabel.font.pointSize)
            notificationsTextLabel.textAlignment = NSTextAlignment.left
            notificationsTextLabel.text = self.notificationsOnOffTitle
        }

        if let notificationsOnOffSwitch = self.notificationsOnOffSwitch {
            notificationsOnOffSwitch.isOn = self.notificationsOn
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        self.updatesOnOffSwitch!.isEnabled = self.notificationsOnOffSwitch!.isOn
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        AppAnalyticsUtil.log(event: .Scherm, segment: .OnOffScherm)
    }

    func preferredContentHeight() -> CGFloat {
        return 90
    }
    
    @IBAction func onOffSwitchValueChanged(_ sender: UISwitch) {
        if sender == self.notificationsOnOffSwitch {
            self.notificationsOn = sender.isOn
            self.updatesOnOffSwitch?.isEnabled = sender.isOn
        } else if sender == self.updatesOnOffSwitch {
            self.updatesOn = sender.isOn
        }
    }
}
