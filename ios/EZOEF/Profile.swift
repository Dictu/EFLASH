// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import Foundation
import Alamofire
import SwiftyJSON

class Profile {

    static let sharedProfile = Profile()

    var profileSaved: Bool {
        set {
            UserDefaults.standard.set(newValue, forKey: "ProfileSavedKey")
            UserDefaults.standard.synchronize()
        }
        get {
            return UserDefaults.standard.bool(forKey: "ProfileSavedKey")
        }
    }
    
    var notificationsOn: Bool {
        get {
            if UserDefaults.standard.object(forKey: "NotificationsOnKey") == nil {
                return UIApplication.shared.currentUserNotificationSettings!.types.contains(.alert)
            }
            return UserDefaults.standard.bool(forKey: "NotificationsOnKey")
        }
    }

    var updatesOn: Bool {
        get {
                if UserDefaults.standard.object(forKey: "UpdatesOnKey") == nil {
                    return true
                }
                return UserDefaults.standard.bool(forKey: "UpdatesOnKey")
        }
    }
    
    func changeNotificationsOn(_ on: Bool, _ updatesOn: Bool, successHandler: @escaping (Error?) -> Void) {
        if self.notificationsOn != on || self.updatesOn != updatesOn {
            self.changePushSettings(self.pushToken, notificationsOn: on, updatesOn: updatesOn, services: self.services, locations: self.locations, successHandler: { error in
                if error == nil {
                    UserDefaults.standard.set(on, forKey: "NotificationsOnKey")
                    UserDefaults.standard.set(updatesOn, forKey: "UpdatesOnKey")
                    UserDefaults.standard.synchronize()
                }
                successHandler(error)
            })
        } else {
            successHandler(nil)
        }
    }
    
    var pushToken: String {
        set {
            if self.pushToken != newValue {
                UserDefaults.standard.setValue(newValue, forKey: "PushTokenKey")
                UserDefaults.standard.set(true, forKey: "PushTokenIsDirtyKey")
                UserDefaults.standard.synchronize()
            }
        }
        get {
            return (UserDefaults.standard.value(forKey: "PushTokenKey") != nil) ? UserDefaults.standard.value(forKey: "PushTokenKey") as! String : ""
        }
    }

    var pushTokenIsDirty: Bool {
        set {
            UserDefaults.standard.set(newValue, forKey: "PushTokenIsDirtyKey")
            UserDefaults.standard.synchronize()
        }
        get {
            if UserDefaults.standard.object(forKey: "PushTokenIsDirtyKey") == nil {
                return true
            }
            return UserDefaults.standard.bool(forKey: "PushTokenIsDirtyKey")
        }
    }
    
    func refreshPushToken() {
        if self.pushTokenIsDirty {
            self.changePushSettings(self.pushToken, notificationsOn: self.notificationsOn, updatesOn: self.updatesOn, services: self.services, locations: self.locations) { _ in }
        }
    }
    
    var services: [Service] {
        get {
            if let values: [NSDictionary] = UserDefaults.standard.object(forKey: "ServicesKey") as? [NSDictionary] {
                return values.map { Service($0) }
            }
            return []
        }
        set {
            let serviceValues: [NSDictionary] = newValue.map { NSDictionary($0) }
            if serviceValues.isEmpty {
                UserDefaults.standard.removeObject(forKey: "ServicesKey")
            } else {
                UserDefaults.standard.set(serviceValues, forKey: "ServicesKey")
            }
            UserDefaults.standard.synchronize()
        }
    }

    var locations: [Location] {
        get {
            if let values: [NSDictionary] = UserDefaults.standard.object(forKey: "LocationsKey") as? [NSDictionary] {
                return values.map { Location($0) }
            }
            return []
        }
        set {
            let locationValues: [NSDictionary] = newValue.map { NSDictionary($0) }
            if locationValues.isEmpty {
                UserDefaults.standard.removeObject(forKey: "LocationsKey")
            } else {
                UserDefaults.standard.set(locationValues, forKey: "LocationsKey")
            }
            UserDefaults.standard.synchronize()
        }
    }
    
    func changeLocations(_ locations: [Location], successHandler: @escaping (Error?) -> Void) {
        if self.locations != locations {
            self.changePushSettings(self.pushToken, notificationsOn: self.notificationsOn, updatesOn: self.updatesOn, services: self.services, locations: locations, successHandler: { error in
                if error == nil {
                    let values: [NSDictionary] = locations.map { NSDictionary($0) }
                    if values.isEmpty {
                        UserDefaults.standard.removeObject(forKey: "LocationsKey")
                    } else {
                        UserDefaults.standard.set(values, forKey: "LocationsKey")
                    }
                    UserDefaults.standard.synchronize()
                }
                successHandler(error)
            })
        } else {
            successHandler(nil)
        }
    }
    
    func changeServices(_ services: [Service], locations: [Location], successHandler: @escaping (Error?) -> Void) {
        if self.services != services {
            self.changePushSettings(self.pushToken, notificationsOn: self.notificationsOn, updatesOn: self.updatesOn, services: services, locations: locations, successHandler: { error in
                if error == nil {
                    self.services = services
                    self.locations = locations
                }
                successHandler(error)
            })
        } else {
            successHandler(nil)
        }
    }

    var locationsText: String {
        get {
            let text: String = self.locations.map { $0.title }.joined(separator: ", ")
            return text.isEmpty ? NSLocalizedString("Geen locaties", comment: "") : text
        }
    }

    var servicesText: String {
        get {
            let text: String = self.services.map { $0.title }.joined(separator: ", ")
            return text.isEmpty ? NSLocalizedString("Geen diensten", comment: "") : text
        }
    }

    func invalidateAll() {
        UserDefaults.standard.removeObject(forKey: "ProfileSavedKey")
        UserDefaults.standard.removeObject(forKey: "LocationsKey")
        UserDefaults.standard.removeObject(forKey: "ServicesKey")
        UserDefaults.standard.removeObject(forKey: "PushTokenIsDirtyKey")
        UserDefaults.standard.synchronize()
    }
    
    fileprivate func changePushSettings(_ pushToken: String, notificationsOn: Bool, updatesOn: Bool, services: [Service], locations: [Location], successHandler: @escaping (Error?) -> Void) {
        
        guard !pushToken.isEmpty else {
            successHandler(nil)
            return
        }

        let completionHandler: (DataResponse<Any>) -> Void = { response in
            if let dictionary = response.result.value {
                let json = JSON(dictionary)
                if let success: Int = json["success"].int {
                    print("Push settings changed succesfully for Services: " + services.map { $0.title }.joined(separator: ", ")
                        + "\t\nLocations: " + locations.map { $0.title }.joined(separator: ", "));
                    
                    if success == 1 {
                        self.pushTokenIsDirty = false
                        
                        successHandler(nil)
                    } else {
                        successHandler(Auth.AuthError.unknown)
                    }

                    return
                }
            }
            
            successHandler(response.result.error)
        }
        
        if notificationsOn {
            let services: [Int] = services.map { $0.id as Int }
            let locations: [Int] = locations.map { $0.id as Int }
            
            ApiConnector.sharedConnector.registerForPushNotifications(pushToken, locations: services + locations, updatesOn: updatesOn, completionHandler: completionHandler)
        } else {
            ApiConnector.sharedConnector.unregisterForPushNotifications(pushToken, completionHandler: completionHandler)
        }
    }

}
