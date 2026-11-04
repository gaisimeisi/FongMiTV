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
public class SpannableGridLayoutManager extends GridLayoutManager {

    /* renamed from: q, reason: collision with root package name */
    public boolean f413q;

    public SpannableGridLayoutManager(Context context) {
        this(context, null);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final BaseLayoutManager.ItemEntry D(View view, TwoWayLayoutManager.b bVar) {
        int position = getPosition(view);
        b.a aVar = ((BaseLayoutManager) this).n;
        aVar.b();
        SpannableItemEntry spannableItemEntry = (SpannableItemEntry) E(position);
        if (spannableItemEntry != null) {
            int i4 = ((BaseLayoutManager.ItemEntry) spannableItemEntry).a;
            int i5 = ((BaseLayoutManager.ItemEntry) spannableItemEntry).b;
            aVar.a = i4;
            aVar.b = i5;
        }
        if (aVar.a()) {
            F(aVar, view, bVar);
        }
        if (spannableItemEntry == null) {
            a aVar2 = (a) view.getLayoutParams();
            spannableItemEntry = new SpannableItemEntry(aVar.a, aVar.b, aVar2.b, aVar2.a);
            com.owen.tvrecyclerview.a aVar3 = ((BaseLayoutManager) this).j;
            if (aVar3 != null) {
                aVar3.a(position);
                aVar3.a[position] = spannableItemEntry;
            }
        } else {
            ((BaseLayoutManager.ItemEntry) spannableItemEntry).a = aVar.a;
            ((BaseLayoutManager.ItemEntry) spannableItemEntry).b = aVar.b;
        }
        return spannableItemEntry;
    }

    public final void F(b.a aVar, View view, TwoWayLayoutManager.b bVar) {
        super.F(aVar, view, bVar);
        if (aVar.a()) {
            getLanes().b(aVar, H(view), bVar);
        }
    }

    @Override // com.owen.tvrecyclerview.widget.GridLayoutManager
    public final void G(b.a aVar, int i4) {
        SpannableItemEntry spannableItemEntry = (SpannableItemEntry) E(i4);
        if (spannableItemEntry == null) {
            aVar.b();
            return;
        }
        int i5 = ((BaseLayoutManager.ItemEntry) spannableItemEntry).a;
        int i6 = ((BaseLayoutManager.ItemEntry) spannableItemEntry).b;
        aVar.a = i5;
        aVar.b = i6;
    }

    public final int H(View view) {
        a aVar = (a) view.getLayoutParams();
        return ((TwoWayLayoutManager) this).b ? aVar.b : aVar.a;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final void K(View view) {
        this.f413q = true;
        a aVar = (a) view.getLayoutParams();
        int width = ((getWidth() - getPaddingLeft()) - getPaddingRight()) - (((int) getLanes().d) * aVar.b);
        a aVar2 = (a) view.getLayoutParams();
        measureChildWithMargins(view, width, ((getHeight() - getPaddingTop()) - getPaddingBottom()) - (((int) getLanes().d) * aVar2.a));
        this.f413q = false;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.owen.tvrecyclerview.widget.GridLayoutManager
    public final void L(RecyclerView.Recycler recycler, int i4, int i5) {
        int i6;
        boolean z3 = ((TwoWayLayoutManager) this).b;
        b lanes = getLanes();
        lanes.f(0);
        int i7 = 0;
        while (true) {
            b.a aVar = ((BaseLayoutManager) this).n;
            if (i7 > i4) {
                Rect rect = lanes.b[aVar.a];
                Rect rect2 = ((BaseLayoutManager) this).m;
                rect2.set(rect);
                lanes.g(TwoWayLayoutManager.b.END);
                lanes.e(i5 - (z3 ? rect2.bottom : rect2.right));
                return;
            }
            SpannableItemEntry spannableItemEntry = (SpannableItemEntry) E(i7);
            if (spannableItemEntry == null) {
                spannableItemEntry = (SpannableItemEntry) D(recycler.getViewForPosition(i7), TwoWayLayoutManager.b.END);
            }
            SpannableItemEntry spannableItemEntry2 = spannableItemEntry;
            int i8 = ((BaseLayoutManager.ItemEntry) spannableItemEntry2).a;
            int i9 = ((BaseLayoutManager.ItemEntry) spannableItemEntry2).b;
            aVar.a = i8;
            aVar.b = i9;
            if (aVar.a()) {
                SpannableItemEntry spannableItemEntry3 = (SpannableItemEntry) E(i7);
                if (spannableItemEntry3 == null) {
                    View childAt = getChildAt(i7 - getFirstVisiblePosition());
                    if (childAt == null) {
                        throw new IllegalStateException(d.d("Could not find span for position ", i7));
                    }
                    i6 = H(childAt);
                } else {
                    i6 = ((TwoWayLayoutManager) this).b ? spannableItemEntry3.d : spannableItemEntry3.e;
                }
                lanes.b(aVar, i6, TwoWayLayoutManager.b.END);
                ((BaseLayoutManager.ItemEntry) spannableItemEntry2).a = aVar.a;
                ((BaseLayoutManager.ItemEntry) spannableItemEntry2).b = aVar.b;
            }
            int i10 = (int) getLanes().d;
            int i11 = spannableItemEntry2.d;
            int i12 = i10 * i11;
            int i13 = (int) getLanes().d;
            int i14 = spannableItemEntry2.e;
            TwoWayLayoutManager.b bVar = TwoWayLayoutManager.b.END;
            lanes.c(((BaseLayoutManager) this).m, i12, i13 * i14, ((BaseLayoutManager) this).n, bVar);
            if (i7 != i4) {
                O(spannableItemEntry2, ((BaseLayoutManager) this).m, ((BaseLayoutManager.ItemEntry) spannableItemEntry2).a, z3 ? i11 : i14, bVar);
            }
            i7++;
        }
    }

    public final boolean canScrollHorizontally() {
        return (((TwoWayLayoutManager) this).b ^ true) && !this.f413q;
    }

    public final boolean canScrollVertically() {
        return ((TwoWayLayoutManager) this).b && !this.f413q;
    }

    public final boolean checkLayoutParams(RecyclerView.LayoutParams layoutParams) {
        int i4;
        int i5;
        super.checkLayoutParams(layoutParams);
        if (((ViewGroup.MarginLayoutParams) layoutParams).width != -1 || ((ViewGroup.MarginLayoutParams) layoutParams).height != -1 || !(layoutParams instanceof a)) {
            return false;
        }
        a aVar = (a) layoutParams;
        return ((TwoWayLayoutManager) this).b ? aVar.a >= 1 && (i5 = aVar.b) >= 1 && i5 <= getLaneCount() : aVar.b >= 1 && (i4 = aVar.a) >= 1 && i4 <= getLaneCount();
    }

    public final RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new a();
    }

    public final RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        a aVar = new a((ViewGroup.MarginLayoutParams) layoutParams);
        ((ViewGroup.MarginLayoutParams) aVar).width = -1;
        ((ViewGroup.MarginLayoutParams) aVar).height = -1;
        if (layoutParams instanceof a) {
            a aVar2 = (a) layoutParams;
            if (((TwoWayLayoutManager) this).b) {
                aVar.b = Math.max(1, Math.min(aVar2.b, getLaneCount()));
                aVar.a = Math.max(1, aVar2.a);
            } else {
                aVar.b = Math.max(1, aVar2.b);
                aVar.a = Math.max(1, Math.min(aVar2.a, getLaneCount()));
            }
        }
        return aVar;
    }

    public SpannableGridLayoutManager(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public static class SpannableItemEntry extends BaseLayoutManager.ItemEntry {
        public static final Parcelable.Creator<SpannableItemEntry> CREATOR = new a();
        public final int d;
        public final int e;

        public class a implements Parcelable.Creator<SpannableItemEntry> {
            @Override // android.os.Parcelable.Creator
            public final SpannableItemEntry createFromParcel(Parcel parcel) {
                return new SpannableItemEntry(parcel);
            }

            @Override // android.os.Parcelable.Creator
            public final SpannableItemEntry[] newArray(int i4) {
                return new SpannableItemEntry[i4];
            }
        }

        public SpannableItemEntry(int i4, int i5, int i6, int i7) {
            super(i4, i5);
            this.d = i6;
            this.e = i7;
        }

        public final void writeToParcel(Parcel parcel, int i4) {
            super.writeToParcel(parcel, i4);
            parcel.writeInt(this.d);
            parcel.writeInt(this.e);
        }

        public SpannableItemEntry(Parcel parcel) {
            super(parcel);
            this.d = parcel.readInt();
            this.e = parcel.readInt();
        }
    }

    public static class a extends RecyclerView.LayoutParams {
        public int a;
        public int b;

        public a() {
            super(-1, -1);
            this.a = 1;
            this.b = 1;
        }

        public a(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TvRecyclerView_SpannableGridViewChild);
            this.b = Math.max(1, obtainStyledAttributes.getInt(R.styleable.TvRecyclerView_SpannableGridViewChild_tv_colSpan, -1));
            this.a = Math.max(1, obtainStyledAttributes.getInt(R.styleable.TvRecyclerView_SpannableGridViewChild_tv_rowSpan, -1));
            obtainStyledAttributes.recycle();
        }

        public a(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            if (marginLayoutParams instanceof a) {
                a aVar = (a) marginLayoutParams;
                this.a = aVar.a;
                this.b = aVar.b;
            } else {
                this.a = 1;
                this.b = 1;
            }
        }
    }

    public SpannableGridLayoutManager(Context context, AttributeSet attributeSet, int i4) {
        super(context, attributeSet, i4, 3, 3);
    }

    public final RecyclerView.LayoutParams generateLayoutParams(Context context, AttributeSet attributeSet) {
        return new a(context, attributeSet);
    }
}
