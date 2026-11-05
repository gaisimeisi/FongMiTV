package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.VerticalGridView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.ui.adapter.HistoryVodAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * 影视仓风格：最近观看独立成页，不再占用首页。
 */
public class HistoryActivity extends BaseActivity {

    private ActivityHistoryBinding mBinding;
    private HistoryVodAdapter mAdapter;
    private HistoryPresenter mPresenter;

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
        loadHistory();
    }

    private void loadHistory() {
        List<History> items = queryHistory();
        mAdapter.setItems(items);
    }

    private void onItemClick(History item) {
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
