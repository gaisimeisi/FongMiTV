package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.AdapterVodRectBinding;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 最近观看页用的海报适配器，版式和首页完全一致（TVRecyclerView + 同一个卡片布局）。
 * 和 {@link HomeVodAdapter} 的区别只是数据源是 {@link History}。
 */
public class HistoryVodAdapter extends RecyclerView.Adapter<HistoryVodAdapter.Holder> {

    public interface OnItemListener {
        void onItemClick(History item);
    }

    private static final float FOCUS_SCALE = 1.08f;

    private final List<History> items = new ArrayList<>();
    private final java.util.Set<String> selected = new java.util.HashSet<>();
    private final OnItemListener listener;
    private int[] size;
    private boolean editing;

    public HistoryVodAdapter(OnItemListener listener) {
        this.listener = listener;
    }

    public HistoryVodAdapter size(int[] size) {
        this.size = size;
        return this;
    }

    public void setItems(List<History> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public List<History> getItems() {
        return items;
    }

    /** 进入/退出多选模式，进入时清空已选 */
    public void setEditing(boolean editing) {
        this.editing = editing;
        this.selected.clear();
        notifyDataSetChanged();
    }

    public boolean isEditing() {
        return editing;
    }

    /** 切换某一条的选中状态，返回切换后是否被选中 */
    public boolean toggleSelect(History item) {
        String key = item.getKey();
        boolean added;
        if (selected.contains(key)) {
            selected.remove(key);
            added = false;
        } else {
            selected.add(key);
            added = true;
        }
        notifyDataSetChanged();
        return added;
    }

    public void clear() {
        items.clear();
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
        holder.bind(items.get(position), listener);
        if (size != null) {
            ViewGroup.LayoutParams lp = holder.binding.getRoot().getLayoutParams();
            lp.width = size[0];
            lp.height = size[1];
            holder.binding.getRoot().setLayoutParams(lp);
            ViewGroup.LayoutParams ip = holder.binding.image.getLayoutParams();
            ip.height = size[1];
            holder.binding.image.setLayoutParams(ip);
        }
        // 多选态：选中的卡片压暗 + 青绿描边
        holder.binding.getRoot().setForeground(
                editing && selected.contains(items.get(position).getKey())
                        ? holder.binding.getRoot().getContext().getDrawable(R.drawable.shape_vod_selected)
                        : null);
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

        void bind(History item, OnItemListener listener) {
            binding.name.setText(item.getVodName());
            binding.remark.setText(item.getVodRemarks());
            binding.site.setText(item.getSiteName());
            binding.site.setVisibility(item.getSiteVisible());
            binding.year.setVisibility(View.GONE);
            binding.name.setVisibility(View.VISIBLE);
            binding.remark.setVisibility(View.VISIBLE);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
            ImgUtil.load(item.getVodName(), item.getVodPic(), binding.image);
        }
    }
}
