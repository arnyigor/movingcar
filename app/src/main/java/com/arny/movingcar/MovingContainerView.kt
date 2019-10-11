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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt


class MovingContainerView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    //    private var bitmap: Bitmap?
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
    private var initAngle: Float = 90f
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
    private val mBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
        mBorderPaint.color = Color.RED;
        mBorderPaint.strokeWidth = 6f
        targetPaint.color = mTargetColor;
        mLinesPaint.strokeWidth = 4f
        path = Path()
        val options = BitmapFactory.Options()
        options.inSampleSize = 2
//        bitmap = BitmapFactory.decodeResource(resources, R.drawable.car, options)
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
        initAngle = 90f
        targetPaint.strokeWidth = 0f
        invalidate()
    }

    private fun moving() {
        targetPaint.strokeWidth = mTargetSize
        val angle = calcAngles(currentAngle)
        val rotateTime = 300L//((1 / diff) * 1000).toLong()
        rotateAnimator?.duration = rotateTime
        rotateAnimate(angle.first, angle.second)
    }

    private fun moveToNewPosition(initialTouchX: Float, initialTouchY: Float) {
        Log.i(MovingContainerView::class.java.simpleName, "moveToNewPosition: centerPosX:$centerPosX,initialTouchX:$initialTouchX")
        Log.i(MovingContainerView::class.java.simpleName, "moveToNewPosition: centerPosY:$centerPosY,initialTouchY:$initialTouchY")
        positionAnimate(initialTouchY.toInt(), initialTouchX.toInt())
    }

    private fun calcAngles(current: Float): Pair<Float, Float> {
        var newCurrent = current
        val from = intArrayOf(centerPosX - centerPosX, centerPosY - centerPosY)
        val target = intArrayOf(
            initialTouchX.roundToInt() - centerPosX,
            (initialTouchY.roundToInt() - centerPosY) * -1
        )
        val angle = getAngle(from, target).toFloat()
        val correctAngle = 90f - angle
        val mirrorAngle = getMirrorAngle(angle.toDouble()).toFloat()
        val mirrorAngleCorrect = normalAngle(90.0 - mirrorAngle).toFloat()
        var resultAngle = correctAngle
        val mirrorCurrent = getMirrorAngle(newCurrent.toDouble()).toFloat()
        val fixCurrent = normalAngle(newCurrent.toDouble())
        val Ix = fixCurrent in 0.0..90.0 || (fixCurrent < -270.0 && fixCurrent >= -360.0)
        val IIx = fixCurrent in 270.0..360.0 || minusII(fixCurrent)
        val IIIx = fixCurrent in 180.0..270.0 || (fixCurrent < -90.0 && fixCurrent >= -180.0)
        val IVx = fixCurrent in 90.0..180.0 || (fixCurrent < -180.0 && fixCurrent >= -270.0)
        val I = angle in 0.0..90.0
        val II = angle in 90.0..180.0
        val III = angle in 180.0..270.0
        val IV = angle in 270.0..360.0
        var diff = abs(newCurrent - correctAngle)
        val right90 = diff < 180 && correctAngle > newCurrent
        val left180 = diff < 180 && correctAngle < newCurrent
        val left90 = diff > 180 && correctAngle > newCurrent
        val right180 = diff > 180 && correctAngle < newCurrent
        if (diff < 0) {
            diff += 360
        }
        if (diff >= 360) {
            diff -= 360
        }
        mirrorAngleCorrect
        correctAngle
        newCurrent
        resultAngle
        var resultAngleMirror = false
        if ((newCurrent > 0 || IIx )&& right180) {
            resultAngleMirror = true
            resultAngle = mirrorAngleCorrect
        }else if ((newCurrent > 0 || Ix) && left180) {
            resultAngleMirror = true
            resultAngle = correctAngle
        }
        if (IIx && right180 && (I || IV)) {
            if (diff > 180 && (!resultAngleMirror || IIx)) {
                newCurrent = mirrorCurrent
            }
        }
        if (IIIx && right180 && (I || IV)) {
            if (diff > 180) {
                newCurrent = mirrorCurrent
            }
        }
        if ((IVx || IIIx) && left90 && (I || II)) {
            if (diff > 180) {
                newCurrent = mirrorCurrent
            }
        }
        return newCurrent to resultAngle
    }

    private fun minusII(angle: Double) = (angle < 0.0 && angle >= -90.0)

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

    private fun rotateAnimate(fromAngle: Float, newAngle: Float) {
        Log.i(MovingContainerView::class.java.simpleName, "rotateAnimate: currentAngle:$fromAngle");
        val rotateProp = PropertyValuesHolder.ofFloat("rotate", fromAngle, newAngle)
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
        drawBorder(canvas)
//        drawGrid(canvas)
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

    private fun drawBorder(canvas: Canvas) {
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, mBorderPaint)
        canvas.drawLine(width.toFloat(), 0f, width.toFloat(), height.toFloat(), mBorderPaint)
        canvas.drawLine(width.toFloat(), height.toFloat(), 0f, height.toFloat(), mBorderPaint)
        canvas.drawLine(0f, height.toFloat(), 0f, 0f, mBorderPaint)
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