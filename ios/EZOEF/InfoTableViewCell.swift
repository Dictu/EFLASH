// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class InfoTableViewCell: UITableViewCell {

    static var padding: CGFloat = 14

    var infoLabel: UILabel = CopyableLabel()
    var subInfoLabel: UILabel = CopyableLabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)

        self.backgroundColor = UIColor.white
        self.selectionStyle = .none
        
        self.infoLabel.numberOfLines = 0
        self.infoLabel.font = UIFont.themeRegularFont(12)
        self.infoLabel.textAlignment = NSTextAlignment.left
        self.infoLabel.textColor = UIColor(white: 0, alpha: 1)

        self.subInfoLabel.font = UIFont.themeRegularFont(12)
        self.subInfoLabel.adjustsFontSizeToFitWidth = true
        self.subInfoLabel.textAlignment = NSTextAlignment.left
        self.subInfoLabel.textColor = UIColor.themeDarkTintColor()
        
        self.contentView.addSubview(self.infoLabel)
        self.contentView.addSubview(self.subInfoLabel)
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        let padding: CGFloat = InfoTableViewCell.padding
        
        self.infoLabel.frame = CGRect(x: self.contentView.frame.origin.x + padding, y: self.contentView.frame.origin.y + padding, width: self.contentView.frame.size.width - padding - padding, height: self.contentView.frame.size.height - padding - padding)
        self.subInfoLabel.frame = CGRect(x: self.contentView.frame.origin.x + padding, y: self.contentView.frame.size.height - self.subInfoLabel.font.pointSize - padding / 2, width: self.contentView.frame.size.width - padding, height: self.subInfoLabel.font.pointSize)
    }

    func setInfo(_ info: String) {
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineSpacing = 4
        
        // EK 2-11-2017 onderstaande is nodig omdat het toevoegen van de font aan de attributed string niet werkt
        // onderstaande werkt dus wel.
        let modifiedFont = NSString(format:"<span style=\"font-family: RijksoverheidSansWebText-Regular; font-size: 14\">%@</span>" as NSString, info) as String
        let attributedString = try! NSMutableAttributedString(
            data: modifiedFont.data(using: String.Encoding.unicode, allowLossyConversion: true)!,
            options: [ .documentType: NSAttributedString.DocumentType.html],
            documentAttributes: nil)
        
        self.infoLabel.attributedText = attributedString
    }
    
    func setSubInfo(_ info: String) {
        self.subInfoLabel.text = info
    }
    
    static func height(_ info: String) -> CGFloat {
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineSpacing = 4
        
        // EK 2-11-2017 onderstaande is nodig omdat het toevoegen van de font aan de attributed string niet werkt
        // onderstaande werkt dus wel.
        let modifiedFont = NSString(format:"<span style=\"font-family: RijksoverheidSansWebText-Regular; font-size: 14\">%@</span>" as NSString, info) as String
        
        let attributedString = try! NSMutableAttributedString(
            data: modifiedFont.data(using: String.Encoding.unicode, allowLossyConversion: true)!,
            options: [ .documentType: NSAttributedString.DocumentType.html],
            documentAttributes: nil)
        
        let constraintRect = CGSize(width: UIScreen.main.bounds.width - padding - padding, height: CGFloat.greatestFiniteMagnitude)
        let boundingBox = attributedString.boundingRect(with: constraintRect, options: [.usesLineFragmentOrigin, .usesFontLeading], context: nil)
        return boundingBox.height + padding + padding + 14
    }

}
