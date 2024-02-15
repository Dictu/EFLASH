// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

extension UINavigationBar {
    
    func themeBackgroundImage() -> UIImage {
        let barFrame: CGRect = CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: self.bounds.height + UIApplication.shared.statusBarFrame.size.height)
        let barView: UIView = UIView(frame: barFrame)
        
        let imageView: UIImageView = UIImageView(image: UIImage(named: "rijkslogo"))
        //imageView.contentMode = UIViewContentMode.bottom
        
        let width:CGFloat = 50.0
        let height:CGFloat = 100.0;
        imageView.frame = CGRect(x: (UIScreen.main.bounds.width - width) / 2, y: barView.bounds.height-90, width: width, height: height)

        barView.addSubview(imageView)
        
        UIGraphicsBeginImageContextWithOptions(barFrame.size, false, UIScreen.main.scale)
        barView.layer.render(in: UIGraphicsGetCurrentContext()!)
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return image!
    }
    
}
