package com.owen.tvrecyclerview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.owen.tvrecyclerview.BaseLayoutManager;
import com.owen.tvrecyclerview.R;
import com.owen.tvrecyclerview.TwoWayLayoutManager;
import com.owen.tvrecyclerview.b;

/* loaded from: C:\Users\Administrator\WorkBuddy\2026-10-22-11-10-01\ysc\dex\classes3.dex */
public class GridLayoutManager extends BaseLayoutManager {

    /* renamed from: o, reason: collision with root package name */
    public int f399o;

    /* renamed from: p, reason: collision with root package name */
    public int f400p;

    public GridLayoutManager(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public void G(b.a aVar, int i4) {
        int laneCount = i4 % getLaneCount();
        aVar.a = laneCount;
        aVar.b = laneCount;
    }

    public void L(RecyclerView.Recycler recycler, int i4, int i5) {
        b lanes = getLanes();
        lanes.f(i5);
        TwoWayLayoutManager.b bVar = TwoWayLayoutManager.b.END;
        b.a aVar = ((BaseLayoutManager) this).n;
        G(aVar, i4);
        int i6 = aVar.a;
        if (i6 == 0) {
            return;
        }
        View viewForPosition = recycler.getViewForPosition(i4);
        x(viewForPosition, bVar);
        int decoratedMeasuredHeight = ((TwoWayLayoutManager) this).b ? getDecoratedMeasuredHeight(viewForPosition) : getDecoratedMeasuredWidth(viewForPosition);
        for (int i7 = i6 - 1; i7 >= 0; i7--) {
            Rect rect = lanes.b[i7];
            int i8 = 0;
            boolean z3 = lanes.a;
            int i9 = z3 ? 0 : decoratedMeasuredHeight;
            if (z3) {
                i8 = decoratedMeasuredHeight;
            }
            rect.offset(i9, i8);
            lanes.d();
        }
    }

    public int getLaneCount() {
        return ((TwoWayLayoutManager) this).b ? this.f399o : this.f400p;
    }

    public int getNumColumns() {
        return this.f399o;
    }

    public int getNumRows() {
        return this.f400p;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void setNumColumns(int i4) {
        if (this.f399o == i4) {
            return;
        }
        this.f399o = i4;
        if (((TwoWayLayoutManager) this).b) {
            requestLayout();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void setNumRows(int i4) {
        if (this.f400p == i4) {
            return;
        }
        this.f400p = i4;
        if (((TwoWayLayoutManager) this).b) {
            return;
        }
        requestLayout();
    }

    public GridLayoutManager(Context context, AttributeSet attributeSet, int i4) {
        this(context, attributeSet, i4, 2, 2);
    }

    public GridLayoutManager(Context context, AttributeSet attributeSet, int i4, int i5, int i6) {
        super(context, attributeSet, i4);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TvRecyclerView, i4, 0);
        this.f399o = Math.max(1, obtainStyledAttributes.getInt(R.styleable.TvRecyclerView_tv_numColumns, i5));
        this.f400p = Math.max(1, obtainStyledAttributes.getInt(R.styleable.TvRecyclerView_tv_numRows, i6));
        obtainStyledAttributes.recycle();
    }
}
