package com.tt.leaktest

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup


class MultimediaGridView @JvmOverloads constructor(context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_SPACING = 4f
        private const val DEFAULT_ICON_CORNERSRADIUS = 8f
        private const val MAX_WIDTH_PERCENTAGE = 1
    }
    private var mColumnCount: Int = 0

    private var mSpacing: Float = 0.toFloat()


    private var cornersRadius: Float = 4.toFloat()
    var mSingleItemAspectRatio: Float = 1f
        set(value) {
            field = value
            if (childCount==1){
                invalidate()
            }
        }

    private var mItemWidth: Int = 0
    private var mItemHeight: Int = 0
    private var mItemClickListener: ((position: Int, view: View) -> Unit)? = null

    init {
        mSpacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_SPACING,
            context.resources.displayMetrics)
        cornersRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            DEFAULT_ICON_CORNERSRADIUS,
            context.resources.displayMetrics)
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var hMeasureSpec = heightMeasureSpec
        val count = childCount
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        if (count == 1) {
            mColumnCount = 1
            val mItemMaxWidth = (width * MAX_WIDTH_PERCENTAGE).toInt()
            if (mSingleItemAspectRatio < 1) {
                mItemHeight = mItemMaxWidth
                mItemWidth = (mItemHeight * mSingleItemAspectRatio).toInt()
            } else {
                mItemWidth = mItemMaxWidth
                mItemHeight = (mItemMaxWidth / mSingleItemAspectRatio).toInt()
            }
        } else {
            mColumnCount = if (count > 4 || count == 3) {
                3
            } else {
                2
            }

            mItemWidth = ((width.toFloat() - paddingLeft.toFloat() - paddingRight.toFloat() - 2 * mSpacing) / mColumnCount).toInt()
            mItemHeight = mItemWidth
        }


        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val layoutParams = view.layoutParams
            layoutParams.width = mItemWidth
            layoutParams.height = mItemHeight
            measureChild(view, widthMeasureSpec, hMeasureSpec)
        }

        val heightMode = View.MeasureSpec.getMode(hMeasureSpec)
        if (heightMode == View.MeasureSpec.AT_MOST || heightMode == View.MeasureSpec.UNSPECIFIED) {
            hMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                getDesiredHeight(mItemHeight), View.MeasureSpec.EXACTLY)
        }

        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        if (widthMode == View.MeasureSpec.AT_MOST || widthMode == View.MeasureSpec.UNSPECIFIED) {
            super.onMeasure(View.MeasureSpec.makeMeasureSpec(
                getDesiredWidth(mItemWidth), View.MeasureSpec.EXACTLY), hMeasureSpec)
        } else {
            super.onMeasure(widthMeasureSpec, hMeasureSpec)
        }
    }


    override fun measureChild(child: View, parentWidthMeasureSpec: Int,
        parentHeightMeasureSpec: Int) {
        val lp = child.layoutParams
        //获取子控件的宽高约束规则
        val childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(parentWidthMeasureSpec,
            paddingLeft + paddingRight, lp.width)
        val childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(parentHeightMeasureSpec,
            paddingLeft + paddingRight, lp.height)

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    private fun getDesiredHeight(mItemHeight: Int): Int {
        var totalHeight = paddingTop + paddingBottom
        val count = childCount
        if (count > 0) {
            val row = (count - 1) / mColumnCount
            totalHeight += ((row + 1) * mItemHeight + row * mSpacing).toInt()
        }
        return totalHeight
    }

    private fun getDesiredWidth(mItemWidth: Int): Int {
        var totalWidth = paddingLeft + paddingRight
        val count = childCount
        if (count > 0) {
            totalWidth += if (count < mColumnCount) {
                (count * mItemWidth + (count - 1) * mSpacing).toInt()
            } else {
                (count * mItemWidth + (count - 1) * mSpacing).toInt()
            }

        }
        return totalWidth
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val imageView = getChildAt(i)

            val column = i % mColumnCount
            val row = i / mColumnCount
            val left = (paddingLeft + column * (mSpacing + mItemWidth)).toInt()
            val top = (paddingTop + row * (mSpacing + mItemHeight)).toInt()

            imageView.layout(left, top, left + mItemWidth, top + mItemHeight)
        }
    }

    fun <T> setMultimediaViewData(dataList: List<T>?,
        view: ((item: T) -> View)) {
        removeAllViews()
        val views = dataList?.mapNotNull { item ->
            view.invoke(item)
        }
        views?.mapIndexed { index, itemView ->
            itemView.setOnClickListener { view ->
                mItemClickListener?.invoke(index, view)
            }
            addView(itemView)
        }
    }

    fun setAllView(viewList: List<View>) {
        removeAllViews()
        viewList.forEachIndexed { index, view ->
            view.setOnClickListener {
                mItemClickListener?.invoke(index, view)
            }
            addView(view)
        }
    }

    fun setOnItemClickListener(onItemClickListener: ((position: Int, view: View) -> Unit)?) {
        mItemClickListener = onItemClickListener
    }


}




