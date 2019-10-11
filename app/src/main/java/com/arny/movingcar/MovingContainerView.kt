package com.arny.movingcar

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.*


class MovingContainerView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var moveDistance: Float = 0.0f
    private var mHasGrid: Boolean
    private var mHasCenterLines: Boolean
    private var mMapWidth: Int
    private var mMapHeight: Int
    private var carBitmap: Bitmap?
    private var mTargetSize: Float = 0f
    private var mCarColor = Color.BLACK
    private var moveAnimator: ValueAnimator? = null
    private var rotateAnimator: ValueAnimator? = null
    private var viewHeight: Int = 0
    private var viewWidth: Int = 0
    private var rotateInPropress: Boolean = false
    private var movingInPropress: Boolean = false
    private var initialTouchY: Float = 0f
    private var initialTouchX: Float = 0f
    private var centerPosY: Int = 0
    private var centerPosX: Int = 0
    private var initAngle: Float = 90f
    private val startAngle: Float = 0f
    private var currentAngle: Float = 0f
    private var bgColor: Int = Color.WHITE
    private val car: Rect
    private var carWidth = 25
    private var carHeight = 100
    private var mMatrix: Matrix
    private val mCarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mCarFPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mLinesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mTargetColor = Color.GREEN
    private val defaultMapSize = 1000
    private var moveSpeed = 1000.0f
    private var borderThick = 6f

    init {
        val typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MovingContainerView)
        bgColor = typedArray.getColor(R.styleable.MovingContainerView_bg_color, Color.WHITE)
        mCarColor = typedArray.getColor(R.styleable.MovingContainerView_car_color, Color.BLACK)
        mTargetColor = typedArray.getColor(R.styleable.MovingContainerView_target_color, Color.GREEN)
        mTargetSize = typedArray.getFloat(R.styleable.MovingContainerView_target_size, 10f)
        mMapWidth = typedArray.getInteger(R.styleable.MovingContainerView_map_width, defaultMapSize)
        mMapHeight = typedArray.getInteger(R.styleable.MovingContainerView_map_height, defaultMapSize)
        mHasCenterLines = typedArray.getBoolean(R.styleable.MovingContainerView_has_center_lines, true)
        mHasGrid = typedArray.getBoolean(R.styleable.MovingContainerView_has_grid, true)
        typedArray.recycle()
        car = Rect()
        mCarFPaint.color = Color.YELLOW;
        mCarPaint.color = mCarColor;
        mLinesPaint.color = Color.LTGRAY;
        mBorderPaint.color = Color.RED;
        mBorderPaint.strokeWidth = borderThick
        targetPaint.color = mTargetColor;
        mLinesPaint.strokeWidth = 4f
        val options = BitmapFactory.Options()
        options.inSampleSize = 2
        carBitmap = BitmapFactory.decodeResource(resources, R.drawable.car, options)
        mMatrix = Matrix()
        initRotateAnimator()
        initPositionAnimator()
    }

    fun clear(callback: () -> Unit = {}) {
        moveAnimator?.cancel()
        centerPosX = getHalf(viewWidth)
        centerPosY = viewHeight - getHalf(viewHeight)
        currentAngle = startAngle
        initAngle = 90f
        targetPaint.strokeWidth = 0f
        invalidate()
        callback.invoke()
    }

    private fun moving() {
        targetPaint.strokeWidth = mTargetSize
        val angles = calcAngles(currentAngle)
        moveSpeed = moveDistance / 2
        val moveTime = (moveDistance / moveSpeed) * 1000//ms
        moveAnimator?.duration = moveTime.roundToLong()
        rotateAnimate(angles.first, angles.second)//simple A version
    }

    private fun moveToNewPosition(initialTouchX: Float, initialTouchY: Float) {
        positionAnimate(initialTouchY.toInt(), initialTouchX.toInt())
    }

    private fun calcAngles(current: Float): Pair<Float, Float> {
        var newCurrent = current
        val x1 = centerPosX - centerPosX
        val y1 = centerPosY - centerPosY
        val from = intArrayOf(x1, y1)
        val x2 = initialTouchX.roundToInt() - centerPosX
        val y2 = (initialTouchY.roundToInt() - centerPosY) * -1
        val target = intArrayOf(x2, y2)
        moveDistance = sqrt((x1 - x2).toDouble().pow(2.0) + (y1 - y2).toDouble().pow(2.0)).toFloat()
        val angle = getAngle(from, target).toFloat()
        val correctAngle = 90f - angle
        val mirrorAngle = getMirrorAngle(angle.toDouble()).toFloat()
        val mirrorAngleCorrect = normalAngle(90.0 - mirrorAngle).toFloat()
        var resultAngle = correctAngle
        val mirrorCurrent = getMirrorAngle(newCurrent.toDouble()).toFloat()
        val fixCurrent = normalAngle(newCurrent.toDouble())
        val Ix = fixCurrent in 0.0..90.0 || (fixCurrent < -270.0 && fixCurrent >= -360.0)
        val IIx = fixCurrent in 270.0..360.0 || (fixCurrent < 0.0 && fixCurrent >= -90.0)
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
        var resultAngleMirror = false
        if ((newCurrent >= 0 || IIx) && right180) {
            resultAngleMirror = true
            resultAngle = mirrorAngleCorrect
        } else if ((newCurrent >= 0 || Ix) && left180) {
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
        rotateAnimator?.duration = 300
        rotateAnimator?.interpolator = AccelerateDecelerateInterpolator()
        rotateAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                rotateInPropress = false
                moveToNewPosition(initialTouchX, initialTouchY)
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

    private fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    private fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    fun getCenterX(): Int {
        val screenWidth = getScreenWidth()
        val centerX = (width.toDouble() / 2) - (screenWidth.toDouble() / 2)
        return centerX.roundToInt()
    }


    fun getCenterY(): Int {
        val screenHeight = getScreenHeight()
        val centerY = (height.toDouble() / 2) - (screenHeight.toDouble() / 2)
        return centerY.roundToInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val desiredWidth = context.getSizeDP(mMapWidth)
        val desiredHeight = context.getSizeDP(mMapHeight)
        val measuredWidth = measureDimension(desiredWidth, widthMeasureSpec)
        val measuredHeight = measureDimension(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun getHalf(size: Number): Int {
        return (size.toDouble() / 2).roundToInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mHasCenterLines) {
            drawCenterGrid(canvas)
        }
        drawBorder(canvas)
        if (mHasGrid) {
            drawGrid(canvas)
        }
        canvas.drawPoint(initialTouchX, initialTouchY, targetPaint)
        car.set(getCarBoundLeft(), getCarBoundTop(), getCarBoundRight(), getCarBoundBottom())
        canvas.rotate(currentAngle, car.exactCenterX(), car.exactCenterY())
        if (carBitmap != null) {
            val scaledWidth = carBitmap?.getScaledWidth(canvas) ?: 0
            val scaledHeight = carBitmap?.getScaledHeight(canvas) ?: 0
            val fl = car.exactCenterX() - (scaledWidth.toFloat() / 2).roundToInt()
            val fl1 = car.exactCenterY() - (scaledHeight.toFloat() / 2).roundToInt()
            canvas.drawBitmap(carBitmap!!, fl, fl1, mCarPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        for (horLineY in 0..height step 40) {
            canvas.drawLine(0f, horLineY.toFloat(), width.toFloat(), horLineY.toFloat(), mLinesPaint)
        }
        for (verLineX in 0..width step 40) {
            canvas.drawLine(verLineX.toFloat(), 0f, verLineX.toFloat(), height.toFloat(), mLinesPaint)
        }
    }

    private fun drawBorder(canvas: Canvas) {
        canvas.drawLine(borderThick, borderThick, width.toFloat() - borderThick, borderThick, mBorderPaint)
        canvas.drawLine(width.toFloat() - borderThick, borderThick, width.toFloat() - borderThick, height.toFloat() - borderThick, mBorderPaint)
        canvas.drawLine(width.toFloat() - borderThick, height.toFloat() - borderThick, borderThick, height.toFloat() - borderThick, mBorderPaint)
        canvas.drawLine(borderThick, height.toFloat(), borderThick, borderThick, mBorderPaint)
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