package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

/**
 * 解决「详情页上下拖动时被拉回第一行」的问题。
 *
 * <p>根因：内部是多个 leanback {@code HorizontalGridView}，触摸拖拽时子 View 会 requestFocus，
 * 而 {@link NestedScrollView#requestChildFocus} 会主动滚动去「让焦点可见」，
 * 表现就是手指还在拖，页面自己跳回去了。
 *
 * <p>做法：拖动期间屏蔽子 View 的焦点请求，抬手后恢复。
 */
public class CustomNestedScrollView extends NestedScrollView {

    private boolean dragging;

    public CustomNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public CustomNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) dragging = true;
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                break;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        // 拖动过程中忽略焦点请求，避免被拉回；其余情况保持原行为（遥控器正常用）
        if (dragging) return;
        super.requestChildFocus(child, focused);
    }
}
