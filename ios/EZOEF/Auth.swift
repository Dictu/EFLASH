// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import Foundation
import Locksmith
import SwiftyJSON
import Alamofire

class Auth {

    enum AuthError: LocalizedError {
        case unknown
        case failed
        case forbidden
        case blocked
        case notRegistered

        public var errorDescription: String? {
            switch self {
            case .unknown:
                return NSLocalizedString("Er is een onbekende fout opgetreden.", comment: "")
            case .failed:
                return NSLocalizedString("Fout tijdens verwerken authenticatie.", comment: "")
            case .forbidden:
                    return NSLocalizedString("Je hebt geen toegang.\nNeem contact op met onze service desk.", comment: "")
            case .blocked:
                    return NSLocalizedString("Je hebt geen toegang.\nNeem contact op met onze service desk.", comment: "")
            case .notRegistered:
                    return NSLocalizedString("Je hebt geen toegang.\nNeem contact op met onze service desk.", comment: "")
            }
        }
    }

    fileprivate static let kIsRegisteredKey: String = "IsRegisteredKey"
    fileprivate static let kDeviceKey: String = "DeviceKey"
    fileprivate static let kRefreshTokenKey: String = "RefreshTokenKey"
    
    var device: String? {
        get {
            let data = Locksmith.loadDataForUserAccount(userAccount: self.bundleIdentifier())
            return data?[Auth.kDeviceKey] as? String
        }
    }
    
    var refreshToken: String? {
        get {
            let data = Locksmith.loadDataForUserAccount(userAccount: self.bundleIdentifier())
            return data?[Auth.kRefreshTokenKey] as? String
        }
    }

    var isRegistered: Bool {
        return UserDefaults.standard.bool(forKey: Auth.kIsRegisteredKey)
    }

    var accessToken: String?
    var accessTokenExpiresAt: Date?

