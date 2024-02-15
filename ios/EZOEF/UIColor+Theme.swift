// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension UIColor {
 
    class func themeTintColor() -> UIColor {
        return UIColor(red: 0xF3 / 255, green: 0x6A / 255, blue: 0x4A / 255, alpha: 1.0)
    }
    
    class func themeDarkTintColor() -> UIColor {
        return UIColor(red: 0xE5 / 255, green: 0xA0 / 255, blue: 0x50 / 255, alpha: 1.0)
    }

    
    class func themeGradientStartColor() -> UIColor {
            return UIColor(red: 0xF3 / 255, green: 0x6A / 255, blue: 0x4A / 255, alpha: 1.0)
    }
    
    class func themeGradientEndColor() -> UIColor {
            return UIColor(red: 0xE5 / 255, green: 0xA0 / 255, blue: 0x50 / 255, alpha: 1.0)
    }

}
