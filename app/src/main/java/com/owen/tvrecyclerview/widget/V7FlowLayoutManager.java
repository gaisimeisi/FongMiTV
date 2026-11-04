package com.owen.tvrecyclerview.widget;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.flexbox.FlexboxLayoutManager;

/* loaded from: C:\Users\Administrator\WorkBuddy\2026-10-22-11-10-01\ysc\dex\classes3.dex */
public class V7FlowLayoutManager extends FlexboxLayoutManager {
    @Override // androidx.recyclerview.widget.RecyclerView.LayoutManager
    public final boolean requestChildRectangleOnScreen(RecyclerView recyclerView, View view, Rect rect, boolean z3, boolean z4) {
        return recyclerView instanceof TvRecyclerView ? recyclerView.requestChildRectangleOnScreen(view, rect, z3) : super.requestChildRectangleOnScreen(recyclerView, view, rect, z3, z4);
    }
}
