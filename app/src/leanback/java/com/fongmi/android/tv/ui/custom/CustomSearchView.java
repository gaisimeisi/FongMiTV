package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

public class CustomSearchView extends CustomEditText {

    private Animation flicker;

    public CustomSearchView(@NonNull Context context) {
        super(context);
    }

    public CustomSearchView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        flicker = ResUtil.getAnim(R.anim.flicker);
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) startAnimation(flicker);
        else clearAnimation();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        // TV 上输入框第一次点击往往只是「聚焦」，光标没进去，用户得点第二次。
        // 这里在 DOWN 时主动抢焦点并把光标移到末尾，一次点击即可输入。
        if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            if (!hasFocus()) requestFocus();
            if (getText() != null) setSelection(getText().length());
            return true;
        }
        return super.onTouchEvent(event);
    }
}
