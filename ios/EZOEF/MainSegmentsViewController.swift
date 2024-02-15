// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit
import KYDrawerController
import Alamofire

class MainSegmentsViewController: UIViewController {

    @IBOutlet var segmentedControl: UISegmentedControl!
    @IBOutlet var disruptionsScrollView: UIScrollView!
    
    var openDisruptionsViewController: DisruptionsViewController = DisruptionsViewController(disruptionType: .openDisruptions)
    var announcementsViewController: DisruptionsViewController = DisruptionsViewController(disruptionType: .announcements)
    var closedDisruptionsViewController: DisruptionsViewController = DisruptionsViewController(disruptionType: .closedDisruptions)
    var dragging: Bool = false
    var shouldRefreshMenu: Bool = true
    
    convenience init() {
        self.init(nibName: String(describing: MainSegmentsViewController.self), bundle: Bundle.main)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Observe filter refreshes
        NotificationCenter.default.addObserver(self, selector: #selector(refresh), name: NSNotification.Name(rawValue: MenuViewController.kRefreshFilterNotification), object: nil)

        self.navigationItem.leftBarButtonItem = UIBarButtonItem(title: "\u{2630}", style: .plain, target: self, action: #selector(openMenu))
        self.view.backgroundColor = UIColor.themeTintColor()
        
        if #available(iOS 13.0, *) {
            segmentedControl.setTitleTextAttributes([.foregroundColor: UIColor.themeTintColor()], for: .selected)
            segmentedControl.selectedSegmentTintColor = .white
        }

        self.segmentedControl.setTitle(NSLocalizedString("Verstoringen", comment: ""), forSegmentAt: 0)
        self.segmentedControl.setTitle(NSLocalizedString("Mededelingen", comment: ""), forSegmentAt: 1)
        self.segmentedControl.setTitle(NSLocalizedString("Opgelost", comment: ""), forSegmentAt: 2)
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        self.openDisruptionsViewController.view.frame = self.disruptionsScrollView.bounds
        self.openDisruptionsViewController.view.frame.origin.x = 0
        
        self.disruptionsScrollView.addSubview(self.openDisruptionsViewController.view)
        self.addChild(self.openDisruptionsViewController)
        
        self.announcementsViewController.view.frame = self.disruptionsScrollView.bounds
        self.announcementsViewController.view.frame.origin.x = self.disruptionsScrollView.bounds.width
        
        self.disruptionsScrollView.addSubview(self.announcementsViewController.view)
        self.addChild(self.announcementsViewController)
        
        self.closedDisruptionsViewController.view.frame = self.disruptionsScrollView.bounds
        self.closedDisruptionsViewController.view.frame.origin.x = self.disruptionsScrollView.bounds.width * 2
        
        self.disruptionsScrollView.addSubview(self.closedDisruptionsViewController.view)
        self.addChild(self.closedDisruptionsViewController)
        
        self.disruptionsScrollView.contentSize = CGSize(width: self.disruptionsScrollView.bounds.width * 3, height: self.disruptionsScrollView.bounds.height)
        
        self.disruptionsScrollView.contentOffset = CGPoint(x: CGFloat(self.segmentedControl.selectedSegmentIndex) * self.disruptionsScrollView.bounds.width, y: 0)
        
        if self.navigationController!.navigationBar.backgroundImage(for: .default) != nil {
            //  Reposition navigation bar logo
            self.navigationController!.navigationBar.setBackgroundImage(
                self.navigationController!.navigationBar.themeBackgroundImage(),
                for: .default
            )
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.navigationController!.navigationBar.setBackgroundImage(
            self.navigationController!.navigationBar.themeBackgroundImage(),
            for: .default
        )
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        self.navigationController!.navigationBar.setBackgroundImage(nil, for: .default)
        
        super.viewWillDisappear(animated)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        AppAnalyticsUtil.log(event: .Scherm, segment: .VerstoringenScherm)

        self.refresh()
        self.refreshMenu()
    }
    
    @objc func refresh() {
        [self.openDisruptionsViewController,
         self.announcementsViewController,
         self.closedDisruptionsViewController][self.segmentedControl.selectedSegmentIndex].tabDidAppear()
    }
    
    func refreshMenu() {
        if self.shouldRefreshMenu, let navigationController = self.navigationController, let drawerController = navigationController.parent as? KYDrawerController, let menuViewController = drawerController.drawerViewController as? MenuViewController {
            menuViewController.viewDidAppear(false)
            self.shouldRefreshMenu = false
        }
    }

    @objc func openMenu(_ sender: Any) {
        if let navigationController: UINavigationController = self.navigationController {
            if let drawerController: KYDrawerController = navigationController.parent as? KYDrawerController {
                drawerController.setDrawerState(.opened, animated: true)
            }
        }
    }

    @IBAction func segmentValueChanged(_ sender: Any) {
        self.dragging = false

        let pageWidth: CGFloat = self.disruptionsScrollView.frame.size.width
        let page: CGFloat = CGFloat((sender as? UISegmentedControl)!.selectedSegmentIndex)
        self.disruptionsScrollView.setContentOffset(CGPoint(x: page * pageWidth, y: 0), animated: true)

        self.refresh()
    }
}

extension MainSegmentsViewController : UIScrollViewDelegate {
    
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        guard self.dragging else {
            return
        }

        let pageWidth: CGFloat = scrollView.frame.size.width
        let fractionalPage: CGFloat = scrollView.contentOffset.x / pageWidth
        let page: NSInteger = Int(round(fractionalPage))

        if self.segmentedControl.selectedSegmentIndex != page  {
            self.segmentedControl.selectedSegmentIndex = page
            self.refresh()
        }
    }
    
    func scrollViewWillBeginDecelerating(_ scrollView: UIScrollView) {
        self.dragging = true
    }

    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        self.dragging = false
    }

    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        self.dragging = true
    }

    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        self.dragging = false
    }
}
