// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class SectionHeaderView : UITableViewHeaderFooterView {
    
    var titleLabel:UILabel = UILabel()
    
    required override init(reuseIdentifier: String?) {
        super.init(reuseIdentifier: reuseIdentifier)

        self.contentView.backgroundColor = UIColor.clear

        self.titleLabel.textColor = UIColor.themeDarkTintColor()
        self.titleLabel.font = UIFont.themeRegularFont(15)
        
        self.contentView.addSubview(self.titleLabel)
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()

        self.titleLabel.frame = CGRect(x: 14, y: 0, width: self.frame.size.width - 14, height: self.frame.size.height)
    }

    func setTitle(_ title: String) {
        self.titleLabel.text = title.uppercased()
    }
}
