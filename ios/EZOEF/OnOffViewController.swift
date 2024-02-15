// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class OnOffViewController: UIViewController {

    @IBOutlet var textLabel: UILabel?
    @IBOutlet var onOffSwitch: UISwitch?
    
    var onOffTitle: String = "" {
        didSet {
            if let textLabel = self.textLabel {
                textLabel.text = onOffTitle
            }
        }
    }
    var on: Bool = false {
        didSet {
            if let onOffSwitch = self.onOffSwitch {
                onOffSwitch.isOn = on
            }
        }
    }

    convenience init() {
        self.init(nibName: String(describing: OnOffViewController.self), bundle: Bundle.main)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        if let textLabel = self.textLabel {
            textLabel.textColor = UIColor(white: 0, alpha: 0xac / 255)
            textLabel.font = UIFont.themeRegularFont(textLabel.font.pointSize)
            textLabel.textAlignment = NSTextAlignment.left
            textLabel.text = self.onOffTitle
        }

        if let onOffSwitch = self.onOffSwitch {
            onOffSwitch.isOn = self.on
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        AppAnalyticsUtil.log(event: .Scherm, segment: .OnOffScherm)
    }

    func preferredContentHeight() -> CGFloat {
        return 62
    }
    
    @IBAction func onOffSwitchValueChanged(_ sender: Any) {
        self.on = (sender as! UISwitch).isOn
    }
}