    func register(_ apiConnector: ApiConnector, authHandler: @escaping (String?, Error?) -> Void) {
        // Invalidate (a possibly) previous registration
        self.invalidateAll()
        
        // Generate new device UUID (unless we already have one)
        let device: String = self.device ?? UUID().uuidString

        // Register the device and email
        self.register(apiConnector, device: device, email: "\(device)@SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authHandler: authHandler)
    }
    
    func register(_ apiConnector: ApiConnector, email: String, authHandler: @escaping (String?, Error?) -> Void) {
        // Invalidate (a possibly) previous registration
        self.invalidateAll()

        // Generate new device UUID (unless we already have one)
        let device: String = self.device ?? UUID().uuidString

        // Register the device and email
        self.register(apiConnector, device: device, email: email, authHandler: authHandler)
    }
    
    fileprivate func register(_ apiConnector: ApiConnector, device: String, email: String, authHandler: @escaping (String?, Error?) -> Void) {
        apiConnector.register(device, email: email) { response in
            let error = self.authErrorFromResponse(response)
            guard error == nil else {
                authHandler(nil, error)
                return
            }
            
            guard let dictionary = response.result.value else {
                authHandler(nil, AuthError.unknown)
                return
            }
            
            let json = JSON(dictionary)
            
            guard json["success"].boolValue else {
                authHandler(nil, AuthError.unknown)
                return
            }
            
            guard let accessToken = json["data"].dictionary?["access_token"]?.string else {
                authHandler(nil, AuthError.forbidden)
                return
            }
            
            guard let refreshToken = json["data"].dictionary?["refresh_token"]?.string else {
                authHandler(nil, AuthError.forbidden)
                return
            }
            
            do {
                // Store device and refresh token in more secure key chain
                try Locksmith.updateData(data: [Auth.kDeviceKey: device, Auth.kRefreshTokenKey: refreshToken], forUserAccount: self.bundleIdentifier())

                UserDefaults.standard.set(true, forKey: Auth.kIsRegisteredKey)
                UserDefaults.standard.synchronize()
                
                if let expiresIn = json["data"].dictionary?["expires_in"]?.double {
                    self.accessTokenExpiresAt = Date().addingTimeInterval(expiresIn)
                }
                
                // Keep access token for this session
                self.accessToken = accessToken
                
                authHandler(accessToken, nil)
            } catch {
                authHandler(nil, AuthError.failed)
            }
        }
    }
    
    func accessToken(_ apiConnector: ApiConnector, authHandler: @escaping (String?, Error?) -> Void) {
        print("Expires at: \(self.accessTokenExpiresAt?.secondsFrom(Date()) ?? 0)")
        
        self.invalidateAccessTokenWhenExpired();
        
        guard self.accessToken == nil else {
            // If access token was already set and not expired, use this one
            authHandler(self.accessToken, nil)
            return
        }

        guard self.refreshToken != nil else {
            authHandler(nil, AuthError.notRegistered)
            return
        }
        
        let oldRefreshToken = self.refreshToken

        apiConnector.authRefreshToken(self.refreshToken!) { response in
            let error = self.authErrorFromResponse(response)
            guard error == nil else {
                if error!._code == Auth.AuthError.blocked._code && self.refreshToken != nil && self.refreshToken != oldRefreshToken {
                    // Ignore this error when refresh tokens are renewed
                    self.accessToken(apiConnector, authHandler: authHandler)
                } else {
                    authHandler(nil, error)
                }
                
                return
            }
            
            guard let dictionary = response.result.value else {
                authHandler(nil, AuthError.unknown)
                return
            }
            
            let json = JSON(dictionary)
            
            guard let accessToken = json["access_token"].string else {
                authHandler(nil, AuthError.forbidden)
                return
            }

            if let refreshToken = json["refresh_token"].string, let device = self.device {
                do {
                    // Store device and refresh token in more secure key chain
                    try Locksmith.updateData(data: [Auth.kDeviceKey: device, Auth.kRefreshTokenKey: refreshToken], forUserAccount: self.bundleIdentifier())
                    
                    if let expiresIn = json["expires_in"].double {
                        self.accessTokenExpiresAt = Date().addingTimeInterval(expiresIn)
                    }
                    
                    // Keep access token for this session
                    self.accessToken = accessToken
                    
                    authHandler(accessToken, nil)
                } catch {
                    authHandler(nil, AuthError.failed)
                }
            }
        }
    }
    
    func authErrorFromResponse(_ response: DataResponse<Any>) -> Error? {
        var error: Error? = response.result.error
        
        if let responseData = response.response {
            switch responseData.statusCode {
            case 200:
                if let dictionary = response.result.value {
                    let json = JSON(dictionary)
                    
                    if let success: Int = json["success"].int, success == 0 {
                        // Email from wrong domain
                        error = AuthError.forbidden
                    }
                }

                break
            case 400:
                error = AuthError.forbidden
                break
            case 403:
                error = AuthError.blocked
                break
            default: break
            }
        }
        
        return error
    }

    func invalidateAccessTokenWhenExpired() {
        if self.accessTokenExpiresAt == nil || ((self.accessTokenExpiresAt as NSDate?)?.earlierDate(Date()) == self.accessTokenExpiresAt!) {
            self.accessToken = nil
            self.accessTokenExpiresAt = nil
        }
    }

    fileprivate func invalidateAll() {
        do {
            self.accessToken = nil
            self.accessTokenExpiresAt = nil
            
            UserDefaults.standard.removeObject(forKey: Auth.kIsRegisteredKey)
            UserDefaults.standard.synchronize()

            // Remove private tokens from key chain
            if let device = self.device {
                // Remove auth token, but keep device
                try Locksmith.updateData(data: [Auth.kDeviceKey: device], forUserAccount: self.bundleIdentifier())
            } else {
                // Delete all available data
                try Locksmith.deleteDataForUserAccount(userAccount: self.bundleIdentifier())
            }
        } catch {
            print("Error while removing private data")
        }
    }

    func reset() {
        self.accessToken = nil
        self.accessTokenExpiresAt = nil
        
        UserDefaults.standard.removeObject(forKey: Auth.kIsRegisteredKey)
        UserDefaults.standard.synchronize()

        do {
            try Locksmith.deleteDataForUserAccount(userAccount: self.bundleIdentifier())
        } catch {
            print("Error while resetting private data")
        }
    }
    
    fileprivate func bundleIdentifier() -> String {
        return Bundle.main.bundleIdentifier!
    }

}
