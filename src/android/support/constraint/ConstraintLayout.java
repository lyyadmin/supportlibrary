package android.support.constraint;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Build.VERSION;
import android.support.constraint.solver.Metrics;
import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintAnchor.Strength;
import android.support.constraint.solver.widgets.ConstraintAnchor.Type;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidget.DimensionBehaviour;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import android.support.constraint.solver.widgets.ResolutionAnchor;
import android.support.constraint.solver.widgets.ResolutionDimension;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import java.util.ArrayList;
import java.util.HashMap;
import com.android.support.R;

@SuppressLint("NewApi")
public class ConstraintLayout extends ViewGroup
{
  static final boolean ALLOWS_EMBEDDED = false;
  public static final String VERSION = "ConstraintLayout-1.1.0";
  private static final String TAG = "ConstraintLayout";
  private static final boolean USE_CONSTRAINTS_HELPER = true;
  private static final boolean DEBUG = false;
  SparseArray<View> mChildrenByIds = new SparseArray();

  private ArrayList<ConstraintHelper> mConstraintHelpers = new ArrayList(4);

  private final ArrayList<ConstraintWidget> mVariableDimensionsWidgets = new ArrayList(100);

  ConstraintWidgetContainer mLayoutWidget = new ConstraintWidgetContainer();

  private int mMinWidth = 0;
  private int mMinHeight = 0;
  private int mMaxWidth = 2147483647;
  private int mMaxHeight = 2147483647;

  private boolean mDirtyHierarchy = true;
  private int mOptimizationLevel = 3;
  private ConstraintSet mConstraintSet = null;

  private int mConstraintSetId = -1;

  private HashMap<String, Integer> mDesignIds = new HashMap();

  private int mLastMeasureWidth = -1;
  private int mLastMeasureHeight = -1;
  int mLastMeasureWidthSize = -1;
  int mLastMeasureHeightSize = -1;
  int mLastMeasureWidthMode = 0;
  int mLastMeasureHeightMode = 0;
  public static final int DESIGN_INFO_ID = 0;
  private Metrics mMetrics;

  public void setDesignInformation(int type, Object value1, Object value2)
  {
    if ((type == 0) && ((value1 instanceof String)) && ((value2 instanceof Integer))) {
      if (this.mDesignIds == null) {
        this.mDesignIds = new HashMap();
      }
      String name = (String)value1;
      int index = name.indexOf("/");
      if (index != -1) {
        name = name.substring(index + 1);
      }
      int id = ((Integer)value2).intValue();
      this.mDesignIds.put(name, Integer.valueOf(id));
    }
  }

  public Object getDesignInformation(int type, Object value)
  {
    if ((type == 0) && ((value instanceof String))) {
      String name = (String)value;
      if ((this.mDesignIds != null) && (this.mDesignIds.containsKey(name))) {
        return this.mDesignIds.get(name);
      }
    }
    return null;
  }

  public ConstraintLayout(Context context) {
    super(context);
    init(null);
  }

  public ConstraintLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public ConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  public void setId(int id)
  {
    this.mChildrenByIds.remove(getId());
    super.setId(id);
    this.mChildrenByIds.put(getId(), this);
  }

  private void init(AttributeSet attrs) {
    this.mLayoutWidget.setCompanionWidget(this);
    this.mChildrenByIds.put(getId(), this);
    this.mConstraintSet = null;
    if (attrs != null) {
      TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
      int N = a.getIndexCount();
      for (int i = 0; i < N; i++) {
        int attr = a.getIndex(i);
        if (attr == R.styleable.ConstraintLayout_Layout_android_minWidth) {
          this.mMinWidth = a.getDimensionPixelOffset(attr, this.mMinWidth);
        } else if (attr == R.styleable.ConstraintLayout_Layout_android_minHeight) {
          this.mMinHeight = a.getDimensionPixelOffset(attr, this.mMinHeight);
        } else if (attr == R.styleable.ConstraintLayout_Layout_android_maxWidth) {
          this.mMaxWidth = a.getDimensionPixelOffset(attr, this.mMaxWidth);
        } else if (attr == R.styleable.ConstraintLayout_Layout_android_maxHeight) {
          this.mMaxHeight = a.getDimensionPixelOffset(attr, this.mMaxHeight);
        } else if (attr == R.styleable.ConstraintLayout_Layout_layout_optimizationLevel) {
          this.mOptimizationLevel = a.getInt(attr, this.mOptimizationLevel);
        } else if (attr == R.styleable.ConstraintLayout_Layout_constraintSet) {
          int id = a.getResourceId(attr, 0);
          try {
            this.mConstraintSet = new ConstraintSet();
            this.mConstraintSet.load(getContext(), id);
          } catch (Resources.NotFoundException e) {
            this.mConstraintSet = null;
          }
          this.mConstraintSetId = id;
        }
      }
      a.recycle();
    }
    this.mLayoutWidget.setOptimizationLevel(this.mOptimizationLevel);
  }

  public void addView(View child, int index, ViewGroup.LayoutParams params)
  {
    super.addView(child, index, params);
    if (Build.VERSION.SDK_INT < 14)
      onViewAdded(child);
  }

  public void removeView(View view)
  {
    super.removeView(view);
    if (Build.VERSION.SDK_INT < 14)
      onViewRemoved(view);
  }

  public void onViewAdded(View view)
  {
    if (Build.VERSION.SDK_INT >= 14) {
      super.onViewAdded(view);
    }
    ConstraintWidget widget = getViewWidget(view);
    if (((view instanceof Guideline)) && 
      (!(widget instanceof android.support.constraint.solver.widgets.Guideline))) {
      LayoutParams layoutParams = (LayoutParams)view.getLayoutParams();
      layoutParams.widget = new android.support.constraint.solver.widgets.Guideline();
      layoutParams.isGuideline = true;
      ((android.support.constraint.solver.widgets.Guideline)layoutParams.widget).setOrientation(layoutParams.orientation);
    }

    if ((view instanceof ConstraintHelper)) {
      ConstraintHelper helper = (ConstraintHelper)view;
      helper.validateParams();
      LayoutParams layoutParams = (LayoutParams)view.getLayoutParams();
      layoutParams.isHelper = true;
      if (!this.mConstraintHelpers.contains(helper)) {
        this.mConstraintHelpers.add(helper);
      }
    }
    this.mChildrenByIds.put(view.getId(), view);
    this.mDirtyHierarchy = true;
  }

  public void onViewRemoved(View view)
  {
    if (Build.VERSION.SDK_INT >= 14) {
      super.onViewRemoved(view);
    }
    this.mChildrenByIds.remove(view.getId());
    ConstraintWidget widget = getViewWidget(view);
    this.mLayoutWidget.remove(widget);
    this.mConstraintHelpers.remove(view);
    this.mVariableDimensionsWidgets.remove(widget);
    this.mDirtyHierarchy = true;
  }

  public void setMinWidth(int value)
  {
    if (value == this.mMinWidth) {
      return;
    }
    this.mMinWidth = value;
    requestLayout();
  }

  public void setMinHeight(int value)
  {
    if (value == this.mMinHeight) {
      return;
    }
    this.mMinHeight = value;
    requestLayout();
  }

  public int getMinWidth()
  {
    return this.mMinWidth;
  }

  public int getMinHeight()
  {
    return this.mMinHeight;
  }

  public void setMaxWidth(int value)
  {
    if (value == this.mMaxWidth) {
      return;
    }
    this.mMaxWidth = value;
    requestLayout();
  }

  public void setMaxHeight(int value)
  {
    if (value == this.mMaxHeight) {
      return;
    }
    this.mMaxHeight = value;
    requestLayout();
  }

  public int getMaxWidth()
  {
    return this.mMaxWidth;
  }

  public int getMaxHeight()
  {
    return this.mMaxHeight;
  }

  private void updateHierarchy() {
    int count = getChildCount();

    boolean recompute = false;
    for (int i = 0; i < count; i++) {
      View child = getChildAt(i);
      if (child.isLayoutRequested()) {
        recompute = true;
        break;
      }
    }
    if (recompute) {
      this.mVariableDimensionsWidgets.clear();
      setChildrenConstraints();
    }
  }

