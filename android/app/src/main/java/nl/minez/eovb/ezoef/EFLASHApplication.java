/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import net.danlew.android.joda.JodaTimeAndroid;

import java.lang.reflect.Field;

import ly.count.android.sdk.Countly;
import ly.count.android.sdk.CountlyConfig;
import nl.minez.eovb.BuildConfig;
import nl.minez.eovb.ezoef.util.LogUtil;
import nl.minez.eovb.ezoef.util.TypeFaceUtils;

public class EFLASHApplication extends Application {

    private static void initCountly(Context context) {
        if (!BuildConfig.COUNTLY_URL.isEmpty() && !BuildConfig.COUNTLY_APP_KEY.isEmpty()) {
            CountlyConfig countlyConfig = new CountlyConfig(context, BuildConfig.COUNTLY_APP_KEY, BuildConfig.COUNTLY_URL);
            countlyConfig.setLoggingEnabled(true);
            countlyConfig.setViewTracking(true);
            countlyConfig.setAutoTrackingUseShortName(true);
            countlyConfig.enableCrashReporting();
            Countly.sharedInstance().init(countlyConfig);
        } else {
            Log.e(LogUtil.class.getSimpleName(), "setup Countly failure");
        }
    }

    private void replaceFont(final String fieldName, final Typeface typeface) {
        try {
            final Field staticField = Typeface.class.getDeclaredField(fieldName);
            staticField.setAccessible(true);
            staticField.set(null, typeface);
        } catch (NoSuchFieldException e) {
            Log.i(this.getClass().getSimpleName(), "Error: " + e);
        } catch (IllegalAccessException e) {
            Log.i(this.getClass().getSimpleName(), "Error: " + e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        JodaTimeAndroid.init(this);

        // Override fonts, monospace will be used for AppTheme
        this.replaceFont("MONOSPACE", TypeFaceUtils.themeBoldTypeFace(this));

//        Service.clearJsonCache(this);
//        ApiConnector.getInstance(this).getAuth().reset();

        if (BuildConfig.COUNTLY_ENABLED) {
            initCountly(getApplicationContext());
        }
    }
}
