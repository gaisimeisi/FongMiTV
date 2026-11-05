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
 * 首页 / 分类页在 {@code com.skw.library.TVRecyclerView} 上使用的海报适配器。
 *
 * <p>为什么不复用 leanback 的 {@code VodPresenter}：{@code RowPresenter} 依赖
 * {@code BaseGridView} 的选中位置 API，{@code TVRecyclerView} 没有，强行混用会崩。
 * 这里用原生 {@link RecyclerView.Adapter}，卡片布局仍然复用 {@code adapter_vod_rect.xml}。
 *
 * <p>焦点放大自己做：拿到焦点时 1.08 倍并提到最前，避免被相邻卡片压住。
 */
public class HomeVodAdapter extends RecyclerView.Adapter<HomeVodAdapter.Holder> {

    public interface OnItemListener {
        void onItemClick(Vod item);
    }

    private static final float FOCUS_SCALE = 1.08f;

    private final List<Vod> items = new ArrayList<>();
    private final OnItemListener listener;
    private int[] size;

    public HomeVodAdapter(OnItemListener listener) {
        this.listener = listener;
    }

    public HomeVodAdapter size(int[] size) {
        this.size = size;
        return this;
    }

    public void setItems(List<Vod> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void addItems(List<Vod> list) {
        if (list == null || list.isEmpty()) return;
        int start = items.size();
        items.addAll(list);
        notifyItemRangeInserted(start, list.size());
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public int getItemCountSafely() {
        return items.size();
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
            // 片名/备注是压在图片内部的（layout_alignBottom="@+id/image"），
            // 所以 itemView 高度就等于图片高度，不能再额外加文字区，否则行间距会空出一大块。
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

        void bind(Vod item, OnItemListener listener) {
            binding.name.setText(item.getName());
            binding.year.setText(item.getYear());
            binding.site.setText(item.getSiteName());
            binding.remark.setText(item.getRemarks());
            binding.site.setVisibility(item.getSiteVisible());
            binding.year.setVisibility(item.getYearVisible());
            binding.name.setVisibility(item.getNameVisible());
            binding.remark.setVisibility(item.getRemarkVisible());
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
            ImgUtil.load(item.getName(), item.getPic(), binding.image);
        }
    }
}
