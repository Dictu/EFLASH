// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import GradientCircularProgress

public struct CircularProgressStyle : StyleProperty {
    public var progressSize: CGFloat = 155
    
    public var arcLineWidth: CGFloat = 5.0
    public var startArcColor: UIColor = UIColor.clear
    public var endArcColor: UIColor = UIColor.themeDarkTintColor()
    
    public var baseLineWidth: CGFloat? = 20.0
    public var baseArcColor: UIColor? = UIColor.clear
    
    public var ratioLabelFont: UIFont? = UIFont(name: "RijksoverheidSansTextTT-Regular", size: 4.0)
    public var ratioLabelFontColor: UIColor? = UIColor.themeTintColor()
    
    public var messageLabelFont: UIFont? = UIFont(name: "RijksoverheidSansTextTT-Bold", size: 4.0)
    public var messageLabelFontColor: UIColor? = UIColor.themeDarkTintColor()
    
    
    public var backgroundStyle: BackgroundStyles = .extraLight
    
    public var dismissTimeInterval: Double? = nil

    public init() {}
}

open class ProgressHelper {
    static var progress: GradientCircularProgress = GradientCircularProgress()

    public static func showProgress(_ inView: UIView, message: String) -> UIView? {
        if let progressView = progress.show(frame: getRect(inView), message: message, style: CircularProgressStyle()) {
            UIApplication.shared.beginIgnoringInteractionEvents()
            progressView.layer.cornerRadius = 8
            inView.addSubview(progressView)
            return progressView
        } else {
            return nil
        }
    }

    public static func hideProgress(_ progressView: UIView?) {
        if let view = progressView {
            UIApplication.shared.endIgnoringInteractionEvents()
            progress.dismiss(progress: view)
        }
    }
    
    fileprivate static func getRect(_ fromView: UIView) -> CGRect {
        let frameWidth: CGFloat = 155
        return CGRect(x: (fromView.bounds.size.width - frameWidth) / 2, y: (fromView.bounds.size.height - frameWidth) / 2, width: frameWidth, height: frameWidth)
    }
}
