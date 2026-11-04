package com.owen.tvrecyclerview.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/* loaded from: C:\Users\Administrator\WorkBuddy\2026-10-22-11-10-01\ysc\dex\classes3.dex */
public class V7LinearLayoutManager extends LinearLayoutManager {
    public int a;

    public V7LinearLayoutManager(Context context) {
        super(context);
        this.a = 500;
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager
    public final int getExtraLayoutSpace(RecyclerView.State state) {
        return this.a;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public final boolean requestChildRectangleOnScreen(RecyclerView recyclerView, View view, Rect rect, boolean z3, boolean z4) {
        return recyclerView instanceof TvRecyclerView ? recyclerView.requestChildRectangleOnScreen(view, rect, z3) : super.requestChildRectangleOnScreen(recyclerView, view, rect, z3, z4);
    }

    public void setExtraLayoutSpace(int i4) {
        this.a = i4;
    }

    public V7LinearLayoutManager(Context context, int i4) {
        super(context, i4, false);
        this.a = 500;
    }

    public V7LinearLayoutManager(Context context, AttributeSet attributeSet, int i4) {
        super(context, attributeSet, i4, 0);
        this.a = 500;
    }
}
