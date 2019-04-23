package android.support.constraint;

import com.android.support.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class Constraints extends ViewGroup {
	public static final String TAG = "Constraints";
	ConstraintSet myConstraintSet;

	public Constraints(Context context) {
		super(context);
		super.setVisibility(8);
	}

	public Constraints(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
		super.setVisibility(8);
	}

	public Constraints(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
		super.setVisibility(8);
	}

	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(-2, -2);
	}

	private void init(AttributeSet attrs) {
		Log.v("Constraints", " ################# init");
	}

	protected ViewGroup.LayoutParams generateLayoutParams(
			ViewGroup.LayoutParams p) {
		return new ConstraintLayout.LayoutParams(p);
	}

	public ConstraintSet getConstraintSet() {
		if (this.myConstraintSet == null) {
			this.myConstraintSet = new ConstraintSet();
		}

		this.myConstraintSet.clone(this);
		return this.myConstraintSet;
	}

	protected void onLayout(boolean changed, int l, int t, int r, int b) {
	}

	public static class LayoutParams extends ConstraintLayout.LayoutParams {
		public float alpha = 1.0F;
		public boolean applyElevation = false;
		public float elevation = 0.0F;
		public float rotation = 0.0F;
		public float rotationX = 0.0F;
		public float rotationY = 0.0F;
		public float scaleX = 1.0F;
		public float scaleY = 1.0F;
		public float transformPivotX = 0.0F;
		public float transformPivotY = 0.0F;
		public float translationX = 0.0F;
		public float translationY = 0.0F;
		public float translationZ = 0.0F;

		public LayoutParams(int width, int height) {
			super(width,height);
		}

		public LayoutParams(LayoutParams source) {
			super(source);
		}

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c,attrs);
			TypedArray a = c.obtainStyledAttributes(attrs,
					R.styleable.ConstraintSet);
			int N = a.getIndexCount();
			for (int i = 0; i < N; i++) {
				int attr = a.getIndex(i);
				if (attr == R.styleable.ConstraintSet_android_alpha) {
					this.alpha = a.getFloat(attr, this.alpha);
				} else if (attr == R.styleable.ConstraintSet_android_elevation) {
					this.elevation = a.getFloat(attr, this.elevation);
					this.applyElevation = true;
				} else if (attr == R.styleable.ConstraintSet_android_rotationX) {
					this.rotationX = a.getFloat(attr, this.rotationX);
				} else if (attr == R.styleable.ConstraintSet_android_rotationY) {
					this.rotationY = a.getFloat(attr, this.rotationY);
				} else if (attr == R.styleable.ConstraintSet_android_rotation) {
					this.rotation = a.getFloat(attr, this.rotation);
				} else if (attr == R.styleable.ConstraintSet_android_scaleX) {
					this.scaleX = a.getFloat(attr, this.scaleX);
				} else if (attr == R.styleable.ConstraintSet_android_scaleY) {
					this.scaleY = a.getFloat(attr, this.scaleY);
				} else if (attr == R.styleable.ConstraintSet_android_transformPivotX) {
					this.transformPivotX = a.getFloat(attr,
							this.transformPivotX);
				} else if (attr == R.styleable.ConstraintSet_android_transformPivotY) {
					this.transformPivotY = a.getFloat(attr,
							this.transformPivotY);
				} else if (attr == R.styleable.ConstraintSet_android_translationX) {
					this.translationX = a.getFloat(attr, this.translationX);
				} else if (attr == R.styleable.ConstraintSet_android_translationY) {
					this.translationY = a.getFloat(attr, this.translationY);
				} else if (attr == R.styleable.ConstraintSet_android_translationZ) {
					this.translationX = a.getFloat(attr, this.translationZ);
				}
			}
		}
	}
}