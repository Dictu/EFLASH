// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


import UIKit
import Countly

class AppAnalyticsUtil: NSObject {
    
    //LET OP: OM DE IDFA UIT HET PROJECT TE HALEN MOET JE ALLE REGELS TUSSEN #ifndef COUNTLY_EXCLUDE_IDFA EN #endif STAAN VERWIJDEREN. DE MELDING KOMT OMHOOG BIJ DE EXPORT COMPLIANCE SETTINGS IN APP STORE CONNECT. ALS JE ZEGT DAT JE GEEN IDFA GEBRUIKT MAAR DE IDFA ZIT WEL IN DE BUNDLE ZAL APPSTORECONNECT EEN FOUT GEVEN.
    //TODO: /\

    private static var apiKey: String {
        #if DEBUG
        return "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        #else
        return "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        #endif
    }
    
    private static var config: CountlyConfig {
        let config = CountlyConfig()
        config.appKey = apiKey
        config.host = "https://SSSSSSSSSSSSSSS/"
        config.deviceID = CLYDefaultDeviceID
        config.features = [CLYFeature.crashReporting, CLYFeature.autoViewTracking]
        config.eventSendThreshold = 3
        return config
    }
    
    static func setupAnalytics() {
        Countly.sharedInstance().start(with: config)
    }
    
    enum CountlyEvent: String {
        case Scherm
        case Event
    }

    enum CountlyView: String {
        case VerstoringenDetailScherm
        case VerstoringenScherm
        case CheckboxesScherm
        case InfoScherm
        case PrivacyScherm
        case OnOffScherm
        case MenuScherm
        case RegistreerScherm
        case RegistratieSuccesvol
    }
    
    static func log(event: CountlyEvent, segment: CountlyView) {
        log(event: event, segment: segment.rawValue)
    }

    static func log(event: CountlyEvent, segment: String) {
        Countly.sharedInstance().recordEvent(event.rawValue, segmentation: [event.rawValue : segment])
    }
    
    static func log(error: Error, userInfo: [String: CountlyLoggable] = [:]) { log(error: error as NSError, userInfo: userInfo) }

    static func log(error: NSError, userInfo: [String: CountlyLoggable] = [:]) {
        var info = userInfo
        info["error"] = error.description
        let exception = NSException(name: NSExceptionName(error.domain), reason: error.domain, userInfo: info)
        Countly.sharedInstance().recordHandledException(exception, withStackTrace: Thread.callStackSymbols)
    }
    
    static func logLogin(userName: String) { Countly.sharedInstance().userLogged(in: userName) }

    static func logLogout() { Countly.sharedInstance().userLoggedOut() }
}

protocol CountlyLoggable { }
extension String: CountlyLoggable { }
extension Int: CountlyLoggable { }
extension Double: CountlyLoggable { }
extension Bool: CountlyLoggable { }
extension Optional: CountlyLoggable where Wrapped: CountlyLoggable { }
