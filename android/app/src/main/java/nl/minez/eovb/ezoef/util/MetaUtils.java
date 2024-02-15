/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class MetaUtils {

    public static String getAppVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(MetaUtils.class.getSimpleName(), "Error fetching version info: " + e);
            return "";
        }
    }

}
