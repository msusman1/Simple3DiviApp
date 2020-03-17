package com.fleeksoft.simple3diviapp.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.fotoapparat.Fotoapparat
import io.fotoapparat.characteristic.LensPosition
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.selector.ResolutionSelector
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.util.FrameProcessor
import io.fotoapparat.view.CameraView


class CustomCamera(
    private val context: Context,
    private val cameraview: CameraView,
    val painter: TheCameraPainter
) :
    LifecycleObserver {
    val TAG = "CustomCamera2"
    private var fotoapparat: Fotoapparat? = null
    var lensPosition: LensPosition = LensPosition.Front
    private val frameProcessor = object : FrameProcessor {
        override fun invoke(frame: Frame) {
            Log.d(TAG, "size: ${frame.size.width}, ${frame.size.height}, ratio: ${frame.size.aspectRatio}, rotation: ${frame.rotation}")
            painter.processingImage(frame.image, frame.size.width, frame.size.height)
        }
    }

    private val cameraConfiguration: CameraConfiguration = CameraConfiguration(frameProcessor = frameProcessor)

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create() {
        Log.d(TAG, "oncreate")
        fotoapparat = Fotoapparat(
            context,
            cameraview,
            lensPosition = if (lensPosition == LensPosition.Front) front() else back(),
            cameraConfiguration = cameraConfiguration
        )
        fotoapparat?.start()
    }

    fun toggleCamera() {
        fotoapparat?.apply {
            switchTo(lensPosition = if (lensPosition == LensPosition.Front) back().also {
                lensPosition = LensPosition.Back
            } else front().also {
                lensPosition = LensPosition.Front
            }, cameraConfiguration = cameraConfiguration)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun resume() {
        Log.d(TAG, "onStart")
        fotoapparat?.start()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun pause() {
        Log.d(TAG, "onStop")
        fotoapparat?.stop()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        Log.d(TAG, "onDestroy")
    }
}

