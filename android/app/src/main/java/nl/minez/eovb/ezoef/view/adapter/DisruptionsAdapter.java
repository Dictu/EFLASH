/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.view.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import nl.minez.eovb.ezoef.model.Disruption;
import nl.minez.eovb.ezoef.view.viewModel.DisruptionViewModel;
import nl.minez.eovb.R;
import nl.minez.eovb.databinding.ItemDisruptionBinding;

public class DisruptionsAdapter extends RecyclerView.Adapter<DisruptionsAdapter.BindingHolder> {

    private List<Disruption> disruptions;
    private Context context;

    public DisruptionsAdapter(Context context) {
        this.context = context;
        this.disruptions = new ArrayList<>();
    }

    @Override
    public BindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final ItemDisruptionBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.item_disruption,
                parent,
                false);
        return new BindingHolder(binding);
    }

    @Override
    public void onBindViewHolder(BindingHolder holder, int position) {
        final ItemDisruptionBinding binding = holder.binding;
        binding.setViewModel(new DisruptionViewModel(this.context, this.disruptions.get(position)));
    }

    @Override
    public int getItemCount() {
        return this.disruptions.size();
    }

    public void setItems(List<Disruption> disruptions) {
        this.disruptions = disruptions;
        notifyDataSetChanged();
    }

    public static class BindingHolder extends RecyclerView.ViewHolder {
        private ItemDisruptionBinding binding;

        public BindingHolder(final ItemDisruptionBinding binding) {
            super(binding.cardView);
            this.binding = binding;
        }
    }

}