package com.arny.movingcar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min
import kotlin.math.roundToInt


class MovingContainerView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var path: Path
    private var initialTouchY: Float = 0f
    private var initialTouchX: Float = 0f
    private var initialY: Int = 0
    private var initialX: Int = 0
    private var newPos: Int = 0
    private var bgColor: Int = Color.WHITE
    private var car: RectF
    private val mCarPaint = Paint(Paint.ANTI_ALIAS_FLAG)


    init {
        val typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MovingContainerView)
        bgColor = typedArray.getColor(R.styleable.MovingContainerView_bg_color, 0)
        typedArray.recycle()
        car = RectF()
        newPos = 0
        mCarPaint.color = Color.BLACK;
        mCarPaint.strokeWidth = 10f
       path = Path()
    }

    fun startAnimate()  {
        val animator = ValueAnimator.ofFloat(0f, 200f)
        animator.duration = 2000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            newPos = (it.animatedValue as Float).toInt()
            this@MovingContainerView.invalidate()
        }
        animator.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                Log.i(
                    MovingContainerView::class.java.simpleName,
                    "onTouchEvent: x:$initialTouchX,y:$initialTouchY"
                );
                return true
            }

            MotionEvent.ACTION_UP ->
                return true
            MotionEvent.ACTION_MOVE -> {
                initialX += (event.rawX - initialTouchX).roundToInt()
                initialY += (event.rawY - initialTouchY).roundToInt()
                return true
            }
        }
        return false
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> min(
                desiredSize,
                specSize
            )//if (desiredSize < specSize) desiredSize else specSize
            MeasureSpec.UNSPECIFIED -> desiredSize
            else -> desiredSize
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom
        val measuredWidth = measureDimension(desiredWidth, widthMeasureSpec)
        val measuredHeight = measureDimension(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save() // first save the state of the canvas
        canvas.rotate(45f) // rotate it
        canvas.drawPath(path, paint) // draw on it
        canvas.restore() // restore previous state (rotate it back)
        car.set(50, newPos + 200, 200, newPos)
        path.addRect(car, Path.Direction.CW)
        canvas.drawRect(car, mCarPaint)
    }
}