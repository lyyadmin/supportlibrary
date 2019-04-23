package android.support.constraint;

import com.android.support.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.util.AttributeSet;
import android.view.View;

public class Placeholder extends View {
	private int mContentId = -1;
	private View mContent = null;
	private int mEmptyVisibility = 4;

	public Placeholder(Context context) {
		super(context);
		init(null);
	}

	public Placeholder(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public Placeholder(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	public Placeholder(Context context, AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	private void init(AttributeSet attrs) {
		super.setVisibility(this.mEmptyVisibility);
		this.mContentId = -1;
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs,
					R.styleable.ConstraintLayout_placeholder);
			int N = a.getIndexCount();
			for (int i = 0; i < N; i++) {
				int attr = a.getIndex(i);
				if (attr == R.styleable.ConstraintLayout_placeholder_content) {
					this.mContentId = a.getResourceId(attr, this.mContentId);
				} else if (attr == R.styleable.ConstraintLayout_placeholder_emptyVisibility)
					this.mEmptyVisibility = a.getInt(attr,
							this.mEmptyVisibility);
			}
		}
	}

	public void setEmptyVisibility(int visibility) {
		this.mEmptyVisibility = visibility;
	}

	public int getEmptyVisibility() {
		return this.mEmptyVisibility;
	}

	public View getContent() {
		return this.mContent;
	}

	public void onDraw(Canvas canvas) {
		if (isInEditMode()) {
			canvas.drawRGB(223, 223, 223);
			Paint paint = new Paint();
			paint.setARGB(255, 210, 210, 210);
			paint.setTextAlign(Paint.Align.CENTER);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, 0));

			Rect r = new Rect();
			canvas.getClipBounds(r);
			paint.setTextSize(r.height());
			int cHeight = r.height();
			int cWidth = r.width();
			paint.setTextAlign(Paint.Align.LEFT);
			String text = "?";
			paint.getTextBounds(text, 0, text.length(), r);
			float x = cWidth / 2.0F - r.width() / 2.0F - r.left;
			float y = cHeight / 2.0F + r.height() / 2.0F - r.bottom;
			canvas.drawText(text, x, y, paint);
		}
	}

	public void updatePreLayout(ConstraintLayout container) {
		if ((this.mContentId == -1) && (!isInEditMode())) {
			setVisibility(this.mEmptyVisibility);
		}

		this.mContent = container.findViewById(this.mContentId);
		if (this.mContent != null) {
			ConstraintLayout.LayoutParams layoutParamsContent = (ConstraintLayout.LayoutParams) this.mContent
					.getLayoutParams();
			layoutParamsContent.isInPlaceholder = true;
			this.mContent.setVisibility(0);
			setVisibility(0);
		}
	}

	public void setContentId(int id) {
		if (this.mContentId == id) {
			return;
		}
		if (this.mContent != null) {
			this.mContent.setVisibility(0);

			ConstraintLayout.LayoutParams layoutParamsContent = (ConstraintLayout.LayoutParams) this.mContent
					.getLayoutParams();
			layoutParamsContent.isInPlaceholder = false;
			this.mContent = null;
		}

		this.mContentId = id;
		if (id != -1) {
			View v = ((View) getParent()).findViewById(id);
			if (v != null)
				v.setVisibility(8);
		}
	}

	public void updatePostMeasure(ConstraintLayout container) {
		if (this.mContent == null) {
			return;
		}
		ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) getLayoutParams();

		ConstraintLayout.LayoutParams layoutParamsContent = (ConstraintLayout.LayoutParams) this.mContent
				.getLayoutParams();
		layoutParamsContent.widget.setVisibility(0);
		layoutParams.widget.setWidth(layoutParamsContent.widget.getWidth());
		layoutParams.widget.setHeight(layoutParamsContent.widget.getHeight());
		layoutParamsContent.widget.setVisibility(8);
	}
}