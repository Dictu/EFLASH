/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.service;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class EZOEFFirebaseMessagingService extends FirebaseMessagingService {

    public static final String REMOTE_MESSAGE_FILTER_BROADCAST = "RemoteMessageFilterBroadcast";
    public static final String REMOTE_MESSAGE_KEY = "RemoteMessageKey";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        final Intent intent = new Intent(REMOTE_MESSAGE_FILTER_BROADCAST);
        intent.putExtra(REMOTE_MESSAGE_KEY, remoteMessage);
        this.sendBroadcast(intent);
    }
}
