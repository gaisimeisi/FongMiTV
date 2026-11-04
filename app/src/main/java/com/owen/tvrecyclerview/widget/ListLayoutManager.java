package com.owen.tvrecyclerview.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.recyclerview.widget.RecyclerView;
import com.owen.tvrecyclerview.BaseLayoutManager;
import com.owen.tvrecyclerview.b;

/* loaded from: C:\Users\Administrator\WorkBuddy\2026-10-22-11-10-01\ysc\dex\classes3.dex */
public class ListLayoutManager extends BaseLayoutManager {
    public ListLayoutManager(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public final void G(b.a aVar, int i4) {
        aVar.a = 0;
        aVar.b = 0;
    }

    public final void L(RecyclerView.Recycler recycler, int i4, int i5) {
        getLanes().f(i5);
    }

    public int getLaneCount() {
        return 1;
    }

    public ListLayoutManager(Context context, AttributeSet attributeSet, int i4) {
        super(context, attributeSet, i4);
    }
}
