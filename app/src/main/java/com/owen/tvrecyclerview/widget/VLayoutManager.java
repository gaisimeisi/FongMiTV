package com.owen.tvrecyclerview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.owen.tvrecyclerview.R;

/* loaded from: C:\Users\Administrator\WorkBuddy\2026-10-22-11-10-01\ysc\dex\classes3.dex */
public class VLayoutManager extends VirtualLayoutManager {
    public VLayoutManager(@NonNull Context context) {
        super(context);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public final boolean requestChildRectangleOnScreen(RecyclerView recyclerView, View view, Rect rect, boolean z3, boolean z4) {
        return recyclerView instanceof TvRecyclerView ? recyclerView.requestChildRectangleOnScreen(view, rect, z3) : super.requestChildRectangleOnScreen(recyclerView, view, rect, z3, z4);
    }

    public VLayoutManager(Context context, AttributeSet attributeSet, int i4) {
        super(context);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TvRecyclerView, i4, 0);
        int i5 = obtainStyledAttributes.getInt(R.styleable.TvRecyclerView_android_orientation, 1);
        obtainStyledAttributes.recycle();
        setOrientation(i5);
    }
}
