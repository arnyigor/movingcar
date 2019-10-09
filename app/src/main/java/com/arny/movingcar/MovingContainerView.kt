package com.arny.movingcar

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt


class MovingContainerView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var viewHeight: Int = 0
    private var viewWidth: Int = 0
    private var touchEnable: Boolean = true
    private var path: Path
    private var initialTouchY: Float = 0f
    private var initialTouchX: Float = 0f
    private var initialY: Int = 0
    private var initialX: Int = 0
    private val startAngle: Float = 0f
    private var centerPosY: Int = 0
    private var centerPosX: Int = 0
    private var currentAngle: Float = 0f
    private var bgColor: Int = Color.WHITE
    private val car: Rect
    private val carWidth = 250
    private val carHeight = 500
    private val mCarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG)


    init {
        val typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MovingContainerView)
        bgColor = typedArray.getColor(R.styleable.MovingContainerView_bg_color, 0)
        typedArray.recycle()
        car = Rect()
        mCarPaint.color = Color.BLACK;
        targetPaint.color = Color.GREEN;
        targetPaint.strokeWidth = 0f
        path = Path()
        isHorizontalScrollBarEnabled = true
        isVerticalScrollBarEnabled = true
    }

    fun clear() {
        centerPosX = getHalf(viewWidth)
        centerPosY = viewHeight - getHalf(carHeight)
        currentAngle = startAngle
        targetPaint.strokeWidth = 0f
        Log.i(MovingContainerView::class.java.simpleName, "onSizeChanged: viewWidth:$viewWidth,viewHeight:$viewHeight,centerPosX:$centerPosX,centerPosY:$centerPosY");
        invalidate()
    }

    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)
        viewWidth = xNew
        viewHeight = yNew
        clear()
    }

    override fun computeHorizontalScrollRange(): Int {
        return 3000
    }

    override fun computeVerticalScrollRange(): Int {
        return 3000
    }

    private fun startMove(newPosY: Int, newPosX: Int, newAngle: Float) {
        val yProp = PropertyValuesHolder.ofInt("yPOS", centerPosY, newPosY)
        val xProp = PropertyValuesHolder.ofInt("xPOS", centerPosX, newPosX)
        val rotateProp = PropertyValuesHolder.ofFloat("rotate", currentAngle, newAngle)
        val animator = ValueAnimator();
        animator.setValues(yProp, xProp, rotateProp);
        animator.duration = 2000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                touchEnable = true
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
                touchEnable = false
            }
        })
        animator.addUpdateListener { valueAnimator ->
            centerPosY = valueAnimator.getAnimatedValue("yPOS") as Int
            centerPosX = valueAnimator.getAnimatedValue("xPOS") as Int
//            currentAngle = valueAnimator.getAnimatedValue("rotate") as Float
            this@MovingContainerView.invalidate()
        }
        animator.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!touchEnable) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }

            MotionEvent.ACTION_UP -> {
                initialTouchX = event.x
                initialTouchY = event.y
                calcNewPosition(initialTouchX, initialTouchY)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                initialX += (event.rawX - initialTouchX).roundToInt()
                initialY += (event.rawY - initialTouchY).roundToInt()
                return true
            }

        }
        return false
    }

    private fun calcNewPosition(initialTouchX: Float, initialTouchY: Float) {
        Log.i(
                MovingContainerView::class.java.simpleName,
                "calcNewPosition: centerPosX:$centerPosX,initialTouchX:$initialTouchX"
        )
        Log.i(
                MovingContainerView::class.java.simpleName,
                "calcNewPosition: centerPosY:$centerPosY,initialTouchY:$initialTouchY"
        )
        val angle = getAngle(Point(initialTouchX.toInt(), initialTouchY.toInt()))
        Log.i(
                MovingContainerView::class.java.simpleName,
                "calcNewPosition: currentAngle:$currentAngle,newAngle:$angle"
        )
        targetPaint.strokeWidth = 30f
        startMove(initialTouchY.toInt(), initialTouchX.toInt(), (currentAngle - angle).toFloat())
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

    private fun getAngle(target: Point): Double {
        var angle =
                Math.toDegrees(atan2((target.y - y).toDouble(), (target.x - x).toDouble()))
        if (angle < 0) {
            angle += 360f
        }
        return angle
    }

    private fun getHalf(size: Number): Int {
        return (size.toDouble() / 2).roundToInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPoint(initialTouchX, initialTouchY, targetPaint)
        car.set((centerPosX - getHalf(carWidth)), centerPosY + getHalf(carHeight), centerPosX + getHalf(carWidth), centerPosY - getHalf(carHeight))
        canvas.rotate(currentAngle, car.exactCenterX(), car.exactCenterY())
        canvas.drawRect(car, mCarPaint)
    }
}