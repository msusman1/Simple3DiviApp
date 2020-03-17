package com.fleeksoft.simple3diviapp.util

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import com.vdt.face_recognition.sdk.LivenessEstimator
import com.vdt.face_recognition.sdk.Point
import com.vdt.face_recognition.sdk.RawSample


class CustomView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

      var rawLivenessPair: Pair<RawSample, LivenessEstimator.Liveness?>? = null
//    private var rawAndLivenessList: List<Pair<RawSample, LivenessEstimator.Liveness?>> = emptyList()

    fun clearCanvas() {
        rawLivenessPair = null
//        rawAndLivenessList = emptyList()
        invalidate()
    }

    fun updateCanvas(pair: Pair<RawSample, LivenessEstimator.Liveness?>) {
        rawLivenessPair = pair
        invalidate()
    }

    /*fun updateCanvas(rects: List<Pair<RawSample, LivenessEstimator.Liveness?>>) {
        rawAndLivenessList = rects
        invalidate()
    }*/

    private val paint =
        Paint().apply {
            isAntiAlias = true
            color = Color.RED
            style = Paint.Style.FILL
            strokeWidth = 3f
        }
    val screenSize by lazy {
        val displayMetrics = DisplayMetrics()
        (context as Activity).getWindowManager()
            .getDefaultDisplay()
            .getMetrics(displayMetrics)
        displayMetrics
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (rawLivenessPair == null) {
             //clear canvas here
        }
        rawLivenessPair?.let { rawLivenessPair ->
            val it = rawLivenessPair.first.rectangle
            val liveness = rawLivenessPair.second
            Log.d("CustomView", "rect: ${it.x}, ${it.y}, ${it.width}, ${it.height}, canvasSize: ${canvas?.width}, ${canvas?.height}, viewSize: ${width}, ${height}")
            val left = screenSize.widthPixels - it.y
            val top = screenSize.heightPixels - it.x
            val right = screenSize.widthPixels - it.y + (it.width / 2)
            val bottom = screenSize.heightPixels - it.x + (it.height / 2)
            Log.d("CustomView", "left: $left, top: $top, right:$right, bottom: $bottom")
            canvas?.drawCircle(left.toFloat(), top.toFloat(), 15f, paint.apply {
                color = if (liveness != null && liveness == LivenessEstimator.Liveness.REAL) Color.GREEN else Color.RED
            })
        }

        /*  rawAndLivenessList.forEach { (samp, liveness) ->
              val it = samp.rectangle
              Log.d("CustomView", "rect: ${it.x}, ${it.y}, ${it.width}, ${it.height}, canvasSize: ${canvas?.width}, ${canvas?.height}, viewSize: ${width}, ${height}")
              val left = screenSize.widthPixels - it.y
              val top = screenSize.heightPixels - it.x
              val right = screenSize.widthPixels - it.y + it.width
              val bottom = screenSize.heightPixels - it.x + it.height
              Log.d("CustomView", "left: $left, top: $top, right:$right, bottom: $bottom")
              canvas?.drawCircle(left.toFloat(), top.toFloat(), 15f, paint.apply {
                  color = if (liveness != null && liveness == LivenessEstimator.Liveness.REAL) Color.GREEN else Color.RED
              })
          }*/


    }


}