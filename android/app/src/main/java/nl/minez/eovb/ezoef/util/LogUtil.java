/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.util;

import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

import ly.count.android.sdk.Countly;
import nl.minez.eovb.BuildConfig;

public class LogUtil {

    // Screen events
    public static final String SCHERM = "Scherm";
    public static final String VERSTORINGEN_SCHERM = "VerstoringenScherm";
    public static final String VERSTORINGEN_DETAIL_SCHERM = "VerstoringenDetailScherm";
    public static final String REGISTREER_SCHERM = "RegistreerScherm";
    public static final String INFO_SCHERM = "InfoScherm";
    public static final String PRIVACY_SCHERM = "PrivacyScherm";
    public static final String CHANGE_NOTIFICATIES = "WijzigNotificatiesScherm";
    public static final String FIREBASE = "Firebase";

    private static LogUtil instance = null;

    protected LogUtil() {
    }

    public static LogUtil getInstance() {
        if (instance == null) {
            instance = new LogUtil();
        }
        return instance;
    }

    public void logWithName(String name, Exception log) {
        String stackTrace = Log.getStackTraceString(log);
        logWithName(name, stackTrace);
    }

    public void logWithName(String name, String log) {

        if (TextUtils.isEmpty(log)) {
            log = "empty log string";
        }

        HashMap<String, Object> segmentation = new HashMap<>();
        segmentation.put(name, log);

        try {
            Countly.sharedInstance().events().recordEvent(name, segmentation, 1);
        } catch (Exception e) {
            // prevent crash if not initialized
        }

        if (BuildConfig.DEBUG) {
            Log.e(name, log);
        }
    }
}
