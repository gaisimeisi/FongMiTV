package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityCollectBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.adapter.CollectAdapter;
import com.fongmi.android.tv.ui.adapter.CollectVodAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.gson.reflect.TypeToken;
import com.skw.library.layoutmanager.TVGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果页：影视仓风格 —— 左侧纵向站点列表，右侧统一结果网格。
 * 默认「全部」显示所有站点的结果，点某个站点只显示该站结果。
 */
public class CollectActivity extends BaseActivity implements CollectAdapter.OnClickListener {

    private ActivityCollectBinding mBinding;
    private CollectAdapter mAdapter;
    private CollectVodAdapter mVodAdapter;
    private SiteViewModel mViewModel;
    private List<Site> mSites;

    /** 每个站点的搜索结果 */
    private final List<Collect> mCollects = new ArrayList<>();

    /** 「全部」的结果（各站结果合并） */
    private final List<Vod> mAll = new ArrayList<>();
    private final List<String> mAllKeys = new ArrayList<>();

    public static void start(Activity activity, String keyword) {
        Intent intent = new Intent(activity, CollectActivity.class);
        intent.putExtra("keyword", keyword);
        activity.startActivity(intent);
    }

    private String getKeyword() {
        return getIntent().getStringExtra("keyword");
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntent().putExtras(intent);
        reset();
        search();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setSiteList();
        setResultGrid();
        setViewModel();
        saveKeyword();
        setSites();
        search();
    }

    private void reset() {
        mCollects.clear();
        mAll.clear();
        mAllKeys.clear();
        if (mAdapter != null) mAdapter.clear();
        if (mVodAdapter != null) mVodAdapter.clear();
    }

    /** 左侧：站点列表（纵向） */
    private void setSiteList() {
        mBinding.siteList.setLayoutManager(new LinearLayoutManager(this));
        mBinding.siteList.setAdapter(mAdapter = new CollectAdapter(this));
    }

    /** 右侧：结果网格，和首页同款 */
    private void setResultGrid() {
        int column = Math.max(1, Product.getColumn());
        TVGridLayoutManager manager = new TVGridLayoutManager(this, column);
        manager.setOrientation(RecyclerView.VERTICAL);
        mBinding.resultGrid.setLayoutManager(manager);
        mVodAdapter = new CollectVodAdapter(this::onItemClick);
        mBinding.resultGrid.setAdapter(mVodAdapter);
        int gap = ResUtil.dp2px(12);
        mBinding.resultGrid.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
                                       RecyclerView parent, RecyclerView.State state) {
                outRect.left = gap / 2;
                outRect.right = gap / 2;
                outRect.bottom = gap;
            }
        });
        // Product.getSpec() 内部用的是屏幕宽度，这里右侧只占屏宽减去左侧站点列表，
        // 必须等容器测量完按实际宽度重算卡片尺寸，否则卡片会挤在一起。
        mBinding.resultGrid.post(() -> {
            int width = mBinding.resultGrid.getWidth();
            if (width <= 0) return;
            int space = ResUtil.dp2px(32) + gap * (column - 1);
            int cardWidth = (width - space) / column;
            int cardHeight = (int) (cardWidth / 0.75f);
            mVodAdapter.size(new int[]{cardWidth, cardHeight});
            mVodAdapter.notifyDataSetChanged();
        });
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getSearch().observe(this, result -> {
            if (result.getList().isEmpty()) return;
            onSearchResult(result.getList());
        });
    }

    /** 每个站点回调一次结果 */
    private void onSearchResult(List<Vod> list) {
        Collect collect = Collect.create(list);
        mCollects.add(collect);
        mAdapter.add(collect);
        // 同步到「全部」
        String key = collect.getSite().getKey();
        for (Vod vod : list) {
            mAll.add(vod);
            mAllKeys.add(key);
        }
        // 当前选中的是「全部」时实时刷新右侧；
        // 原来写成 getItemCount()==1，但上面 add(collect) 已经让数量变成 2，永远不成立 → 右侧一直空白
        if (mAdapter.getSelected() == 0) showAll();
    }

    private void saveKeyword() {
        List<String> items = Setting.getKeyword().isEmpty() ? new ArrayList<>() : App.gson().fromJson(Setting.getKeyword(), TypeToken.getParameterized(List.class, String.class).getType());
        items.remove(getKeyword());
        items.add(0, getKeyword());
        if (items.size() > 9) items.remove(9);
        Setting.putKeyword(App.gson().toJson(items));
    }

    private void setSites() {
        mSites = VodConfig.get().getSites().stream().filter(Site::isSearchable).toList();
    }

    private void search() {
        if (mSites == null || mSites.isEmpty()) return;
        mAdapter.add(Collect.all());
        mBinding.result.setText(getString(R.string.collect_result, getKeyword()));
        mViewModel.searchContent(mSites, getKeyword(), false);
    }

    /** 点左侧站点：0 是「全部」，其它是具体站点 */
    @Override
    public void onItemClick(Collect item, int position) {
        mAdapter.setSelected(position);
        if (position == 0) {
            showAll();
            return;
        }
        Collect collect = position - 1 < mCollects.size() ? mCollects.get(position - 1) : null;
        if (collect == null) return;
        List<String> keys = new ArrayList<>();
        String key = collect.getSite().getKey();
        for (int i = 0; i < collect.getList().size(); i++) keys.add(key);
        mVodAdapter.setItems(collect.getList(), keys);
    }

    private void showAll() {
        mVodAdapter.setItems(mAll, mAllKeys);
    }

    private void onItemClick(Vod item, String siteKey) {
        VideoActivity.start(this, siteKey, item.getId(), item.getName(), item.getPic());
    }

    @Override
    protected void onBackInvoked() {
        mViewModel.stopSearch();
        super.onBackInvoked();
    }
}
