package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.databinding.AdapterTypeBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果页左侧的站点列表（纵向）。
 * 第一项是「全部」，后面是各站点，后面跟各自的结果数量。
 */
public class CollectAdapter extends RecyclerView.Adapter<CollectAdapter.ViewHolder> {

    public interface OnClickListener {
        void onItemClick(Collect item, int position);
    }

    private final List<Collect> mItems;
    private final OnClickListener listener;
    private int selected = 0;

    public CollectAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mItems = new ArrayList<>();
    }

    public void add(Collect item) {
        mItems.add(item);
        notifyItemInserted(mItems.size() - 1);
    }

    public void clear() {
        mItems.clear();
        selected = 0;
        notifyDataSetChanged();
    }

    public Collect get(int position) {
        return mItems.get(position);
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int position) {
        int old = selected;
        selected = position;
        notifyItemChanged(old);
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterTypeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Collect item = mItems.get(position);
        holder.binding.getRoot().setSelected(position == selected);
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, holder.getBindingAdapterPosition());
        });
        String name = item.getSite() == null ? "全部" : item.getSite().getName();
        int count = item.getList() == null ? 0 : item.getList().size();
        holder.binding.text.setText(count > 0 ? name + " (" + count + ")" : name);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterTypeBinding binding;

        ViewHolder(@NonNull AdapterTypeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
