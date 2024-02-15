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

import nl.minez.eovb.ezoef.model.Update;
import nl.minez.eovb.ezoef.view.viewModel.UpdateViewModel;
import nl.minez.eovb.R;
import nl.minez.eovb.databinding.ItemUpdateBinding;

public class UpdatesAdapter extends RecyclerView.Adapter<UpdatesAdapter.BindingHolder> {

    private List<Update> updates;
    private Context context;

    public UpdatesAdapter(Context context) {
        this.context = context;
        this.updates = new ArrayList<>();
    }

    @Override
    public BindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final ItemUpdateBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.item_update,
                parent,
                false);
        return new BindingHolder(binding);
    }

    @Override
    public void onBindViewHolder(BindingHolder holder, int position) {
        final ItemUpdateBinding binding = holder.binding;
        binding.setViewModel(new UpdateViewModel(this.context, this.updates.get(position)));
    }

    @Override
    public int getItemCount() {
        return this.updates.size();
    }

    public void setItems(List<Update> updates) {
        this.updates = updates;
        notifyDataSetChanged();
    }

    public static class BindingHolder extends RecyclerView.ViewHolder {
        private ItemUpdateBinding binding;

        public BindingHolder(final ItemUpdateBinding binding) {
            super(binding.updateView);
            this.binding = binding;
        }
    }

}