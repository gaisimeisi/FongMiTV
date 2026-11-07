package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.ui.adapter.HistoryVodAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 影视仓风格：最近观看独立成页，不再占用首页。
 * 支持编辑模式下的多选删除。
 */
public class HistoryActivity extends BaseActivity {

    private ActivityHistoryBinding mBinding;
    private HistoryVodAdapter mAdapter;

    private boolean editing;
    /** 已选中的记录（用 History.key 做标识，删除时按 key 逐条删） */
    private final Set<String> selected = new LinkedHashSet<>();

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HistoryActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        // 和首页同一套：TVRecyclerView + 5 列网格 + 同一个卡片布局
        int column = Math.max(1, Product.getColumn());
        com.skw.library.layoutmanager.TVGridLayoutManager manager =
                new com.skw.library.layoutmanager.TVGridLayoutManager(this, column);
        manager.setOrientation(androidx.recyclerview.widget.RecyclerView.VERTICAL);
        mBinding.recycler.setLayoutManager(manager);
        mAdapter = new HistoryVodAdapter(this::onItemClick).size(Product.getSpec(com.fongmi.android.tv.bean.Style.rect()));
        mBinding.recycler.setAdapter(mAdapter);
        mBinding.recycler.addItemDecoration(new androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
                                       androidx.recyclerview.widget.RecyclerView parent,
                                       androidx.recyclerview.widget.RecyclerView.State state) {
                outRect.bottom = ResUtil.dp2px(16);
            }
        });

        mBinding.edit.setOnClickListener(v -> toggleEdit());
        mBinding.delete.setOnClickListener(v -> deleteSelected());

        loadHistory();
    }

    private void loadHistory() {
        List<History> items = queryHistory();
        mAdapter.setItems(items);
        if (editing) {
            selected.clear();
            updateDeleteLabel();
        }
    }

    /** 「编辑」/「取消」切换 */
    private void toggleEdit() {
        editing = !editing;
        selected.clear();
        mBinding.edit.setText(editing ? "取消" : "编辑");
        mBinding.delete.setVisibility(editing ? View.VISIBLE : View.GONE);
        mAdapter.setEditing(editing);
        updateDeleteLabel();
        if (editing) mBinding.delete.requestFocus();
    }

    private void updateDeleteLabel() {
        int count = selected.size();
        mBinding.delete.setText(count == 0 ? "删除" : "删除(" + count + ")");
        mBinding.delete.setAlpha(count == 0 ? 0.4f : 1f);
    }

    /** 删除所有选中项 */
    private void deleteSelected() {
        if (selected.isEmpty()) return;
        List<History> snapshot = new ArrayList<>(mAdapter.getItems());
        for (History item : snapshot) {
            if (selected.contains(item.getKey())) item.delete();
        }
        selected.clear();
        editing = false;
        mBinding.edit.setText("编辑");
        mBinding.delete.setVisibility(View.GONE);
        mAdapter.setEditing(false);
        loadHistory();
        mBinding.edit.requestFocus();
    }

    private void onItemClick(History item) {
        // 编辑模式下点击卡片 = 勾选/取消勾选，不跳播放
        if (editing) {
            if (mAdapter.toggleSelect(item)) selected.add(item.getKey());
            else selected.remove(item.getKey());
            updateDeleteLabel();
            return;
        }
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    private List<History> queryHistory() {
        try {
            // 未配置数据源时 VodConfig.getConfig() 为 null，getCid() 会 NPE
            int cid = VodConfig.get().getConfig() != null ? VodConfig.getCid() : 0;
            List<History> items = History.get(cid);
            return items == null ? new ArrayList<>() : items;
        } catch (Throwable e) {
            return new ArrayList<>();
        }
    }
}
