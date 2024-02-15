/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.view.activity;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import ly.count.android.sdk.Countly;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Countly.sharedInstance().onStart(this);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Countly not set ...");
        }
    }

    @Override
    protected void onStop() {
        try {
            Countly.sharedInstance().onStop();
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Countly not set ...");
        }
        super.onStop();
    }
}
