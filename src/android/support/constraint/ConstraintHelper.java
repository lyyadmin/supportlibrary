package android.support.constraint;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.constraint.solver.widgets.Helper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.android.support.R;

public abstract class ConstraintHelper extends View {
	protected int[] mIds = new int[32];

	protected int mCount = 0;
	protected Context myContext;
	protected Helper mHelperWidget = null;

	protected boolean mUseViewMeasure = false;
	private String mReferenceIds;

	public ConstraintHelper(Context context) {
		super(context);
		this.myContext = context;
		init(null);
	}

	public ConstraintHelper(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.myContext = context;
		init(attrs);
	}

	public ConstraintHelper(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.myContext = context;
		init(attrs);
	}

	protected void init(AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs,
					R.styleable.ConstraintLayout_Layout);
			int N = a.getIndexCount();
			for (int i = 0; i < N; i++) {
				int attr = a.getIndex(i);
				if (attr == R.styleable.ConstraintLayout_Layout_constraint_referenced_ids) {
					this.mReferenceIds = a.getString(attr);
					setIds(this.mReferenceIds);
				}
			}
		}
	}

	public int[] getReferencedIds() {
		return Arrays.copyOf(this.mIds, this.mCount);
	}

	public void setReferencedIds(int[] ids) {
		this.mCount = 0;
		for (int i = 0; i < ids.length; i++)
			setTag(ids[i], null);
	}

	public void setTag(int tag, Object value) {
		if (this.mCount + 1 > this.mIds.length) {
			this.mIds = Arrays.copyOf(this.mIds, this.mIds.length * 2);
		}
		this.mIds[this.mCount] = tag;
		this.mCount += 1;
	}

	public void onDraw(Canvas canvas) {
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (this.mUseViewMeasure)
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		else
			setMeasuredDimension(0, 0);
	}

	public void validateParams() {
		if (this.mHelperWidget == null) {
			return;
		}
		ViewGroup.LayoutParams params = getLayoutParams();
		if ((params instanceof ConstraintLayout.LayoutParams)) {
			ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) params;
			layoutParams.widget = this.mHelperWidget;
		}
	}

	private void addID(String idString) {
		if (idString == null) {
			return;
		}
		if (this.myContext == null) {
			return;
		}
		idString = idString.trim();
		int tag = 0;
		try {
			Class res = R.id.class;
			Field field = res.getField(idString);
			tag = field.getInt(null);
		} catch (Exception localException) {
		}
		if (tag == 0) {
			tag = this.myContext.getResources().getIdentifier(idString, "id",
					this.myContext.getPackageName());
		}
		if ((tag == 0) && (isInEditMode())
				&& ((getParent() instanceof ConstraintLayout))) {
			ConstraintLayout constraintLayout = (ConstraintLayout) getParent();
			Object value = constraintLayout.getDesignInformation(0, idString);
			if ((value != null) && ((value instanceof Integer))) {
				tag = ((Integer) value).intValue();
			}
		}

		if (tag != 0)
			setTag(tag, null);
		else
			Log.w("ConstraintHelper", "Could not find id of \"" + idString
					+ "\"");
	}

	private void setIds(String idList) {
		if (idList == null) {
			return;
		}
		int begin = 0;
		while (true) {
			int end = idList.indexOf(',', begin);
			if (end == -1) {
				addID(idList.substring(begin));
				break;
			}
			addID(idList.substring(begin, end));
			begin = end + 1;
		}
	}

	public void updatePreLayout(ConstraintLayout container) {
		if (isInEditMode()) {
			setIds(this.mReferenceIds);
		}
		if (this.mHelperWidget == null) {
			return;
		}
		this.mHelperWidget.removeAllIds();
		for (int i = 0; i < this.mCount; i++) {
			int id = this.mIds[i];
			View view = container.findViewById(id);
			if (view != null)
				this.mHelperWidget.add(container.getViewWidget(view));
		}
	}

	public void updatePostLayout(ConstraintLayout container) {
	}

	public void updatePostMeasure(ConstraintLayout container) {
	}
}