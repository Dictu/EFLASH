// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import Foundation
import Alamofire
import KYDrawerController

class ApiConnector {
    
    #if DEBUG
    fileprivate static let baseUrl: String = "https://SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS/"
    // Gebruik onderstaande url voor ACC tests
    // ACC omgeving stuurt notificaties naar debug apns certificaat
    // fileprivate static let baseUrl: String = "https://SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS/"
    
    #elseif TESTFLIGHT
    // in testflight mode werken notificaties niet op ACC omgeving (zie boven)
    fileprivate static let baseUrl: String = "https://SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS/"
    #else
    fileprivate static let baseUrl: String = "https://SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS/"
    #endif
         
    let dryRun: Bool = false
    let v1: String = ApiConnector.baseUrl + "api/v1/"
    let auth: Auth = Auth()
    let sessionManager = Alamofire.SessionManager(configuration: URLSessionConfiguration.ephemeral)
    
    var refreshingAccessTokenEndPoints: [String] = []
    
    static let sharedConnector: ApiConnector = {
        let connector = ApiConnector()
        ApiConnector.listenForReachability()
        return connector
    }()
    
    // MARK: Simple OAuth
    
    func register(_ device: String, email: String, completionHandler: @escaping (DataResponse<Any>) -> Void) {
        
        guard !self.dryRun else {
            testJson("registerUserTest", completionHandler: completionHandler)
            return
        }
        
        let params: [String : Any] = [
            "_format":"json" as Any,
            "name":device as Any,
            "mail":email as Any
        ]
        
        self.sessionManager.request(self.v1 + "register?_format=json", method: .post, parameters: params, encoding: JSONEncoding.default)
            .validate(statusCode: 200..<500)
            .validate(contentType: ["application/json"])
            .responseJSON(completionHandler: completionHandler)
    }
    
    func authRefreshToken(_ refreshToken: String, completionHandler: @escaping (DataResponse<Any>) -> Void) {
        
        guard !self.dryRun else {
            testJson("refreshTokenTest", completionHandler: completionHandler)
            return
        }
        
        let headers = ["Authorization":"Bearer \(refreshToken)"]
        let params: [String: Any] = [:]
        
        self.sessionManager.request(ApiConnector.baseUrl + "simple-oauth/refresh", method: .get, parameters: params, headers: headers)
            .validate(statusCode: 200..<500)
            .validate(contentType: ["application/json"])
            .responseJSON(completionHandler: completionHandler)
    }
    
    // MARK: Push
    
    func registerForPushNotifications(_ pushToken: String, locations: [Int], updatesOn: Bool, completionHandler: @escaping (DataResponse<Any>) -> Void) {
        guard !self.dryRun else {
            
            testJson("registerForPushTest", completionHandler: completionHandler)
            return
        }
        
        let params: [String : Any] = [
            "token":pushToken as Any,
            "type":"ios" as Any,
            "messagetypes":locations as Any,
            "notifications":[
                "updates":updatesOn ? 1 : 0 as Any
            ]
        ]
        self.doDelayedRequest(.post, endpoint: "push_notifications?_format=json", parameters: params, completionHandler: completionHandler)
    }
    
    func unregisterForPushNotifications(_ pushToken: String, completionHandler: @escaping (DataResponse<Any>) -> Void) {
        
        guard !self.dryRun else {
            testJson("registerForPushTest", completionHandler: completionHandler)
            return
        }
        
        self.doDelayedRequest(.delete, endpoint: "push_notifications/" + pushToken, parameters: ["_format":"json"], completionHandler: completionHandler)
    }
    
    // MARK: Disruptions
    
    func openDisruptions(_ services: [Int], locations: [Int], completionHandler: @escaping (DataResponse<Any>) -> Void) {
        
        guard !self.dryRun else {
            testJson("openDisruptionsTestPublic", completionHandler: completionHandler)
            return
        }
        
        let locations: [Int] = services + locations
        let params: [String : Any] = [
            "type":[["target_id":"incident"]],
            "state":["open"],
            "messagetypes":locations
        ]
        
        self.doDelayedRequest(.post, endpoint: "incidents?_format=json", parameters: params, completionHandler: completionHandler)
        
    }
    
    func closedDisruptions(_ services: [Int], locations: [Int], completionHandler: @escaping
        (DataResponse<Any>) -> Void) {
        
        guard !self.dryRun else {
            testJson("closedDisruptionsTest", completionHandler: completionHandler)
            return
        }
        
        let locations: [Int] = services + locations
        let params: [String : Any] = [
            "type":[["target_id":"incident"]],
            "state":["closed"],
            "messagetypes":locations
        ]
        
        self.doDelayedRequest(.post, endpoint: "incidents?_format=json", parameters: params, completionHandler: completionHandler)
    }
    
    func announcements(_ services: [Int], locations: [Int], completionHandler: @escaping (DataResponse<Any>) -> Void) {
        
        guard !self.dryRun else {
            testJson("openDisruptionsTest", completionHandler: completionHandler)
            return
        }
        
        let locations: [Int] = services + locations
        let params: [String : Any] = [
            "type":[["target_id":"announcement"]],
            "state":["open"],
            "messagetypes":locations
        ]
        
        self.doDelayedRequest(.post, endpoint: "announcements?_format=json", parameters: params, completionHandler: completionHandler)
    }
    
    
    // MARK: Locations
    
