package android.support.constraint;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.util.AttributeSet;
import android.view.View;

public class Group extends ConstraintHelper {
	public Group(Context context) {
		super(context);
	}

	public Group(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Group(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	protected void init(AttributeSet attrs) {
		super.init(attrs);
		this.mUseViewMeasure = false;
	}

	public void updatePreLayout(ConstraintLayout container) {
		int visibility = getVisibility();
		float elevation = 0.0F;
		if (Build.VERSION.SDK_INT >= 21) {
			elevation = getElevation();
		}
		for (int i = 0; i < this.mCount; i++) {
			int id = this.mIds[i];
			View view = container.getViewById(id);
			if (view != null) {
				view.setVisibility(visibility);
				if ((elevation > 0.0F) && (Build.VERSION.SDK_INT >= 21))
					view.setElevation(elevation);
			}
		}
	}

	public void updatePostLayout(ConstraintLayout container) {
		ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
		params.widget.setWidth(0);
		params.widget.setHeight(0);
	}
}