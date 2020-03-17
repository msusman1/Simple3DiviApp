package com.fleeksoft.simple3diviapp.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.fleeksoft.simple3diviapp.R
import com.fleeksoft.simple3diviapp.util.CustomCamera
import com.fleeksoft.simple3diviapp.util.TheCameraPainter
import com.fleeksoft.tmaattendancelocal.AppDatabase
import com.fleeksoft.tmaattendanceviewModel.RecognizeViewModel
import com.vdt.face_recognition.sdk.*
import io.fotoapparat.characteristic.LensPosition
import kotlinx.android.synthetic.main.activity_checkin.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class RecognizeActivity : DiviBaseActivity(), TheCameraPainter, VideoWorker.TrackingLostCallbackU,
    VideoWorker.TrackingCallbackU, VideoWorker.MatchFoundCallbackU {
    val TAG = "RecognizeActivity"
    private var service: FacerecService? = null
    private var recognizer: Recognizer? = null
    private val checkinViewModel by viewModels<RecognizeViewModel>()
    val threshHold = 8800f
    val methodRecognizer = "method8v7_recognizer.xml"
    private val stream_id = 0
    private var videoWorker: VideoWorker? = null
    val mAppDatabase by lazy { AppDatabase.getInstance(this) }
    val faceMatched = mutableSetOf<Int>()
    private val id2le = HashMap<Int, LivenessEstimator>()

    var isSdkReady = false
    lateinit var customCamera: CustomCamera
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkin)
        setBackArrowEnabled(toolbar, "Recognize person")
        customCamera = CustomCamera(this, camera_view, this)
        lifecycle.addObserver(customCamera)
        camera_switch_button.setOnClickListener {
            lifecycleScope.launch {
                isSdkReady = false
                id2le.clear()
                frame_layout.clearCanvas()
                layout_progress.visibility = View.VISIBLE
                customCamera.toggleCamera()
                initService()
                isSdkReady = true
                layout_progress.visibility = View.GONE
            }
        }
        lifecycleScope.launch {
            initService()
            isSdkReady = true
            layout_progress.visibility = View.GONE
        }
        image_close.setOnClickListener {
            layout_worker_attendance.visibility = View.GONE
        }
    }


    suspend fun initService() = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            service = FacerecService.createService(dllNativePath, confDirPath, licenseDirPath)
            val videoWorkerConfig =
                service!!.Config("video_worker_fdatracker.xml")  //    video_worker_fdatracker.w.l2d.xml
                    .overrideParameter("search_k", 10.0)
                    .overrideParameter("downscale_rawsamples_to_preferred_size", 0.0)
//              .overrideParameter("depth_data_flag", 1.0)
                    .overrideParameter(
                        "base_angle",
                        if (customCamera.lensPosition == LensPosition.Front) 2.0 else 1.0
                    )
            videoWorker = service!!.createVideoWorker(
                VideoWorker.Params()
                    .video_worker_config(videoWorkerConfig)
                    .recognizer_ini_file(methodRecognizer)
                    .streams_count(1)
                    .processing_threads_count(1)
                    .matching_threads_count(1)
                    .short_time_identification_enabled(true)
                    .short_time_identification_distance_threshold(threshHold)
                    .short_time_identification_outdate_time_seconds(5f)
            )
            videoWorker!!.addTrackingCallbackU(this@RecognizeActivity)
            videoWorker!!.addTrackingLostCallbackU(this@RecognizeActivity)
            videoWorker!!.addMatchFoundCallbackU(this@RecognizeActivity)
            recognizer = service!!.createRecognizer(methodRecognizer, false, false, false)
            checkinViewModel.createInstance(mAppDatabase.employeeDao(), recognizer!!)
            val vw_elements = checkinViewModel.initVUElements()
            videoWorker!!.setDatabase(
                vw_elements,
                Recognizer.SearchAccelerationType.SEARCH_ACCELERATION_1
            )
        }.onFailure {
            onException(it.message ?: "Unknown Error")
        }


    }


    override fun processingImage(data: ByteArray, width: Int, height: Int) {
        if (isSdkReady) kotlin.runCatching {
                videoWorker?.addVideoFrame(
                    RawImage(
                        width,
                        height,
                        RawImage.Format.FORMAT_YUV_NV21,
                        data
                    ), stream_id
                )
            }
            .onFailure { onException(it.message ?: "Unknown error") }
    }

    override fun call(data: TrackingLostCallbackData) {
        Log.d(TAG, "addTrackingLostCallbackU person id:${data.track_id}")
        faceMatched.remove(data.track_id)
        if (faceMatched.size == 0) {
            runOnUiThread { layout_worker_attendance.visibility = View.GONE }
        }
        runOnUiThread { frame_layout.clearCanvas() }

    }

    override fun call(data: TrackingCallbackData) {
        val samples = data.samples
        if (samples.size == 1) {
            val rawSample = samples.firstElement()
            val id: Int = rawSample.getID()
            if (!id2le.containsKey(id)) {
                id2le[id] = service!!.createLivenessEstimator()
            }
            val livenessEstimator = id2le.get(id);
            livenessEstimator?.addSample(rawSample)
            val liveness = livenessEstimator?.estimateLiveness()
            Log.d(TAG, "addTrackingCallbackU samples:${samples.size} liveness:${liveness}")
            runOnUiThread {
                text_multifaces.visibility = View.GONE;frame_layout.updateCanvas(
                Pair(
                    rawSample,
                    liveness
                )
            )
            }
        } else if (samples.size > 1) {
            Log.d(TAG, "addTrackingCallbackU more:${samples.size}")
            runOnUiThread {
                frame_layout.clearCanvas()
                text_multifaces.visibility = View.VISIBLE
                text_multifaces.text = "Please focus camera on single face."
            }
        } else {
            runOnUiThread { text_multifaces.visibility = View.GONE;frame_layout.clearCanvas() }
        }
    }

    override fun call(data: MatchFoundCallbackData) {
        Log.d(TAG, "addMatchFoundCallbackU")
        val personIdsListFound = data.search_result.map { it.person_id }.toSet()
        lifecycleScope.launch {
            val personsFaceMatched = personIdsListFound
                .map { mAppDatabase.employeeDao().getById(it) }
                .filterNotNull()
            faceMatched.add(data.sample.id)
            runOnUiThread {
                if (personsFaceMatched.size > 0) {
                    layout_worker_attendance.visibility = View.VISIBLE
                    text_attendance_worker.text =
                        personsFaceMatched.map { it.personName }.joinToString()
                } else {
                    layout_worker_attendance.visibility = View.GONE
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        isSdkReady = false
        service?.dispose()
        recognizer?.dispose()
        videoWorker?.dispose()
    }
}