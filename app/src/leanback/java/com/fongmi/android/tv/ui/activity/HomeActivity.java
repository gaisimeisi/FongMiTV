package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.api.node.NodeRuntime;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.db.BackupManager;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.playback.ExternalPlayback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.DLNARendererService;
import com.fongmi.android.tv.service.PlaybackService;
import com.skw.library.TVRecyclerView;
import com.skw.library.layoutmanager.TVGridLayoutManager;
import com.fongmi.android.tv.ui.adapter.HomeVodAdapter;
import com.fongmi.android.tv.ui.adapter.BaseDiffCallback;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.custom.CustomTitleView;
import com.fongmi.android.tv.ui.dialog.LocalConnectDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;
import com.fongmi.android.tv.ui.presenter.HeaderPresenter;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.ui.presenter.ProgressPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.google.common.collect.Lists;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HomeActivity extends BaseActivity implements CustomTitleView.Listener, VodPresenter.OnClickListener, FuncPresenter.OnClickListener, HistoryPresenter.OnClickListener {

    private static final String STATE_ACTION_HANDLED = "action_handled";
    private static volatile int instanceCount;

    public static boolean isRunning() {
        return instanceCount > 0;
    }

    private ActivityHomeBinding mBinding;
    private ArrayObjectAdapter mHistoryAdapter;
    private HomeVodAdapter mAdapter;
    private HistoryPresenter mPresenter;
    /** 首页返回的分类，切到分类页时 mResult 会被覆盖，必须单独存 */
    private List<Class> homeTypes = new ArrayList<>();
    /** 当前选中的 Tab 下标，重建 Tab 时要恢复 */
    private int currentTab;
    /** 上次构建 Tab 用的内容签名，避免每次刷新都重建（重建会让下方列表重新布局、丢失滚动位置） */
    private String tabSignature = "";
    private int page = 1;
    /** 当前分类的 typeId，供分页加载用；主页时为 null */
    private String currentTypeId;
    private VodPresenter mVodPresenter;
    private SiteViewModel mViewModel;
    private Intent pendingAction;
    private boolean actionReady;
    private Result mResult;
    private Clock mClock;

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    private Config getConfig() {
        return VodConfig.get().getConfig();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingAction = intent;
        consumeAction();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != ExternalPlayback.REQUEST_CODE) return;
        setResult(resultCode, data);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instanceCount++;
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        if (savedInstanceState == null || !savedInstanceState.getBoolean(STATE_ACTION_HANDLED)) pendingAction = getIntent();
        mResult = Result.empty();
        mClock = Clock.create(mBinding.clock).format("yyyy年MM月dd日 HH:mm:ss");
        mBinding.progressLayout.showProgress();
        Updater.get().start(this, false);
        setRecyclerView();
        setViewModel();
        setAdapter();
        checkPermission();
    }

    @Override
    protected void initEvent() {
        mBinding.title.setListener(this);
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_VIEW.equals(intent.getAction())) {
            ExternalPlayback.open(this, intent, this::loadLive);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(keyword)) SearchActivity.start(this, keyword);
        }
    }

    private void consumeAction() {
        if (!actionReady || pendingAction == null) return;
        Intent intent = pendingAction;
        pendingAction = null;
        checkAction(intent);
    }

    private void setRecyclerView() {
        // 使用 TVRecyclerView（影视仓同款），触摸滚动不会回弹到第一行
        Style style = mResult.getStyle(getHome().getStyle());
        int column = Math.max(1, Product.getColumn(style));
        TVGridLayoutManager manager = new TVGridLayoutManager(this, column);
        manager.setOrientation(RecyclerView.VERTICAL);
        mBinding.recycler.setLayoutManager(manager);
        mAdapter = new HomeVodAdapter(this::onItemClick).size(Product.getSpec(style));
        mBinding.recycler.setAdapter(mAdapter);
        mBinding.recycler.setOnLoadMoreListener(this::onLoadMore);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getAction().observe(this, result -> WebActivity.handleAction(this, result));
        mViewModel.getResult().observe(this, this::onResult);
    }

    private void onResult(Result result) {
        mResult = result;
        if (!result.getTypes().isEmpty()) homeTypes = result.getTypes();
        if (mBinding != null && mBinding.progressLayout.isProgress()) mBinding.progressLayout.showContent();
        // 每次有数据都重建 Tab：冷启动第一次 onResult 时 types 可能还是 0
        setTabBar();
        addVideo(result);
        // 通知 TVRecyclerView：本页加载完毕，可以继续加载更多
        mBinding.recycler.setLoadMoreComplete();
        Cache.clear().put(result);
    }

    private void setAdapter() {
        mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        // 影视仓风格：功能入口移到顶部图标栏，列表区不再有行标题
    }

    private void checkPermission() {
        PermissionUtil.requestLocalNetwork(this, this::onNetworkPermissionResult);
    }

    private void onNetworkPermissionResult(boolean allGranted) {
        PermissionUtil.requestNotify(this);
        if (allGranted) DLNARendererService.start(this);
        initConfig();
        setTitle();
    }

    private void setTitle() {
        List<String> items = Arrays.asList(getHome().getName(), getConfig().getName(), getString(R.string.app_name));
        Optional<String> optional = items.stream().filter(s -> !TextUtils.isEmpty(s)).findFirst();
        optional.ifPresent(s -> mBinding.title.setText(s));
    }

    private void initConfig() {
        VodConfig.get().init().load(getCallback());
        LiveConfig.get().init().load();
        WallConfig.get().init();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                onConfigReady();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                onConfigReady();
            }
        };
    }

    private void onConfigReady() {
        actionReady = true;
        showContent();
        consumeAction();
    }

    private void showContent() {
        mBinding.progressLayout.showContent();
        setFocus();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_ACTION_HANDLED, pendingAction == null);
        super.onSaveInstanceState(outState);
    }

    private void loadLive(String url) {
        if (isFinishing() || isDestroyed()) return;
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                if (getIntent().getBooleanExtra(ExternalPlayback.FORWARD_RESULT, false)) startActivityForResult(new Intent(HomeActivity.this, LiveActivity.class).putExtra("empty", LiveConfig.isEmpty()), ExternalPlayback.REQUEST_CODE);
                else LiveActivity.start(getActivity());
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                if (getIntent().getBooleanExtra(ExternalPlayback.FORWARD_RESULT, false)) finish();
            }
        });
    }

    private void setFocus() {
        mBinding.title.setSelected(true);
        App.post(() -> mBinding.title.setFocusable(true), 500);
        if (!mBinding.title.hasFocus()) mBinding.recycler.requestFocus();
    }

    private void getVideo() {
        mResult = Result.empty();
        if (mAdapter != null) mAdapter.clear();
        mViewModel.homeContent();
    }

    private void addVideo(Result result) {
        List<Vod> list = result.getList();
        if (list.isEmpty()) return;
        if (mAdapter.getItemCountSafely() == 0) mAdapter.setItems(list);
        else mAdapter.addItems(list);
    }

    private void setFunc() {
        // 影视仓风格：9 个横排图标（图标在左、文字在右、无背景块）
        mBinding.funcBar.removeAllViews();
        List<Func> items = new ArrayList<>();
        items.add(Func.create(R.string.home_history));
        if (LiveConfig.hasUrl()) items.add(Func.create(R.string.home_live));
        items.add(Func.create(R.string.home_search));
        items.add(Func.create(R.string.home_config));
        items.add(Func.create(R.string.home_line));
        items.add(Func.create(R.string.home_local));
        items.add(Func.create(R.string.home_keep));
        items.add(Func.create(R.string.home_push));
        items.add(Func.create(R.string.home_setting));
        for (Func item : items) {
            View view = getLayoutInflater().inflate(R.layout.adapter_func, mBinding.funcBar, false);
            ((android.widget.ImageView) view.findViewById(R.id.icon)).setImageResource(item.getDrawable());
            ((android.widget.TextView) view.findViewById(R.id.text)).setText(item.getText());
            view.setOnClickListener(v -> onItemClick(item));
            mBinding.funcBar.addView(view);
        }
    }

    private volatile boolean typesLoading;

    /** 首页接口没给 class 时，用 ?ac=list 独立拉一次分类。 */
    private void ensureTypes() {
        if (typesLoading) return;
        typesLoading = true;
        new Thread(() -> {
            try {
                Result r = SiteApi.categoryList(getHome());
                android.util.Log.d("TabBar", "ac=list -> types=" + r.getTypes().size());
                if (!r.getTypes().isEmpty()) App.post(() -> {
                    homeTypes = r.getTypes();
                    mResult.setTypes(homeTypes);
                    setTabBar();
                });
            } catch (Throwable e) {
                android.util.Log.w("TabBar", "ac=list failed", e);
            } finally {
                typesLoading = false;
            }
        }).start();
    }

    private void setTabBar() {
        // 影视仓风格：主页 + 站点返回的真实分类（电影/连续剧/综艺…）
        List<String> names = new ArrayList<>();
        names.add("主页");
        List<Class> types = homeTypes.isEmpty() ? mResult.getTypes() : homeTypes;
        List<String> cates = getHome().getCategories();
        android.util.Log.d("TabBar", "types=" + types.size() + " categories=" + cates.size() + " cates=" + cates);
        if (!types.isEmpty()) {
            for (Class type : types) names.add(type.getTypeName());
        } else if (!cates.isEmpty()) {
            // 有些站点的 homeContent 不返回 types，分类只在 Site.categories 里
            for (String cate : cates) {
                if (!TextUtils.isEmpty(cate)) names.add(cate);
            }
        } else {
            // 两层都空：异步拉 ?ac=list 拿真实分类（不再塞假分类）
            ensureTypes();
        }
        if (currentTab >= names.size()) currentTab = 0;
        String signature = String.join("|", names);
        if (signature.equals(tabSignature) && mBinding.tabBar.getChildCount() == names.size()) {
            // Tab 内容没变：只更新选中态，绝不重建（重建会打断下方列表滚动）
            for (int i = 0; i < mBinding.tabBar.getChildCount(); i++) {
                mBinding.tabBar.getChildAt(i).setSelected(i == currentTab);
            }
            return;
        }
        tabSignature = signature;
        mBinding.tabBar.removeAllViews();
        for (int i = 0; i < names.size(); i++) {
            android.widget.TextView tab = (android.widget.TextView) getLayoutInflater().inflate(R.layout.item_tab, mBinding.tabBar, false);
            tab.setText(names.get(i));
            tab.setSelected(i == currentTab);
            final int index = i;
            tab.setOnClickListener(v -> selectTab(index));
            mBinding.tabBar.addView(tab);
        }
    }

    private void selectTab(int index) {
        currentTab = index;
        for (int i = 0; i < mBinding.tabBar.getChildCount(); i++) {
            mBinding.tabBar.getChildAt(i).setSelected(i == index);
        }
        // 影视仓行为：切到分类后收起图标行与站点名，只留 Tab + 海报墙
        int chrome = index == 0 ? View.VISIBLE : View.GONE;
        mBinding.funcScroll.setVisibility(chrome);
        mBinding.toolbar.setVisibility(index == 0 ? View.VISIBLE : View.GONE);

        mAdapter.clear();
        page = 1;
        if (index == 0) {
            currentTypeId = null;
            mViewModel.homeContent();
        } else {
            List<Class> types = homeTypes.isEmpty() ? mResult.getTypes() : homeTypes;
            List<String> cates = getHome().getCategories();
            String typeId;
            if (!types.isEmpty()) {
                if (index - 1 >= types.size()) return;
                typeId = types.get(index - 1).getTypeId();
            } else if (!cates.isEmpty()) {
                if (index - 1 >= cates.size()) return;
                typeId = cates.get(index - 1);
            } else {
                return;
            }
            android.util.Log.d("TabBar", "select index=" + index + " typeId=" + typeId);
            currentTypeId = typeId;
            mViewModel.categoryContent(getHome().getKey(), typeId, "1", true, new java.util.HashMap<>());
        }
    }

    /** 列表滚到底时由 TVRecyclerView 回调，加载下一页 */
    private void onLoadMore() {
        if (currentTypeId == null || currentTypeId.isEmpty()) return;
        page++;
        mViewModel.categoryContent(getHome().getKey(), currentTypeId, String.valueOf(page), true, new java.util.HashMap<>());
    }

    private void getHistory() {
        getHistory(false);
    }

    private void getHistory(boolean renew) {
        List<History> items = History.get();
        // 影视仓风格：最近观看移到独立页面（HistoryActivity），首页不再插入历史行
        if (renew) mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        mHistoryAdapter.setItems(items, new BaseDiffCallback<History>());
    }

    private void setHistoryDelete(boolean delete) {
        mPresenter.setDelete(delete);
        mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
    }

    private void clearHistory() {
        History.clear(VodConfig.getCid());
        mPresenter.setDelete(false);
        mHistoryAdapter.clear();
    }

    private void setLogo() {
        // 首页不再显示站点 logo 图标（按需求只留站点名）
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                RefreshEvent.history();
                RefreshEvent.home();
                break;
            case COMMON:
                setFunc();
                break;
            case BOOT:
                LiveActivity.start(this);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case HOME:
                getVideo();
                setTitle();
                break;
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                getVideo();
                getHistory(true);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.type()) {
            case SEARCH:
                SearchActivity.start(this, event.text());
                break;
            case PUSH:
                VideoActivity.push(this, event.text());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (VodConfig.get().getConfig().equals(event.config())) VideoActivity.cast(this, event.history());
        else VodConfig.load(event.config(), getCallback(event));
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    @Override
    public void onItemClick(Func item) {
        if (item.getResId() == R.string.home_vod) VodActivity.start(this, mResult);
        else if (item.getResId() == R.string.home_live) LiveActivity.start(this);
        else if (item.getResId() == R.string.home_keep) KeepActivity.start(this);
        else if (item.getResId() == R.string.home_push) LocalConnectDialog.show(this, "push");
        else if (item.getResId() == R.string.home_search) SearchActivity.start(this);
        else if (item.getResId() == R.string.home_setting) SettingActivity.start(this);
        else if (item.getResId() == R.string.home_history) HistoryActivity.start(this);
        else if (item.getResId() == R.string.home_config) SettingActivity.start(this);
        else if (item.getResId() == R.string.home_line) SettingActivity.start(this);
        else if (item.getResId() == R.string.home_local) SettingActivity.start(this);
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isAction()) mViewModel.action(getHome().getKey(), item.getAction());
        else if (getHome().isIndex()) CollectActivity.start(this, item.getName());
        else VideoActivity.start(this, getHome().getKey(), item.getId(), item.getName(), item.getPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        if (item.isAction()) return false;
        CollectActivity.start(this, item.getName());
        return true;
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        if (mHistoryAdapter.size() > 0) return;
        mPresenter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        if (mPresenter.isDelete()) clearHistory();
        else setHistoryDelete(true);
        return true;
    }

    @Override
    public void showDialog() {
        SiteDialog.create().show(this);
    }

    @Override
    public void onRefresh() {
        getVideo();
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) showDialog();
        if (KeyUtil.isActionDown(event) & KeyUtil.isDownKey(event) && getCurrentFocus() == mBinding.title) return mBinding.recycler.getChildAt(0).requestFocus();
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
    }

    @Override
    protected void onBackInvoked() {
        if (mBinding.progressLayout.isProgress()) {
            showContent();
        } else if (mPresenter.isDelete()) {
            setHistoryDelete(false);
        } else if (mBinding.recycler.getChildCount() > 0
                && mBinding.recycler.getChildAdapterPosition(mBinding.recycler.getChildAt(0)) > 0) {
            mBinding.recycler.scrollToPosition(0);
        } else {
            if (PlaybackService.isRunning()) Util.moveToBackground(this);
            else super.onBackInvoked();
        }
    }

    @Override
    protected void onDestroy() {
        if (--instanceCount == 0) {
            BackupManager.backup();
            releaseResources();
        }
        super.onDestroy();
    }

    public static void releaseResources() {
        if (isRunning() || PlaybackService.isRunning()) return;
        DLNARendererService.stop(App.get());
        LiveConfig.get().clear();
        VodConfig.get().clear();
        OkHttp.get().clear();
        Source.get().exit();
        Server.get().stop();
        NodeRuntime.get().releaseIfUnused();
    }
}
