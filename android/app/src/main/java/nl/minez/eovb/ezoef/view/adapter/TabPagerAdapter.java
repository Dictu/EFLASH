/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.view.adapter;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

import nl.minez.eovb.ezoef.view.fragment.DisruptionsFragment;

public class TabPagerAdapter extends FragmentPagerAdapter {

    private ArrayList<DisruptionsFragment> disruptionsFragments = new ArrayList<>();
    private ArrayList<String> pageTitles = new ArrayList<>();

    public TabPagerAdapter(FragmentManager supportFragmentManager, TabLayout tabLayout) {
        super(supportFragmentManager);

        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            this.disruptionsFragments.add(DisruptionsFragment.newInstance(DisruptionsFragment.DisruptionType.values()[i]));
            this.pageTitles.add(String.valueOf(tabLayout.getTabAt(i)));
        }
    }

    @Override
    public Fragment getItem(int position) {
        return this.disruptionsFragments.get(position);
    }

    @Override
    public int getCount() {
        return this.disruptionsFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return this.pageTitles.get(position);
    }

}