// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class CardTableViewCell: UITableViewCell {
    
    var cardView: UIView = UIView()
    var titleLabel: UILabel = UILabel()
    var leftSubTitleLabel: UILabel = UILabel()
    var leftSubTitleLabel2: UILabel = UILabel()
    var leftDateLabel: UILabel = UILabel()
    var rightDateLabel: UILabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)

        self.backgroundColor = UIColor.clear
        self.selectionStyle = .none
        
        self.cardView.layer.cornerRadius = 2.5
        self.cardView.layer.shadowColor = UIColor.black.cgColor
        self.cardView.layer.shadowRadius = 1.5
        self.cardView.layer.shadowOffset = CGSize(width: 0, height: 1)
        self.cardView.layer.shadowOpacity = 0.2
        
        self.titleLabel.font = UIFont.themeBoldFont(16)
        self.titleLabel.numberOfLines = 2
        self.titleLabel.textAlignment = NSTextAlignment.left

        self.leftSubTitleLabel.font = UIFont.themeRegularFont(12)
        self.leftSubTitleLabel.numberOfLines = 1
        self.leftSubTitleLabel.textAlignment = NSTextAlignment.left

        self.leftSubTitleLabel2.font = UIFont.themeRegularFont(12)
        self.leftSubTitleLabel2.numberOfLines = 1
        self.leftSubTitleLabel2.textAlignment = NSTextAlignment.left

        self.leftDateLabel.font = UIFont.themeRegularFont(12)
        self.leftDateLabel.textAlignment = NSTextAlignment.left
        
        self.rightDateLabel.font = UIFont.themeRegularFont(12)
        self.rightDateLabel.textAlignment = NSTextAlignment.right
        
        self.accessoryType = .disclosureIndicator

        self.contentView.addSubview(self.cardView)
        self.contentView.addSubview(self.titleLabel)
        self.contentView.addSubview(self.leftSubTitleLabel)
        self.contentView.addSubview(self.leftSubTitleLabel2)
        self.contentView.addSubview(self.leftDateLabel)
        self.contentView.addSubview(self.rightDateLabel)
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        super.setHighlighted(highlighted, animated: animated)

        if highlighted {
            self.cardView.backgroundColor = UIColor(white: 1, alpha: 0xac / 255)
            self.titleLabel.textColor = UIColor(white: 0, alpha: 0xac / 255)
            self.leftSubTitleLabel.textColor = UIColor(white: 0, alpha: 0xac / 255)
            self.leftSubTitleLabel2.textColor = UIColor(white: 0, alpha: 0xac / 255)
            self.leftDateLabel.textColor = UIColor.themeDarkTintColor().withAlphaComponent(0xac / 255)
            self.rightDateLabel.textColor = UIColor.themeDarkTintColor().withAlphaComponent(0xac / 255)
        } else {
            self.cardView.backgroundColor = UIColor(white: 1, alpha: 1)
            self.titleLabel.textColor = UIColor(white: 0, alpha: 1)
            self.leftSubTitleLabel.textColor = UIColor(white: 0, alpha: 1)
            self.leftSubTitleLabel2.textColor = UIColor(white: 0, alpha: 1)
            self.leftDateLabel.textColor = UIColor.themeDarkTintColor()
            self.rightDateLabel.textColor = UIColor.themeDarkTintColor()
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        let cardPadding: CGFloat = 10
        let padding: CGFloat = 7
        
        self.cardView.frame = CGRect(x: self.frame.origin.x + cardPadding, y: self.contentView.frame.origin.y + cardPadding, width: self.frame.size.width - 2 * cardPadding, height: self.frame.size.height - cardPadding)

        let paddingFrame: CGRect = self.cardView.frame.insetBy(dx: padding * 2, dy: padding)
        
        self.titleLabel.frame = paddingFrame
        self.titleLabel.frame.size.height = self.titleLabel.font.pointSize * 2

        self.leftSubTitleLabel.frame = CGRect(x: paddingFrame.origin.x, y: paddingFrame.origin.y + paddingFrame.size.height - self.leftSubTitleLabel.font.pointSize * 3, width: paddingFrame.size.width * 7.5 / 10, height: 12)
        
        self.leftSubTitleLabel2.frame = CGRect(x: paddingFrame.origin.x, y: paddingFrame.origin.y + paddingFrame.size.height - self.leftSubTitleLabel.font.pointSize * 2, width: paddingFrame.size.width * 7.5 / 10, height: 12)

        self.leftDateLabel.frame = CGRect(x: paddingFrame.origin.x, y: paddingFrame.origin.y + paddingFrame.size.height - self.leftSubTitleLabel.font.pointSize, width: (paddingFrame.size.width) / 2, height: self.leftDateLabel.font.pointSize)
        
        self.rightDateLabel.frame = CGRect(x: self.leftDateLabel.frame.width + padding, y: paddingFrame.origin.y + paddingFrame.size.height - self.leftSubTitleLabel.font.pointSize, width: (paddingFrame.size.width - self.leftDateLabel.frame.size.width + padding + padding), height: self.rightDateLabel.font.pointSize)
    }

    func setTitle(_ title: String) {
        self.titleLabel.text = title
    }
    
    func setLeftSubTitle(_ subTitle: String) {
        self.leftSubTitleLabel.text = subTitle
    }

    func setLeftSubTitle2(_ subTitle: String) {
        self.leftSubTitleLabel2.text = subTitle
    }

    func setLeftDateLabel(_ subTitle: String) {
        self.leftDateLabel.text = subTitle
    }
    
    func setRightDateLabel(_ subTitle: String) {
        self.rightDateLabel.text = subTitle
    }
}
