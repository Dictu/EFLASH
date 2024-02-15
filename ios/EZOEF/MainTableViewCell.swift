// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class MainTableViewCell: UITableViewCell {
    
    var gradientView: GradientView = GradientView(startColor: UIColor.themeGradientStartColor(), endColor: UIColor.themeGradientEndColor())
    var titleLabel: UILabel = CopyableLabel()
    var leftSubTitleLabel: UILabel = CopyableLabel()
    var leftDateLabel: UILabel = CopyableLabel()
    var leftSubTitleLabel2: UILabel = CopyableLabel()
    var rightDateLabel: UILabel = UILabel()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        
        self.backgroundColor = UIColor.themeTintColor()
        self.selectionStyle = .none
        
        self.titleLabel.font = UIFont.themeBoldFont(18)
        self.titleLabel.numberOfLines = 4
        self.titleLabel.textAlignment = NSTextAlignment.left
        self.titleLabel.textColor = UIColor(white: 1, alpha: 1)
        
        self.leftSubTitleLabel.font = UIFont.themeRegularFont(16)
        self.leftSubTitleLabel.textAlignment = NSTextAlignment.left
        self.leftSubTitleLabel.textColor = UIColor(white: 1, alpha: 0.7)
        
        self.leftSubTitleLabel2.font = UIFont.themeRegularFont(16)
        self.leftSubTitleLabel2.textAlignment = NSTextAlignment.left
        self.leftSubTitleLabel2.textColor = UIColor(white: 1, alpha: 0.7)
        
        self.leftDateLabel.font = UIFont.themeRegularFont(11)
        self.leftDateLabel.textAlignment = NSTextAlignment.left
        self.leftDateLabel.textColor = UIColor(white: 1, alpha: 0.7)
        
        self.rightDateLabel.font = UIFont.themeRegularFont(11)
        self.rightDateLabel.textAlignment = NSTextAlignment.right
        self.rightDateLabel.textColor = UIColor(white: 1, alpha: 0.7)
        
        self.contentView.addSubview(self.gradientView)
        self.contentView.addSubview(self.titleLabel)
        self.contentView.addSubview(self.leftSubTitleLabel)
        self.contentView.addSubview(self.leftDateLabel)
        self.contentView.addSubview(self.leftSubTitleLabel2)
        self.contentView.addSubview(self.rightDateLabel)
        
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        let padding: CGFloat = 14
        
        self.gradientView.frame = self.contentView.frame
        self.titleLabel.frame = CGRect(x: self.frame.origin.x + padding, y: self.frame.origin.y + padding / 2, width: self.frame.size.width - padding - padding, height: self.titleLabel.font.pointSize * 3)
        self.leftSubTitleLabel.frame = CGRect(x: self.frame.origin.x + padding, y: self.titleLabel.frame.size.height + padding / 2, width: (self.frame.size.width - padding) / 2, height: self.leftSubTitleLabel.font.pointSize)
        
        self.leftSubTitleLabel2.frame = CGRect(x: self.frame.origin.x + padding, y: self.titleLabel.frame.size.height + padding + self.leftSubTitleLabel.frame.size.height, width: (self.frame.size.width - padding) / 2, height: 12)
        
        self.leftDateLabel.frame = CGRect(x: self.leftSubTitleLabel2.frame.origin.x, y: self.frame.size.height - self.leftDateLabel.font.pointSize - padding / 7 , width: (self.frame.size.width - padding) / 2, height: self.leftDateLabel.font.pointSize)
        
        self.rightDateLabel.frame = CGRect(x: self.leftDateLabel.frame.size.width + padding, y: self.leftDateLabel.frame.origin.y, width: (self.frame.size.width - self.leftDateLabel.frame.size.width - padding - padding), height: self.rightDateLabel.font.pointSize)
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
    
    func setLeftDateSubTitle(_ subTitle2: String) {
        self.leftDateLabel.text = subTitle2
    }
    
    func setRightDateSubTitle(_ subTitle2: String) {
        self.rightDateLabel.text = subTitle2
    }
    
    static func height() -> CGFloat {
        return 116
    }
    
}
