package com.owen.tvrecyclerview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.github.tvbox.osc.ui.activity.t0;
import com.github.tvbox.osc.util.ReleasePhonePolicy;
import com.owen.tvrecyclerview.BaseLayoutManager;
import com.owen.tvrecyclerview.R;
import com.owen.tvrecyclerview.TwoWayLayoutManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import z1.m;

/* loaded from: C:\Users\Administrator\WorkBuddy\2026-10-22-11-10-01\ysc\dex\classes3.dex */
public class TvRecyclerView extends RecyclerView implements View.OnClickListener, View.OnFocusChangeListener {
    public static final Class<?>[] A = {Context.class, AttributeSet.class, Integer.TYPE};
    public static d B;
    public int a;
    public int b;
    public int c;
    public int d;
    public int e;

    /* renamed from: f, reason: collision with root package name */
    public int f415f;

    /* renamed from: g, reason: collision with root package name */
    public boolean f416g;

    /* renamed from: h, reason: collision with root package name */
    public boolean f417h;

    /* renamed from: i, reason: collision with root package name */
    public boolean f418i;

    /* renamed from: j, reason: collision with root package name */
    public boolean f419j;

    /* renamed from: k, reason: collision with root package name */
    public int f420k;

    /* renamed from: l, reason: collision with root package name */
    public int f421l;

    /* renamed from: m, reason: collision with root package name */
    public boolean f422m;

    /* renamed from: n, reason: collision with root package name */
    public boolean f423n;

    /* renamed from: o, reason: collision with root package name */
    public boolean f424o;

    /* renamed from: p, reason: collision with root package name */
    public boolean f425p;

    /* renamed from: q, reason: collision with root package name */
    public boolean f426q;

    /* renamed from: r, reason: collision with root package name */
    public int f427r;

    /* renamed from: s, reason: collision with root package name */
    public f f428s;

    /* renamed from: t, reason: collision with root package name */
    public i3.a f429t;

    /* renamed from: u, reason: collision with root package name */
    public e f430u;

    /* renamed from: v, reason: collision with root package name */
    public final Rect f431v;

    /* renamed from: w, reason: collision with root package name */
    public final c f432w;

    /* renamed from: x, reason: collision with root package name */
    public boolean f433x;

    /* renamed from: y, reason: collision with root package name */
    public final boolean f434y;

    /* renamed from: z, reason: collision with root package name */
    public final Point f435z;

    public static class ISavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<ISavedState> CREATOR = new a();
        public int a;
        public int b;
        public int c;
        public int d;
        public int e;

        /* renamed from: f, reason: collision with root package name */
        public int f436f;

        /* renamed from: g, reason: collision with root package name */
        public int f437g;

        /* renamed from: h, reason: collision with root package name */
        public boolean f438h;

        /* renamed from: i, reason: collision with root package name */
        public boolean f439i;

        /* renamed from: j, reason: collision with root package name */
        public boolean f440j;

        /* renamed from: k, reason: collision with root package name */
        public boolean f441k;

        /* renamed from: l, reason: collision with root package name */
        public Parcelable f442l;

        public class a implements Parcelable.Creator<ISavedState> {
            @Override // android.os.Parcelable.Creator
            public final ISavedState createFromParcel(Parcel parcel) {
                ISavedState iSavedState = new ISavedState(parcel);
                iSavedState.f442l = parcel.readParcelable(RecyclerView.class.getClassLoader());
                iSavedState.a = parcel.readInt();
                iSavedState.b = parcel.readInt();
                iSavedState.d = parcel.readInt();
                iSavedState.c = parcel.readInt();
                iSavedState.e = parcel.readInt();
                iSavedState.f436f = parcel.readInt();
                iSavedState.f437g = parcel.readInt();
                boolean[] zArr = new boolean[4];
                parcel.readBooleanArray(zArr);
                iSavedState.f438h = zArr[0];
                iSavedState.f439i = zArr[1];
                iSavedState.f440j = zArr[2];
                iSavedState.f441k = zArr[3];
                return iSavedState;
            }

            @Override // android.os.Parcelable.Creator
            public final ISavedState[] newArray(int i4) {
                return new ISavedState[i4];
            }
        }

