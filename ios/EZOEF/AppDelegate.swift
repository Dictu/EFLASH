// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit
import KYDrawerController

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?
    var registerViewController: UIViewController = UINavigationController(rootViewController: RegisterViewController())

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        #if DEBUG
//            ApiConnector.sharedConnector.auth.reset()
//            Service.clearJsonCache()
        #endif

        AppAnalyticsUtil.setupAnalytics(); // setup Countly

        // Set app appearance
        UINavigationBar.appearance().tintColor = UIColor.white
        UINavigationBar.appearance().titleTextAttributes = [NSAttributedString.Key.font: UIFont.themeBoldFont(18), NSAttributedString.Key.foregroundColor: UIColor.white]
        UINavigationBar.appearance().barTintColor = UIColor.themeTintColor()
        UINavigationBar.appearance().shadowImage = UIImage()
        UINavigationBar.appearance().isTranslucent = false

        UISegmentedControl.appearance().setTitleTextAttributes([NSAttributedString.Key.font: UIFont.themeRegularFont(14), NSAttributedString.Key.foregroundColor: UIColor.white], for: UIControl.State())

        UIButton.appearance(whenContainedInInstancesOf: [UINavigationBar.self]).tintColor = .white

        let rootViewController = KYDrawerController(drawerDirection: .left, drawerWidth: 280)
        rootViewController.mainViewController = UINavigationController(rootViewController: MainSegmentsViewController())
        rootViewController.drawerViewController = MenuViewController(mainViewController: rootViewController.mainViewController)

        self.window = UIWindow(frame: UIScreen.main.bounds)
        if let window = self.window {
            window.rootViewController = rootViewController
            self.window!.makeKeyAndVisible()
        }

        self.window?.tintColor = UIColor.themeDarkTintColor()

        // Ask to receive push notification messages
        UIApplication.shared.registerUserNotificationSettings(UIUserNotificationSettings(types: [.badge, .sound, .alert], categories: nil))
        return true
    }

    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable: Any]) {
        let state: UIApplication.State = application.applicationState
        if state == .inactive || state == .background {
            // TODO: Show corresponding screen
        } else {
            if let aps = userInfo["aps"] as? NSDictionary {
                var alertMessage: NSString?

                if let alert = aps["alert"] as? NSDictionary {
                    alertMessage = alert["message"] as? NSString
                } else if let alert = aps["alert"] as? NSString {
                    alertMessage = alert
                }

                if let message = alertMessage {
                    let alertController: UIAlertController = UIAlertController(
                        title: NSLocalizedString("Notificatie", comment: ""),
                        message: message as String,
                        preferredStyle: .alert
                    )

                    let ok = UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: { (action) in
                            // TODO: Show corresponding screen
                        })
                    alertController.addAction(ok)

                    self.window?.rootViewController?.present(alertController, animated: true, completion: nil)
                }
            }
        }

        // Send refresh into the world
        NotificationCenter.default.post(name: Notification.Name(rawValue: MenuViewController.kRefreshFilterNotification), object: nil)
    }

    func application(_ application: UIApplication, didRegister notificationSettings: UIUserNotificationSettings) {
        if notificationSettings.types != UIUserNotificationType() {
            application.registerForRemoteNotifications()
        }
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let tokenChars = (deviceToken as NSData).bytes.bindMemory(to: CChar.self, capacity: deviceToken.count)
        var tokenString = ""

        for i in 0..<deviceToken.count {
            tokenString += String(format: "%02.2hhx", arguments: [tokenChars[i]])
        }

        Profile.sharedProfile.pushToken = tokenString
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("Failed to register:", error)
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
        // Send refresh into the world
        NotificationCenter.default.post(name: Notification.Name(rawValue: MenuViewController.kRefreshFilterNotification), object: nil)

        if UIApplication.shared.currentUserNotificationSettings!.types.contains(.alert) {
            // Refresh remote notifications
            application.registerForRemoteNotifications()
        }
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }
}

