/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.view.activity;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.minez.eovb.ezoef.view.adapter.TabPagerAdapter;
import nl.minez.eovb.R;

public class MainTabsFragment extends Fragment {

    private TabPagerAdapter tabPagerAdapter;

    @BindView(R.id.view_pager)
    ViewPager viewPager;

    public static MainTabsFragment newInstance() {
        return new MainTabsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View fragmentMainTabsView = inflater.inflate(R.layout.fragment_main_tabs, container, false);
        ButterKnife.bind(this, fragmentMainTabsView);

        final TabLayout tabLayout = ((MainActivity) this.getActivity()).tabLayout;
        if (tabLayout.getTabCount() == 0) {
            // Add tabs for open and closed disruptions
            tabLayout.addTab(tabLayout.newTab().setText(R.string.open_disruptions));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.announcements));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.closed_disruptions));
        }

        // Child fragment manager is important, because we use child fragments
        this.tabPagerAdapter = new TabPagerAdapter(getChildFragmentManager(), tabLayout);

        this.viewPager.setAdapter(this.tabPagerAdapter);
        this.viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        return fragmentMainTabsView;
    }

}