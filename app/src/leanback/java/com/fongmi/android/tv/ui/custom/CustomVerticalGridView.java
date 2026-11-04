package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.KeyUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CustomVerticalGridView extends VerticalGridView {

    private List<View> views;
    private boolean pressDown;
    private boolean pressUp;
    private boolean moveTop;
    private boolean touching;

    public CustomVerticalGridView(@NonNull Context context) {
        this(context, null);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setMoveTop(true);
    }

    @Override
    protected void initAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        super.initAttributes(context, attrs);
        setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable ViewHolder child, int position, int subposition) {
                if (pressDown && position == 1) hideHeader();
                if (pressUp && position == 0) showHeader();
            }
        });
    }

    public void setHeader(FragmentActivity activity, int... layoutIds) {
        if (activity != null) views = Arrays.stream(layoutIds).mapToObj(id -> (View) activity.findViewById(id)).filter(Objects::nonNull).toList();
    }

    public void setMoveTop(boolean moveTop) {
        this.moveTop = moveTop;
    }

    public void hideHeader() {
        if (views != null) for (View view : views) view.setVisibility(View.GONE);
    }

    public void showHeader() {
        if (views != null) for (View view : views) view.setVisibility(View.VISIBLE);
    }

    public boolean isHeaderVisible() {
        if (views != null) for (View view : views) if (view.getId() == R.id.recycler && view.getVisibility() == View.VISIBLE) return true;
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (!KeyUtil.isActionDown(event)) return super.dispatchKeyEvent(event);
        if (KeyUtil.isBackKey(event)) return moveTop && moveToTop();
        pressUp = KeyUtil.isUpKey(event);
        pressDown = KeyUtil.isDownKey(event);
        return super.dispatchKeyEvent(event);
    }

    public boolean moveToTop() {
        if (views == null || getSelectedPosition() == 0 || getAdapter() == null || getAdapter().getItemCount() == 0) return false;
        for (View view : views) if (view.getId() == R.id.recycler) view.requestFocus();
        super.scrollToPosition(0);
        showHeader();
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touching = true;
                // 双保险：
                // 1) 自己退出触摸焦点
                // 2) 禁止子 View 抢焦点 —— 子 View 一抢，父容器就会滚动去「让焦点可见」，
                //    这正是列表被拉回第一行的直接原因
                setFocusableInTouchMode(false);
                setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touching = false;
                setFocusableInTouchMode(true);
                setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
                // 手指抬起后，把「选中位置」同步到当前第一个可见项。
                // 否则选中位置还停在 0，一旦重布局/按方向键就会跳回顶部（影视仓 TvRecyclerView 的做法）
                post(this::syncSelectedToVisible);
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void syncSelectedToVisible() {
        if (getChildCount() == 0) return;
        int first = getChildAdapterPosition(getChildAt(0));
        if (first < 0 || first == getSelectedPosition()) return;
        setSelectedPosition(first);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        // 手指拖动期间不跟随焦点滚动，否则列表会被拉回第一行
        if (touching) return;
        super.requestChildFocus(child, focused);
    }

    @Override
    public void scrollToPosition(int position) {
        android.util.Log.d("ScrollDbg", "scrollToPosition(" + position + ") touching=" + touching
                + "\n" + android.util.Log.getStackTraceString(new Throwable()));
        // 手指拖动期间，忽略任何程序性滚动（焦点跟随 / 数据刷新都会走到这里）
        if (touching) return;
        super.scrollToPosition(position);
    }

    @Override
    public void smoothScrollToPosition(int position) {
        if (touching) return;
        super.smoothScrollToPosition(position);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        android.util.Log.d("ScrollDbg", "onScrollChanged t=" + t + " old=" + oldt + " touching=" + touching
                + "\n" + android.util.Log.getStackTraceString(new Throwable()));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        android.util.Log.d("ScrollDbg", "onTouch " + e.getActionMasked() + " y=" + e.getY());
        return super.onTouchEvent(e);
    }
}
