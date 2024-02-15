/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.view.fragment;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.minez.eovb.R;
import nl.minez.eovb.databinding.FragmentDisruptionDetailBinding;
import nl.minez.eovb.ezoef.model.Disruption;
import nl.minez.eovb.ezoef.model.Update;
import nl.minez.eovb.ezoef.util.LogUtil;
import nl.minez.eovb.ezoef.view.activity.MainActivity;
import nl.minez.eovb.ezoef.view.adapter.UpdatesAdapter;
import nl.minez.eovb.ezoef.view.viewModel.DisruptionViewModel;

public class DisruptionDetailFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String DISRUPTION_KEY = "DisruptionKey";

    private Disruption disruption;
    private UpdatesAdapter updatesAdapter;

    @BindView(R.id.recycler_updates)
    RecyclerView listUpdatesRecyclerView;

    public static DisruptionDetailFragment newInstance(Disruption disruption) {
        final DisruptionDetailFragment disruptionDetailFragment = new DisruptionDetailFragment();

        final Bundle bundle = new Bundle();
        bundle.putParcelable(DISRUPTION_KEY, disruption);
        disruptionDetailFragment.setArguments(bundle);
        return disruptionDetailFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getArguments();
        if (bundle != null) {
            this.disruption = bundle.getParcelable(DISRUPTION_KEY);
        }

        this.updatesAdapter = new UpdatesAdapter(this.getActivity());
        this.updatesAdapter.setItems(this.disruption.updates);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelable(DISRUPTION_KEY, this.disruption);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final FragmentDisruptionDetailBinding binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_disruption_detail,
                container,
                false);
        binding.setViewModel(new DisruptionViewModel(getContext(), this.disruption));

        final View root = binding.getRoot();

        // Also bind with butter knife to update the recycler view
        ButterKnife.bind(this, root);

        // listUpdatesRecyclerView is now filled
        this.listUpdatesRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        this.listUpdatesRecyclerView.setAdapter(this.updatesAdapter);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        final AppCompatActivity activity = (AppCompatActivity) this.getActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(R.string.details);
        }

        final FloatingActionButton floatingActionButton = this.getFloatingActionButton();
        if (floatingActionButton != null) {
            floatingActionButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        final FloatingActionButton floatingActionButton = this.getFloatingActionButton();
        if (floatingActionButton != null) {
            floatingActionButton.setVisibility(View.GONE);
        }
    }

    private FloatingActionButton getFloatingActionButton() {
        final FloatingActionButton floatingActionButton;

        if ((this.getActivity() instanceof MainActivity)) {
            floatingActionButton = ((MainActivity) this.getActivity()).getFloatingActionButton();
        } else {
            floatingActionButton = null;
        }

        if (floatingActionButton != null) {
            floatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onShareButtonClicked();
                }
            });
        }

        return floatingActionButton;
    }

    private void onShareButtonClicked() {
        if (this.disruption == null) {
            return;
        }

        final String separator = this.disruption.service.isEmpty() || this.disruption.location.isEmpty() ? "" : " - ";
        final String serviceLocationText = this.disruption.service + separator + this.disruption.location;

        final DateTimeFormatter readableDateTimeFormatter = DateTimeFormat.forPattern("EE dd MMM' 'HH:mm'u'").withLocale(new Locale("nl", "NL"));
        final String updates = Joiner.on("\n").join(Lists.transform(this.disruption.updates, new Function<Update, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Update input) {
                return Joiner.on("\n").join("", input.description, readableDateTimeFormatter.print(input.dateTime));
            }
        }));

        final String htmlTextToShare = Joiner.on("\n").join(this.disruption.title, readableDateTimeFormatter.print(this.disruption.dateTime), "", this.disruption.description, serviceLocationText, updates).replaceAll("\n", "<br/>");
        final String textToShare = Html.fromHtml(htmlTextToShare).toString().trim();
        final String subject = this.disruption.title;

        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        final List<Intent> targets = new ArrayList<>();
        for (ResolveInfo resolveInfo : this.getActivity().getPackageManager().queryIntentActivities(shareIntent, 0)) {
            final String packageName = resolveInfo.activityInfo.packageName.toLowerCase();
            if (packageName.contains("google.android")) {
                final Intent targetShareIntent = new Intent(Intent.ACTION_SEND);
                targetShareIntent.setType("text/plain");
                targetShareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
                targetShareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                targetShareIntent.setPackage(resolveInfo.activityInfo.packageName);
                targets.add(targetShareIntent);
            }
        }

        if (targets.isEmpty()) {
            return;
        }

        final Intent chooser = Intent.createChooser(targets.remove(0), this.getResources().getText(R.string.share_to));
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targets.toArray(new Parcelable[]{}));
        this.startActivity(chooser);
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtil.getInstance().logWithName(LogUtil.SCHERM, LogUtil.VERSTORINGEN_DETAIL_SCHERM);
    }

    @Override
    public void onRefresh() {
    }

}