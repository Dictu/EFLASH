/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NetworkChangeReceiver extends BroadcastReceiver {

    public static final String REACHABILITY_CHANGED_FILTER_BROADCAST = "ReachabilityChangedFilterBroadcast";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Reachability changed
        context.sendBroadcast(new Intent(REACHABILITY_CHANGED_FILTER_BROADCAST));
    }
}