package com.fleeksoft.simple3diviapp.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.fleeksoft.simple3diviapp.R
import com.fleeksoft.simple3diviapp.showNameEnterDialog
import com.fleeksoft.simple3diviapp.snackError
import com.fleeksoft.simple3diviapp.util.CustomCamera
import com.fleeksoft.simple3diviapp.util.TheCameraPainter
import com.fleeksoft.tmaattendancelocal.AppDatabase
import com.fleeksoft.tmaattendanceviewModel.EnrollViewModel
import com.vdt.face_recognition.sdk.*
import io.fotoapparat.characteristic.LensPosition
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class EnrollActivity : DiviBaseActivity(), TheCameraPainter {

    val TAG = "EnrollActivity"
    private lateinit var service: FacerecService
    private var capturer: Capturer? = null
    private var faceQualityEstimator: FaceQualityEstimator? = null
    private val registerViewModel by viewModels<EnrollViewModel>()
    val mDatabase by lazy { AppDatabase.getInstance(this) }
    private var bestFaceQuality = -Float.MAX_VALUE
    private var capturedSample: RawSample? = null
    var isSdkReady = false
    var cameraPaused = false
    private val faceDetectedObservable = MutableLiveData<Boolean>(false)
    lateinit var customCamera: CustomCamera
    private val id2le = HashMap<Int, LivenessEstimator>()


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        setBackArrowEnabled(toolbar, "Enroll Person")
        customCamera = CustomCamera(this, camera_view, this)
        lifecycle.addObserver(customCamera)
        lifecycleScope.launch {
            initService()
            isSdkReady = true
            layout_progress.visibility = View.GONE
        }
        faceDetectedObservable.observe(this, androidx.lifecycle.Observer { camera_capture_button.isEnabled = it })
        registerViewModel.personRegistertionResult.observe(this, androidx.lifecycle.Observer {
            it.onSuccess {
                layout_progress.visibility = View.GONE
                layout_register_success.visibility = View.VISIBLE
                text_success.text = it
                cameraPaused = false
                customCamera.resume()
            }.onFailure {
                layout_progress.visibility = View.GONE
                layout_register_error.visibility = View.VISIBLE
                text_error.text = it.message ?: "Unkown Error"
                cameraPaused = false
                customCamera.resume()
            }
        })
        initClickListener()
    }

    private fun initClickListener() {
        camera_capture_button.setOnClickListener {
            if (capturedSample != null) {
                cameraPaused = true
                customCamera.pause()
                showNameEnterDialog{ personName->
                    if (personName != null) {
                        layout_progress.visibility = View.VISIBLE
                        text_progress.text = "Enrolling,  Please Wait..."
                        registerViewModel.registerWorkerFace(personName, capturedSample!!)
                    }else{
                        cameraPaused = false
                        customCamera.resume()
                    }
                }

            } else {
                snackError("Please focus camera on face.")
            }
        }
        camera_switch_button.setOnClickListener {
            lifecycleScope.launch {
                isSdkReady = false
                cameraPaused = true
                frame_layout.clearCanvas()
                id2le.clear()
                customCamera.toggleCamera()
                layout_progress.visibility = View.VISIBLE
                text_progress.text = "Initializing,  Please Wait..."
                initService()
                isSdkReady = true
                cameraPaused = false
                layout_progress.visibility = View.GONE
            }
        }

        button_register_success.setOnClickListener {
            layout_register_success.visibility = View.GONE
            bestFaceQuality = -Float.MAX_VALUE
            capturedSample = null
            customCamera.resume()
            cameraPaused = false
        }
        button_register_error.setOnClickListener {
            layout_register_error.visibility = View.GONE
            customCamera.resume()
            cameraPaused = false
        }

    }

    suspend fun initService() = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            service = FacerecService.createService(dllNativePath, confDirPath, licenseDirPath)
            val capturer_conf = service.Config("fda_tracker_capturer.xml")
            capturer_conf.overrideParameter("downscale_rawsamples_to_preferred_size", 0.0)
            capturer_conf.overrideParameter("base_angle", if (customCamera.lensPosition == LensPosition.Front) 2.0 else 1.0)
            capturer = service.createCapturer(capturer_conf)
            faceQualityEstimator = service.createFaceQualityEstimator("face_quality_estimator.xml")
            val recognizerWithProcessor = service.createRecognizer("method8v7_recognizer.xml", true, false, false)
            val recognizerWithMatcher = service.createRecognizer("method8v7_recognizer.xml", false, true, false)
            registerViewModel.initilize(mDatabase.employeeDao(), recognizerWithProcessor, recognizerWithMatcher)
            registerViewModel.prepareTemplateIndex()
        }.onFailure {
            onException(it.message ?: "Unknown Error")
        }
    }


    override fun processingImage(data: ByteArray, width: Int, height: Int) { // get RawImage
        Log.d(TAG, "processingImage, $width, $height")
        if (isSdkReady.not() || cameraPaused) {
            return
        }
        val rawImage = RawImage(width, height, RawImage.Format.FORMAT_YUV_NV21, data)
        kotlin.runCatching {
            val samples = capturer!!.capture(rawImage)
            if (samples.size == 1) {
                samples.firstOrNull()?.let { rawSam ->
                    val id: Int = rawSam.getID()
                    if (!id2le.containsKey(id)) {
                        id2le[id] = service.createLivenessEstimator()
                    }
                    val livenessEstimator = id2le.get(id);
//                    val livenessEstimator = service.createLivenessEstimator();
                    livenessEstimator?.addSample(rawSam)
                    val liveness = livenessEstimator?.estimateLiveness()
                    val live = liveness?.name
                    Log.d(TAG, "livneness size:${id2le.size} id $id , live:$live")
                    runOnUiThread { frame_layout.updateCanvas(Pair(rawSam, liveness)) }
                    faceDetectedObservable.postValue(samples.size == 1)
                    val currentFaceQuality = faceQualityEstimator!!.estimateQuality(rawSam)
                    if (currentFaceQuality > bestFaceQuality) {
                        bestFaceQuality = currentFaceQuality
                        capturedSample = samples.first()
                    }
                }
            } else if (samples.size > 1) {
                runOnUiThread { frame_layout.clearCanvas() }
                bestFaceQuality = -Float.MAX_VALUE
                capturedSample = null
                snackError("Please focus camera on single face.")
            } else {
                runOnUiThread { frame_layout.clearCanvas() }
            }
        }.onFailure {
            onException(it.message ?: "Unkown error")
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        isSdkReady = false
        capturer?.dispose()
        service.dispose()
        faceQualityEstimator?.dispose()
    }
}

