package com.arny.movingcar

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
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
    private var mTargetSize: Float = 0f
    private var mCarColor = Color.BLACK
    private var moveAnimator: ValueAnimator? = null
    private var rotateAnimator: ValueAnimator? = null
    private var viewHeight: Int = 0
    private var viewWidth: Int = 0
    private var rotateInPropress: Boolean = false
    private var movingInPropress: Boolean = false
    private var path: Path
    private var initialTouchY: Float = 0f
    private var initialTouchX: Float = 0f
    private var centerPosY: Int = 0
    private var centerPosX: Int = 0
    private val startAngle: Float = 0f
    private var currentAngle: Float = 0f
    private var bgColor: Int = Color.WHITE
    private val car: Rect
    private val carF: RectF
    private var carWidth = 12
    private var carHeight = 100
    private var mMatrix: Matrix
    private val mCarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mCarFPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mLinesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mTargetColor = Color.GREEN

    init {
        val typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MovingContainerView)
        bgColor = typedArray.getColor(R.styleable.MovingContainerView_bg_color, Color.WHITE)
        mCarColor = typedArray.getColor(R.styleable.MovingContainerView_car_color, Color.BLACK)
        mTargetColor = typedArray.getColor(R.styleable.MovingContainerView_target_color, Color.GREEN)
        mTargetSize = typedArray.getFloat(R.styleable.MovingContainerView_target_size, 10f)
        typedArray.recycle()
        car = Rect()
        carF = RectF()
        mCarFPaint.color = Color.YELLOW;
        mCarPaint.color = mCarColor;
        mLinesPaint.color = Color.LTGRAY;
        targetPaint.color = mTargetColor;
        targetPaint.strokeWidth = 0f
        mLinesPaint.strokeWidth = 4f
        path = Path()
        mMatrix = Matrix()
        isHorizontalScrollBarEnabled = true
        isVerticalScrollBarEnabled = true
        initRotateAnimator()
        initPositionAnimator()
    }

    fun clear() {
        moveAnimator?.cancel()
        centerPosX = getHalf(viewWidth)
        centerPosY = viewHeight - getHalf(viewHeight)
        currentAngle = startAngle
        targetPaint.strokeWidth = 0f
        invalidate()
    }

    private fun moving() {
        targetPaint.strokeWidth = mTargetSize
        val angle = calcAngle()
        val rotateTime = 300L//((1 / diff) * 1000).toLong()
        rotateAnimator?.duration = rotateTime
        rotateAnimate(angle)
    }

    private fun moveToNewPosition(initialTouchX: Float, initialTouchY: Float) {
        Log.i(MovingContainerView::class.java.simpleName, "moveToNewPosition: centerPosX:$centerPosX,initialTouchX:$initialTouchX")
        Log.i(MovingContainerView::class.java.simpleName, "moveToNewPosition: centerPosY:$centerPosY,initialTouchY:$initialTouchY")
        positionAnimate(initialTouchY.toInt(), initialTouchX.toInt())
    }

    private fun calcAngle(): Float {
        val x1 = centerPosX - centerPosX
        val y1 = centerPosY - centerPosY
        val from = intArrayOf(x1, y1)
        val x2 = initialTouchX.roundToInt() - centerPosX
        val y2 = (initialTouchY.roundToInt() - centerPosY) * -1
        val target = intArrayOf(x2, y2)
        var angle = getAngle(from, target).toFloat()
        Log.i(MovingContainerView::class.java.simpleName, "calcAngle: from:${from.contentToString()},target:${target.contentToString()},angle:$angle,currentAngle:$currentAngle");
        val fromCurrent = currentAngle - angle
//        Log.i(MovingContainerView::class.java.simpleName, "calcAngle: currentAngle:$currentAngle,newAngle:$angle, fromCurrent:$fromCurrent")
        if (fromCurrent < 0) {
            angle *= -1
        }
        var correction = 90f
        var resultAngle = angle + correction
        Log.i(MovingContainerView::class.java.simpleName, "calcAngle:currentAngle:$currentAngle->resultAngle :$resultAngle")
        val resultDiff = currentAngle - resultAngle
        Log.i(MovingContainerView::class.java.simpleName, "calcAngle:resultDiff:$resultDiff")
        return resultAngle
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

    private fun positionAnimate(newPosY: Int, newPosX: Int) {
        val yProp = PropertyValuesHolder.ofInt("yPOS", centerPosY, newPosY)
        val xProp = PropertyValuesHolder.ofInt("xPOS", centerPosX, newPosX)
        moveAnimator?.setValues(yProp, xProp);
        moveAnimator?.start()
    }

    private fun initRotateAnimator() {
        rotateAnimator = ValueAnimator();
        rotateAnimator?.duration = 1000
        rotateAnimator?.interpolator = AccelerateDecelerateInterpolator()
        rotateAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                rotateInPropress = false
                Log.i(MovingContainerView::class.java.simpleName, "onAnimationEnd: currentAngle:$currentAngle");
//                moveToNewPosition(initialTouchX, initialTouchY)
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
                rotateInPropress = true
            }
        })
        rotateAnimator?.addUpdateListener { valueAnimator ->
            currentAngle = valueAnimator.getAnimatedValue("rotate") as Float
            this@MovingContainerView.invalidate()
        }
    }

    private fun initPositionAnimator() {
        moveAnimator = ValueAnimator();
        moveAnimator?.duration = 2000
        moveAnimator?.interpolator = AccelerateDecelerateInterpolator()
        moveAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                movingInPropress = false
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
                movingInPropress = true
            }
        })
        moveAnimator?.addUpdateListener { valueAnimator ->
            centerPosY = valueAnimator.getAnimatedValue("yPOS") as Int
            centerPosX = valueAnimator.getAnimatedValue("xPOS") as Int
            this@MovingContainerView.invalidate()
        }
    }

    private fun rotateAnimate(newAngle: Float) {
        val rotateProp = PropertyValuesHolder.ofFloat("rotate", currentAngle, newAngle)
        rotateAnimator?.setValues(rotateProp);
        rotateAnimator?.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (movingInPropress || rotateInPropress) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }

            MotionEvent.ACTION_UP -> {
                initialTouchX = event.x
                initialTouchY = event.y
                moving()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
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

    private fun getHalf(size: Number): Int {
        return (size.toDouble() / 2).roundToInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCenterGrid(canvas)
//        drawGrid(canvas)
        /*  path.reset()
          carF.set(getCarBoundLeft().toFloat(), getCarBoundTop().toFloat(), getCarBoundRight().toFloat(), getCarBoundBottom().toFloat())
          path.addRect(carF, Path.Direction.CW);
          matrix.reset();
          matrix.setRotate(currentAngle,carF.centerX(), carF.centerY());
          path.transform(matrix);
          canvas.drawPath(path, mCarFPaint);*/
        canvas.drawPoint(initialTouchX, initialTouchY, targetPaint)
        car.set(getCarBoundLeft(), getCarBoundTop(), getCarBoundRight(), getCarBoundBottom())
        canvas.rotate(currentAngle, car.exactCenterX(), car.exactCenterY())
        canvas.drawRect(car, mCarPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        for (horLineY in 0..height step 40) {
            canvas.drawLine(0f, horLineY.toFloat(), width.toFloat(), horLineY.toFloat() + 2, mLinesPaint)
        }
        for (verLineX in 0..width step 40) {
            canvas.drawLine(verLineX.toFloat(), 0f, verLineX.toFloat() + 2, height.toFloat(), mLinesPaint)
        }
    }

    private fun drawCenterGrid(canvas: Canvas) {
        canvas.drawLine(0f, (height.toFloat() / 2), width.toFloat(), (height.toFloat() / 2), mLinesPaint)
        canvas.drawLine((width.toFloat() / 2), 0f, (width.toFloat() / 2), height.toFloat(), mLinesPaint)
    }

    private fun getCarBoundBottom() = centerPosY - getHalf(carHeight)

    private fun getCarBoundRight() = centerPosX + getHalf(carWidth)

    private fun getCarBoundTop() = centerPosY + getHalf(carHeight)

    private fun getCarBoundLeft() = (centerPosX - getHalf(carWidth))
}