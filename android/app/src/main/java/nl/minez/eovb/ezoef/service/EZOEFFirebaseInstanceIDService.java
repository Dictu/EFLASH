/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.service;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import nl.minez.eovb.ezoef.profile.Profile;

public class EZOEFFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        final String pushToken = FirebaseInstanceId.getInstance().getToken();
        Log.i(this.getClass().getSimpleName(), "Push token: " + pushToken);

        Profile.getInstance(this).setPushToken(pushToken);
    }

}