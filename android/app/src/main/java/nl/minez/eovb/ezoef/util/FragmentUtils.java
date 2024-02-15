/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.util;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public class FragmentUtils {

    public static void addFragmentToBackStack(Context context, int containerViewId, Fragment fragment) {
        if (context instanceof AppCompatActivity) {
            final AppCompatActivity activity = (AppCompatActivity) context;
            if (!activity.isFinishing()) {
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(containerViewId, fragment)
                        .addToBackStack(fragment.getClass().getSimpleName())
                        .commit();
            }
        }
    }

    public static void popBackStackToRootFragment(Context context, int containerViewId, Fragment fragment) {
        if (context instanceof AppCompatActivity) {
            final AppCompatActivity activity = (AppCompatActivity) context;
            if (!activity.isFinishing()) {
                final FragmentManager fragmentManager = activity.getSupportFragmentManager();

                final int entryCount = fragmentManager.getBackStackEntryCount();
                for (int i = 0; i < entryCount; i++) {
                    fragmentManager.popBackStack();
                }

                fragmentManager
                        .beginTransaction()
                        .replace(containerViewId, fragment)
                        .commit();
            }
        }
    }
}
