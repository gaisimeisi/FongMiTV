package com.owen.tvrecyclerview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.d;
import android.support.v4.media.e;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.owen.tvrecyclerview.BaseLayoutManager;
import com.owen.tvrecyclerview.R;
import com.owen.tvrecyclerview.TwoWayLayoutManager;
import com.owen.tvrecyclerview.b;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

/* loaded from: C:\Users\Administrator\WorkBuddy\2026-10-22-11-10-01\ysc\dex\classes3.dex */
public class MetroGridLayoutManager extends GridLayoutManager {

    /* renamed from: w, reason: collision with root package name */
    public static final int[] f401w = {2};

    /* renamed from: q, reason: collision with root package name */
    public final int[] f402q;

    /* renamed from: r, reason: collision with root package name */
    public boolean f403r;

    /* renamed from: s, reason: collision with root package name */
    public int f404s;

    /* renamed from: t, reason: collision with root package name */
    public boolean f405t;

    /* renamed from: u, reason: collision with root package name */
    public ArrayList f406u;

    /* renamed from: v, reason: collision with root package name */
    public int f407v;

    public interface b {
        void a();
    }

    public MetroGridLayoutManager(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final BaseLayoutManager.ItemEntry D(View view, TwoWayLayoutManager.b bVar) {
        int i4;
        int i5;
        int position = getPosition(view);
        b.a aVar = ((BaseLayoutManager) this).n;
        aVar.b();
        MetroItemEntry metroItemEntry = (MetroItemEntry) E(position);
        if (metroItemEntry != null) {
            int i6 = ((BaseLayoutManager.ItemEntry) metroItemEntry).a;
            int i7 = ((BaseLayoutManager.ItemEntry) metroItemEntry).b;
            aVar.a = i6;
            aVar.b = i7;
        }
        if (aVar.a()) {
            F(aVar, view, bVar);
        }
        a aVar2 = (a) view.getLayoutParams();
        if (metroItemEntry == null) {
            Objects.toString(bVar);
            int i8 = 0;
            for (int firstVisiblePosition = getFirstVisiblePosition(); firstVisiblePosition < position; firstVisiblePosition++) {
                MetroItemEntry metroItemEntry2 = (MetroItemEntry) E(firstVisiblePosition);
                if (metroItemEntry2 != null) {
                    if (metroItemEntry2.f409g == aVar2.d) {
                        i8 += (((TwoWayLayoutManager) this).b ? metroItemEntry2.d : metroItemEntry2.e) * metroItemEntry2.f408f;
                        if (i8 > getLaneCount()) {
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            }
            if (((TwoWayLayoutManager) this).b) {
                i4 = aVar2.b;
                i5 = aVar2.c;
            } else {
                i4 = aVar2.a;
                i5 = aVar2.c;
            }
            boolean z3 = (i4 * i5) + i8 <= getLaneCount();
            aVar2.e = z3;
            metroItemEntry = new MetroItemEntry(aVar.a, aVar.b, z3, aVar2.b, aVar2.a, aVar2.c, aVar2.d);
            com.owen.tvrecyclerview.a aVar3 = ((BaseLayoutManager) this).j;
            if (aVar3 != null) {
                aVar3.a(position);
                aVar3.a[position] = metroItemEntry;
            }
        } else {
            ((BaseLayoutManager.ItemEntry) metroItemEntry).a = aVar.a;
            ((BaseLayoutManager.ItemEntry) metroItemEntry).b = aVar.b;
            aVar2.e = metroItemEntry.f410h;
        }
        boolean z4 = aVar2.e;
        getTopDecorationHeight(view);
        return metroItemEntry;
    }

    public final void F(b.a aVar, View view, TwoWayLayoutManager.b bVar) {
        super.F(aVar, view, bVar);
        if (aVar.a()) {
            getLanes().b(aVar, H(view), bVar);
        }
    }

    @Override // com.owen.tvrecyclerview.widget.GridLayoutManager
    public final void G(b.a aVar, int i4) {
        MetroItemEntry metroItemEntry = (MetroItemEntry) E(i4);
        if (metroItemEntry == null) {
            aVar.b();
            return;
        }
        int i5 = ((BaseLayoutManager.ItemEntry) metroItemEntry).a;
        int i6 = ((BaseLayoutManager.ItemEntry) metroItemEntry).b;
        aVar.a = i5;
        aVar.b = i6;
    }

    public final int H(View view) {
        int i4;
        int i5;
        a aVar = (a) view.getLayoutParams();
        if (((TwoWayLayoutManager) this).b) {
            i4 = aVar.b;
            i5 = aVar.c;
        } else {
            i4 = aVar.a;
            i5 = aVar.c;
        }
        return i4 * i5;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final void K(View view) {
        this.f403r = true;
        a aVar = (a) view.getLayoutParams();
        int width = ((getWidth() - getPaddingLeft()) - getPaddingRight()) - ((int) (getLanes().d * (aVar.b * aVar.c)));
        a aVar2 = (a) view.getLayoutParams();
        measureChildWithMargins(view, width, ((getHeight() - getPaddingTop()) - getPaddingBottom()) - P(aVar2.a * aVar2.c));
        this.f403r = false;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.owen.tvrecyclerview.widget.GridLayoutManager
    public final void L(RecyclerView.Recycler recycler, int i4, int i5) {
        int i6;
        boolean z3 = ((TwoWayLayoutManager) this).b;
        com.owen.tvrecyclerview.b lanes = getLanes();
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
            MetroItemEntry metroItemEntry = (MetroItemEntry) E(i7);
            if (metroItemEntry == null) {
                metroItemEntry = (MetroItemEntry) D(recycler.getViewForPosition(i7), TwoWayLayoutManager.b.END);
            }
            MetroItemEntry metroItemEntry2 = metroItemEntry;
            int i8 = ((BaseLayoutManager.ItemEntry) metroItemEntry2).a;
            int i9 = ((BaseLayoutManager.ItemEntry) metroItemEntry2).b;
            aVar.a = i8;
            aVar.b = i9;
            if (aVar.a()) {
                MetroItemEntry metroItemEntry3 = (MetroItemEntry) E(i7);
                if (metroItemEntry3 == null) {
                    View childAt = getChildAt(i7 - getFirstVisiblePosition());
                    if (childAt == null) {
                        throw new IllegalStateException(d.d("Could not find span for position ", i7));
                    }
                    i6 = H(childAt);
                } else {
                    i6 = (((TwoWayLayoutManager) this).b ? metroItemEntry3.d : metroItemEntry3.e) * metroItemEntry3.f408f;
                }
                lanes.b(aVar, i6, TwoWayLayoutManager.b.END);
                ((BaseLayoutManager.ItemEntry) metroItemEntry2).a = aVar.a;
                ((BaseLayoutManager.ItemEntry) metroItemEntry2).b = aVar.b;
            }
            int i10 = metroItemEntry2.d;
            int i11 = metroItemEntry2.f408f;
            int i12 = (int) (getLanes().d * i10 * i11);
            int i13 = metroItemEntry2.e;
            int P = P(i13 * i11);
            TwoWayLayoutManager.b bVar = TwoWayLayoutManager.b.END;
            lanes.c(((BaseLayoutManager) this).m, i12, P, ((BaseLayoutManager) this).n, bVar);
            if (i7 != i4) {
                O(metroItemEntry2, ((BaseLayoutManager) this).m, ((BaseLayoutManager.ItemEntry) metroItemEntry2).a, z3 ? i10 * i11 : i13 * i11, bVar);
            }
            i7++;
        }
    }

    public final int P(int i4) {
        return (int) (getLanes().d * i4);
    }

    public void addOnSectionSelectedListener(b bVar) {
        if (this.f406u == null) {
            this.f406u = new ArrayList();
        }
        this.f406u.add(bVar);
    }

    public final boolean canScrollHorizontally() {
        return (((TwoWayLayoutManager) this).b ^ true) && !this.f403r;
    }

    public final boolean canScrollVertically() {
        return ((TwoWayLayoutManager) this).b && !this.f403r;
    }

    public final boolean checkLayoutParams(RecyclerView.LayoutParams layoutParams) {
        int i4;
        int i5;
        int i6;
        int i7;
        super.checkLayoutParams(layoutParams);
        if (!(layoutParams instanceof a)) {
            return false;
        }
        a aVar = (a) layoutParams;
        int laneCount = getLaneCount() / this.f402q[aVar.d];
        aVar.c = laneCount;
        if (((TwoWayLayoutManager) this).b) {
            ((ViewGroup.MarginLayoutParams) aVar).height = (P(aVar.a * laneCount) - ((ViewGroup.MarginLayoutParams) aVar).topMargin) - ((ViewGroup.MarginLayoutParams) aVar).bottomMargin;
            ((ViewGroup.MarginLayoutParams) aVar).width = -1;
            return aVar.a >= 1 && (i6 = aVar.b) >= 1 && i6 <= getLaneCount() && (i7 = aVar.d) >= 0 && i7 < this.f402q.length;
        }
        ((ViewGroup.MarginLayoutParams) aVar).height = -1;
        ((ViewGroup.MarginLayoutParams) aVar).width = (P(aVar.b * laneCount) - ((ViewGroup.MarginLayoutParams) aVar).leftMargin) - ((ViewGroup.MarginLayoutParams) aVar).rightMargin;
        return aVar.b >= 1 && (i4 = aVar.a) >= 1 && i4 <= getLaneCount() && (i5 = aVar.d) >= 0 && i5 < this.f402q.length;
    }

    public final RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new a();
    }

    public final RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        a aVar = new a((ViewGroup.MarginLayoutParams) layoutParams);
        if (layoutParams instanceof a) {
            a aVar2 = (a) layoutParams;
            aVar.d = Math.max(0, Math.min(aVar2.d, this.f402q.length - 1));
            int laneCount = getLaneCount();
            int[] iArr = this.f402q;
            aVar.c = laneCount / iArr[aVar.d];
            if (((TwoWayLayoutManager) this).b) {
                aVar.b = Math.max(1, Math.min(aVar2.b, iArr[aVar2.d]));
                int max = Math.max(1, aVar2.a);
                aVar.a = max;
                ((ViewGroup.MarginLayoutParams) aVar).height = (P(max * aVar.c) - ((ViewGroup.MarginLayoutParams) aVar).topMargin) - ((ViewGroup.MarginLayoutParams) aVar).bottomMargin;
                ((ViewGroup.MarginLayoutParams) aVar).width = -1;
            } else {
                aVar.b = Math.max(1, aVar2.b);
                aVar.a = Math.max(1, Math.min(aVar2.a, this.f402q[aVar2.d]));
                ((ViewGroup.MarginLayoutParams) aVar).height = -1;
                ((ViewGroup.MarginLayoutParams) aVar).width = (P(aVar.b * aVar.c) - ((ViewGroup.MarginLayoutParams) aVar).leftMargin) - ((ViewGroup.MarginLayoutParams) aVar).rightMargin;
            }
        }
        return aVar;
    }

    public int getSelectedSectionIndex() {
        return this.f404s;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final void onScrollStateChanged(int i4) {
        if (i4 == 0) {
            View findViewByPosition = findViewByPosition(this.f407v);
            if (findViewByPosition != null && !isSmoothScrolling() && !hasFocus()) {
                a aVar = (a) findViewByPosition.getLayoutParams();
                int i5 = this.f404s;
                int i6 = aVar.d;
                if (i5 != i6) {
                    this.f404s = i6;
                    ArrayList arrayList = this.f406u;
                    if (arrayList != null) {
                        Iterator it = arrayList.iterator();
                        while (it.hasNext()) {
                            ((b) it.next()).a();
                        }
                    }
                }
            }
            this.f407v = -1;
        }
    }

    public void removeOnSectionSelectedListener(b bVar) {
        ArrayList arrayList = this.f406u;
        if (arrayList != null) {
            arrayList.remove(bVar);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final boolean requestChildRectangleOnScreen(RecyclerView recyclerView, View view, Rect rect, boolean z3, boolean z4) {
        boolean z5;
        int width;
        int selectedItemOffsetEnd;
        int paddingLeft;
        int selectedItemOffsetStart;
        int i4;
        a aVar = (a) view.getLayoutParams();
        int i5 = this.f404s;
        int i6 = aVar.d;
        if (i5 != i6) {
            TwoWayLayoutManager.b bVar = i5 < i6 ? TwoWayLayoutManager.b.END : TwoWayLayoutManager.b.START;
            TwoWayLayoutManager.b bVar2 = TwoWayLayoutManager.b.END;
            boolean z6 = bVar == bVar2 ? aVar.f412g : aVar.f411f;
            if (this.f405t && z6 && (recyclerView instanceof TvRecyclerView)) {
                Rect rect2 = ((BaseLayoutManager) this).m;
                getDecoratedBoundsWithMargins(view, rect2);
                TvRecyclerView tvRecyclerView = (TvRecyclerView) recyclerView;
                if (((TwoWayLayoutManager) this).b) {
                    if (bVar == bVar2) {
                        paddingLeft = rect2.top - getPaddingTop();
                        selectedItemOffsetStart = tvRecyclerView.getSelectedItemOffsetStart();
                        i4 = paddingLeft - selectedItemOffsetStart;
                    } else {
                        width = (tvRecyclerView.getHeight() - rect2.bottom) - getPaddingBottom();
                        selectedItemOffsetEnd = tvRecyclerView.getSelectedItemOffsetEnd();
                        i4 = -(width - selectedItemOffsetEnd);
                    }
                } else if (bVar == bVar2) {
                    paddingLeft = rect2.left - getPaddingLeft();
                    selectedItemOffsetStart = tvRecyclerView.getSelectedItemOffsetStart();
                    i4 = paddingLeft - selectedItemOffsetStart;
                } else {
                    width = (tvRecyclerView.getWidth() - rect2.right) - getPaddingRight();
                    selectedItemOffsetEnd = tvRecyclerView.getSelectedItemOffsetEnd();
                    i4 = -(width - selectedItemOffsetEnd);
                }
                int i7 = (((TwoWayLayoutManager) this).b || !ViewCompat.canScrollHorizontally(recyclerView, i4)) ? 0 : i4;
                if (!((TwoWayLayoutManager) this).b || !ViewCompat.canScrollVertically(recyclerView, i4)) {
                    i4 = 0;
                }
                tvRecyclerView.smoothScrollBy(i7, i4);
                z5 = true;
            } else {
                z5 = false;
            }
            this.f404s = aVar.d;
            ArrayList arrayList = this.f406u;
            if (arrayList != null) {
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    ((b) it.next()).a();
                }
            }
            if (z5) {
                return true;
            }
        }
        return super/*com.owen.tvrecyclerview.TwoWayLayoutManager*/.requestChildRectangleOnScreen(recyclerView, view, rect, z3, z4);
    }

    public void setIntelligentScroll(boolean z3) {
        this.f405t = z3;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final void startSmoothScroll(RecyclerView.SmoothScroller smoothScroller) {
        this.f407v = smoothScroller.getTargetPosition();
        super/*androidx.recyclerview.widget.RecyclerView.LayoutManager*/.startSmoothScroll(smoothScroller);
    }

    public final void v(View view, TwoWayLayoutManager.b bVar) {
        super.v(view, bVar);
    }

    public static class a extends RecyclerView.LayoutParams {
        public int a;
        public int b;
        public int c;
        public int d;
        public boolean e;

        /* renamed from: f, reason: collision with root package name */
        public boolean f411f;

        /* renamed from: g, reason: collision with root package name */
        public boolean f412g;

        public a() {
            super(-1, -1);
            p(null);
        }

        public final void p(ViewGroup.LayoutParams layoutParams) {
            if (layoutParams != null && (layoutParams instanceof a)) {
                a aVar = (a) layoutParams;
                this.c = aVar.c;
                this.d = aVar.d;
                this.e = aVar.e;
                this.f411f = aVar.f411f;
                this.f412g = aVar.f412g;
                return;
            }
            this.a = 1;
            this.b = 1;
            this.c = 1;
            this.d = 0;
            this.e = false;
            this.f411f = true;
            this.f412g = true;
        }

        public final String toString() {
            StringBuilder sb = new StringBuilder("[rowSpan=");
            sb.append(this.a);
            sb.append(" colSpan=");
            sb.append(this.b);
            sb.append(" sectionIndex=");
            sb.append(this.d);
            sb.append(" scale=");
            return e.g(sb, this.c, "]");
        }

        public a(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TvRecyclerView_SpannableGridViewChild);
            this.b = Math.max(1, obtainStyledAttributes.getInt(R.styleable.TvRecyclerView_SpannableGridViewChild_tv_colSpan, -1));
            this.a = Math.max(1, obtainStyledAttributes.getInt(R.styleable.TvRecyclerView_SpannableGridViewChild_tv_rowSpan, -1));
            obtainStyledAttributes.recycle();
            p(null);
        }

        public a(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            p(marginLayoutParams);
        }
    }

    public MetroGridLayoutManager(Context context, AttributeSet attributeSet, int i4) {
        super(context, attributeSet, i4);
        int[] iArr;
        this.f404s = 0;
        this.f405t = true;
        this.f407v = -1;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TvRecyclerView, i4, 0);
        String string = obtainStyledAttributes.getString(R.styleable.TvRecyclerView_tv_laneCountsStr);
        this.f405t = obtainStyledAttributes.getBoolean(R.styleable.TvRecyclerView_tv_isIntelligentScroll, true);
        obtainStyledAttributes.recycle();
        if (TextUtils.isEmpty(string)) {
            iArr = f401w;
        } else {
            String[] split = string.split(",");
            int length = split.length;
            iArr = new int[length];
            for (int i5 = 0; i5 < length; i5++) {
                iArr[i5] = Integer.parseInt(split[i5]);
            }
        }
        this.f402q = iArr;
        int i6 = iArr[0];
        for (int i7 = 1; i7 < iArr.length; i7++) {
            int i8 = iArr[i7];
            int i9 = i6 * i8;
            while (true) {
                int i10 = i6 % i8;
                if (i10 != 0) {
                    int i11 = i8;
                    i8 = i10;
                    i6 = i11;
                }
            }
            i6 = i9 / i8;
        }
        setNumColumns(i6);
        setNumRows(i6);
    }

    public static class MetroItemEntry extends BaseLayoutManager.ItemEntry {
        public static final Parcelable.Creator<MetroItemEntry> CREATOR = new a();
        public final int d;
        public final int e;

        /* renamed from: f, reason: collision with root package name */
        public final int f408f;

        /* renamed from: g, reason: collision with root package name */
        public final int f409g;

        /* renamed from: h, reason: collision with root package name */
        public final boolean f410h;

        public class a implements Parcelable.Creator<MetroItemEntry> {
            @Override // android.os.Parcelable.Creator
            public final MetroItemEntry createFromParcel(Parcel parcel) {
                return new MetroItemEntry(parcel);
            }

            @Override // android.os.Parcelable.Creator
            public final MetroItemEntry[] newArray(int i4) {
                return new MetroItemEntry[i4];
            }
        }

        public MetroItemEntry(int i4, int i5, boolean z3, int i6, int i7, int i8, int i9) {
            super(i4, i5);
            this.d = i6;
            this.e = i7;
            this.f408f = i8;
            this.f409g = i9;
            this.f410h = z3;
        }

        public final void writeToParcel(Parcel parcel, int i4) {
            super.writeToParcel(parcel, i4);
            parcel.writeInt(this.d);
            parcel.writeInt(this.e);
            parcel.writeInt(this.f408f);
            parcel.writeInt(this.f409g);
            parcel.writeBooleanArray(new boolean[]{this.f410h});
        }

        public MetroItemEntry(Parcel parcel) {
            super(parcel);
            this.d = parcel.readInt();
            this.e = parcel.readInt();
            this.f408f = parcel.readInt();
            this.f409g = parcel.readInt();
            boolean[] zArr = new boolean[1];
            parcel.readBooleanArray(zArr);
            this.f410h = zArr[0];
        }
    }

    public final RecyclerView.LayoutParams generateLayoutParams(Context context, AttributeSet attributeSet) {
        return new a(context, attributeSet);
    }
}
