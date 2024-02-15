// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

open class MenuHeaderView: UIView {
    
    var iconImageView: UIImageView = UIImageView()
    var appTitleLabel: UILabel = UILabel()
    var appVersionLabel: UILabel = UILabel()
    var titleLabel: UILabel = UILabel()
    var subTitleLabel: UILabel = UILabel()
    
    override init(frame: CGRect) {
        super.init(frame: frame)

        self.backgroundColor = UIColor.themeDarkTintColor()
        
        self.iconImageView.backgroundColor = UIColor(white: 0, alpha: 0.4)

        self.appTitleLabel.textColor = UIColor.white
        self.appTitleLabel.font = UIFont.themeBoldFont(32)
        self.appTitleLabel.textAlignment = NSTextAlignment.left
        self.appTitleLabel.text = Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String ?? ""

        self.appVersionLabel.textColor = UIColor.white
        self.appVersionLabel.font = UIFont.themeBoldFont(12)
        self.appVersionLabel.textAlignment = NSTextAlignment.left
        self.appVersionLabel.text = "v" + (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String)!

        self.titleLabel.textColor = UIColor.white
        self.titleLabel.font = UIFont.themeBoldFont(16)
        self.titleLabel.textAlignment = NSTextAlignment.left

        self.subTitleLabel.textColor = UIColor.white
        self.subTitleLabel.font = UIFont.themeRegularFont(14)
        self.subTitleLabel.textAlignment = NSTextAlignment.left
        
        self.iconImageView.image = self.icon
        self.iconImageView.layer.masksToBounds = true

        self.addSubview(self.iconImageView)
        self.addSubview(self.appTitleLabel)
        self.addSubview(self.appVersionLabel)
        self.addSubview(self.titleLabel)
        self.addSubview(self.subTitleLabel)
    }
    
    public var icon: UIImage? {
        if let icons = Bundle.main.infoDictionary?["CFBundleIcons"] as? [String: Any],
            let primaryIcon = icons["CFBundlePrimaryIcon"] as? [String: Any],
            let iconFiles = primaryIcon["CFBundleIconFiles"] as? [String],
            let lastIcon = iconFiles.last {
            return UIImage(named: lastIcon)
        }
        return nil
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override open func layoutSubviews() {
        super.layoutSubviews()
        
        let padding: CGFloat = 18
        let iconSize: CGFloat = 40

        self.iconImageView.frame = CGRect(x: 14, y: 36, width: iconSize, height: iconSize)
        self.iconImageView.layer.cornerRadius = iconSize * 0.14
        
        self.appTitleLabel.frame = CGRect(x: iconSize + 14 + 6, y: self.iconImageView.frame.origin.y - 2, width: self.frame.size.width - (iconSize + 14 + 6), height: iconSize)
        self.appVersionLabel.frame = self.appTitleLabel.frame.offsetBy(dx: 2, dy: 16)

        self.titleLabel.frame = CGRect(x: padding, y: self.frame.size.height - self.titleLabel.font.pointSize * 2 - padding, width: self.frame.size.width - padding, height: self.titleLabel.font.pointSize)
        self.subTitleLabel.frame = CGRect(x: padding, y: self.titleLabel.frame.origin.y + self.titleLabel.frame.size.height + 6, width: self.frame.size.width - padding, height: self.subTitleLabel.font.pointSize)
    }
    
    func setTitle(_ title: String) {
        self.titleLabel.text = title
    }
    
    func setSubTitle(_ subTitle: String) {
        self.subTitleLabel.text = subTitle
    }
}