  private void setChildrenConstraints() {
    boolean isInEditMode = isInEditMode();

    int count = getChildCount();
    if (isInEditMode)
    {
      for (int i = 0; i < count; i++) {
        View view = getChildAt(i);
        try {
          String IdAsString = getResources().getResourceName(view.getId());
          setDesignInformation(0, IdAsString, Integer.valueOf(view.getId()));
          int slashIndex = IdAsString.indexOf(47);
          if (slashIndex != -1) {
            IdAsString = IdAsString.substring(slashIndex + 1);
          }
          getTargetWidget(view.getId()).setDebugName(IdAsString);
        }
        catch (Resources.NotFoundException localNotFoundException)
        {
        }
      }
    }

    for (int i = 0; i < count; i++) {
      View child = getChildAt(i);
      ConstraintWidget widget = getViewWidget(child);
      if (widget != null)
      {
        widget.reset();
      }
    }
    if (this.mConstraintSetId != -1) {
      for (int i = 0; i < count; i++) {
        View child = getChildAt(i);
        if ((child.getId() == this.mConstraintSetId) && ((child instanceof Constraints))) {
          this.mConstraintSet = ((Constraints)child).getConstraintSet();
        }
      }
    }
    if (this.mConstraintSet != null) {
      this.mConstraintSet.applyToInternal(this);
    }

    this.mLayoutWidget.removeAllChildren();

    int helperCount = this.mConstraintHelpers.size();
    if (helperCount > 0) {
      for (int i = 0; i < helperCount; i++) {
        ConstraintHelper helper = (ConstraintHelper)this.mConstraintHelpers.get(i);
        helper.updatePreLayout(this);
      }
    }

    for (int i = 0; i < count; i++) {
      View child = getChildAt(i);
      if ((child instanceof Placeholder)) {
        ((Placeholder)child).updatePreLayout(this);
      }
    }
    for (int i = 0; i < count; i++) {
      View child = getChildAt(i);
      ConstraintWidget widget = getViewWidget(child);
      if (widget != null)
      {
        LayoutParams layoutParams = (LayoutParams)child.getLayoutParams();
        layoutParams.validate();
        if (layoutParams.helped) {
          layoutParams.helped = false;
        }
        else if (isInEditMode)
        {
          try
          {
            String IdAsString = getResources().getResourceName(child.getId());
            setDesignInformation(0, IdAsString, Integer.valueOf(child.getId()));
            IdAsString = IdAsString.substring(IdAsString.indexOf("id/") + 3);
            getTargetWidget(child.getId()).setDebugName(IdAsString);
          }
          catch (Resources.NotFoundException localNotFoundException1)
          {
          }
        }
        widget.setVisibility(child.getVisibility());
        if (layoutParams.isInPlaceholder) {
          widget.setVisibility(8);
        }
        widget.setCompanionWidget(child);
        this.mLayoutWidget.add(widget);

        if ((!layoutParams.verticalDimensionFixed) || (!layoutParams.horizontalDimensionFixed)) {
          this.mVariableDimensionsWidgets.add(widget);
        }

        if (layoutParams.isGuideline) {
          android.support.constraint.solver.widgets.Guideline guideline = (android.support.constraint.solver.widgets.Guideline)widget;
          int resolvedGuideBegin = layoutParams.resolvedGuideBegin;
          int resolvedGuideEnd = layoutParams.resolvedGuideEnd;
          float resolvedGuidePercent = layoutParams.resolvedGuidePercent;
          if (Build.VERSION.SDK_INT < 17) {
            resolvedGuideBegin = layoutParams.guideBegin;
            resolvedGuideEnd = layoutParams.guideEnd;
            resolvedGuidePercent = layoutParams.guidePercent;
          }
          if (resolvedGuidePercent != -1.0F)
            guideline.setGuidePercent(resolvedGuidePercent);
          else if (resolvedGuideBegin != -1)
            guideline.setGuideBegin(resolvedGuideBegin);
          else if (resolvedGuideEnd != -1)
            guideline.setGuideEnd(resolvedGuideEnd);
        }
        else if ((layoutParams.leftToLeft != -1) || (layoutParams.leftToRight != -1) || (layoutParams.rightToLeft != -1) || (layoutParams.rightToRight != -1) || (layoutParams.startToStart != -1) || (layoutParams.startToEnd != -1) || (layoutParams.endToStart != -1) || (layoutParams.endToEnd != -1) || (layoutParams.topToTop != -1) || (layoutParams.topToBottom != -1) || (layoutParams.bottomToTop != -1) || (layoutParams.bottomToBottom != -1) || (layoutParams.baselineToBaseline != -1) || (layoutParams.editorAbsoluteX != -1) || (layoutParams.editorAbsoluteY != -1) || (layoutParams.circleConstraint != -1) || (layoutParams.width == -1) || (layoutParams.height == -1))
        {
          int resolvedLeftToLeft = layoutParams.resolvedLeftToLeft;
          int resolvedLeftToRight = layoutParams.resolvedLeftToRight;
          int resolvedRightToLeft = layoutParams.resolvedRightToLeft;
          int resolvedRightToRight = layoutParams.resolvedRightToRight;
          int resolveGoneLeftMargin = layoutParams.resolveGoneLeftMargin;
          int resolveGoneRightMargin = layoutParams.resolveGoneRightMargin;
          float resolvedHorizontalBias = layoutParams.resolvedHorizontalBias;

          if (Build.VERSION.SDK_INT < 17)
          {
            resolvedLeftToLeft = layoutParams.leftToLeft;
            resolvedLeftToRight = layoutParams.leftToRight;
            resolvedRightToLeft = layoutParams.rightToLeft;
            resolvedRightToRight = layoutParams.rightToRight;
            resolveGoneLeftMargin = layoutParams.goneLeftMargin;
            resolveGoneRightMargin = layoutParams.goneRightMargin;
            resolvedHorizontalBias = layoutParams.horizontalBias;

            if ((resolvedLeftToLeft == -1) && (resolvedLeftToRight == -1)) {
              if (layoutParams.startToStart != -1)
                resolvedLeftToLeft = layoutParams.startToStart;
              else if (layoutParams.startToEnd != -1) {
                resolvedLeftToRight = layoutParams.startToEnd;
              }
            }
            if ((resolvedRightToLeft == -1) && (resolvedRightToRight == -1)) {
              if (layoutParams.endToStart != -1)
                resolvedRightToLeft = layoutParams.endToStart;
              else if (layoutParams.endToEnd != -1) {
                resolvedRightToRight = layoutParams.endToEnd;
              }
            }

          }

          if (layoutParams.circleConstraint != -1) {
            ConstraintWidget target = getTargetWidget(layoutParams.circleConstraint);
            if (target != null)
              widget.connectCircularConstraint(target, layoutParams.circleAngle, layoutParams.circleRadius);
          }
          else
          {
            if (resolvedLeftToLeft != -1) {
              ConstraintWidget target = getTargetWidget(resolvedLeftToLeft);
              if (target != null) {
                widget.immediateConnect(ConstraintAnchor.Type.LEFT, target, ConstraintAnchor.Type.LEFT, layoutParams.leftMargin, resolveGoneLeftMargin);
              }

            }
            else if (resolvedLeftToRight != -1) {
              ConstraintWidget target = getTargetWidget(resolvedLeftToRight);
              if (target != null) {
                widget.immediateConnect(ConstraintAnchor.Type.LEFT, target, ConstraintAnchor.Type.RIGHT, layoutParams.leftMargin, resolveGoneLeftMargin);
              }

            }

            if (resolvedRightToLeft != -1) {
              ConstraintWidget target = getTargetWidget(resolvedRightToLeft);
              if (target != null) {
                widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target, ConstraintAnchor.Type.LEFT, layoutParams.rightMargin, resolveGoneRightMargin);
              }

            }
            else if (resolvedRightToRight != -1) {
              ConstraintWidget target = getTargetWidget(resolvedRightToRight);
              if (target != null) {
                widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target, ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin, resolveGoneRightMargin);
              }

            }

            if (layoutParams.topToTop != -1) {
              ConstraintWidget target = getTargetWidget(layoutParams.topToTop);
              if (target != null) {
                widget.immediateConnect(ConstraintAnchor.Type.TOP, target, ConstraintAnchor.Type.TOP, layoutParams.topMargin, layoutParams.goneTopMargin);
              }

            }
            else if (layoutParams.topToBottom != -1) {
              ConstraintWidget target = getTargetWidget(layoutParams.topToBottom);
              if (target != null) {
                widget.immediateConnect(ConstraintAnchor.Type.TOP, target, ConstraintAnchor.Type.BOTTOM, layoutParams.topMargin, layoutParams.goneTopMargin);
              }

            }

            if (layoutParams.bottomToTop != -1) {
              ConstraintWidget target = getTargetWidget(layoutParams.bottomToTop);
              if (target != null) {
                widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target, ConstraintAnchor.Type.TOP, layoutParams.bottomMargin, layoutParams.goneBottomMargin);
              }

            }
            else if (layoutParams.bottomToBottom != -1) {
              ConstraintWidget target = getTargetWidget(layoutParams.bottomToBottom);
              if (target != null) {
                widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target, ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin, layoutParams.goneBottomMargin);
              }

            }

            if (layoutParams.baselineToBaseline != -1) {
              View view = (View)this.mChildrenByIds.get(layoutParams.baselineToBaseline);
              ConstraintWidget target = getTargetWidget(layoutParams.baselineToBaseline);
              if ((target != null) && (view != null) && ((view.getLayoutParams() instanceof LayoutParams))) {
                LayoutParams targetParams = (LayoutParams)view.getLayoutParams();
                layoutParams.needsBaseline = true;
                targetParams.needsBaseline = true;
                ConstraintAnchor baseline = widget.getAnchor(ConstraintAnchor.Type.BASELINE);

                ConstraintAnchor targetBaseline = target
                  .getAnchor(ConstraintAnchor.Type.BASELINE);

                baseline.connect(targetBaseline, 0, -1, ConstraintAnchor.Strength.STRONG, 0, true);

                widget.getAnchor(ConstraintAnchor.Type.TOP).reset();
                widget.getAnchor(ConstraintAnchor.Type.BOTTOM).reset();
              }
            }

            if ((resolvedHorizontalBias >= 0.0F) && (resolvedHorizontalBias != 0.5F)) {
              widget.setHorizontalBiasPercent(resolvedHorizontalBias);
            }
            if ((layoutParams.verticalBias >= 0.0F) && (layoutParams.verticalBias != 0.5F)) {
              widget.setVerticalBiasPercent(layoutParams.verticalBias);
            }
          }

