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
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * 影视仓风格：最近观看独立成页，不再占用首页。
 */
public class HistoryActivity extends BaseActivity implements HistoryPresenter.OnClickListener {

    private ActivityHistoryBinding mBinding;
    private ArrayObjectAdapter mAdapter;
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
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mPresenter = new HistoryPresenter(this);
        ClassPresenterSelector selector = new ClassPresenterSelector();
        selector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mAdapter = new ArrayObjectAdapter(selector);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter));
        loadHistory();
    }

    private void loadHistory() {
        List<History> items = queryHistory();
        if (items.isEmpty()) return;
        // 全部塞进一行：ListRow 内部是 HorizontalGridView，超出部分横向滚动，
        // 不再按列数折行（原来 partition 成 5+2 两行，看着很散）。
        ArrayObjectAdapter row = new ArrayObjectAdapter(mPresenter);
        row.addAll(0, new ArrayList<>(items));
        mAdapter.add(new ListRow(row));
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

    @Override
    public void onItemClick(History item) {
        if (item == null || item.getKey() == null || item.getVodId() == null) return;
        VideoActivity.start(this, item.getKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        if (item == null) return;
        item.delete();
        recreate();
    }

    @Override
    public boolean onLongClick() {
        return false;
    }
}