        @Override // android.view.View.BaseSavedState, android.view.AbsSavedState, android.os.Parcelable
        public final void writeToParcel(Parcel parcel, int i4) {
            super.writeToParcel(parcel, i4);
            parcel.writeParcelable(this.f442l, i4);
            parcel.writeInt(this.a);
            parcel.writeInt(this.b);
            parcel.writeInt(this.d);
            parcel.writeInt(this.c);
            parcel.writeInt(this.e);
            parcel.writeInt(this.f436f);
            parcel.writeInt(this.f437g);
            parcel.writeBooleanArray(new boolean[]{this.f438h, this.f439i, this.f440j, this.f441k});
        }
    }

    public class a implements Runnable {
        public final /* synthetic */ boolean a;
        public final /* synthetic */ View b;

        public a(boolean z3, View view) {
            this.a = z3;
            this.b = view;
        }

        @Override // java.lang.Runnable
        public final void run() {
            TvRecyclerView tvRecyclerView = TvRecyclerView.this;
            if (tvRecyclerView.hasFocus()) {
                return;
            }
            if (tvRecyclerView.f418i && !this.a) {
                this.b.setActivated(true);
            }
            if (tvRecyclerView.getOnFocusChangeListener() != null) {
                tvRecyclerView.getOnFocusChangeListener().onFocusChange(tvRecyclerView, false);
            }
        }
    }

    public class b implements Runnable {
        public final /* synthetic */ int a;

        public b(int i4) {
            this.a = i4;
        }

        @Override // java.lang.Runnable
        public final void run() {
            int i4;
            TvRecyclerView tvRecyclerView = TvRecyclerView.this;
            if (tvRecyclerView.getAdapter() == null || (i4 = this.a) < 0 || i4 >= tvRecyclerView.getItemCount()) {
                return;
            }
            tvRecyclerView.f421l = i4;
            View findViewByPosition = tvRecyclerView.getLayoutManager() != null ? tvRecyclerView.getLayoutManager().findViewByPosition(i4) : null;
            if (findViewByPosition != null) {
                if (!tvRecyclerView.hasFocus()) {
                    tvRecyclerView.onFocusChanged(true, 130, null);
                }
                findViewByPosition.requestFocus();
            } else {
                h hVar = tvRecyclerView.new h(tvRecyclerView.getContext(), true, false, tvRecyclerView.e);
                hVar.setTargetPosition(i4);
                tvRecyclerView.getLayoutManager().startSmoothScroll(hVar);
            }
        }
    }

    public class c extends RecyclerView.AdapterDataObserver {
        public c() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
        public final void onChanged() {
            TvRecyclerView.this.f433x = true;
        }
    }

    public interface d {
    }

    public interface e {
        boolean a(int i4);
    }

    public interface f {
        void a(View view, int i4);

        void b(View view, int i4);

        void c(View view, int i4);
    }

    public interface g {
    }

    public class h extends LinearSmoothScroller {
        public final boolean a;
        public final boolean b;
        public int c;

        public class a implements Runnable {
            public final /* synthetic */ int a;

            public a(int i4) {
                this.a = i4;
            }

            @Override // java.lang.Runnable
            public final void run() {
                h hVar = h.this;
                View findViewByPosition = TvRecyclerView.this.getLayoutManager().findViewByPosition(this.a);
                if (findViewByPosition != null) {
                    TvRecyclerView tvRecyclerView = TvRecyclerView.this;
                    if (!tvRecyclerView.hasFocus()) {
                        tvRecyclerView.onFocusChanged(true, 130, null);
                    }
                    findViewByPosition.requestFocus();
                }
            }
        }

        public h(Context context, boolean z3, boolean z4, int i4) {
            super(context);
            this.a = z3;
            this.b = z4;
            this.c = i4;
        }

        @Override // androidx.recyclerview.widget.LinearSmoothScroller
        public final int calculateDtToFit(int i4, int i5, int i6, int i7, int i8) {
            return (i6 - i4) + this.c;
        }

        @Override // androidx.recyclerview.widget.LinearSmoothScroller
        public final int calculateTimeForScrolling(int i4) {
            if (this.b) {
                return super.calculateTimeForScrolling(i4);
            }
            return (int) Math.ceil((4.0f / TvRecyclerView.this.getContext().getResources().getDisplayMetrics().densityDpi) * Math.abs(i4));
        }

        @Override // androidx.recyclerview.widget.LinearSmoothScroller, androidx.recyclerview.widget.RecyclerView.SmoothScroller
        public final void onStop() {
            if (this.a) {
                TvRecyclerView.this.postDelayed(new a(getTargetPosition()), 200L);
            }
            super.onStop();
        }

        @Override // androidx.recyclerview.widget.LinearSmoothScroller, androidx.recyclerview.widget.RecyclerView.SmoothScroller
        public final void onTargetFound(View view, RecyclerView.State state, RecyclerView.SmoothScroller.Action action) {
            int freeHeight;
            int height;
            TvRecyclerView tvRecyclerView = TvRecyclerView.this;
            if (tvRecyclerView.f416g && getLayoutManager() != null && view.getTop() > 0) {
                tvRecyclerView.getDecoratedBoundsWithMargins(view, tvRecyclerView.f431v);
                if (getLayoutManager().canScrollHorizontally()) {
                    freeHeight = tvRecyclerView.getFreeWidth();
                    height = tvRecyclerView.f431v.width();
                } else {
                    freeHeight = tvRecyclerView.getFreeHeight();
                    height = tvRecyclerView.f431v.height();
                }
                this.c = (freeHeight - height) / 2;
            }
            super.onTargetFound(view, state, action);
        }

        @Override // androidx.recyclerview.widget.LinearSmoothScroller
        public final void updateActionForInterimTarget(RecyclerView.SmoothScroller.Action action) {
            action.jumpTo(TvRecyclerView.this.f421l);
        }
    }

    public TvRecyclerView(Context context) {
        this(context, null);
    }

    public static String g(Context context, String str) {
        if (str.charAt(0) == '.') {
            return context.getPackageName() + str;
        }
        if (str.contains(".")) {
            return str;
        }
        return TvRecyclerView.class.getPackage().getName() + '.' + str;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getFreeHeight() {
        return (getHeight() - getPaddingTop()) - getPaddingBottom();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getFreeWidth() {
        return (getWidth() - getPaddingLeft()) - getPaddingRight();
    }

    public static void setLeanbackTouchModeProvider(d dVar) {
        B = dVar;
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup, android.view.View
    public final void addFocusables(ArrayList<View> arrayList, int i4, int i5) {
        if (hasFocus() || !isFocusable()) {
            super.addFocusables(arrayList, i4, i5);
        } else {
            arrayList.add(this);
        }
    }

    @Override // android.view.View
    public final boolean canScrollHorizontally(int i4) {
        if (getLayoutManager() == null || !(getLayoutManager() instanceof TwoWayLayoutManager)) {
            return super.canScrollHorizontally(i4);
        }
        TwoWayLayoutManager layoutManager = getLayoutManager();
        return layoutManager.canScrollHorizontally() && (i4 <= 0 ? layoutManager.getFirstVisiblePosition() != 0 || layoutManager.f < layoutManager.getStartWithPadding() || i4 > 0 : !layoutManager.m(i4));
    }

    @Override // android.view.View
    public final boolean canScrollVertically(int i4) {
        if (getLayoutManager() == null || !(getLayoutManager() instanceof TwoWayLayoutManager)) {
            return super.canScrollVertically(i4);
        }
        TwoWayLayoutManager layoutManager = getLayoutManager();
        return layoutManager.canScrollVertically() && (i4 <= 0 ? layoutManager.getFirstVisiblePosition() != 0 || layoutManager.f < layoutManager.getStartWithPadding() || i4 > 0 : !layoutManager.m(i4));
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup
    public final boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        int i4;
        boolean checkLayoutParams = super.checkLayoutParams(layoutParams);
        if (checkLayoutParams && ((i4 = this.a) >= 0 || this.b >= 0)) {
            int i5 = i4 / 2;
            int i6 = this.b / 2;
            ((RecyclerView.LayoutParams) layoutParams).setMargins(i6, i5, i6, i5);
        }
        return checkLayoutParams;
    }

    @Override // android.view.ViewGroup, android.view.View
    public final boolean dispatchTouchEvent(MotionEvent motionEvent) {
        int childAdapterPosition;
        if (this.f423n && ((motionEvent.getSource() & 4098) == 4098 || (motionEvent.getPointerCount() > 0 && motionEvent.getToolType(0) == 1))) {
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 0) {
                this.f425p = true;
                this.f426q = false;
                View focusedChild = getFocusedChild();
                if (focusedChild == null || (childAdapterPosition = getChildAdapterPosition(focusedChild)) == -1) {
                    int i4 = this.f421l;
                    if (i4 >= 0) {
                        this.f427r = i4;
                    } else {
                        this.f427r = Math.max(getFirstVisibleAndFocusablePosition(), 0);
                    }
                } else {
                    this.f427r = childAdapterPosition;
                }
            } else if (actionMasked == 1 || actionMasked == 3) {
                this.f425p = false;
                this.f426q = true;
                if (getScrollState() == 0) {
                    post(new androidx.core.widget.a(this, 20));
                }
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override // android.view.ViewGroup, android.view.View
    public final boolean dispatchUnhandledMove(View view, int i4) {
        return super.dispatchUnhandledMove(view, i4);
    }

    public final boolean e(View view) {
        int i4;
        int i5;
        int freeHeight;
        int height;
        if (view == null || getLayoutManager() == null) {
            return false;
        }
        boolean z3 = this.f416g;
        Rect rect = this.f431v;
        if (z3) {
            getDecoratedBoundsWithMargins(view, rect);
            if (getLayoutManager().canScrollHorizontally()) {
                freeHeight = getFreeWidth();
                height = rect.width();
            } else {
                freeHeight = getFreeHeight();
                height = rect.height();
            }
            int i6 = (freeHeight - height) / 2;
            this.e = i6;
            this.f415f = i6;
        }
        int i7 = this.e;
        int i8 = this.f415f;
        if (getLayoutManager() != null) {
            getDecoratedBoundsWithMargins(view, rect);
            if (getLayoutManager().canScrollHorizontally()) {
                i4 = f(rect.left - getPaddingLeft(), (getPaddingRight() + rect.right) - getWidth(), i7, i8);
            } else {
                i4 = 0;
            }
            if (getLayoutManager().canScrollVertically()) {
                i5 = f(rect.top - getPaddingTop(), (getPaddingBottom() + rect.bottom) - getHeight(), i7, i8);
            } else {
                i5 = 0;
            }
        } else {
            i4 = 0;
            i5 = 0;
        }
        int[] iArr = {i4, i5};
        int i9 = iArr[0];
        int i10 = iArr[1];
        smoothScrollBy(i9, i10);
        if (i9 != 0 || i10 != 0) {
            return true;
        }
        postInvalidate();
        return false;
    }

    public final int f(int i4, int i5, int i6, int i7) {
        canScrollHorizontally(-1);
        canScrollVertically(-1);
        if (i5 > 0) {
            return getLastVisiblePosition() != getItemCount() - 1 ? i5 + i7 : i5;
        }
        if (i4 < 0) {
            return getFirstVisiblePosition() != 0 ? i4 - i6 : i4;
        }
        if (i4 > 0 && i4 < i6 && (canScrollHorizontally(-1) || canScrollVertically(-1))) {
            return i4 - i6;
        }
        if (Math.abs(i5) <= 0 || Math.abs(i5) >= i7) {
            return 0;
        }
        if (canScrollHorizontally(1) || canScrollVertically(1)) {
            return i7 - Math.abs(i5);
        }
        return 0;
    }

    /* JADX WARN: Code restructure failed: missing block: B:14:0x0046, code lost:
    
        if (java.lang.Math.abs(((getHeight() - r5.getBottom()) - ((android.view.ViewGroup.MarginLayoutParams) r5.getLayoutParams()).bottomMargin) - getPaddingBottom()) <= 2) goto L33;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0083, code lost:
    
        if ((!r1) == false) goto L42;
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x0070, code lost:
    
        if (java.lang.Math.abs(((getWidth() - r5.getRight()) - ((android.view.ViewGroup.MarginLayoutParams) r5.getLayoutParams()).rightMargin) - getPaddingRight()) <= 2) goto L33;
     */
    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup, android.view.ViewParent
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final android.view.View focusSearch(android.view.View focused, int direction) {
        // jadx 未能反编译原始实现（smali 有跨块跳转），退回 RecyclerView 默认焦点查找。
        // 触摸滚动不依赖此方法，功能影响可忽略。
        return super.focusSearch(focused, direction);
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup
    public final int getChildDrawingOrder(int i4, int i5) {
        View focusedChild = getFocusedChild();
        if (focusedChild != null) {
            int indexOfChild = indexOfChild(focusedChild);
            int i6 = i4 - 1;
            if (i5 == i6) {
                return indexOfChild > i5 ? i5 : indexOfChild;
            }
            if (indexOfChild == i5) {
                return i6;
            }
        }
        return i5;
    }

    public int getFirstVisibleAndFocusablePosition() {
        if (getLayoutManager() == null) {
            return -1;
        }
        for (int firstVisiblePosition = getFirstVisiblePosition(); firstVisiblePosition < getChildCount(); firstVisiblePosition++) {
            View findViewByPosition = getLayoutManager().findViewByPosition(firstVisiblePosition);
            if (findViewByPosition != null && findViewByPosition.isFocusable()) {
                return firstVisiblePosition;
            }
        }
        return -1;
    }

    public int getFirstVisiblePosition() {
        if (getChildCount() == 0) {
            return 0;
        }
        return getLayoutManager() instanceof LinearLayoutManager ? ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition() : getChildAdapterPosition(getChildAt(0));
    }

    public int getItemCount() {
        if (getAdapter() != null) {
            return getAdapter().getItemCount();
        }
        return 0;
    }

    public int getLastVisibleAndFocusablePosition() {
        int firstVisiblePosition = getFirstVisiblePosition();
        if (getLayoutManager() == null) {
            return -1;
        }
        for (int lastVisiblePosition = getLastVisiblePosition(); lastVisiblePosition >= firstVisiblePosition; lastVisiblePosition--) {
            View findViewByPosition = getLayoutManager().findViewByPosition(lastVisiblePosition);
            if (findViewByPosition != null && findViewByPosition.isFocusable()) {
                return lastVisiblePosition;
            }
        }
        return -1;
    }

    public int getLastVisiblePosition() {
        int childCount = getChildCount();
        if (childCount == 0) {
            return 0;
        }
        return getLayoutManager() instanceof LinearLayoutManager ? ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition() : getChildAdapterPosition(getChildAt(childCount - 1));
    }

    public int getLoadMoreBeforehandCount() {
        return this.f420k;
    }

    public int getSelectedItemOffsetEnd() {
        return this.f415f;
    }

    public int getSelectedItemOffsetStart() {
        return this.e;
    }

    public int getSelectedPosition() {
        return this.f421l;
    }

    public f getmOnItemListener() {
        return this.f428s;
    }

    public final void h(RecyclerView.Adapter adapter) {
        RecyclerView.Adapter adapter2 = getAdapter();
        c cVar = this.f432w;
        if (adapter2 != null) {
            adapter2.unregisterAdapterDataObserver(cVar);
            this.f433x = true;
        }
        adapter.registerAdapterDataObserver(cVar);
        View childAt = getChildAt(0);
        if (childAt == null || adapter2 == null || !(getLayoutManager() instanceof BaseLayoutManager)) {
            this.f421l = getFirstVisibleAndFocusablePosition();
            return;
        }
        this.f422m = hasFocus();
        int decoratedTop = (getLayoutManager().canScrollVertically() ? getLayoutManager().getDecoratedTop(childAt) : getLayoutManager().getDecoratedLeft(childAt)) - (getLayoutManager().canScrollVertically() ? getPaddingTop() : getPaddingLeft());
        scrollBy(decoratedTop, decoratedTop);
    }

    public final void i(int i4, int i5, boolean z3, boolean z4) {
        this.f421l = i4;
        if (i4 == -1) {
            return;
        }
        if (getLayoutManager() != null && i4 > getLayoutManager().getItemCount()) {
            i4 = getLayoutManager().getItemCount() - 1;
        }
        h hVar = new h(getContext(), z3, z4, i5);
        hVar.setTargetPosition(i4);
        getLayoutManager().startSmoothScroll(hVar);
    }

    @Override // android.view.View
    public final boolean isInEditMode() {
        return true;
    }

    public final void j(int i4, int i5) {
        int i6 = this.a;
        if (i6 == i4 && this.b == i5) {
            return;
        }
        this.c = i6;
        int i7 = this.b;
        this.d = i7;
        this.a = i4;
        this.b = i5;
        if (i4 >= 0 || i5 >= 0) {
            int i8 = i4 / 2;
            int i9 = i5 / 2;
            int i10 = i6 / 2;
            int i11 = i7 / 2;
            setPadding((getPaddingLeft() + i11) - i9, (getPaddingTop() + i10) - i8, (getPaddingRight() + i11) - i9, (getPaddingBottom() + i10) - i8);
        }
    }

    public final void k() {
        if (this.f423n) {
            this.f426q = false;
            int i4 = this.f427r;
            if (i4 < 0 && (i4 = this.f421l) < 0) {
                i4 = 0;
            }
            this.f421l = i4;
            this.f427r = i4;
            View findViewByPosition = getLayoutManager() != null ? getLayoutManager().findViewByPosition(i4) : null;
            if (findViewByPosition == null) {
                i(i4, this.e, false, false);
                post(new t0(this, i4, 4));
            } else {
                if (findViewByPosition.isFocusable() && !findViewByPosition.hasFocus()) {
                    findViewByPosition.requestFocus();
                }
                e(findViewByPosition);
            }
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup, android.view.View
    public final void onAttachedToWindow() {
        if (!this.f423n && B != null && ReleasePhonePolicy.enabled(getContext())) {
            this.f423n = true;
        }
        super.onAttachedToWindow();
    }

    @Override // androidx.recyclerview.widget.RecyclerView
    public final void onChildAttachedToWindow(View view) {
        if (this.f424o && view.isFocusable()) {
            view.setFocusableInTouchMode(false);
        } else if (this.f423n && view.isFocusable()) {
            view.setFocusableInTouchMode(true);
        }
        if (view.isClickable() && !ViewCompat.hasOnClickListeners(view)) {
            view.setOnClickListener(this);
            view.setOnLongClickListener(new m(this, 2));
        }
        if (view.isFocusable() && view.getOnFocusChangeListener() == null) {
            view.setOnFocusChangeListener(this);
        }
    }

    @Override // android.view.View.OnClickListener
    public final void onClick(View view) {
        f fVar = this.f428s;
        if (fVar == null || this == view) {
            return;
        }
        fVar.a(view, getChildAdapterPosition(view));
    }

    @Override // android.view.View.OnFocusChangeListener
    public final void onFocusChange(View view, boolean z3) {
        if (view != null) {
            int childAdapterPosition = getChildAdapterPosition(view);
            boolean z4 = view instanceof RecyclerView;
            if (!z4) {
                view.setSelected(z3);
            }
            if (!z3) {
                view.postDelayed(new a(z4, view), 6L);
                f fVar = this.f428s;
                if (fVar == null || z4) {
                    return;
                }
                fVar.c(view, childAdapterPosition);
                return;
            }
            this.f421l = childAdapterPosition;
            if (z4) {
                return;
            }
            if (this.f418i && view.isActivated()) {
                view.setActivated(false);
            }
            f fVar2 = this.f428s;
            if (fVar2 != null) {
                fVar2.b(view, childAdapterPosition);
            }
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup, android.view.View
    public final void onLayout(boolean z3, int i4, int i5, int i6, int i7) {
        System.currentTimeMillis();
        this.f422m = this.f422m || hasFocus();
        boolean z4 = this.f433x;
        if (!this.f434y || z3 || z4) {
            super.onLayout(z3, i4, i5, i6, i7);
            this.f433x = false;
            if (!hasFocus()) {
                int i8 = this.f421l;
                if (i8 < 0) {
                    this.f421l = getFirstVisibleAndFocusablePosition();
                } else if (i8 >= getItemCount()) {
                    this.f421l = getLastVisibleAndFocusablePosition();
                }
                if (this.f422m && getPreserveFocusAfterLayout()) {
                    if (this.f418i || this.f417h) {
                        if (this.f421l < 0) {
                            this.f421l = getFirstVisibleAndFocusablePosition();
                        }
                        setSelection(this.f421l);
                    } else {
                        setSelection(getFirstVisibleAndFocusablePosition());
                    }
                } else if (this.f418i) {
                    setItemActivated(this.f421l);
                }
            }
        } else {
            hasFocus();
        }
        this.f422m = false;
        System.currentTimeMillis();
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.View
    public final void onMeasure(int i4, int i5) {
        System.currentTimeMillis();
        super.onMeasure(i4, i5);
        System.currentTimeMillis();
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup
    public final boolean onRequestFocusInDescendants(int i4, Rect rect) {
        int i5 = this.f421l;
        if (i5 == -1 || !this.f417h) {
            i5 = getFirstVisibleAndFocusablePosition();
        }
        View findViewByPosition = getLayoutManager() != null ? getLayoutManager().findViewByPosition(i5) : null;
        if (findViewByPosition == null) {
            return super.onRequestFocusInDescendants(i4, rect);
        }
        if (getOnFocusChangeListener() != null) {
            getOnFocusChangeListener().onFocusChange(this, true);
        }
        return findViewByPosition.requestFocus(i4, rect);
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.View
    public final void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable != null) {
            try {
                if (parcelable instanceof ISavedState) {
                    ISavedState iSavedState = (ISavedState) parcelable;
                    this.f421l = iSavedState.a;
                    this.a = iSavedState.b;
                    this.b = iSavedState.d;
                    this.c = iSavedState.c;
                    this.d = iSavedState.e;
                    this.e = iSavedState.f436f;
                    this.f415f = iSavedState.f437g;
                    this.f416g = iSavedState.f438h;
                    this.f418i = iSavedState.f439i;
                    this.f419j = iSavedState.f440j;
                    this.f417h = iSavedState.f441k;
                    super.onRestoreInstanceState(iSavedState.f442l);
                } else {
                    super.onRestoreInstanceState(parcelable);
                }
            } catch (Exception unused) {
            }
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.View
    public final Parcelable onSaveInstanceState() {
        AbsSavedState absSavedState = (RecyclerView.SavedState) super.onSaveInstanceState();
        ISavedState iSavedState = new ISavedState(absSavedState != null ? absSavedState.getSuperState() : null);
        iSavedState.f442l = absSavedState;
        iSavedState.a = this.f421l;
        iSavedState.b = this.a;
        iSavedState.d = this.b;
        iSavedState.c = this.c;
        iSavedState.e = this.d;
        iSavedState.f436f = this.e;
        iSavedState.f437g = this.f415f;
        iSavedState.f438h = this.f416g;
        iSavedState.f439i = this.f418i;
        iSavedState.f440j = this.f419j;
        iSavedState.f441k = this.f417h;
        return iSavedState;
    }

    @Override // androidx.recyclerview.widget.RecyclerView
    public final void onScrollStateChanged(int i4) {
        if (i4 == 0) {
            setTag(null);
            if (this.f423n && this.f426q) {
                k();
            }
        }
        super.onScrollStateChanged(i4);
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup, android.view.ViewParent
    public final boolean requestChildRectangleOnScreen(View view, Rect rect, boolean z3) {
        if (this.f423n && this.f425p) {
            return false;
        }
        return e(view);
    }

    @Override // androidx.recyclerview.widget.RecyclerView
    public final void scrollToPosition(int i4) {
        i(i4, this.e, false, false);
    }

    @Override // androidx.recyclerview.widget.RecyclerView
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (adapter == null) {
            return;
        }
        h(adapter);
        super.setAdapter(adapter);
    }

    public void setContentSingleTapMode(boolean z3) {
        this.f424o = z3;
        if (z3) {
            setFocusableInTouchMode(false);
            int childCount = getChildCount();
            for (int i4 = 0; i4 < childCount; i4++) {
                View childAt = getChildAt(i4);
                if (childAt != null && childAt.isFocusable()) {
                    childAt.setFocusableInTouchMode(false);
                }
            }
        }
    }

    public void setHasMoreData(boolean z3) {
        this.f419j = z3;
    }

    public void setItemActivated(int i4) {
        int i5 = this.f421l;
        if (i4 != i5) {
            RecyclerView.ViewHolder findViewHolderForLayoutPosition = findViewHolderForLayoutPosition(i5);
            if (findViewHolderForLayoutPosition != null && findViewHolderForLayoutPosition.itemView.isActivated()) {
                findViewHolderForLayoutPosition.itemView.setActivated(false);
            }
            this.f421l = i4;
        }
        RecyclerView.ViewHolder findViewHolderForLayoutPosition2 = findViewHolderForLayoutPosition(i4);
        if (findViewHolderForLayoutPosition2 == null || findViewHolderForLayoutPosition2.itemView.isActivated()) {
            return;
        }
        findViewHolderForLayoutPosition2.itemView.setActivated(true);
    }

    public void setLeanbackTouchMode(boolean z3) {
        this.f423n = z3;
    }

    public void setLoadMoreBeforehandCount(int i4) {
        this.f420k = i4;
    }

    public void setMemoryFocus(boolean z3) {
        this.f417h = z3;
    }

    public void setMenu(boolean z3) {
        this.f418i = z3;
    }

    public void setOnInBorderKeyEventListener(e eVar) {
        this.f430u = eVar;
    }

    public void setOnItemListener(f fVar) {
        this.f428s = fVar;
    }

    public void setOnItemLongListener(i3.a aVar) {
        this.f429t = aVar;
    }

    public void setOnLoadMoreListener(g gVar) {
    }

    public void setSelectedItemAtCentered(boolean z3) {
        this.f416g = z3;
    }

    public void setSelectedPosition(int i4) {
        this.f421l = i4;
    }

    public void setSelection(int i4) {
        post(new b(i4));
    }

    public void setSelectionWithSmooth(int i4) {
        if (getAdapter() == null || i4 < 0 || i4 >= getItemCount()) {
            return;
        }
        this.f421l = i4;
        h hVar = new h(getContext(), true, true, this.e);
        hVar.setTargetPosition(i4);
        getLayoutManager().startSmoothScroll(hVar);
    }

    @Override // androidx.recyclerview.widget.RecyclerView
    public final void smoothScrollBy(int i4, int i5, Interpolator interpolator) {
        if (i4 == 0 && i5 == 0) {
            setTag(null);
        } else {
            Point point = this.f435z;
            point.set(i4, i5);
            setTag(point);
        }
        super.smoothScrollBy(i4, i5, interpolator);
    }

    @Override // androidx.recyclerview.widget.RecyclerView
    public final void smoothScrollToPosition(int i4) {
        i(i4, this.e, false, true);
    }

    @Override // androidx.recyclerview.widget.RecyclerView
    public final void swapAdapter(RecyclerView.Adapter adapter, boolean z3) {
        if (adapter == null) {
            return;
        }
        h(adapter);
        super.swapAdapter(adapter, z3);
    }

    public TvRecyclerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TvRecyclerView(Context context, AttributeSet attributeSet, int i4) {
        super(context, attributeSet, i4);
        Constructor constructor;
        Object[] objArr;
        this.a = 0;
        this.b = 0;
        this.c = 0;
        this.d = 0;
        this.f419j = true;
        this.f421l = -1;
        this.f422m = false;
        this.f427r = -1;
        this.f431v = new Rect();
        this.f432w = new c();
        this.f433x = true;
        this.f435z = new Point();
        setChildrenDrawingOrderEnabled(true);
        setWillNotDraw(true);
        setHasFixedSize(true);
        setOverScrollMode(2);
        setClipChildren(false);
        setClipToPadding(false);
        setFocusable(true);
        setFocusableInTouchMode(true);
        if (getItemAnimator() != null) {
            ((SimpleItemAnimator) getItemAnimator()).setSupportsChangeAnimations(false);
        }
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TvRecyclerView, i4, 0);
        String string = obtainStyledAttributes.getString(R.styleable.TvRecyclerView_tv_layoutManager);
        if (!TextUtils.isEmpty(string) && string != null) {
            String trim = string.trim();
            if (trim.length() != 0) {
                String g4 = g(context, trim);
                try {
                    Class<? extends U> asSubclass = getClass().getClassLoader().loadClass(g4).asSubclass(RecyclerView.LayoutManager.class);
                    try {
                        constructor = asSubclass.getConstructor(A);
                        objArr = new Object[]{context, attributeSet, Integer.valueOf(i4)};
                    } catch (NoSuchMethodException e4) {
                        try {
                            constructor = asSubclass.getConstructor(new Class[0]);
                            objArr = null;
                        } catch (NoSuchMethodException e5) {
                            e5.initCause(e4);
                            throw new IllegalStateException(attributeSet.getPositionDescription() + ": Error creating LayoutManager " + g4, e5);
                        }
                    }
                    constructor.setAccessible(true);
                    setLayoutManager((RecyclerView.LayoutManager) constructor.newInstance(objArr));
                } catch (ClassCastException e6) {
                    throw new IllegalStateException(attributeSet.getPositionDescription() + ": Class is not a LayoutManager " + g4, e6);
                } catch (ClassNotFoundException e7) {
                    throw new IllegalStateException(attributeSet.getPositionDescription() + ": Unable to find LayoutManager " + g4, e7);
                } catch (IllegalAccessException e8) {
                    throw new IllegalStateException(attributeSet.getPositionDescription() + ": Cannot access non-public constructor " + g4, e8);
                } catch (InstantiationException e9) {
                    throw new IllegalStateException(attributeSet.getPositionDescription() + ": Could not instantiate the LayoutManager: " + g4, e9);
                } catch (InvocationTargetException e10) {
                    throw new IllegalStateException(attributeSet.getPositionDescription() + ": Could not instantiate the LayoutManager: " + g4, e10);
                }
            }
        }
        this.f416g = obtainStyledAttributes.getBoolean(R.styleable.TvRecyclerView_tv_selectedItemIsCentered, false);
        this.f418i = obtainStyledAttributes.getBoolean(R.styleable.TvRecyclerView_tv_isMenu, false);
        this.f417h = obtainStyledAttributes.getBoolean(R.styleable.TvRecyclerView_tv_isMemoryFocus, true);
        this.f420k = obtainStyledAttributes.getInt(R.styleable.TvRecyclerView_tv_loadMoreBeforehandCount, 4);
        this.e = obtainStyledAttributes.getDimensionPixelOffset(R.styleable.TvRecyclerView_tv_selectedItemOffsetStart, 0);
        this.f415f = obtainStyledAttributes.getDimensionPixelOffset(R.styleable.TvRecyclerView_tv_selectedItemOffsetEnd, 0);
        this.f434y = obtainStyledAttributes.getBoolean(R.styleable.TvRecyclerView_tv_optimizeLayout, false);
        this.f423n = obtainStyledAttributes.getBoolean(R.styleable.TvRecyclerView_tv_leanbackTouchMode, false);
        j(obtainStyledAttributes.getDimensionPixelOffset(R.styleable.TvRecyclerView_tv_verticalSpacingWithMargins, 0), obtainStyledAttributes.getDimensionPixelOffset(R.styleable.TvRecyclerView_tv_horizontalSpacingWithMargins, 0));
        obtainStyledAttributes.recycle();
    }
}
