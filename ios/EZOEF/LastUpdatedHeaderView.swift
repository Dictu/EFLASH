// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

open class LastUpdatedHeaderView: UIView {
    
    var titleLabel: UILabel = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)

        self.backgroundColor = UIColor.clear
        
        self.titleLabel.textColor = UIColor(white: 0, alpha: 0.7)
        self.titleLabel.font = UIFont.themeRegularFont(14)
        self.titleLabel.textAlignment = .center

        self.addSubview(self.titleLabel)
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override open func layoutSubviews() {
        super.layoutSubviews()

        self.titleLabel.frame = CGRect(x: self.frame.origin.x, y: self.frame.origin.y + 10, width: self.frame.size.width, height: self.frame.size.height - 10)
    }
    
    func setTitle(_ title: String) {
        self.titleLabel.text = title
    }
}
