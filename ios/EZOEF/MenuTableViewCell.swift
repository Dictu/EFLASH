// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class MenuTableViewCell: UITableViewCell {
    
    var iconImageView: UIImageView = UIImageView()
    var titleLabel: UILabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)

        self.backgroundColor = UIColor.clear
        
        self.titleLabel.font = UIFont.themeRegularFont(16)
        self.titleLabel.textAlignment = NSTextAlignment.left

        self.contentView.addSubview(self.titleLabel)
        self.contentView.addSubview(self.iconImageView)
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        super.setHighlighted(highlighted, animated: animated)

        if highlighted {
            self.titleLabel.textColor = UIColor(white: 0, alpha: 1)
            self.iconImageView.alpha = 1
        } else {
            self.titleLabel.textColor = UIColor(white: 0, alpha: 0xac / 255)
            self.iconImageView.alpha = 0xac / 255
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        let iconPadding: CGFloat = 18
        let iconSize: CGFloat = self.contentView.frame.size.height - iconPadding - iconPadding

        self.iconImageView.frame = CGRect(x: self.contentView.frame.origin.x + iconPadding, y: self.contentView.frame.origin.y + iconPadding, width: iconSize, height: iconSize)
        self.titleLabel.frame = CGRect(x: self.contentView.frame.origin.x + self.iconImageView.frame.size.width + iconPadding + iconPadding, y: self.contentView.frame.origin.y, width: self.contentView.frame.size.width - iconPadding, height: self.contentView.frame.size.height)
    }

    func setTitle(_ title: String) {
        self.titleLabel.text = title
    }
    
    func setIcon(_ icon: UIImage?) {
        self.iconImageView.image = icon
    }

}
