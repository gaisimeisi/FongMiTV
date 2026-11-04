package com.owen.tvrecyclerview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.d;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.owen.tvrecyclerview.BaseLayoutManager;
import com.owen.tvrecyclerview.R;
import com.owen.tvrecyclerview.TwoWayLayoutManager;
import com.owen.tvrecyclerview.b;

/* loaded from: C:\Users\Administrator\WorkBuddy\2026-10-22-11-10-01\ysc\dex\classes3.dex */
public class StaggeredGridLayoutManager extends GridLayoutManager {
    public StaggeredGridLayoutManager(Context context) {
        this(context, null);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final BaseLayoutManager.ItemEntry C(Rect rect, View view) {
        StaggeredItemEntry staggeredItemEntry = (StaggeredItemEntry) E(getPosition(view));
        if (staggeredItemEntry == null) {
            throw new IllegalStateException("Tried to cache frame on undefined item");
        }
        staggeredItemEntry.e = rect.right - rect.left;
        staggeredItemEntry.f414f = rect.bottom - rect.top;
        return staggeredItemEntry;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final BaseLayoutManager.ItemEntry D(View view, TwoWayLayoutManager.b bVar) {
        int position = getPosition(view);
        b.a aVar = ((BaseLayoutManager) this).n;
        aVar.b();
        StaggeredItemEntry staggeredItemEntry = (StaggeredItemEntry) E(position);
        if (staggeredItemEntry != null) {
            int i4 = ((BaseLayoutManager.ItemEntry) staggeredItemEntry).a;
            int i5 = ((BaseLayoutManager.ItemEntry) staggeredItemEntry).b;
            aVar.a = i4;
            aVar.b = i5;
        }
        if (aVar.a()) {
            F(aVar, view, bVar);
        }
        if (staggeredItemEntry == null) {
            staggeredItemEntry = new StaggeredItemEntry(aVar.a, aVar.b, H(view));
            com.owen.tvrecyclerview.a aVar2 = ((BaseLayoutManager) this).j;
            if (aVar2 != null) {
                aVar2.a(position);
                aVar2.a[position] = staggeredItemEntry;
            }
        } else {
            ((BaseLayoutManager.ItemEntry) staggeredItemEntry).a = aVar.a;
            ((BaseLayoutManager.ItemEntry) staggeredItemEntry).b = aVar.b;
        }
        return staggeredItemEntry;
    }

    public final void F(b.a aVar, View view, TwoWayLayoutManager.b bVar) {
        super.F(aVar, view, bVar);
        if (aVar.a()) {
            getLanes().b(aVar, H(view), bVar);
        }
    }

    @Override // com.owen.tvrecyclerview.widget.GridLayoutManager
    public final void G(b.a aVar, int i4) {
        StaggeredItemEntry staggeredItemEntry = (StaggeredItemEntry) E(i4);
        if (staggeredItemEntry == null) {
            aVar.b();
            return;
        }
        int i5 = ((BaseLayoutManager.ItemEntry) staggeredItemEntry).a;
        int i6 = ((BaseLayoutManager.ItemEntry) staggeredItemEntry).b;
        aVar.a = i5;
        aVar.b = i6;
    }

    public final int H(View view) {
        return ((a) view.getLayoutParams()).a;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.owen.tvrecyclerview.widget.GridLayoutManager
    public final void L(RecyclerView.Recycler recycler, int i4, int i5) {
        StaggeredItemEntry staggeredItemEntry;
        int i6;
        boolean z3 = ((TwoWayLayoutManager) this).b;
        b lanes = getLanes();
        lanes.f(0);
        int i7 = 0;
        while (true) {
            Rect rect = ((BaseLayoutManager) this).m;
            b.a aVar = ((BaseLayoutManager) this).n;
            if (i7 > i4) {
                rect.set(lanes.b[aVar.a]);
                lanes.g(TwoWayLayoutManager.b.END);
                lanes.e(i5 - (z3 ? rect.bottom : rect.right));
                return;
            }
            StaggeredItemEntry staggeredItemEntry2 = (StaggeredItemEntry) E(i7);
            if (staggeredItemEntry2 != null) {
                int i8 = ((BaseLayoutManager.ItemEntry) staggeredItemEntry2).a;
                int i9 = ((BaseLayoutManager.ItemEntry) staggeredItemEntry2).b;
                aVar.a = i8;
                aVar.b = i9;
                if (aVar.a()) {
                    StaggeredItemEntry staggeredItemEntry3 = (StaggeredItemEntry) E(i7);
                    if (staggeredItemEntry3 == null) {
                        View childAt = getChildAt(i7 - getFirstVisiblePosition());
                        if (childAt == null) {
                            throw new IllegalStateException(d.d("Could not find span for position ", i7));
                        }
                        i6 = ((a) childAt.getLayoutParams()).a;
                    } else {
                        i6 = staggeredItemEntry3.d;
                    }
                    lanes.b(aVar, i6, TwoWayLayoutManager.b.END);
                    ((BaseLayoutManager.ItemEntry) staggeredItemEntry2).a = aVar.a;
                    ((BaseLayoutManager.ItemEntry) staggeredItemEntry2).b = aVar.b;
                }
                lanes.c(((BaseLayoutManager) this).m, staggeredItemEntry2.e, staggeredItemEntry2.f414f, ((BaseLayoutManager) this).n, TwoWayLayoutManager.b.END);
                staggeredItemEntry = staggeredItemEntry2;
            } else {
                View viewForPosition = recycler.getViewForPosition(i7);
                TwoWayLayoutManager.b bVar = TwoWayLayoutManager.b.END;
                x(viewForPosition, bVar);
                StaggeredItemEntry staggeredItemEntry4 = (StaggeredItemEntry) E(i7);
                int i10 = ((BaseLayoutManager.ItemEntry) staggeredItemEntry4).a;
                int i11 = ((BaseLayoutManager.ItemEntry) staggeredItemEntry4).b;
                aVar.a = i10;
                aVar.b = i11;
                lanes.c(((BaseLayoutManager) this).m, getDecoratedMeasuredWidth(viewForPosition), getDecoratedMeasuredHeight(viewForPosition), ((BaseLayoutManager) this).n, bVar);
                staggeredItemEntry4.e = rect.right - rect.left;
                staggeredItemEntry4.f414f = rect.bottom - rect.top;
                staggeredItemEntry = staggeredItemEntry4;
            }
            if (i7 != i4) {
                O(staggeredItemEntry, ((BaseLayoutManager) this).m, ((BaseLayoutManager.ItemEntry) staggeredItemEntry).a, staggeredItemEntry.d, TwoWayLayoutManager.b.END);
            }
            i7++;
        }
    }

    public final boolean checkLayoutParams(RecyclerView.LayoutParams layoutParams) {
        boolean checkLayoutParams = super.checkLayoutParams(layoutParams);
        if (!(layoutParams instanceof a)) {
            return checkLayoutParams;
        }
        int i4 = ((a) layoutParams).a;
        return checkLayoutParams & (i4 >= 1 && i4 <= getLaneCount());
    }

    public final RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return ((TwoWayLayoutManager) this).b ? new a(-1, -2) : new a(-2, -1);
    }

    public final RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        a aVar = new a((ViewGroup.MarginLayoutParams) layoutParams);
        if (((TwoWayLayoutManager) this).b) {
            ((ViewGroup.MarginLayoutParams) aVar).width = -1;
            ((ViewGroup.MarginLayoutParams) aVar).height = layoutParams.height;
        } else {
            ((ViewGroup.MarginLayoutParams) aVar).width = layoutParams.width;
            ((ViewGroup.MarginLayoutParams) aVar).height = -1;
        }
        if (layoutParams instanceof a) {
            aVar.a = Math.max(1, Math.min(((a) layoutParams).a, getLaneCount()));
        }
        return aVar;
    }

    public static class StaggeredItemEntry extends BaseLayoutManager.ItemEntry {
        public static final Parcelable.Creator<StaggeredItemEntry> CREATOR = new a();
        public final int d;
        public int e;

        /* renamed from: f, reason: collision with root package name */
        public int f414f;

        public class a implements Parcelable.Creator<StaggeredItemEntry> {
            @Override // android.os.Parcelable.Creator
            public final StaggeredItemEntry createFromParcel(Parcel parcel) {
                return new StaggeredItemEntry(parcel);
            }

            @Override // android.os.Parcelable.Creator
            public final StaggeredItemEntry[] newArray(int i4) {
                return new StaggeredItemEntry[i4];
            }
        }

        public StaggeredItemEntry(int i4, int i5, int i6) {
            super(i4, i5);
            this.d = i6;
        }

        public final void writeToParcel(Parcel parcel, int i4) {
            super.writeToParcel(parcel, i4);
            parcel.writeInt(this.d);
            parcel.writeInt(this.e);
            parcel.writeInt(this.f414f);
        }

        public StaggeredItemEntry(Parcel parcel) {
            super(parcel);
            this.d = parcel.readInt();
            this.e = parcel.readInt();
            this.f414f = parcel.readInt();
        }
    }

    public static class a extends RecyclerView.LayoutParams {
        public int a;

        public a(int i4, int i5) {
            super(i4, i5);
            this.a = 1;
        }

        public a(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TvRecyclerView_StaggeredGridViewChild);
            this.a = Math.max(1, obtainStyledAttributes.getInt(R.styleable.TvRecyclerView_StaggeredGridViewChild_tv_span, -1));
            obtainStyledAttributes.recycle();
        }

        public a(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            if (marginLayoutParams instanceof a) {
                this.a = ((a) marginLayoutParams).a;
            } else {
                this.a = 1;
            }
        }
    }

    public StaggeredGridLayoutManager(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public StaggeredGridLayoutManager(Context context, AttributeSet attributeSet, int i4) {
        super(context, attributeSet, i4, 2, 2);
    }

    public final RecyclerView.LayoutParams generateLayoutParams(Context context, AttributeSet attributeSet) {
        return new a(context, attributeSet);
    }
}
