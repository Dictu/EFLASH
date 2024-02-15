// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class GradientView: UIView {
    
    @IBInspectable var startColor: UIColor!
    @IBInspectable var endColor: UIColor!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }
    
    required convenience init(startColor: UIColor, endColor: UIColor) {
        self.init()

        self.startColor = startColor
        self.endColor = endColor
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func draw(_ rect: CGRect) {
        let currentContext = UIGraphicsGetCurrentContext()

        currentContext!.saveGState();

        let colorSpace = CGColorSpaceCreateDeviceRGB()

        let startColor = self.startColor
        let startColorComponents = (startColor?.cgColor)?.components
        let endColor = self.endColor
        let endColorComponents = (endColor?.cgColor)?.components

        let colorComponents: [CGFloat] = [startColorComponents![0], startColorComponents![1], startColorComponents![2], startColorComponents![3], endColorComponents![0], endColorComponents![1], endColorComponents![2], endColorComponents![3]]

        var locations:[CGFloat] = [0.0, 1.0]

        let gradient = CGGradient(colorSpace: colorSpace, colorComponents: colorComponents, locations: &locations, count: 2)
        let startPoint = CGPoint(x: 0, y: self.bounds.height)
        let endPoint = CGPoint(x: self.bounds.width, y: self.bounds.height)

        currentContext!.drawLinearGradient(gradient!, start: startPoint, end: endPoint, options: .drawsAfterEndLocation)
        currentContext!.restoreGState();
    }
    
}
