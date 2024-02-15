// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class NoRecordsTableViewCell: UITableViewCell {

    var titleLabel: UILabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)

        self.backgroundColor = UIColor.clear
        self.selectionStyle = .none

        self.titleLabel.font = UIFont.themeBoldFont(16)
        self.titleLabel.numberOfLines = 2
        self.titleLabel.textAlignment = NSTextAlignment.center

        self.contentView.addSubview(self.titleLabel)
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        super.setHighlighted(highlighted, animated: animated)

        if highlighted {
            self.titleLabel.textColor = UIColor(white: 0, alpha: 0xac / 255)
        } else {
            self.titleLabel.textColor = UIColor(white: 0, alpha: 1)
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        self.titleLabel.frame = self.contentView.frame
    }

    func setTitle(_ title: String) {
        self.titleLabel.text = title
    }

}
