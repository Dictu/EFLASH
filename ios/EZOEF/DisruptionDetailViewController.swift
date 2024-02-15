// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit

class DisruptionDetailViewController: UIViewController {

    @IBOutlet var tableView: UITableView!

    var disruption: Disruption?

    convenience init() {
        self.init(nibName: String(describing: DisruptionDetailViewController.self), bundle: Bundle.main)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = NSLocalizedString("Details", comment: "")

        self.view.backgroundColor = UIColor.themeTintColor()
        self.tableView.separatorStyle = .singleLine
        self.tableView.tableFooterView = UIImageView()
        
        let flexibleSpace = UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil)
        let shareBarButtonItem = UIBarButtonItem(barButtonSystemItem: .action, target: self, action: #selector(shareButtonPressed))
        self.toolbarItems = [flexibleSpace, shareBarButtonItem, flexibleSpace]
        
        self.tableView.register(SectionHeaderView.self, forHeaderFooterViewReuseIdentifier: "sectionHeaderIdentifier")
        self.tableView.register(MainTableViewCell.self, forCellReuseIdentifier: "mainCellIdentifier")
        self.tableView.register(InfoTableViewCell.self, forCellReuseIdentifier: "infoCellIdentifier")
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        self.navigationController?.setToolbarHidden(false, animated: animated)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        AppAnalyticsUtil.log(event: .Scherm, segment: .VerstoringenDetailScherm)
    }

    func mainTableViewCell(cellForRowAtIndexPath indexPath: IndexPath) -> MainTableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "mainCellIdentifier", for: indexPath) as! MainTableViewCell

        if let disruption = self.disruption {
            cell.setTitle(disruption.title)
            cell.setLeftSubTitle(disruption.service)
            cell.setLeftSubTitle2(disruption.location)

            // Get last updated datetime
            cell.setLeftDateSubTitle(NSLocalizedString("Aangemaakt: ", comment: "") + disruption.dateTime.relativeTimeNew)
            cell.setRightDateSubTitle(NSLocalizedString("Bijgewerkt: ", comment: "") + disruption.lastUpdatedDateTime.relativeTimeNew)
        }
        
        return cell
    }
    
    func updateTableViewCell(cellForRowAtIndexPath indexPath: IndexPath) -> InfoTableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "infoCellIdentifier", for: indexPath) as! InfoTableViewCell
        
        if let disruption = self.disruption {
            if indexPath.section == 0 {
                cell.setInfo(disruption.description)
            } else {
                let update: Update = disruption.updates[indexPath.row]
                cell.setInfo(update.description)
                cell.setSubInfo(update.dateTime.relativeTimeToday)
            }
        }
        
        return cell
    }
    
    @objc func shareButtonPressed(sender: UIBarButtonItem) {
        guard let disruption = self.disruption else {
            return
        }
        
        let serviceLocationText = [disruption.service, disruption.location].filter { !$0.isEmpty }
            .joined(separator: " - ")
        let updates = disruption.updates.flatMap { [" ", String(htmlEncodedString: $0.description), String(htmlEncodedString: $0.dateTime.readableDateTime)] }
        let textToShare = [String(htmlEncodedString: disruption.title), String(htmlEncodedString: disruption.dateTime.readableDateTime), " ", String(htmlEncodedString: disruption.description), String(htmlEncodedString: serviceLocationText)] + updates

        let activityViewController = UIActivityViewController(activityItems: textToShare, applicationActivities: nil)
        activityViewController.popoverPresentationController?.sourceView = self.view
        activityViewController.excludedActivityTypes = [.postToFacebook, .postToTwitter, .postToWeibo, .postToFlickr, .postToVimeo, .postToTencentWeibo]

        self.present(activityViewController, animated: true, completion: nil)
    }
    
}

extension DisruptionDetailViewController : UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        if let disruption = self.disruption {
            if !disruption.updates.isEmpty {
                return 2
            }
        }
        return 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if section == 0 {
            return 2
        } else if let disruption = self.disruption {
            return disruption.updates.count
        } else {
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.section == 0 && indexPath.row == 0 {
            return self.mainTableViewCell(cellForRowAtIndexPath: indexPath)
        }
        return self.updateTableViewCell(cellForRowAtIndexPath: indexPath)
    }
    
}

extension DisruptionDetailViewController : UITableViewDelegate {

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return section == 0 ? 0 : 36
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        if section == 0 {
            return nil
        }

        let header: SectionHeaderView = tableView.dequeueReusableHeaderFooterView(withIdentifier: "sectionHeaderIdentifier") as! SectionHeaderView
        header.setTitle("Aanvullende info")
        return header
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        if indexPath.section == 0 && indexPath.row == 0 {
            return MainTableViewCell.height()
        }
        
        var text: String = ""
        if let disruption = self.disruption {
            if indexPath.section == 0 {
                text = disruption.description
            } else {
                let update: Update = disruption.updates[indexPath.row]
                text = update.description
            }
        }

        return InfoTableViewCell.height(text)
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        self.tableView.deselectRow(at: indexPath, animated: true)
    }
    
}
