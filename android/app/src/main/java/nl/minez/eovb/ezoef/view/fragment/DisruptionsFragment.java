/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.view.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.minez.eovb.R;
import nl.minez.eovb.ezoef.api.ApiConnector;
import nl.minez.eovb.ezoef.model.Disruption;
import nl.minez.eovb.ezoef.profile.Profile;
import nl.minez.eovb.ezoef.util.DialogUtils;
import nl.minez.eovb.ezoef.view.activity.MainActivity;
import nl.minez.eovb.ezoef.view.adapter.DisruptionsAdapter;

public class DisruptionsFragment extends Fragment {

    public enum DisruptionType {
        OPEN_DISRUPTIONS,
        ANNOUNCEMENTS,
        CLOSED_DISRUPTIONS
    }

    private static final String TYPE_KEY = "TypeKey";

    @BindView(R.id.swipe_container)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_disruptions)
    RecyclerView listDisruptionsRecyclerView;

    @BindView(R.id.empty)
    TextView emptyView;

    @BindView(R.id.progress_indicator)
    ProgressBar progressBar;

    @BindView(R.id.latest_refresh)
    TextView latestRefreshTextView;

    private DisruptionsAdapter disruptionsAdapter;
    private List<Disruption> disruptions;
    private Date latestRefreshDate;
    private DisruptionType disruptionType;
    private BroadcastReceiver refreshFilterReceiver;

    public static DisruptionsFragment newInstance(DisruptionType disruptionType) {
        final DisruptionsFragment disruptionsFragment = new DisruptionsFragment();

        final Bundle bundle = new Bundle();
        bundle.putInt(TYPE_KEY, disruptionType.ordinal());
        disruptionsFragment.setArguments(bundle);
        return disruptionsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getArguments();
        if (bundle != null) {
            this.disruptionType = DisruptionType.values()[bundle.getInt(TYPE_KEY)];
        }

        this.disruptions = new ArrayList<>();
        this.disruptionsAdapter = new DisruptionsAdapter(this.getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View fragmentDisruptionsView = inflater.inflate(R.layout.fragment_disruptions, container, false);
        ButterKnife.bind(this, fragmentDisruptionsView);

        this.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh(true);
            }
        });

        this.setupRecyclerView();
        return fragmentDisruptionsView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (this.refreshFilterReceiver != null) {
            this.getContext().unregisterReceiver(this.refreshFilterReceiver);
            this.refreshFilterReceiver = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (this.refreshFilterReceiver == null) {
            this.refreshFilterReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    refresh(true);
                }
            };
            this.getContext().registerReceiver(this.refreshFilterReceiver, new IntentFilter(MainActivity.REFRESH_FILTER_BROADCAST));
        }

        this.refresh(this.latestRefreshDate == null);
    }

    private void refresh(boolean showLoadingIndicators) {
        final ApiConnector apiConnector = ApiConnector.getInstance(this.getContext());
        if (!apiConnector.getAuth().isRegistered()) {
            return;
        }

        if (showLoadingIndicators) {
            showLoadingIndicators();
        }

        final Profile profile = Profile.getInstance(this.getContext());

        final List<Long> services = profile.getServiceIds();
        final List<Long> locations = profile.getLocationIds();
        final ApiConnector.Listener<JSONArray> listener = new ApiConnector.Listener<JSONArray>() {
            @Override
            public void onSuccess(JSONArray response) {
                disruptionsAdapter.setItems(Disruption.fromJSON(response));
                showEmptyView(disruptionsAdapter.getItemCount() == 0);

                latestRefreshDate = new Date();

                hideLoadingIndicators();
            }

            @Override
            public void onError(VolleyError error) {
                if (!DialogUtils.errorAlertDialogIsShowing(getContext())) {
                    DialogUtils.createErrorAlertDialog(getContext(), String.format(getString(R.string.fetch_disruptions_failed_reason_ps), error.getLocalizedMessage())).show();
                }
                showEmptyView(disruptionsAdapter.getItemCount() == 0);
                hideLoadingIndicators();
            }
        };

        if (this.disruptionType == DisruptionType.OPEN_DISRUPTIONS) {
            apiConnector.openDisruptions(services, locations, listener);
        } else if (this.disruptionType == DisruptionType.ANNOUNCEMENTS) {
            apiConnector.announcements(services, locations, listener);
        } else {
            apiConnector.closedDisruptions(services, locations, listener);
        }
    }

    private void showEmptyView(boolean visible) {
//        emptyView.setVisibility(visible ? View.VISIBLE : View.GONE);
//        listDisruptionsRecyclerView.setVisibility(visible ? View.GONE : View.VISIBLE);
        //no records found text depends on the kind of view we see
        String emptyText = "";
        if (this.disruptionType == DisruptionType.OPEN_DISRUPTIONS) {
            emptyText = getString(R.string.no_disruptions_found);
        } else if (this.disruptionType == DisruptionType.ANNOUNCEMENTS) {
            emptyText = getString(R.string.no_announcements_found);
        } else {
            emptyText = getString(R.string.no_solved_disruptions_found);
        }
        this.emptyView.setText(emptyText);
        this.emptyView.setVisibility(visible ? View.VISIBLE : View.GONE);
        this.listDisruptionsRecyclerView.setVisibility(visible ? View.GONE : View.VISIBLE);

    }

    private void setupRecyclerView() {
        this.listDisruptionsRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        this.listDisruptionsRecyclerView.setHasFixedSize(true);
        this.disruptionsAdapter.setItems(this.disruptions);
        this.listDisruptionsRecyclerView.setAdapter(this.disruptionsAdapter);
    }

    public String getLatestRefreshText() {
        final String refreshDateText;
        if (this.latestRefreshDate == null) {
            refreshDateText = this.getContext().getString(R.string.not_refreshed);
        } else {
            refreshDateText = new SimpleDateFormat("HH:mm'u'", Locale.getDefault()).format(this.latestRefreshDate);
        }
        return String.format(getString(R.string.latest_refresh_ps), refreshDateText);
    }

    private void showLoadingIndicators() {
        this.progressBar.setVisibility(View.VISIBLE);
        this.swipeRefreshLayout.setRefreshing(true);
    }

    private void hideLoadingIndicators() {
        this.progressBar.setVisibility(View.GONE);
        this.swipeRefreshLayout.setRefreshing(false);
        this.latestRefreshTextView.setText(getLatestRefreshText());
    }

}