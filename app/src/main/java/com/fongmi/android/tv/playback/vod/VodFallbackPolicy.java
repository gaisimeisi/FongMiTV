package com.fongmi.android.tv.playback.vod;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;

import java.util.ArrayList;
import java.util.List;

class VodFallbackPolicy {

    private final VodPlaybackController controller;
    private final VodPlaybackState state;
    private final VodPlaybackHost host;
    private final VodDataSource dataSource;

    VodFallbackPolicy(VodPlaybackController controller, VodPlaybackState state, VodPlaybackHost host, VodDataSource dataSource) {
        this.controller = controller;
        this.state = state;
        this.host = host;
        this.dataSource = dataSource;
    }

    void playbackError() {
        fallbackToNextLineOrSource();
    }

    void emptyFlag() {
        fallbackToNextLineOrSource();
    }

    void emptyDetail() {
        fallbackToNextSource(false);
    }

    void manualSwitchSource() {
        fallbackToNextSource(true);
    }

    void search(String keyword, boolean autoFallback) {
        state.setSearchKeyword(keyword);
        state.setAutoFallback(autoFallback);
        state.setSelectFirstSource(autoFallback);
        host.onSearchStarted(keyword);
        dataSource.searchContent(getSearchableSites(), keyword, true);
    }

    void onSearchResult(Result result) {
        List<Vod> items = new ArrayList<>(result.getList());
        items.removeIf(this::mismatch);
        state.setSources(items);
        if (state.isSelectFirstSource() && state.hasSources()) nextSource();
        else host.renderSources(state.getSources());
        if (items.isEmpty()) return;
        host.onSearchResult();
    }

    private void fallbackToNextLineOrSource() {
        if (!host.isSiteChangeable()) return;
        if (fallbackToNextLine()) return;
        fallbackToNextSource(false);
    }

    private boolean fallbackToNextLine() {
        int position = state.getFlagPosition() + 1;
        if (position >= state.getFlags().size()) return false;
        Flag flag = state.getFlags().get(position);
        host.showSwitchLine(flag);
        controller.selectFlag(flag);
        return true;
    }

    private void fallbackToNextSource(boolean force) {
        if (!state.hasSources()) search(host.getVodName(), true);
        else if (state.isAutoFallback() || force) nextSource();
    }

    private void nextSource() {
        if (!state.hasSources()) return;
        Vod item = state.removeFirstSource();
        host.renderSources(state.getSources());
        host.showSwitchSource(item);
        state.addFailedId(host.getVodId());
        state.setSelectFirstSource(false);
        controller.fallbackSource(item);
    }

    private List<Site> getSearchableSites() {
        // 原站点必须排最前：换源时优先用当前站点，实在不行才轮到其它站源。
        // 之前直接按配置顺序塞，导致自动换源时经常跳到完全不相干的站点。
        String homeKey = "";
        try {
            Site home = VodConfig.get().getHome();
            if (home != null) homeKey = home.getKey();
        } catch (Throwable ignored) {
        }

        List<Site> sites = new ArrayList<>();
        if (!homeKey.isEmpty()) {
            for (Site site : VodConfig.get().getSites()) {
                if (homeKey.equals(site.getKey()) && isPass(site)) sites.add(site);
            }
        }
        for (Site site : VodConfig.get().getSites()) {
            if (!homeKey.equals(site.getKey()) && isPass(site)) sites.add(site);
        }
        return sites;
    }

    private boolean isPass(Site item) {
        if (state.isAutoFallback() && !item.isChangeable()) return false;
        return item.isSearchable();
    }

    private boolean mismatch(Vod item) {
        // 原来这里还有 `if (host.getVodId().equals(item.getId())) return true;`，
        // 会把「当前正在播的这一条」排除掉；而原站点往往只有这一个片源，
        // 结果就是原站点永远选不到，直接跳到别家。是否重复由 addFailedId 兜底，不会死循环。
        if (state.hasFailedId(item.getId())) return true;
        if (state.isAutoFallback()) return !item.getName().equals(state.getSearchKeyword());
        return !item.getName().contains(state.getSearchKeyword());
    }
}
