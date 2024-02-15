// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension UIFont {
    
    class func themeRegularFont(_ size: CGFloat) -> UIFont {
        return UIFont(name: "RijksoverheidSansWebText-Regular", size: size)!
    }
    
    class func themeBoldFont(_ size: CGFloat) -> UIFont {
        return UIFont(name: "RijksoverheidSansWebText-Bold", size: size)!
    }
    
    class func themeItalicFont(_ size: CGFloat) -> UIFont {
        return UIFont(name: "RijksoverheidSansWebText-Italic", size: size)!
    }
    
    
}
