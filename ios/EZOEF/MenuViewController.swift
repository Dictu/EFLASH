// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit
import KYDrawerController

class MenuViewController: UIViewController {
    
    @IBOutlet var tableView: UITableView!
    
    static let kRefreshFilterNotification: String = "RefreshFilterNotification"
    
    var mainViewController: UIViewController!
    var menuHeaderView: MenuHeaderView!
    
    // Menu titles and selectors when logged in
    let menuTitles = [NSLocalizedString("Verstoringen", comment: ""), NSLocalizedString("Voorzieningen wijzigen", comment: ""), NSLocalizedString("Meldingstypen wijzigen", comment: ""), NSLocalizedString("Push notificaties", comment: ""), NSLocalizedString("Info", comment: ""), NSLocalizedString("Privacy", comment: ""), ]

    let menuIcons = ["ic_warning", "ic_local_play", "ic_location_searching", "ic_notifications", "ic_info", "ic_visibility" ]

    let menuSelectors = [#selector(MenuViewController.disruptionsMenuPressed), #selector(MenuViewController.changeServiceMenuPressed), #selector(MenuViewController.changeLocationMenuPressed), #selector(MenuViewController.changeNotificationsMenuPressed), #selector(MenuViewController.infoMenuPressed), #selector(MenuViewController.privacyMenuPressed),]
    
    var locations: [Location] = []
    var services: [Service] = []
    var emptyServiceAsked: Bool = false
    var emptyLocationAsked: Bool = false
    
    convenience init(mainViewController: UIViewController) {
        self.init(nibName: String(describing: MenuViewController.self), bundle: Bundle.main)
        
        self.mainViewController = mainViewController
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.menuHeaderView = MenuHeaderView(frame: CGRect(x: 0, y: 0, width: 280, height: 154))
        
        self.tableView.tableHeaderView = self.menuHeaderView
        self.tableView.tableFooterView = UIImageView()
        self.tableView.register(MenuTableViewCell.self, forCellReuseIdentifier: "cellIdentifier")

        if #available(iOS 11.0, *) {
            self.tableView.contentInsetAdjustmentBehavior = .never
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        // Refresh push token at the server when needed
        Profile.sharedProfile.refreshPushToken()

        self.refreshMenu()
    }
    
    // MARK: Private
    
    fileprivate func refreshMenu() {
        guard ApiConnector.sharedConnector.auth.isRegistered else {
                ApiConnector.sharedConnector.auth.register(ApiConnector.sharedConnector) { (response, error) in
                    guard error == nil else {
                        let alertController: UIAlertController = UIAlertController(
                            title: NSLocalizedString("Oops!", comment: ""),
                            message: NSLocalizedString("Geen verbinding mogelijk.\nControleer uw internetverbinding en probeer het opnieuw.", comment: ""),
                            preferredStyle: .alert
                        )

                        let again = UIAlertAction(title: NSLocalizedString("Opnieuw", comment: ""), style: .default, handler: { (action) -> Void in
                            self.refreshMenu()
                        })
                        alertController.addAction(again)

                        self.present(alertController, animated: true, completion: nil)
                        return
                    }

                    self.refreshMenu()
                }
            return
        }

        if self.services.isEmpty {
            // Preload the cached JSON
            if let JSON = LocationsTree.jsonFromCache() {
                let locationsTree = LocationsTree.fromJSON(JSON)
                
                self.services = locationsTree.services
                self.locations = self.locationsForServices(Profile.sharedProfile.services)
            } else {
                self.loadServices()
            }
            self.refreshServices()
            self.filterChanged(false)
            

        }
        self.tableView.reloadData()

        AppAnalyticsUtil.log(event: .Scherm, segment: .MenuScherm)
    }
    
    fileprivate func loadServices() {
        let progressView = ProgressHelper.showProgress(self.view, message: NSLocalizedString("Ophalen voorzieningen...", comment: ""))
        
         ApiConnector.sharedConnector.locations { response in
            ProgressHelper.hideProgress(progressView)
            
            if let JSON = response.result.value {
                self.update(from: JSON)
                self.locations = self.locationsForServices(Profile.sharedProfile.services)
                self.filterChanged()
            }
            
            if let error: Error = response.result.error {
                let errorAlertController = UIAlertController.errorAlertController("Ophalen voorzieningen mislukt.\nReden: \(error.localizedDescription)")
                self.present(errorAlertController, animated: true) {}
            }
        }
    }
    
    fileprivate func refreshServices() {
        if (Profile.sharedProfile.profileSaved) {
            // refresh data
            let progressView = ProgressHelper.showProgress(self.view, message: NSLocalizedString("Ophalen diensten...", comment: ""))
            
            ApiConnector.sharedConnector.locations { response in
                ProgressHelper.hideProgress(progressView)
                if let JSON = response.result.value {
                    self.update(from: JSON)
                }
                    
                if let error: Error = response.result.error {
                    let errorAlertController = UIAlertController.errorAlertController("Ophalen diensten mislukt.\nReden: \(error.localizedDescription)")
                    self.present(errorAlertController, animated: true) {}
                }
            }
        }
    }
    
    fileprivate func update(from JSON: Any) {
        LocationsTree.fillJsonCache(JSON)
        
        let locationsTree = LocationsTree.fromJSON(JSON)

        self.services = locationsTree.services
        
        // Update all profile settings
        Profile.sharedProfile.update(services: self.services)
        
        self.locations = self.locationsForServices(Profile.sharedProfile.services)
    }
    
    
    fileprivate func filterChanged(_ refresh: Bool = true) {
        // Update menu header
        self.menuHeaderView.setTitle(Profile.sharedProfile.servicesText)
        self.menuHeaderView.setSubTitle(Profile.sharedProfile.locationsText)
        
        guard (self.presentedViewController as? UIAlertController) == nil else {
            // Early out when an alert was already presented
            return
        }
        
        if refresh {
            // Send refresh into the world
            NotificationCenter.default.post(name: Notification.Name(rawValue: MenuViewController.kRefreshFilterNotification), object: nil)
        }
        
        guard !Profile.sharedProfile.profileSaved else {
            // Don't show dialogs when profile was already saved for the first time
            return
        }
        
        if !self.services.isEmpty {
            // Ask for service first
            if Profile.sharedProfile.services.isEmpty && !self.emptyServiceAsked {
                self.changeService(true)
                self.emptyServiceAsked = true
            } else if !self.locations.isEmpty && self.emptyServiceAsked && !self.emptyLocationAsked {
                // After service, ask for the user's location
                self.changeLocation(true)
                self.emptyLocationAsked = true
            }
        }
    }
    
    override func viewWillAppear(_ animated: Bool){
        loadServices()
    }
    
    fileprivate func getServiceForTitle(_ title: String) -> Service? {
        let serviceForTitle: [Service] = self.services.filter { $0.title == title }
        if !serviceForTitle.isEmpty {
            return serviceForTitle[0]
        }
        return nil
    }
    
    fileprivate func locationsByServices(_ selectedServices: [Service]) -> [[String]] {
        var filteredLocations: [[String]] = []
        for selectedService: Service in selectedServices {
            filteredLocations.append(selectedService.locations.map { $0.title })
        }
        return filteredLocations
    }
    
    fileprivate func locationsForServices(_ selectedServices: [Service]) -> [Location] {
        var filteredLocations: [Location] = []
        for selectedService: Service in selectedServices {
            filteredLocations += selectedService.locations
        }
        return filteredLocations
    }
    
    fileprivate func processSelectedProfileServices(_ services: [Service]) {
        // Find newly added services
        let newServices: [Service] = services.filter { (service) in
            (Profile.sharedProfile.services.index { service.id == $0.id }) == .none
        }
        
        // Get all locations for the selected services
        let allLocations = self.locationsForServices(services)
        
        // Remove all profile locations that are not in allLocations
        var newLocations: [Location] = Profile.sharedProfile.locations.filter { (location) in
            (allLocations.index { location.id == $0.id }) != .none
        }
        
        // Auto select all locations for the newly added services
        for service: Service in newServices {
            newLocations += service.locations
        }
        
        let progressView = ProgressHelper.showProgress(self.view, message: NSLocalizedString("Verwerken...", comment: ""))
        
        // Set services and locations
        Profile.sharedProfile.changeServices(services, locations: newLocations) { error in
            ProgressHelper.hideProgress(progressView)
            
            guard error == nil else {
                let errorAlertController = UIAlertController.errorAlertController("Voorzieningen wijzigen mislukt.\nReden: \(error!.localizedDescription)")
                self.present(errorAlertController, animated: true) {}
                
                return
            }
            
            self.locations = allLocations
            
            self.filterChanged()
        }
    }
    
    fileprivate func hasAtLeastOneLocationForEachService(_ locations: [Location], services: [Service]) -> Bool {
        for service in services {
            if (service.locations.filter { (location) in
                (locations.index { location.id == $0.id }) != .none
                }).count == 0 {
                return false
            }
        }
        return true
    }
    
    fileprivate func createPopoverForiPadAlert(_ alertController: UIAlertController, menuItem: NSInteger) {
        if alertController.preferredStyle == .actionSheet && UIDevice.current.userInterfaceIdiom == .pad {
            if let cell: UITableViewCell = self.tableView.cellForRow(at: IndexPath(row: menuItem, section: 0)) {
                alertController.popoverPresentationController?.sourceView = cell.contentView
                alertController.popoverPresentationController?.sourceRect = cell.contentView.bounds
            }
        }
    }
    
    // MARK: Menu selectors
    
    @objc func disruptionsMenuPressed() {
        if let drawerController: KYDrawerController = self.parent as? KYDrawerController {
            drawerController.mainViewController = self.mainViewController
            drawerController.setDrawerState(.closed, animated: true)
        }
    }
    
    @objc func changeServiceMenuPressed() {
        self.changeService(false)
    }
    
    func changeService(_ withAlert: Bool) {
        guard !self.services.isEmpty else {
            // Services should be filled, otherwise show an alert
            let alertController: UIAlertController = UIAlertController(
                title: NSLocalizedString("Voorzieningen wijzigen", comment: ""),
                message: NSLocalizedString("Er zijn nog geen voorzieningen opgehaald.\nControleer uw internetverbinding en probeer het opnieuw.", comment: ""),
                preferredStyle: .alert
            )
            
            let cancel = UIAlertAction(title: NSLocalizedString("Annuleren", comment: ""), style: .cancel, handler: nil)
            alertController.addAction(cancel)
            
            let again = UIAlertAction(title: NSLocalizedString("Opnieuw", comment: ""), style: .default, handler: { (action) -> Void in
                self.loadServices()
            })
            alertController.addAction(again)
            
            self.present(alertController, animated: true, completion: nil)
            return
        }
        
        let alertController: UIAlertController = UIAlertController(
            title: NSLocalizedString("Kies één of meerdere voorzieningen", comment: ""),
            message: nil,
            preferredStyle: withAlert ? .alert : .actionSheet)
        alertController.checkboxItems = [self.services.map { $0.title }]
        alertController.selectedCheckboxItems = Profile.sharedProfile.services.map { $0.title }
        
        let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: { (action) -> Void in
            self.processSelectedProfileServices(alertController.selectedCheckboxItems.map { self.getServiceForTitle($0)! })
        })
        alertController.addAction(ok)
        
        let cancel = UIAlertAction(title: NSLocalizedString("Annuleren", comment: ""), style: .cancel, handler: nil)
        alertController.addAction(cancel)
        
        self.createPopoverForiPadAlert(alertController, menuItem: self.menuIcons.index(of: "ic_local_play")!)
        self.present(alertController, animated: true) {}
    }
    
