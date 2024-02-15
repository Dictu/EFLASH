// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class InfoViewController: UIViewController {

    @IBOutlet var infoTextView: UITextView!
    
    var info: String = "" {
        didSet {
            self.setHtmlAttributedText(info)
        }
    }

    convenience init() {
        self.init(nibName: String(describing: InfoViewController.self), bundle: Bundle.main)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.setHtmlAttributedText(self.info)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        AppAnalyticsUtil.log(event: .Scherm, segment: .InfoScherm)
    }

    func preferredContentHeight() -> CGFloat {
        let attributedString: NSAttributedString = self.attributedHtmlText(self.info)
        let constraintRect = CGSize(width: self.view.bounds.width, height: CGFloat.greatestFiniteMagnitude)
        let boundingBox = attributedString.boundingRect(with: constraintRect, options: [.usesLineFragmentOrigin, .usesFontLeading], context: nil)
        return boundingBox.height + 40
    }

    fileprivate func setHtmlAttributedText(_ htmlText: String) {
        if let textView = self.infoTextView {
            textView.attributedText = self.attributedHtmlText(htmlText)
        }
    }
    
    fileprivate func attributedHtmlText(_ text: String) -> NSAttributedString {
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineSpacing = 4
        paragraphStyle.alignment = .center
        
        let attributedString = try! NSMutableAttributedString(
            data: text.data(using: String.Encoding.unicode, allowLossyConversion: true)!,
            options: [.documentType: NSAttributedString.DocumentType.html],
            documentAttributes: nil)
        attributedString.addAttribute(NSAttributedString.Key.paragraphStyle, value:paragraphStyle, range:NSMakeRange(0, attributedString.length))
        attributedString.addAttribute(NSAttributedString.Key.font, value: UIFont.themeRegularFont(17), range:NSMakeRange(0, attributedString.length))
        
        return attributedString
    }
}

extension InfoViewController: UITextViewDelegate {

    func textView(_ textView: UITextView, shouldInteractWith URL: URL, in characterRange: NSRange) -> Bool {
        if let scheme = URL.scheme {
            if UIApplication.shared.canOpenURL(URL) {
                let link = (URL.absoluteString as NSString).replacingOccurrences(of: "\(scheme)" + ":", with: "") as String
                
                var message: String?
                switch scheme {
                case "tel":
                    message = NSLocalizedString("\(link)\nbellen?", comment: "")
                    break;
                case "mailto":
                    message = NSLocalizedString("\(link)\nemailen?", comment: "")
                    break;
                default:
                    message = NSLocalizedString("\(link)\nopenen?", comment: "")
                }

                let alertController: UIAlertController = UIAlertController(title: nil, message: message, preferredStyle: .alert)
                alertController.addAction(UIAlertAction(title: NSLocalizedString("Ok", comment: ""), style: .default) { (alertAction) in
                    UIApplication.shared.openURL(URL)
                    })
                alertController.addAction(UIAlertAction(title: NSLocalizedString("Annuleren", comment: ""), style: .cancel) { (alertAction) in
                    })
                self.present(alertController, animated: true) {}
            }
        }
        return false
    }
    
}
