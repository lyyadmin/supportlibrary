package android.support.constraint;

import com.android.support.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.AttributeSet;

@SuppressLint("NewApi")
public class Barrier extends ConstraintHelper {
	public static final int LEFT = 0;
	public static final int TOP = 2;
	public static final int RIGHT = 1;
	public static final int BOTTOM = 3;
	public static final int START = 5;
	public static final int END = 6;
	private int mIndicatedType = 0;
	private int mResolvedType = 0;
	private android.support.constraint.solver.widgets.Barrier mBarrier;

	public Barrier(Context context) {
		super(context);
		super.setVisibility(8);
	}

	public Barrier(Context context, AttributeSet attrs) {
		super(context, attrs);
		super.setVisibility(8);
	}

	public Barrier(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		super.setVisibility(8);
	}

	public int getType() {
		return this.mIndicatedType;
	}

	public void setType(int type) {
		this.mIndicatedType = type;
		this.mResolvedType = type;
		if (Build.VERSION.SDK_INT < 17) {
			if (this.mIndicatedType == 5)
				this.mResolvedType = 0;
			else if (this.mIndicatedType == 6)
				this.mResolvedType = 1;
		} else {
			Configuration config = getResources().getConfiguration();
			boolean isRtl = 1 == config.getLayoutDirection();
			if (isRtl) {
				if (this.mIndicatedType == 5)
					this.mResolvedType = 1;
				else if (this.mIndicatedType == 6) {
					this.mResolvedType = 0;
				}
			} else if (this.mIndicatedType == 5)
				this.mResolvedType = 0;
			else if (this.mIndicatedType == 6) {
				this.mResolvedType = 1;
			}
		}

		this.mBarrier.setBarrierType(this.mResolvedType);
	}

	protected void init(AttributeSet attrs) {
		super.init(attrs);
		this.mBarrier = new android.support.constraint.solver.widgets.Barrier();
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs,
					R.styleable.ConstraintLayout_Layout);
			int N = a.getIndexCount();
			for (int i = 0; i < N; i++) {
				int attr = a.getIndex(i);
				if (attr == R.styleable.ConstraintLayout_Layout_barrierDirection)
					setType(a.getInt(attr, 0));
				else if (attr == R.styleable.ConstraintLayout_Layout_barrierAllowsGoneWidgets) {
					this.mBarrier.setAllowsGoneWidget(a.getBoolean(attr, true));
				}
			}
		}
		this.mHelperWidget = this.mBarrier;
		validateParams();
	}
}