    @objc func changeLocationMenuPressed() {
        self.changeLocation(false)
    }
    
    func changeLocation(_ withAlert: Bool, selectedLocations: [Location]) {
        guard !self.locations.isEmpty else {
            // Locations should be filled, otherwise show an alert
            let alertController: UIAlertController = UIAlertController.errorAlertController(
                NSLocalizedString("Berichtttypen wijzigen", comment: ""),
                message: NSLocalizedString("U heeft nog geen voorziening gekozen.\nKies eerst één of meerdere voorzieningen.", comment: "")
            )
            self.present(alertController, animated: true, completion: nil)
            return
        }
        
            let alertController: UIAlertController = UIAlertController(title: NSLocalizedString("Kies één of meerdere meldingstypen per voorziening.", comment: ""), message: nil, preferredStyle: withAlert ? .alert : .actionSheet)
        
        let locations = self.locationsForServices(Profile.sharedProfile.services)
        
        alertController.checkboxSectionTitles = Profile.sharedProfile.services.map { $0.title }
        alertController.checkboxItems = self.locationsByServices(Profile.sharedProfile.services)
        alertController.selectedCheckboxIndexes = selectedLocations.map { (location) in
            if let index = (locations.index { location.id == $0.id }) {
                return index
            } else {
                return -1
            }
        }
        
        let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: { (action) -> Void in
            let newSelectedLocations = alertController.selectedCheckboxIndexes.map { locations[$0] }
            guard self.hasAtLeastOneLocationForEachService(newSelectedLocations, services: Profile.sharedProfile.services) else {
                let message = NSLocalizedString("Kies één of meerdere meldingstypen per voorziening.", comment: "")
                let alertController: UIAlertController = UIAlertController(
                    title: NSLocalizedString("Meldingstypen wijzigen", comment: ""),
                    message: message,
                    preferredStyle: .alert
                )
                
                let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .destructive, handler: { action in
                    self.changeLocation(withAlert, selectedLocations: newSelectedLocations)
                })
                alertController.addAction(ok)
                self.present(alertController, animated: true, completion: {})
                
                return
            }
            
            let progressView = ProgressHelper.showProgress(self.view, message: NSLocalizedString("Verwerken...", comment: ""))
            
            Profile.sharedProfile.changeLocations(newSelectedLocations, successHandler: { error in
                ProgressHelper.hideProgress(progressView)
                
                guard error == nil else {
                    let errorAlertController = UIAlertController.errorAlertController("Berichtttypen wijzigen mislukt.\nReden: \(error!.localizedDescription)")
                    self.present(errorAlertController, animated: true) {}
                    
                    return
                }
                
                Profile.sharedProfile.profileSaved = true
                self.filterChanged()
            })
        })
        alertController.addAction(ok)
        
        let cancel = UIAlertAction(title: NSLocalizedString("Annuleren", comment: ""), style: .cancel, handler: nil)
        alertController.addAction(cancel)
        
        self.createPopoverForiPadAlert(alertController, menuItem: self.menuIcons.index(of: "ic_location_searching")!)
        self.present(alertController, animated: true) {}
    }
    
    func changeLocation(_ withAlert: Bool) {
        self.changeLocation(withAlert, selectedLocations: Profile.sharedProfile.locations)
    }
    
    @objc func changeNotificationsMenuPressed() {
        self.changeNotifications(false)
    }
    
    func changeNotifications(_ withAlert: Bool) {
        let alertController: UIAlertController = UIAlertController(title: NSLocalizedString("Push notificaties", comment: ""), message: nil, preferredStyle: withAlert ? .alert : .actionSheet)
        
            alertController.notificationsOnOffTitle = NSLocalizedString("Notificaties ontvangen?", comment: "")
            alertController.notificationsOn = Profile.sharedProfile.notificationsOn
            alertController.updatesOnOffTitle = NSLocalizedString("Updates ontvangen?", comment: "")
            alertController.updatesOn = Profile.sharedProfile.updatesOn
        
        let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: { (action) -> Void in
            let progressView = ProgressHelper.showProgress(self.view, message: NSLocalizedString("Verwerken...", comment: ""))

                let on = alertController.notificationsOn
                let updatesOn = alertController.updatesOn
            
            Profile.sharedProfile.changeNotificationsOn(on, updatesOn, successHandler: { error in
                ProgressHelper.hideProgress(progressView)
                
                guard error == nil else {
                    let errorAlertController = UIAlertController.errorAlertController("Notificaties wijzigen mislukt.\nReden: \(error!.localizedDescription)")
                    self.present(errorAlertController, animated: true) {}
                    
                    return
                }

                if on && !UIApplication.shared.currentUserNotificationSettings!.types.contains(.alert) {
                    let appTitle = Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String ?? ""
                    
                    let warningAlertController = UIAlertController(title: NSLocalizedString("Waarschuwing", comment: ""), message: NSLocalizedString("Notificaties worden pas actief nadat u berichtgeving toestaat in uw iOS Instellingen > \(appTitle) > Berichtgeving.", comment: ""), preferredStyle: .alert)
                    
                    let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: nil)
                    warningAlertController.addAction(ok)

                    self.present(warningAlertController, animated: true) {}
                }
                
                self.tableView.reloadData()
            })
        })
        alertController.addAction(ok)
        
        let cancel = UIAlertAction(title: NSLocalizedString("Annuleren", comment: ""), style: .cancel, handler: nil)
        alertController.addAction(cancel)
        
        self.createPopoverForiPadAlert(alertController, menuItem: self.menuIcons.index(of: "ic_notifications")!)
        self.present(alertController, animated: true) {}
    }
    
    @objc func infoMenuPressed() {
        #if DEBUG
            let version: String = (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String)! + "D"
        #else
            let version: String = (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String)!
        #endif
        
        let appName = Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String ?? ""
        let alertController: UIAlertController = UIAlertController(title: NSLocalizedString("Info", comment: ""), message: nil, preferredStyle: UIDevice.current.userInterfaceIdiom == .phone ? .alert : .actionSheet)

            alertController.info = NSLocalizedString("Opgeloste verstoringen zijn maximaal een week zichtbaar.<br/><br/>\n\n"
                + "Versie \(appName) \(version)<br/><br/>\n\n"
                + "Voor meldingen en vragen:<br/>\n"
                + "<a href='mailto:SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'>SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS</a><br/><br/>\n\n"
                + "Bellen op werkdagen tussen 9u en 17u (GMT+1):<br/>\n"
                + "Tel: <a href='tel:SSSSSSSSSS'>+31 SSSSSSSSSS</a><br/>\n", comment: "")

        let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: nil)
        alertController.addAction(ok)
        
        self.createPopoverForiPadAlert(alertController, menuItem: self.menuIcons.index(of: "ic_info")!)
        self.present(alertController, animated: true) {}
    }
    
    @objc func privacyMenuPressed() {
        
        let alertController: UIAlertController = UIAlertController(title: NSLocalizedString("Privacy", comment: ""), message: nil, preferredStyle: UIAlertController.Style.alert)
        alertController.view.tintColor = .red
        
        alertController.info = NSLocalizedString("<p>Dictu, onderdeel van het Ministerie van Economische Zaken en Klimaat, hecht een groot belang aan de bescherming van uw privacy en de veiligheid van persoonsgegevens. "
            + "Wij verzamelen alleen persoonsgegevens die noodzakelijk zijn voor het tonen van de apps van Dictu, onderdeel van het Ministerie van Economische Zaken en Klimaat, en de daarop aangeboden diensten. "
            + "Deze persoonsgegevens worden verwerkt in overeenstemming met de Algemene Verordening Persoonsgegevens (AVG) en andere toepasselijke privacywetgeving.</p><a href='https://www.rijksoverheid.nl/ministeries/ministerie-van-economische-zaken-en-klimaat/privacy'>Meer informatie</a>", comment: "")

        let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: nil)
        alertController.addAction(ok)

        self.present(alertController, animated: true) {}
        AppAnalyticsUtil.log(event: .Scherm, segment: .PrivacyScherm)
    }
    
}

extension MenuViewController : UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.menuTitles.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cellIdentifier", for: indexPath) as! MenuTableViewCell
        
        var iconName = self.menuIcons[indexPath.row]
        if "ic_notifications" == iconName {
            iconName += (Profile.sharedProfile.notificationsOn ? "" : "_off")
        }
        
        cell.setIcon(UIImage(named: iconName))
        cell.setTitle(self.menuTitles[indexPath.row])
        
        return cell
    }
}

extension MenuViewController : UITableViewDelegate {
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        self.tableView.deselectRow(at: indexPath, animated: true)
        
        self.perform(self.menuSelectors[indexPath.row], with: false)
    }
    
}