          if ((isInEditMode) && ((layoutParams.editorAbsoluteX != -1) || (layoutParams.editorAbsoluteY != -1)))
          {
            widget.setOrigin(layoutParams.editorAbsoluteX, layoutParams.editorAbsoluteY);
          }

          if (!layoutParams.horizontalDimensionFixed) {
            if (layoutParams.width == -1) {
              widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
              widget.getAnchor(ConstraintAnchor.Type.LEFT).mMargin = layoutParams.leftMargin;
              widget.getAnchor(ConstraintAnchor.Type.RIGHT).mMargin = layoutParams.rightMargin;
            } else {
              widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
              widget.setWidth(0);
            }
          } else {
            widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            widget.setWidth(layoutParams.width);
          }
          if (!layoutParams.verticalDimensionFixed) {
            if (layoutParams.height == -1) {
              widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
              widget.getAnchor(ConstraintAnchor.Type.TOP).mMargin = layoutParams.topMargin;
              widget.getAnchor(ConstraintAnchor.Type.BOTTOM).mMargin = layoutParams.bottomMargin;
            } else {
              widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
              widget.setHeight(0);
            }
          } else {
            widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            widget.setHeight(layoutParams.height);
          }

          if (layoutParams.dimensionRatio != null) {
            widget.setDimensionRatio(layoutParams.dimensionRatio);
          }
          widget.setHorizontalWeight(layoutParams.horizontalWeight);
          widget.setVerticalWeight(layoutParams.verticalWeight);
          widget.setHorizontalChainStyle(layoutParams.horizontalChainStyle);
          widget.setVerticalChainStyle(layoutParams.verticalChainStyle);
          widget.setHorizontalMatchStyle(layoutParams.matchConstraintDefaultWidth, layoutParams.matchConstraintMinWidth, layoutParams.matchConstraintMaxWidth, layoutParams.matchConstraintPercentWidth);

