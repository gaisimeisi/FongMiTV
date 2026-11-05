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
        // TV 上第一次点击通常只把焦点给控件，系统并不会自动弹软键盘，
        // 所以用户得点第二次才出现输入法。
        // 这里在 DOWN 时主动抢焦点 + 把光标移到末尾 + 主动 showSoftInput，一次点击即可输入。
        if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            if (!hasFocus()) requestFocus();
            if (getText() != null) setSelection(getText().length());
            showKeyboard();
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 点击后把输入态准备好。
     * 搜索页用的是自定义键盘（CustomKeyboard，一直在下方显示），不需要系统输入法，
     * 所以这里做两件事：确保焦点和光标到位（第一次点击就要能打字），
     * 同时把可能弹出来的系统输入法收掉，否则会盖住自定义键盘。
     */
    private void showKeyboard() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!hasFocus()) requestFocus();
            if (getText() != null) setSelection(getText().length());
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }, 100);
    }
}
