// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit
import KYDrawerController
import Alamofire

class DisruptionsViewController: UIViewController {

    @IBOutlet var tableView: UITableView!

    enum DisruptionType: Int {
        case openDisruptions,
        announcements,
        closedDisruptions
    }
    
    var refreshing: Bool = false
    var disruptions: [Disruption] = []
    var disruptionType: DisruptionType!
    var refreshDate: Date?
    var refreshControl: UIRefreshControl!
    var lastUpdatedHeaderView: LastUpdatedHeaderView!
    var lastUpdatedDateFormatter = DateFormatter()
    
    var lastUpdatedTitle: String {
        get {
            let refreshDateText: String
            if let refreshDate = self.refreshDate {
                refreshDateText = self.lastUpdatedDateFormatter.string(from: refreshDate)
            } else {
                refreshDateText = NSLocalizedString("geen", comment: "")
            }
            return NSLocalizedString("Laatst bijgewerkt: ", comment: "") + refreshDateText
        }
    }
    
    convenience init(disruptionType: DisruptionType) {
        self.init(nibName: String(describing: DisruptionsViewController.self), bundle: Bundle.main)
        self.disruptionType = disruptionType
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        self.lastUpdatedHeaderView = LastUpdatedHeaderView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 30))
        self.lastUpdatedDateFormatter.dateFormat = "HH:mm'u'"
        self.lastUpdatedDateFormatter.locale = Locale(identifier: "nl-NL")
        self.lastUpdatedHeaderView.setTitle(self.lastUpdatedTitle)

        self.tableView.tableFooterView = UIImageView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 10))
        self.tableView.tableHeaderView = self.lastUpdatedHeaderView

        self.refreshControl = UIRefreshControl()
        self.refreshControl.addTarget(self, action: #selector(refresh), for: UIControl.Event.valueChanged)
        self.tableView.addSubview(refreshControl)

        self.tableView.register(NoRecordsTableViewCell.self, forCellReuseIdentifier: "noRecordsCellIdentifier")
        self.tableView.register(CardTableViewCell.self, forCellReuseIdentifier: "cardCellIdentifier")
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        self.navigationController?.setToolbarHidden(true, animated: animated)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }
    
    func tabDidAppear() {
        guard Profile.sharedProfile.profileSaved else {
            return
        }
        
        self.refresh(self.refreshDate != nil ? nil : self)
    }
    
    @objc func refresh(_ sender: Any?) {
        guard ApiConnector.sharedConnector.auth.isRegistered else {
            self.refreshControl.endRefreshing()
            return
        }
        
        guard !self.refreshing else {
            return
        }

        self.refreshing = true
        self.refreshControl.beginRefreshing()
        
        // Only show a progress view sender was filled
        let progressView: UIView? = sender != nil
            ? ProgressHelper.showProgress(self.view, message: NSLocalizedString("Ophalen verstoringen...", comment: ""))
            : nil

        let completionHandler: (DataResponse<Any>) -> Void = { response in
            if let JSON = response.result.value {
                self.disruptions = Disruption.fromJSON(JSON)
                self.tableView.reloadData()
            }
            
            if let error: Error = response.result.error {
                let errorAlertController = UIAlertController.errorAlertController("Ophalen verstoringen mislukt.\nReden: \(error.localizedDescription)")
                self.present(errorAlertController, animated: true) {}
            } else {
                self.refreshDate = Date()
            }

            self.refreshing = false
            self.refreshControl.endRefreshing()
            ProgressHelper.hideProgress(progressView)

            self.lastUpdatedHeaderView.setTitle(self.lastUpdatedTitle)
        }
        
        if self.disruptionType == .openDisruptions {
            // Fetch open disruptions
            ApiConnector.sharedConnector.openDisruptions(
                Profile.sharedProfile.services.map { $0.id },
                locations: Profile.sharedProfile.locations.map { $0.id },
                completionHandler: completionHandler
            )
            } else if (self.disruptionType == .announcements) {
                // Fetch announcements
                ApiConnector.sharedConnector.announcements(
                    Profile.sharedProfile.services.map { $0.id },
                    locations: Profile.sharedProfile.locations.map { $0.id },
                    completionHandler: completionHandler
                )
            
            
        } else {
            // Fetch closed disruptions
            ApiConnector.sharedConnector.closedDisruptions(
                Profile.sharedProfile.services.map { $0.id },
                locations: Profile.sharedProfile.locations.map { $0.id },
                completionHandler: completionHandler
            )
        }
    }
    
}

extension DisruptionsViewController : UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.disruptions.isEmpty ? 1 : self.disruptions.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard !self.disruptions.isEmpty else {
            let cell = tableView.dequeueReusableCell(withIdentifier: "noRecordsCellIdentifier", for: indexPath) as! NoRecordsTableViewCell

            if self.refreshing {
                cell.setTitle("")
            } else {
                if self.disruptionType == .openDisruptions {
                    cell.setTitle(NSLocalizedString("Er zijn op dit moment geen verstoringen", comment: ""))
                } else if (self.disruptionType == .announcements) {
                    cell.setTitle(NSLocalizedString("Er zijn op dit moment geen mededelingen", comment: ""))
                } else {
                    cell.setTitle(NSLocalizedString("Er zijn op dit moment geen opgeloste verstoringen", comment: ""))
                }
            }

            return cell
        }

        let cell = tableView.dequeueReusableCell(withIdentifier: "cardCellIdentifier", for: indexPath) as! CardTableViewCell
        
        let disruption: Disruption = self.disruptions[indexPath.row]

        cell.setTitle(disruption.title)
        cell.setLeftSubTitle(disruption.service)
        cell.setLeftSubTitle2(disruption.location.isEmpty ? "-" : disruption.location)
        cell.setLeftDateLabel(NSLocalizedString("Aangemaakt: ", comment: "") + disruption.dateTime.relativeTimeNew)
        cell.setRightDateLabel(NSLocalizedString("Bijgewerkt: ", comment: "") + disruption.lastUpdatedDateTime.relativeTimeNew)
                
        return cell
    }
}

extension DisruptionsViewController : UITableViewDelegate {
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return self.disruptions.isEmpty ? 44 : 100
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        self.tableView.deselectRow(at: indexPath, animated: true)
        
        guard !self.disruptions.isEmpty else {
            return
        }
        
        let disruptionDetailViewController: DisruptionDetailViewController = DisruptionDetailViewController()
        disruptionDetailViewController.disruption = self.disruptions[indexPath.row]
        self.navigationController?.pushViewController(disruptionDetailViewController, animated: true)
    }
    
}
