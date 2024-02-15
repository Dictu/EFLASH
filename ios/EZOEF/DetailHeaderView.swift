// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

open class DetailHeaderView: UIView {
    
    var titleLabel: UILabel = UILabel()
    var subTitleLabel: UILabel = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)

        self.backgroundColor = UIColor.themeTintColor()
        
        self.titleLabel.textColor = UIColor.white
        self.titleLabel.font = UIFont.themeBoldFont(16)
        self.titleLabel.textAlignment = NSTextAlignment.left

        self.subTitleLabel.textColor = UIColor.white
        self.subTitleLabel.font = UIFont.themeRegularFont(14)
        self.subTitleLabel.textAlignment = NSTextAlignment.left

        self.addSubview(self.titleLabel)
        self.addSubview(self.subTitleLabel)
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override open func layoutSubviews() {
        super.layoutSubviews()
        
        let padding: CGFloat = 18
        let iconSize: CGFloat = 56

        self.titleLabel.frame = CGRect(x: padding, y: self.frame.origin.y + iconSize, width: self.frame.size.width - padding, height: self.titleLabel.font.pointSize)
        self.subTitleLabel.frame = CGRect(x: padding, y: self.frame.size.height - titleLabel.font.pointSize, width: self.frame.size.width - padding, height: self.subTitleLabel.font.pointSize)
    }
    
    func setTitle(_ title: String) {
        self.titleLabel.text = title
    }
    
    func setSubTitle(_ subTitle: String) {
        self.subTitleLabel.text = subTitle
    }
}