          widget.setVerticalMatchStyle(layoutParams.matchConstraintDefaultHeight, layoutParams.matchConstraintMinHeight, layoutParams.matchConstraintMaxHeight, layoutParams.matchConstraintPercentHeight);
        }
      }
    }
  }

  private final ConstraintWidget getTargetWidget(int id)
  {
    if (id == 0) {
      return this.mLayoutWidget;
    }
    View view = (View)this.mChildrenByIds.get(id);
    if (view == this) {
      return this.mLayoutWidget;
    }
    return view == null ? null : ((LayoutParams)view.getLayoutParams()).widget;
  }

  public final ConstraintWidget getViewWidget(View view)
  {
    if (view == this) {
      return this.mLayoutWidget;
    }
    return view == null ? null : ((LayoutParams)view.getLayoutParams()).widget;
  }

  private void internalMeasureChildren(int parentWidthSpec, int parentHeightSpec) {
    int heightPadding = getPaddingTop() + getPaddingBottom();
    int widthPadding = getPaddingLeft() + getPaddingRight();

    int widgetsCount = getChildCount();
    for (int i = 0; i < widgetsCount; i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != 8)
      {
        LayoutParams params = (LayoutParams)child.getLayoutParams();
        ConstraintWidget widget = params.widget;
        if ((!params.isGuideline) && (!params.isHelper))
        {
          widget.setVisibility(child.getVisibility());

          int width = params.width;
          int height = params.height;

          boolean doMeasure = (params.horizontalDimensionFixed) || (params.verticalDimensionFixed) || ((!params.horizontalDimensionFixed) && (params.matchConstraintDefaultWidth == 1)) || (params.width == -1) || ((!params.verticalDimensionFixed) && ((params.matchConstraintDefaultHeight == 1) || (params.height == -1)));

          boolean didWrapMeasureWidth = false;
          boolean didWrapMeasureHeight = false;

          if (doMeasure)
          {
            int childWidthMeasureSpec;
            if (width == 0) {
              childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec, widthPadding, -2);

              didWrapMeasureWidth = true;
            }
            else
            {
              if (width == -1) {
                childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec, widthPadding, -1);
              }
              else {
                if (width == -2) {
                  didWrapMeasureWidth = true;
                }
                childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec, widthPadding, width);
              }
            }
            int childHeightMeasureSpec;
            if (height == 0) {
              childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, -2);

              didWrapMeasureHeight = true;
            }
            else
            {
              if (height == -1) {
                childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, -1);
              }
              else {
                if (height == -2) {
                  didWrapMeasureHeight = true;
                }
                childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, height);
              }
            }
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            if (this.mMetrics != null) {
              this.mMetrics.measures += 1L;
            }

            widget.setWidthWrapContent(width == -2);
            widget.setHeightWrapContent(height == -2);
            width = child.getMeasuredWidth();
            height = child.getMeasuredHeight();
          }

          widget.setWidth(width);
          widget.setHeight(height);

          if (didWrapMeasureWidth) {
            widget.setWrapWidth(width);
          }
          if (didWrapMeasureHeight) {
            widget.setWrapHeight(height);
          }

          if (params.needsBaseline) {
            int baseline = child.getBaseline();
            if (baseline != -1)
              widget.setBaselineDistance(baseline); 
          }
        }
      }
    }
  }

  private void updatePostMeasures() { int widgetsCount = getChildCount();
    for (int i = 0; i < widgetsCount; i++) {
      View child = getChildAt(i);
      if ((child instanceof Placeholder)) {
        ((Placeholder)child).updatePostMeasure(this);
      }
    }

    int helperCount = this.mConstraintHelpers.size();
    if (helperCount > 0)
      for (int i = 0; i < helperCount; i++) {
        ConstraintHelper helper = (ConstraintHelper)this.mConstraintHelpers.get(i);
        helper.updatePostMeasure(this);
      }
  }

  private void internalMeasureDimensions(int parentWidthSpec, int parentHeightSpec)
  {
    int heightPadding = getPaddingTop() + getPaddingBottom();
    int widthPadding = getPaddingLeft() + getPaddingRight();

    int widgetsCount = getChildCount();
    for (int i = 0; i < widgetsCount; i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != 8)
      {
        LayoutParams params = (LayoutParams)child.getLayoutParams();
        ConstraintWidget widget = params.widget;
        if ((!params.isGuideline) && (!params.isHelper))
        {
          widget.setVisibility(child.getVisibility());

          int width = params.width;
          int height = params.height;

          if ((width == 0) || (height == 0)) {
            widget.getResolutionWidth().invalidate();
            widget.getResolutionHeight().invalidate();
          }
          else
          {
            boolean didWrapMeasureWidth = false;
            boolean didWrapMeasureHeight = false;

            if (width == -2) {
              didWrapMeasureWidth = true;
            }
            int childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec, widthPadding, width);

            if (height == -2) {
              didWrapMeasureHeight = true;
            }
            int childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, height);

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            if (this.mMetrics != null) {
              this.mMetrics.measures += 1L;
            }

            widget.setWidthWrapContent(width == -2);
            widget.setHeightWrapContent(height == -2);
            width = child.getMeasuredWidth();
            height = child.getMeasuredHeight();

            widget.setWidth(width);
            widget.setHeight(height);

            if (didWrapMeasureWidth) {
              widget.setWrapWidth(width);
            }
            if (didWrapMeasureHeight) {
              widget.setWrapHeight(height);
            }

            if (params.needsBaseline) {
              int baseline = child.getBaseline();
              if (baseline != -1) {
                widget.setBaselineDistance(baseline);
              }
            }

            if ((params.horizontalDimensionFixed) && (params.verticalDimensionFixed)) {
              widget.getResolutionWidth().resolve(width);
              widget.getResolutionHeight().resolve(height);
            }
          }
        }
      }
    }
    this.mLayoutWidget.solveGraph();

    for (int i = 0; i < widgetsCount; i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != 8)
      {
        LayoutParams params = (LayoutParams)child.getLayoutParams();
        ConstraintWidget widget = params.widget;
        if ((!params.isGuideline) && (!params.isHelper))
        {
          widget.setVisibility(child.getVisibility());

          int width = params.width;
          int height = params.height;

          if ((width == 0) || (height == 0))
          {
            ResolutionAnchor left = widget.getAnchor(ConstraintAnchor.Type.LEFT).getResolutionNode();
            ResolutionAnchor right = widget.getAnchor(ConstraintAnchor.Type.RIGHT).getResolutionNode();

            boolean bothHorizontal = (widget.getAnchor(ConstraintAnchor.Type.LEFT).getTarget() != null) && 
              (widget
              .getAnchor(ConstraintAnchor.Type.RIGHT)
              .getTarget() != null);
            ResolutionAnchor top = widget.getAnchor(ConstraintAnchor.Type.TOP).getResolutionNode();
            ResolutionAnchor bottom = widget.getAnchor(ConstraintAnchor.Type.BOTTOM).getResolutionNode();

            boolean bothVertical = (widget.getAnchor(ConstraintAnchor.Type.TOP).getTarget() != null) && 
              (widget
              .getAnchor(ConstraintAnchor.Type.BOTTOM)
              .getTarget() != null);

            if ((width != 0) || (height != 0) || (!bothHorizontal) || (!bothVertical))
            {
              boolean didWrapMeasureWidth = false;
              boolean didWrapMeasureHeight = false;
              boolean resolveWidth = this.mLayoutWidget.getHorizontalDimensionBehaviour() != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
              boolean resolveHeight = this.mLayoutWidget.getVerticalDimensionBehaviour() != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;

              if (!resolveWidth) {
                widget.getResolutionWidth().invalidate();
              }
              if (!resolveHeight)
                widget.getResolutionHeight().invalidate();
              int childWidthMeasureSpec;
              if (width == 0)
              {
                if ((resolveWidth) && (widget.isSpreadWidth()) && (bothHorizontal) && (left.isResolved()) && (right.isResolved())) {
                  width = (int)(right.getResolvedValue() - left.getResolvedValue());
                  widget.getResolutionWidth().resolve(width);
                  childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec, widthPadding, width);
                }
                else {
                  childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec, widthPadding, -2);

                  didWrapMeasureWidth = true;
                  resolveWidth = false;
                }
              }
              else
              {
                if (width == -1) {
                  childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec, widthPadding, -1);
                }
                else {
                  if (width == -2) {
                    didWrapMeasureWidth = true;
                  }
                  childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec, widthPadding, width);
                }
              }
              int childHeightMeasureSpec;
              if (height == 0)
              {
                if ((resolveHeight) && (widget.isSpreadHeight()) && (bothVertical) && (top.isResolved()) && (bottom.isResolved())) {
                  height = (int)(bottom.getResolvedValue() - top.getResolvedValue());
                  widget.getResolutionHeight().resolve(height);
                  childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, height);
                }
                else {
                  childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, -2);

                  didWrapMeasureHeight = true;
                  resolveHeight = false;
                }
              }
              else
              {
                if (height == -1) {
                  childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, -1);
                }
                else {
                  if (height == -2) {
                    didWrapMeasureHeight = true;
                  }
                  childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, height);
                }
              }
              child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
              if (this.mMetrics != null) {
                this.mMetrics.measures += 1L;
              }

              widget.setWidthWrapContent(width == -2);
              widget.setHeightWrapContent(height == -2);
              width = child.getMeasuredWidth();
              height = child.getMeasuredHeight();

              widget.setWidth(width);
              widget.setHeight(height);

              if (didWrapMeasureWidth) {
                widget.setWrapWidth(width);
              }
              if (didWrapMeasureHeight) {
                widget.setWrapHeight(height);
              }
              if (resolveWidth)
                widget.getResolutionWidth().resolve(width);
              else {
                widget.getResolutionWidth().remove();
              }
              if (resolveHeight)
                widget.getResolutionHeight().resolve(height);
              else {
                widget.getResolutionHeight().remove();
              }

              if (params.needsBaseline) {
                int baseline = child.getBaseline();
                if (baseline != -1)
                  widget.setBaselineDistance(baseline);
              }
            }
          }
        }
      }
    }
  }

  public void fillMetrics(Metrics metrics)
  {
    this.mMetrics = metrics;
    this.mLayoutWidget.fillMetrics(metrics);
  }

  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    long time = System.currentTimeMillis();
    int REMEASURES_A = 0;
    int REMEASURES_B = 0;

    int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

    boolean validLastMeasure = (this.mLastMeasureWidth != -1) && (this.mLastMeasureHeight != -1);
    boolean sameSize = (widthMode == 1073741824) && (heightMode == 1073741824) && (widthSize == this.mLastMeasureWidth) && (heightSize == this.mLastMeasureHeight);

    boolean sameMode = (widthMode == this.mLastMeasureWidthMode) && (heightMode == this.mLastMeasureHeightMode);
    boolean sameMeasure = (sameMode) && (widthSize == this.mLastMeasureWidthSize) && (heightSize == this.mLastMeasureHeightSize);

    boolean fitSizeWidth = (sameMode) && (widthMode == -2147483648) && (heightMode == 1073741824) && (widthSize >= this.mLastMeasureWidth) && (heightSize == this.mLastMeasureHeight);

    boolean fitSizeHeight = (sameMode) && (widthMode == 1073741824) && (heightMode == -2147483648) && (widthSize == this.mLastMeasureWidth) && (heightSize >= this.mLastMeasureHeight);

    this.mLastMeasureWidthMode = widthMode;
    this.mLastMeasureHeightMode = heightMode;
    this.mLastMeasureWidthSize = widthSize;
    this.mLastMeasureHeightSize = heightSize;

    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();

    this.mLayoutWidget.setX(paddingLeft);
    this.mLayoutWidget.setY(paddingTop);
    this.mLayoutWidget.setMaxWidth(this.mMaxWidth);
    this.mLayoutWidget.setMaxHeight(this.mMaxHeight);

    if (Build.VERSION.SDK_INT >= 17) {
      this.mLayoutWidget.setRtl(getLayoutDirection() == 1);
    }

    setSelfDimensionBehaviour(widthMeasureSpec, heightMeasureSpec);
    int startingWidth = this.mLayoutWidget.getWidth();
    int startingHeight = this.mLayoutWidget.getHeight();
    if (this.mDirtyHierarchy) {
      this.mDirtyHierarchy = false;
      updateHierarchy();
    }

    boolean optimiseDimensions = (this.mOptimizationLevel & 0x8) == 8;

    if (optimiseDimensions) {
      this.mLayoutWidget.preOptimize();
      this.mLayoutWidget.optimizeForDimensions(startingWidth, startingHeight);
      internalMeasureDimensions(widthMeasureSpec, heightMeasureSpec);
    } else {
      internalMeasureChildren(widthMeasureSpec, heightMeasureSpec);
    }
    updatePostMeasures();

    if (getChildCount() > 0) {
      solveLinearSystem("First pass");
    }
    int childState = 0;

    int sizeDependentWidgetsCount = this.mVariableDimensionsWidgets.size();

    int heightPadding = paddingTop + getPaddingBottom();
    int widthPadding = paddingLeft + getPaddingRight();

    if (sizeDependentWidgetsCount > 0) {
      boolean needSolverPass = false;
      boolean containerWrapWidth = this.mLayoutWidget.getHorizontalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;

      boolean containerWrapHeight = this.mLayoutWidget.getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;

      int minWidth = Math.max(this.mLayoutWidget.getWidth(), this.mMinWidth);
      int minHeight = Math.max(this.mLayoutWidget.getHeight(), this.mMinHeight);
      for (int i = 0; i < sizeDependentWidgetsCount; i++) {
        ConstraintWidget widget = (ConstraintWidget)this.mVariableDimensionsWidgets.get(i);
        View child = (View)widget.getCompanionWidget();
        if (child != null)
        {
          LayoutParams params = (LayoutParams)child.getLayoutParams();
          if ((!params.isHelper) && (!params.isGuideline))
          {
            if (child.getVisibility() != 8)
            {
              if ((!optimiseDimensions) || (!widget.getResolutionWidth().isResolved()) || 
                (!widget
                .getResolutionHeight().isResolved()))
              {
                int widthSpec = 0;
                int heightSpec = 0;

                if ((params.width == -2) && (params.horizontalDimensionFixed))
                  widthSpec = getChildMeasureSpec(widthMeasureSpec, widthPadding, params.width);
                else {
                  widthSpec = View.MeasureSpec.makeMeasureSpec(widget.getWidth(), 1073741824);
                }
                if ((params.height == -2) && (params.verticalDimensionFixed))
                  heightSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding, params.height);
                else {
                  heightSpec = View.MeasureSpec.makeMeasureSpec(widget.getHeight(), 1073741824);
                }

                child.measure(widthSpec, heightSpec);
                if (this.mMetrics != null) {
                  this.mMetrics.additionalMeasures += 1L;
                }

                REMEASURES_A++;

                int measuredWidth = child.getMeasuredWidth();
                int measuredHeight = child.getMeasuredHeight();

                if (measuredWidth != widget.getWidth()) {
                  widget.setWidth(measuredWidth);
                  if (optimiseDimensions) {
                    widget.getResolutionWidth().resolve(measuredWidth);
                  }
                  if ((containerWrapWidth) && (widget.getRight() > minWidth))
                  {
                    int w = widget.getRight() + widget
                      .getAnchor(ConstraintAnchor.Type.RIGHT)
                      .getMargin();
                    minWidth = Math.max(minWidth, w);
                  }
                  needSolverPass = true;
                }
                if (measuredHeight != widget.getHeight()) {
                  widget.setHeight(measuredHeight);
                  if (optimiseDimensions) {
                    widget.getResolutionHeight().resolve(measuredHeight);
                  }
                  if ((containerWrapHeight) && (widget.getBottom() > minHeight))
                  {
                    int h = widget.getBottom() + widget
                      .getAnchor(ConstraintAnchor.Type.BOTTOM)
                      .getMargin();
                    minHeight = Math.max(minHeight, h);
                  }
                  needSolverPass = true;
                }
                if (params.needsBaseline) {
                  int baseline = child.getBaseline();
                  if ((baseline != -1) && (baseline != widget.getBaselineDistance())) {
                    widget.setBaselineDistance(baseline);
                    needSolverPass = true;
                  }
                }

                if (Build.VERSION.SDK_INT >= 11)
                  childState = combineMeasuredStates(childState, child.getMeasuredState()); 
              }
            }
          }
        }
      }
      if (needSolverPass) {
        this.mLayoutWidget.setWidth(startingWidth);
        this.mLayoutWidget.setHeight(startingHeight);
        if (optimiseDimensions) {
          this.mLayoutWidget.solveGraph();
        }
        solveLinearSystem("2nd pass");
        needSolverPass = false;
        if (this.mLayoutWidget.getWidth() < minWidth) {
          this.mLayoutWidget.setWidth(minWidth);
          needSolverPass = true;
        }
        if (this.mLayoutWidget.getHeight() < minHeight) {
          this.mLayoutWidget.setHeight(minHeight);
          needSolverPass = true;
        }
        if (needSolverPass) {
          solveLinearSystem("3rd pass");
        }
      }
      for (int i = 0; i < sizeDependentWidgetsCount; i++) {
        ConstraintWidget widget = (ConstraintWidget)this.mVariableDimensionsWidgets.get(i);
        View child = (View)widget.getCompanionWidget();
        if (child != null)
        {
          if ((child.getMeasuredWidth() != widget.getWidth()) || (child.getMeasuredHeight() != widget.getHeight())) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(widget.getWidth(), 1073741824);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(widget.getHeight(), 1073741824);
            child.measure(widthSpec, heightSpec);
            if (this.mMetrics != null) {
              this.mMetrics.additionalMeasures += 1L;
            }

            REMEASURES_B++;
          }
        }
      }
    }
    int androidLayoutWidth = this.mLayoutWidget.getWidth() + widthPadding;
    int androidLayoutHeight = this.mLayoutWidget.getHeight() + heightPadding;

    if (Build.VERSION.SDK_INT >= 11) {
      int resolvedWidthSize = resolveSizeAndState(androidLayoutWidth, widthMeasureSpec, childState);
      int resolvedHeightSize = resolveSizeAndState(androidLayoutHeight, heightMeasureSpec, childState << 16);

      resolvedWidthSize &= 16777215;
      resolvedHeightSize &= 16777215;
      resolvedWidthSize = Math.min(this.mMaxWidth, resolvedWidthSize);
      resolvedHeightSize = Math.min(this.mMaxHeight, resolvedHeightSize);
      if (this.mLayoutWidget.isWidthMeasuredTooSmall()) {
        resolvedWidthSize |= 16777216;
      }
      if (this.mLayoutWidget.isHeightMeasuredTooSmall()) {
        resolvedHeightSize |= 16777216;
      }
      setMeasuredDimension(resolvedWidthSize, resolvedHeightSize);
      this.mLastMeasureWidth = resolvedWidthSize;
      this.mLastMeasureHeight = resolvedHeightSize;
    } else {
      setMeasuredDimension(androidLayoutWidth, androidLayoutHeight);
      this.mLastMeasureWidth = androidLayoutWidth;
      this.mLastMeasureHeight = androidLayoutHeight;
    }
  }

  private void setSelfDimensionBehaviour(int widthMeasureSpec, int heightMeasureSpec)
  {
    int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

    int heightPadding = getPaddingTop() + getPaddingBottom();
    int widthPadding = getPaddingLeft() + getPaddingRight();

    ConstraintWidget.DimensionBehaviour widthBehaviour = ConstraintWidget.DimensionBehaviour.FIXED;
    ConstraintWidget.DimensionBehaviour heightBehaviour = ConstraintWidget.DimensionBehaviour.FIXED;
    int desiredWidth = 0;
    int desiredHeight = 0;

    ViewGroup.LayoutParams params = getLayoutParams();
    switch (widthMode) {
    case -2147483648:
      widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
      desiredWidth = widthSize;

      break;
    case 0:
      widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;

      break;
    case 1073741824:
      desiredWidth = Math.min(this.mMaxWidth, widthSize) - widthPadding;
    }

    switch (heightMode) {
    case -2147483648:
      heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
      desiredHeight = heightSize;

      break;
    case 0:
      heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;

      break;
    case 1073741824:
      desiredHeight = Math.min(this.mMaxHeight, heightSize) - heightPadding;
    }

    this.mLayoutWidget.setMinWidth(0);
    this.mLayoutWidget.setMinHeight(0);
    this.mLayoutWidget.setHorizontalDimensionBehaviour(widthBehaviour);
    this.mLayoutWidget.setWidth(desiredWidth);
    this.mLayoutWidget.setVerticalDimensionBehaviour(heightBehaviour);
    this.mLayoutWidget.setHeight(desiredHeight);
    this.mLayoutWidget.setMinWidth(this.mMinWidth - getPaddingLeft() - getPaddingRight());
    this.mLayoutWidget.setMinHeight(this.mMinHeight - getPaddingTop() - getPaddingBottom());
  }

  protected void solveLinearSystem(String reason)
  {
    this.mLayoutWidget.layout();
    if (this.mMetrics != null)
      this.mMetrics.resolutions += 1L;
  }

  protected void onLayout(boolean changed, int left, int top, int right, int bottom)
  {
    int widgetsCount = getChildCount();
    boolean isInEditMode = isInEditMode();
    for (int i = 0; i < widgetsCount; i++) {
      View child = getChildAt(i);
      LayoutParams params = (LayoutParams)child.getLayoutParams();
      ConstraintWidget widget = params.widget;

      if ((child.getVisibility() != 8) || (params.isGuideline) || (params.isHelper) || (isInEditMode))
      {
        if (!params.isInPlaceholder)
        {
          int l = widget.getDrawX();
          int t = widget.getDrawY();
          int r = l + widget.getWidth();
          int b = t + widget.getHeight();

          child.layout(l, t, r, b);
          if ((child instanceof Placeholder)) {
            Placeholder holder = (Placeholder)child;
            View content = holder.getContent();
            if (content != null) {
              content.setVisibility(0);
              content.layout(l, t, r, b);
            }
          }
        }
      }
    }
    int helperCount = this.mConstraintHelpers.size();
    if (helperCount > 0)
      for (int i = 0; i < helperCount; i++) {
        ConstraintHelper helper = (ConstraintHelper)this.mConstraintHelpers.get(i);
        helper.updatePostLayout(this);
      }
  }

  public void setOptimizationLevel(int level)
  {
    this.mLayoutWidget.setOptimizationLevel(level);
  }

  public int getOptimizationLevel()
  {
    return this.mLayoutWidget.getOptimizationLevel();
  }

  public LayoutParams generateLayoutParams(AttributeSet attrs)
  {
    return new LayoutParams(getContext(), attrs);
  }

  protected LayoutParams generateDefaultLayoutParams()
  {
    return new LayoutParams(-2, -2);
  }

  protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p)
  {
    return new LayoutParams(p);
  }

  protected boolean checkLayoutParams(ViewGroup.LayoutParams p)
  {
    return p instanceof LayoutParams;
  }

  public void setConstraintSet(ConstraintSet set)
  {
    this.mConstraintSet = set;
  }

  public View getViewById(int id)
  {
    return (View)this.mChildrenByIds.get(id);
  }

  public void dispatchDraw(Canvas canvas)
  {
    super.dispatchDraw(canvas);
    if (isInEditMode()) {
      int count = getChildCount();
      float cw = getWidth();
      float ch = getHeight();
      float ow = 1080.0F;
      float oh = 1920.0F;
      for (int i = 0; i < count; i++) {
        View child = getChildAt(i);
        if (child.getVisibility() != 8)
        {
          Object tag = child.getTag();
          if ((tag != null) && ((tag instanceof String))) {
            String coordinates = (String)tag;
            String[] split = coordinates.split(",");
            if (split.length == 4) {
              int x = Integer.parseInt(split[0]);
              int y = Integer.parseInt(split[1]);
              int w = Integer.parseInt(split[2]);
              int h = Integer.parseInt(split[3]);
              x = (int)(x / ow * cw);
              y = (int)(y / oh * ch);
              w = (int)(w / ow * cw);
              h = (int)(h / oh * ch);
              Paint paint = new Paint();
              paint.setColor(-65536);
              canvas.drawLine(x, y, x + w, y, paint);
              canvas.drawLine(x + w, y, x + w, y + h, paint);
              canvas.drawLine(x + w, y + h, x, y + h, paint);
              canvas.drawLine(x, y + h, x, y, paint);
              paint.setColor(-16711936);
              canvas.drawLine(x, y, x + w, y + h, paint);
              canvas.drawLine(x, y + h, x + w, y, paint);
            }
          }
        }
      }
    }
  }

  public void requestLayout()
  {
    super.requestLayout();
    this.mDirtyHierarchy = true;

    this.mLastMeasureWidth = -1;
    this.mLastMeasureHeight = -1;
    this.mLastMeasureWidthSize = -1;
    this.mLastMeasureHeightSize = -1;
    this.mLastMeasureWidthMode = 0;
    this.mLastMeasureHeightMode = 0;
  }

  public boolean shouldDelayChildPressedState()
  {
    return false;
  }

  public static class LayoutParams extends ViewGroup.MarginLayoutParams
  {
    public static final int MATCH_CONSTRAINT = 0;
    public static final int PARENT_ID = 0;
    public static final int UNSET = -1;
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int TOP = 3;
    public static final int BOTTOM = 4;
    public static final int BASELINE = 5;
    public static final int START = 6;
    public static final int END = 7;
    public static final int MATCH_CONSTRAINT_WRAP = 1;
    public static final int MATCH_CONSTRAINT_SPREAD = 0;
    public static final int MATCH_CONSTRAINT_PERCENT = 2;
    public static final int CHAIN_SPREAD = 0;
    public static final int CHAIN_SPREAD_INSIDE = 1;
    public static final int CHAIN_PACKED = 2;
    public int guideBegin = -1;

    public int guideEnd = -1;

    public float guidePercent = -1.0F;

    public int leftToLeft = -1;

    public int leftToRight = -1;

    public int rightToLeft = -1;

    public int rightToRight = -1;

    public int topToTop = -1;

    public int topToBottom = -1;

    public int bottomToTop = -1;

    public int bottomToBottom = -1;

    public int baselineToBaseline = -1;

    public int circleConstraint = -1;

    public int circleRadius = 0;

    public float circleAngle = 0.0F;

    public int startToEnd = -1;

    public int startToStart = -1;

    public int endToStart = -1;

    public int endToEnd = -1;

    public int goneLeftMargin = -1;

    public int goneTopMargin = -1;

    public int goneRightMargin = -1;

    public int goneBottomMargin = -1;

    public int goneStartMargin = -1;

    public int goneEndMargin = -1;

    public float horizontalBias = 0.5F;

    public float verticalBias = 0.5F;

    public String dimensionRatio = null;

    float dimensionRatioValue = 0.0F;

    int dimensionRatioSide = 1;

    public float horizontalWeight = 0.0F;

    public float verticalWeight = 0.0F;

    public int horizontalChainStyle = 0;

    public int verticalChainStyle = 0;

    public int matchConstraintDefaultWidth = 0;

    public int matchConstraintDefaultHeight = 0;

    public int matchConstraintMinWidth = 0;

    public int matchConstraintMinHeight = 0;

    public int matchConstraintMaxWidth = 0;

    public int matchConstraintMaxHeight = 0;

    public float matchConstraintPercentWidth = 1.0F;

    public float matchConstraintPercentHeight = 1.0F;

    public int editorAbsoluteX = -1;

    public int editorAbsoluteY = -1;

    public int orientation = -1;

    public boolean constrainedWidth = false;

    public boolean constrainedHeight = false;

    boolean horizontalDimensionFixed = true;
    boolean verticalDimensionFixed = true;

    boolean needsBaseline = false;
    boolean isGuideline = false;
    boolean isHelper = false;
    boolean isInPlaceholder = false;

    int resolvedLeftToLeft = -1;
    int resolvedLeftToRight = -1;
    int resolvedRightToLeft = -1;
    int resolvedRightToRight = -1;
    int resolveGoneLeftMargin = -1;
    int resolveGoneRightMargin = -1;
    float resolvedHorizontalBias = 0.5F;
    int resolvedGuideBegin;
    int resolvedGuideEnd;
    float resolvedGuidePercent;
    ConstraintWidget widget = new ConstraintWidget();

    public boolean helped = false;

    public void reset()
    {
      if (this.widget != null)
        this.widget.reset();
    }

    public LayoutParams(LayoutParams source)
    {
      super(source);
      this.guideBegin = source.guideBegin;
      this.guideEnd = source.guideEnd;
      this.guidePercent = source.guidePercent;
      this.leftToLeft = source.leftToLeft;
      this.leftToRight = source.leftToRight;
      this.rightToLeft = source.rightToLeft;
      this.rightToRight = source.rightToRight;
      this.topToTop = source.topToTop;
      this.topToBottom = source.topToBottom;
      this.bottomToTop = source.bottomToTop;
      this.bottomToBottom = source.bottomToBottom;
      this.baselineToBaseline = source.baselineToBaseline;
      this.circleConstraint = source.circleConstraint;
      this.circleRadius = source.circleRadius;
      this.circleAngle = source.circleAngle;
      this.startToEnd = source.startToEnd;
      this.startToStart = source.startToStart;
      this.endToStart = source.endToStart;
      this.endToEnd = source.endToEnd;
      this.goneLeftMargin = source.goneLeftMargin;
      this.goneTopMargin = source.goneTopMargin;
      this.goneRightMargin = source.goneRightMargin;
      this.goneBottomMargin = source.goneBottomMargin;
      this.goneStartMargin = source.goneStartMargin;
      this.goneEndMargin = source.goneEndMargin;
      this.horizontalBias = source.horizontalBias;
      this.verticalBias = source.verticalBias;
      this.dimensionRatio = source.dimensionRatio;
      this.dimensionRatioValue = source.dimensionRatioValue;
      this.dimensionRatioSide = source.dimensionRatioSide;
      this.horizontalWeight = source.horizontalWeight;
      this.verticalWeight = source.verticalWeight;
      this.horizontalChainStyle = source.horizontalChainStyle;
      this.verticalChainStyle = source.verticalChainStyle;
      this.constrainedWidth = source.constrainedWidth;
      this.constrainedHeight = source.constrainedHeight;
      this.matchConstraintDefaultWidth = source.matchConstraintDefaultWidth;
      this.matchConstraintDefaultHeight = source.matchConstraintDefaultHeight;
      this.matchConstraintMinWidth = source.matchConstraintMinWidth;
      this.matchConstraintMaxWidth = source.matchConstraintMaxWidth;
      this.matchConstraintMinHeight = source.matchConstraintMinHeight;
      this.matchConstraintMaxHeight = source.matchConstraintMaxHeight;
      this.matchConstraintPercentWidth = source.matchConstraintPercentWidth;
      this.matchConstraintPercentHeight = source.matchConstraintPercentHeight;
      this.editorAbsoluteX = source.editorAbsoluteX;
      this.editorAbsoluteY = source.editorAbsoluteY;
      this.orientation = source.orientation;
      this.horizontalDimensionFixed = source.horizontalDimensionFixed;
      this.verticalDimensionFixed = source.verticalDimensionFixed;
      this.needsBaseline = source.needsBaseline;
      this.isGuideline = source.isGuideline;
      this.resolvedLeftToLeft = source.resolvedLeftToLeft;
      this.resolvedLeftToRight = source.resolvedLeftToRight;
      this.resolvedRightToLeft = source.resolvedRightToLeft;
      this.resolvedRightToRight = source.resolvedRightToRight;
      this.resolveGoneLeftMargin = source.resolveGoneLeftMargin;
      this.resolveGoneRightMargin = source.resolveGoneRightMargin;
      this.resolvedHorizontalBias = source.resolvedHorizontalBias;
      this.widget = source.widget;
    }

    public LayoutParams(Context c, AttributeSet attrs)
    {
      super(c,attrs);
      TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
      int N = a.getIndexCount();
      for (int i = 0; i < N; i++) {
        int attr = a.getIndex(i);
        int look = Table.map.get(attr);
        switch (look)
        {
        case 0:
          break;
        case 8:
          this.leftToLeft = a.getResourceId(attr, this.leftToLeft);
          if (this.leftToLeft == -1)
            this.leftToLeft = a.getInt(attr, -1); break;
        case 9:
          this.leftToRight = a.getResourceId(attr, this.leftToRight);
          if (this.leftToRight == -1)
            this.leftToRight = a.getInt(attr, -1); break;
        case 10:
          this.rightToLeft = a.getResourceId(attr, this.rightToLeft);
          if (this.rightToLeft == -1)
            this.rightToLeft = a.getInt(attr, -1); break;
        case 11:
          this.rightToRight = a.getResourceId(attr, this.rightToRight);
          if (this.rightToRight == -1)
            this.rightToRight = a.getInt(attr, -1); break;
        case 12:
          this.topToTop = a.getResourceId(attr, this.topToTop);
          if (this.topToTop == -1)
            this.topToTop = a.getInt(attr, -1); break;
        case 13:
          this.topToBottom = a.getResourceId(attr, this.topToBottom);
          if (this.topToBottom == -1)
            this.topToBottom = a.getInt(attr, -1); break;
        case 14:
          this.bottomToTop = a.getResourceId(attr, this.bottomToTop);
          if (this.bottomToTop == -1)
            this.bottomToTop = a.getInt(attr, -1); break;
        case 15:
          this.bottomToBottom = a.getResourceId(attr, this.bottomToBottom);
          if (this.bottomToBottom == -1)
            this.bottomToBottom = a.getInt(attr, -1); break;
        case 16:
          this.baselineToBaseline = a.getResourceId(attr, this.baselineToBaseline);
          if (this.baselineToBaseline == -1)
            this.baselineToBaseline = a.getInt(attr, -1); break;
        case 2:
          this.circleConstraint = a.getResourceId(attr, this.circleConstraint);
          if (this.circleConstraint == -1)
            this.circleConstraint = a.getInt(attr, -1); break;
        case 3:
          this.circleRadius = a.getDimensionPixelSize(attr, this.circleRadius);
          break;
        case 4:
          this.circleAngle = (a.getFloat(attr, this.circleAngle) % 360.0F);
          if (this.circleAngle < 0.0F)
            this.circleAngle = ((360.0F - this.circleAngle) % 360.0F); break;
        case 49:
          this.editorAbsoluteX = a.getDimensionPixelOffset(attr, this.editorAbsoluteX);
          break;
        case 50:
          this.editorAbsoluteY = a.getDimensionPixelOffset(attr, this.editorAbsoluteY);
          break;
        case 5:
          this.guideBegin = a.getDimensionPixelOffset(attr, this.guideBegin);
          break;
        case 6:
          this.guideEnd = a.getDimensionPixelOffset(attr, this.guideEnd);
          break;
        case 7:
          this.guidePercent = a.getFloat(attr, this.guidePercent);
          break;
        case 1:
          this.orientation = a.getInt(attr, this.orientation);
          break;
        case 17:
          this.startToEnd = a.getResourceId(attr, this.startToEnd);
          if (this.startToEnd == -1)
            this.startToEnd = a.getInt(attr, -1); break;
        case 18:
          this.startToStart = a.getResourceId(attr, this.startToStart);
          if (this.startToStart == -1)
            this.startToStart = a.getInt(attr, -1); break;
        case 19:
          this.endToStart = a.getResourceId(attr, this.endToStart);
          if (this.endToStart == -1)
            this.endToStart = a.getInt(attr, -1); break;
        case 20:
          this.endToEnd = a.getResourceId(attr, this.endToEnd);
          if (this.endToEnd == -1)
            this.endToEnd = a.getInt(attr, -1); break;
        case 21:
          this.goneLeftMargin = a.getDimensionPixelSize(attr, this.goneLeftMargin);
          break;
        case 22:
          this.goneTopMargin = a.getDimensionPixelSize(attr, this.goneTopMargin);
          break;
        case 23:
          this.goneRightMargin = a.getDimensionPixelSize(attr, this.goneRightMargin);
          break;
        case 24:
          this.goneBottomMargin = a.getDimensionPixelSize(attr, this.goneBottomMargin);
          break;
        case 25:
          this.goneStartMargin = a.getDimensionPixelSize(attr, this.goneStartMargin);
          break;
        case 26:
          this.goneEndMargin = a.getDimensionPixelSize(attr, this.goneEndMargin);
          break;
        case 29:
          this.horizontalBias = a.getFloat(attr, this.horizontalBias);
          break;
        case 30:
          this.verticalBias = a.getFloat(attr, this.verticalBias);
          break;
        case 44:
          this.dimensionRatio = a.getString(attr);
          this.dimensionRatioValue = (0.0F / 0.0F);
          this.dimensionRatioSide = -1;
          if (this.dimensionRatio != null) {
            int len = this.dimensionRatio.length();
            int commaIndex = this.dimensionRatio.indexOf(',');
            if ((commaIndex > 0) && (commaIndex < len - 1)) {
              String dimension = this.dimensionRatio.substring(0, commaIndex);
              if (dimension.equalsIgnoreCase("W"))
                this.dimensionRatioSide = 0;
              else if (dimension.equalsIgnoreCase("H")) {
                this.dimensionRatioSide = 1;
              }
              commaIndex++;
            } else {
              commaIndex = 0;
            }
            int colonIndex = this.dimensionRatio.indexOf(':');
            if ((colonIndex >= 0) && (colonIndex < len - 1)) {
              String nominator = this.dimensionRatio.substring(commaIndex, colonIndex);
              String denominator = this.dimensionRatio.substring(colonIndex + 1);
              if ((nominator.length() > 0) && (denominator.length() > 0))
                try {
                  float nominatorValue = Float.parseFloat(nominator);
                  float denominatorValue = Float.parseFloat(denominator);
                  if ((nominatorValue > 0.0F) && (denominatorValue > 0.0F))
                    if (this.dimensionRatioSide == 1)
                      this.dimensionRatioValue = Math.abs(denominatorValue / nominatorValue);
                    else
                      this.dimensionRatioValue = Math.abs(nominatorValue / denominatorValue);
                }
                catch (NumberFormatException localNumberFormatException)
                {
                }
            }
            else
            {
              String r = this.dimensionRatio.substring(commaIndex);
              if (r.length() > 0)
                try {
                  this.dimensionRatioValue = Float.parseFloat(r);
                }
                catch (NumberFormatException localNumberFormatException1) {
                }
            }
          }
          break;
        case 45:
          this.horizontalWeight = a.getFloat(attr, 0.0F);
          break;
        case 46:
          this.verticalWeight = a.getFloat(attr, 0.0F);
          break;
        case 47:
          this.horizontalChainStyle = a.getInt(attr, 0);
          break;
        case 48:
          this.verticalChainStyle = a.getInt(attr, 0);
          break;
        case 27:
          this.constrainedWidth = a.getBoolean(attr, this.constrainedWidth);
          break;
        case 28:
          this.constrainedHeight = a.getBoolean(attr, this.constrainedHeight);
          break;
        case 31:
          this.matchConstraintDefaultWidth = a.getInt(attr, 0);
          if (this.matchConstraintDefaultWidth == 1)
            Log.e("ConstraintLayout", "layout_constraintWidth_default=\"wrap\" is deprecated.\nUse layout_width=\"WRAP_CONTENT\" and layout_constrainedWidth=\"true\" instead."); break;
        case 32:
          this.matchConstraintDefaultHeight = a.getInt(attr, 0);
          if (this.matchConstraintDefaultHeight == 1)
            Log.e("ConstraintLayout", "layout_constraintHeight_default=\"wrap\" is deprecated.\nUse layout_height=\"WRAP_CONTENT\" and layout_constrainedHeight=\"true\" instead."); break;
        case 33:
          try
          {
            this.matchConstraintMinWidth = a.getDimensionPixelSize(attr, this.matchConstraintMinWidth);
          } catch (Exception e) {
            int value = a.getInt(attr, this.matchConstraintMinWidth);
            if (value == -2) {
              this.matchConstraintMinWidth = -2;
            }
          }

        case 34:
          try
          {
            this.matchConstraintMaxWidth = a.getDimensionPixelSize(attr, this.matchConstraintMaxWidth);
          } catch (Exception e) {
            int value = a.getInt(attr, this.matchConstraintMaxWidth);
            if (value == -2) {
              this.matchConstraintMaxWidth = -2;
            }

          }

        case 35:
          this.matchConstraintPercentWidth = Math.max(0.0F, a.getFloat(attr, this.matchConstraintPercentWidth));
          break;
        case 36:
          try
          {
            this.matchConstraintMinHeight = a.getDimensionPixelSize(attr, this.matchConstraintMinHeight);
          } catch (Exception e) {
            int value = a.getInt(attr, this.matchConstraintMinHeight);
            if (value == -2) {
              this.matchConstraintMinHeight = -2;
            }
          }

        case 37:
          try
          {
            this.matchConstraintMaxHeight = a.getDimensionPixelSize(attr, this.matchConstraintMaxHeight);
          } catch (Exception e) {
            int value = a.getInt(attr, this.matchConstraintMaxHeight);
            if (value == -2) {
              this.matchConstraintMaxHeight = -2;
            }

          }

        case 38:
          this.matchConstraintPercentHeight = Math.max(0.0F, a.getFloat(attr, this.matchConstraintPercentHeight));
          break;
        case 39:
          break;
        case 40:
          break;
        case 41:
          break;
        case 42:
        case 43:
        }

      }

      a.recycle();
      validate();
    }

    public void validate() {
      this.isGuideline = false;
      this.horizontalDimensionFixed = true;
      this.verticalDimensionFixed = true;
      if ((this.width == -2) && (this.constrainedWidth)) {
        this.horizontalDimensionFixed = false;
        this.matchConstraintDefaultWidth = 1;
      }
      if ((this.height == -2) && (this.constrainedHeight)) {
        this.verticalDimensionFixed = false;
        this.matchConstraintDefaultHeight = 1;
      }
      if ((this.width == 0) || (this.width == -1)) {
        this.horizontalDimensionFixed = false;

        if ((this.width == 0) && (this.matchConstraintDefaultWidth == 1)) {
          this.width = -2;
          this.constrainedWidth = true;
        }
      }
      if ((this.height == 0) || (this.height == -1)) {
        this.verticalDimensionFixed = false;

        if ((this.height == 0) && (this.matchConstraintDefaultHeight == 1)) {
          this.height = -2;
          this.constrainedHeight = true;
        }
      }
      if ((this.guidePercent != -1.0F) || (this.guideBegin != -1) || (this.guideEnd != -1)) {
        this.isGuideline = true;
        this.horizontalDimensionFixed = true;
        this.verticalDimensionFixed = true;
        if (!(this.widget instanceof android.support.constraint.solver.widgets.Guideline)) {
          this.widget = new android.support.constraint.solver.widgets.Guideline();
        }
        ((android.support.constraint.solver.widgets.Guideline)this.widget).setOrientation(this.orientation);
      }
    }

    public LayoutParams(int width, int height) { super(width,height); }

    public LayoutParams(ViewGroup.LayoutParams source)
    {
      super(source);
    }

    @TargetApi(17)
    public void resolveLayoutDirection(int layoutDirection)
    {
      int preLeftMargin = this.leftMargin;
      int preRightMargin = this.rightMargin;

      super.resolveLayoutDirection(layoutDirection);

      this.resolvedRightToLeft = -1;
      this.resolvedRightToRight = -1;
      this.resolvedLeftToLeft = -1;
      this.resolvedLeftToRight = -1;

      this.resolveGoneLeftMargin = -1;
      this.resolveGoneRightMargin = -1;
      this.resolveGoneLeftMargin = this.goneLeftMargin;
      this.resolveGoneRightMargin = this.goneRightMargin;
      this.resolvedHorizontalBias = this.horizontalBias;

      this.resolvedGuideBegin = this.guideBegin;
      this.resolvedGuideEnd = this.guideEnd;
      this.resolvedGuidePercent = this.guidePercent;

      boolean isRtl = 1 == getLayoutDirection();

      if (isRtl) {
        boolean startEndDefined = false;
        if (this.startToEnd != -1) {
          this.resolvedRightToLeft = this.startToEnd;
          startEndDefined = true;
        } else if (this.startToStart != -1) {
          this.resolvedRightToRight = this.startToStart;
          startEndDefined = true;
        }
        if (this.endToStart != -1) {
          this.resolvedLeftToRight = this.endToStart;
          startEndDefined = true;
        }
        if (this.endToEnd != -1) {
          this.resolvedLeftToLeft = this.endToEnd;
          startEndDefined = true;
        }
        if (this.goneStartMargin != -1) {
          this.resolveGoneRightMargin = this.goneStartMargin;
        }
        if (this.goneEndMargin != -1) {
          this.resolveGoneLeftMargin = this.goneEndMargin;
        }
        if (startEndDefined) {
          this.resolvedHorizontalBias = (1.0F - this.horizontalBias);
        }

        if ((this.isGuideline) && (this.orientation == 1))
          if (this.guidePercent != -1.0F) {
            this.resolvedGuidePercent = (1.0F - this.guidePercent);
            this.resolvedGuideBegin = -1;
            this.resolvedGuideEnd = -1;
          } else if (this.guideBegin != -1) {
            this.resolvedGuideEnd = this.guideBegin;
            this.resolvedGuideBegin = -1;
            this.resolvedGuidePercent = -1.0F;
          } else if (this.guideEnd != -1) {
            this.resolvedGuideBegin = this.guideEnd;
            this.resolvedGuideEnd = -1;
            this.resolvedGuidePercent = -1.0F;
          }
      }
      else {
        if (this.startToEnd != -1) {
          this.resolvedLeftToRight = this.startToEnd;
        }
        if (this.startToStart != -1) {
          this.resolvedLeftToLeft = this.startToStart;
        }
        if (this.endToStart != -1) {
          this.resolvedRightToLeft = this.endToStart;
        }
        if (this.endToEnd != -1) {
          this.resolvedRightToRight = this.endToEnd;
        }
        if (this.goneStartMargin != -1) {
          this.resolveGoneLeftMargin = this.goneStartMargin;
        }
        if (this.goneEndMargin != -1) {
          this.resolveGoneRightMargin = this.goneEndMargin;
        }
      }

      if ((this.endToStart == -1) && (this.endToEnd == -1) && (this.startToStart == -1) && (this.startToEnd == -1))
      {
        if (this.rightToLeft != -1) {
          this.resolvedRightToLeft = this.rightToLeft;
          if ((this.rightMargin <= 0) && (preRightMargin > 0))
            this.rightMargin = preRightMargin;
        }
        else if (this.rightToRight != -1) {
          this.resolvedRightToRight = this.rightToRight;
          if ((this.rightMargin <= 0) && (preRightMargin > 0)) {
            this.rightMargin = preRightMargin;
          }
        }
        if (this.leftToLeft != -1) {
          this.resolvedLeftToLeft = this.leftToLeft;
          if ((this.leftMargin <= 0) && (preLeftMargin > 0))
            this.leftMargin = preLeftMargin;
        }
        else if (this.leftToRight != -1) {
          this.resolvedLeftToRight = this.leftToRight;
          if ((this.leftMargin <= 0) && (preLeftMargin > 0))
            this.leftMargin = preLeftMargin;
        }
      }
    }

    private static class Table
    {
      public static final int UNUSED = 0;
      public static final int ANDROID_ORIENTATION = 1;
      public static final int LAYOUT_CONSTRAINT_CIRCLE = 2;
      public static final int LAYOUT_CONSTRAINT_CIRCLE_RADIUS = 3;
      public static final int LAYOUT_CONSTRAINT_CIRCLE_ANGLE = 4;
      public static final int LAYOUT_CONSTRAINT_GUIDE_BEGIN = 5;
      public static final int LAYOUT_CONSTRAINT_GUIDE_END = 6;
      public static final int LAYOUT_CONSTRAINT_GUIDE_PERCENT = 7;
      public static final int LAYOUT_CONSTRAINT_LEFT_TO_LEFT_OF = 8;
      public static final int LAYOUT_CONSTRAINT_LEFT_TO_RIGHT_OF = 9;
      public static final int LAYOUT_CONSTRAINT_RIGHT_TO_LEFT_OF = 10;
      public static final int LAYOUT_CONSTRAINT_RIGHT_TO_RIGHT_OF = 11;
      public static final int LAYOUT_CONSTRAINT_TOP_TO_TOP_OF = 12;
      public static final int LAYOUT_CONSTRAINT_TOP_TO_BOTTOM_OF = 13;
      public static final int LAYOUT_CONSTRAINT_BOTTOM_TO_TOP_OF = 14;
      public static final int LAYOUT_CONSTRAINT_BOTTOM_TO_BOTTOM_OF = 15;
      public static final int LAYOUT_CONSTRAINT_BASELINE_TO_BASELINE_OF = 16;
      public static final int LAYOUT_CONSTRAINT_START_TO_END_OF = 17;
      public static final int LAYOUT_CONSTRAINT_START_TO_START_OF = 18;
      public static final int LAYOUT_CONSTRAINT_END_TO_START_OF = 19;
      public static final int LAYOUT_CONSTRAINT_END_TO_END_OF = 20;
      public static final int LAYOUT_GONE_MARGIN_LEFT = 21;
      public static final int LAYOUT_GONE_MARGIN_TOP = 22;
      public static final int LAYOUT_GONE_MARGIN_RIGHT = 23;
      public static final int LAYOUT_GONE_MARGIN_BOTTOM = 24;
      public static final int LAYOUT_GONE_MARGIN_START = 25;
      public static final int LAYOUT_GONE_MARGIN_END = 26;
      public static final int LAYOUT_CONSTRAINED_WIDTH = 27;
      public static final int LAYOUT_CONSTRAINED_HEIGHT = 28;
      public static final int LAYOUT_CONSTRAINT_HORIZONTAL_BIAS = 29;
      public static final int LAYOUT_CONSTRAINT_VERTICAL_BIAS = 30;
      public static final int LAYOUT_CONSTRAINT_WIDTH_DEFAULT = 31;
      public static final int LAYOUT_CONSTRAINT_HEIGHT_DEFAULT = 32;
      public static final int LAYOUT_CONSTRAINT_WIDTH_MIN = 33;
      public static final int LAYOUT_CONSTRAINT_WIDTH_MAX = 34;
      public static final int LAYOUT_CONSTRAINT_WIDTH_PERCENT = 35;
      public static final int LAYOUT_CONSTRAINT_HEIGHT_MIN = 36;
      public static final int LAYOUT_CONSTRAINT_HEIGHT_MAX = 37;
      public static final int LAYOUT_CONSTRAINT_HEIGHT_PERCENT = 38;
      public static final int LAYOUT_CONSTRAINT_LEFT_CREATOR = 39;
      public static final int LAYOUT_CONSTRAINT_TOP_CREATOR = 40;
      public static final int LAYOUT_CONSTRAINT_RIGHT_CREATOR = 41;
      public static final int LAYOUT_CONSTRAINT_BOTTOM_CREATOR = 42;
      public static final int LAYOUT_CONSTRAINT_BASELINE_CREATOR = 43;
      public static final int LAYOUT_CONSTRAINT_DIMENSION_RATIO = 44;
      public static final int LAYOUT_CONSTRAINT_HORIZONTAL_WEIGHT = 45;
      public static final int LAYOUT_CONSTRAINT_VERTICAL_WEIGHT = 46;
      public static final int LAYOUT_CONSTRAINT_HORIZONTAL_CHAINSTYLE = 47;
      public static final int LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE = 48;
      public static final int LAYOUT_EDITOR_ABSOLUTEX = 49;
      public static final int LAYOUT_EDITOR_ABSOLUTEY = 50;
      public static final SparseIntArray map = new SparseIntArray();

      static {
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toLeftOf, 8);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toRightOf, 9);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintRight_toLeftOf, 10);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintRight_toRightOf, 11);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintTop_toTopOf, 12);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintTop_toBottomOf, 13);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toTopOf, 14);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toBottomOf, 15);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_toBaselineOf, 16);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintCircle, 2);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintCircleRadius, 3);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintCircleAngle, 4);
        map.append(R.styleable.ConstraintLayout_Layout_layout_editor_absoluteX, 49);
        map.append(R.styleable.ConstraintLayout_Layout_layout_editor_absoluteY, 50);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintGuide_begin, 5);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintGuide_end, 6);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintGuide_percent, 7);
        map.append(R.styleable.ConstraintLayout_Layout_android_orientation, 1);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintStart_toEndOf, 17);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintStart_toStartOf, 18);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toStartOf, 19);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toEndOf, 20);
        map.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginLeft, 21);
        map.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginTop, 22);
        map.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginRight, 23);
        map.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginBottom, 24);
        map.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginStart, 25);
        map.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginEnd, 26);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_bias, 29);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintVertical_bias, 30);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintDimensionRatio, 44);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_weight, 45);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintVertical_weight, 46);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_chainStyle, 47);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintVertical_chainStyle, 48);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constrainedWidth, 27);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constrainedHeight, 28);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_default, 31);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_default, 32);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_min, 33);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_max, 34);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_percent, 35);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_min, 36);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_max, 37);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_percent, 38);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintLeft_creator, 39);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintTop_creator, 40);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintRight_creator, 41);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintBottom_creator, 42);
        map.append(R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_creator, 43);
      }
    }
  }
}