package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodRectBinding;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果页右侧网格用的 adapter，版式与首页一致。
 * 数据里同时带着站点 key，点卡片时直接跳对应站点播放。
 */
public class CollectVodAdapter extends RecyclerView.Adapter<CollectVodAdapter.Holder> {

    public interface OnItemListener {
        void onItemClick(Vod item, String siteKey);
    }

    private static final float FOCUS_SCALE = 1.08f;

    private final List<Vod> items = new ArrayList<>();
    private final List<String> siteKeys = new ArrayList<>();
    private final OnItemListener listener;
    private int[] size;

    public CollectVodAdapter(OnItemListener listener) {
        this.listener = listener;
    }

    public CollectVodAdapter size(int[] size) {
        this.size = size;
        return this;
    }

    public void setItems(List<Vod> list, List<String> keys) {
        items.clear();
        siteKeys.clear();
        if (list != null) items.addAll(list);
        if (keys != null) siteKeys.addAll(keys);
        notifyDataSetChanged();
    }

    public void addItems(List<Vod> list, String siteKey) {
        if (list == null || list.isEmpty()) return;
        int start = items.size();
        items.addAll(list);
        for (int i = 0; i < list.size(); i++) siteKeys.add(siteKey);
        notifyItemRangeInserted(start, list.size());
    }

    public void clear() {
        items.clear();
        siteKeys.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterVodRectBinding binding = AdapterVodRectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position), siteKeys.get(position), listener);
        if (size != null) {
            ViewGroup.LayoutParams lp = holder.binding.getRoot().getLayoutParams();
            lp.width = size[0];
            lp.height = size[1];
            holder.binding.getRoot().setLayoutParams(lp);
            ViewGroup.LayoutParams ip = holder.binding.image.getLayoutParams();
            ip.height = size[1];
            holder.binding.image.setLayoutParams(ip);
        }
        holder.binding.getRoot().setOnFocusChangeListener((v, hasFocus) -> {
            float s = hasFocus ? FOCUS_SCALE : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(160).start();
            if (hasFocus) v.bringToFront();
        });
    }

    static class Holder extends RecyclerView.ViewHolder {

        final AdapterVodRectBinding binding;

        Holder(AdapterVodRectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Vod item, String siteKey, OnItemListener listener) {
            binding.name.setText(item.getName());
            binding.remark.setText(item.getRemarks());
            binding.site.setText(item.getSiteName());
            binding.site.setVisibility(item.getSiteVisible());
            binding.year.setVisibility(View.GONE);
            binding.name.setVisibility(View.VISIBLE);
            binding.remark.setVisibility(View.VISIBLE);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item, siteKey);
            });
            ImgUtil.load(item.getName(), item.getPic(), binding.image);
        }
    }
}