    func locations(_ completionHandler: @escaping (DataResponse<Any>) -> Void) {
        guard !self.dryRun else {
            
            testJson("locationsTestPublic", completionHandler: completionHandler)
            return
        }
        
        self.doDelayedRequest(.get, endpoint: "messagetypes?_format=json", parameters: [:], completionHandler: completionHandler)
    }
    
    // MARK: General JSON request
    
    func endpointDescription(_ endpoint: String, parameters: [String : Any]?) -> String {
        if let params = parameters {
            
            let json = try! JSONSerialization.data(withJSONObject: params, options: JSONSerialization.WritingOptions(rawValue: UInt(0)))
            return "\(endpoint):\(NSString(data: json, encoding: String.Encoding.ascii.rawValue)!))"
        } else {
            return "\(endpoint)"
        }
    }
    
    /// NOTE: Work around for NSPOSIXErrorDomain Code=53, issued as an iOS 12 (beta) bug
    /// See: https://github.com/AFNetworking/AFNetworking/issues/4279
    func doDelayedRequest(_ method: HTTPMethod, endpoint: String, parameters: [String: Any]?, completionHandler: @escaping (DataResponse<Any>) -> Void) {
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.doRequest(method, endpoint: endpoint, parameters: parameters, completionHandler: completionHandler)
        }
    }
    
    func doRequest(_ method: HTTPMethod, endpoint: String, parameters: [String: Any]?, completionHandler: @escaping (DataResponse<Any>) -> Void) {
        
        // Try to get an access token
        self.auth.accessToken(self) { (accessToken, error) in
            let endpointDescription = self.endpointDescription(endpoint, parameters: parameters)
            
            guard error == nil else {
                self.refreshingAccessTokenEndPoints = self.refreshingAccessTokenEndPoints.filter() {$0 != endpointDescription}
                self.handleAuthError(error!, errorHandler: completionHandler)
                return
            }
            
            let headers = ["Authorization":"Bearer \(accessToken!)"]
            let encoding: ParameterEncoding = (method == .get) ? URLEncoding.default : JSONEncoding.default
            
            self.sessionManager.request(self.v1 + endpoint, method: method, parameters: parameters, encoding: encoding, headers: headers)
                .validate(contentType: ["application/json"])
                .responseJSON(completionHandler: { response in
                    if let error = self.auth.authErrorFromResponse(response) {
                        if error._code == Auth.AuthError.blocked._code && !self.refreshingAccessTokenEndPoints.contains(endpointDescription) {
                            self.refreshingAccessTokenEndPoints.append(endpointDescription)
                            
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                                self.doDelayedRequest(method, endpoint: endpoint, parameters: parameters, completionHandler: completionHandler)
                            }
                        } else {
                            self.refreshingAccessTokenEndPoints = self.refreshingAccessTokenEndPoints.filter() {$0 != endpointDescription}
                            self.handleAuthError(error, errorHandler: completionHandler)
                        }
                    } else {
                        self.refreshingAccessTokenEndPoints = self.refreshingAccessTokenEndPoints.filter() {$0 != endpointDescription}
                        completionHandler(DataResponse<Any>(
                            request: response.request,
                            response: response.response,
                            data: response.data,
                            result: response.result,
                            timeline: response.timeline
                            )
                        )
                    }
                })
        }
    }
    
    // MARK: Test
    
    func testJson(_ testFile: String, completionHandler: @escaping (DataResponse<Any>) -> Void) {
        
        if let path = Bundle.main.path(forResource: testFile, ofType: "json") {
            do {
                let data = try Data(contentsOf: URL(fileURLWithPath: path), options: NSData.ReadingOptions.dataReadingMapped)
                
                DispatchQueue.main.async {
                    completionHandler(
                        DataResponse<Any>(
                            request: nil,
                            response: HTTPURLResponse(),
                            data: data,
                            result: Request.serializeResponseJSON(options: [], response: nil, data: data, error: nil),
                            timeline: Timeline()
                        )
                    )
                }
            } catch {
            }
        }
    }
    
    // MARK: Private
    
    fileprivate static func listenForReachability() {
        if let manager = NetworkReachabilityManager() {
            manager.listener = { status in
                if status == .notReachable {
                    if let app = UIApplication.shared.delegate as? AppDelegate, let window = app.window {
                        let errorAlertController = UIAlertController.errorAlertController(NSLocalizedString("Geen internetverbinding.\nControleer je internetverbinding.", comment: ""))
                        window.rootViewController?.present(errorAlertController, animated: true) {}
                    }
                }
            }
            manager.startListening()
        }
    }
    
    fileprivate func handleAuthError(_ error: Error, errorHandler: ((DataResponse<Any>) -> Void)?) {
        if Auth.AuthError.unknown._code ... Auth.AuthError.notRegistered._code ~= error._code {
            if let app = UIApplication.shared.delegate as? AppDelegate, let window = app.window {
                let alertController = UIAlertController.authErrorAlertController(error) { action in
                    // Show registration screen after pressing "Register"
                    if !app.registerViewController.isBeingPresented {
                        window.rootViewController?.present(app.registerViewController, animated: true) {}
                    }
                }
                window.rootViewController!.present(alertController, animated: true) {}
            }
        }
        
        if let handler = errorHandler {
            let r = DataResponse<Any>(
                request: nil,
                response: nil,
                data: nil,
                result: Request.serializeResponseJSON(options: [], response: nil, data: nil, error: error),
                timeline: Timeline()
            )
            
            handler(r)
        }
    }
    
}

