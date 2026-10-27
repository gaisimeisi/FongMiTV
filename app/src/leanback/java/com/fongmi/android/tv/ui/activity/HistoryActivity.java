package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
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
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this))));
        loadHistory();
    }

    private void loadHistory() {
        List<History> items = History.get(VodConfig.getCid());
        if (items == null || items.isEmpty()) return;
        int column = Product.getColumn();
        for (List<History> part : Lists.partition(items, column)) {
            ArrayObjectAdapter row = new ArrayObjectAdapter(mPresenter);
            row.addAll(0, new ArrayList<>(part));
            mAdapter.add(new ListRow(row));
        }
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        item.delete();
        recreate();
    }
}
