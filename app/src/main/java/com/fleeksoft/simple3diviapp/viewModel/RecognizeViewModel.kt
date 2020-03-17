package com.fleeksoft.tmaattendanceviewModel;

import androidx.lifecycle.ViewModel
import com.fleeksoft.tmaattendancelocal.PersonDao
import com.vdt.face_recognition.sdk.Recognizer
import com.vdt.face_recognition.sdk.VideoWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*


class RecognizeViewModel : ViewModel() {

    private val distance_threshold = 8800f

    private lateinit var personDao: PersonDao;
    private lateinit var recognizer: Recognizer

    fun createInstance(personDao: PersonDao, recognizer: Recognizer) {
        this.personDao = personDao
        this.recognizer = recognizer

    }

    suspend fun initVUElements(): Vector<VideoWorker.DatabaseElement> = withContext(Dispatchers.IO) {
        val vw_elements = Vector<VideoWorker.DatabaseElement>()
        val registered = personDao.getAll().toMutableList()
        registered.forEach { employee ->
                val template = recognizer.loadTemplate(ByteArrayInputStream(employee.personFaceTemplate ))
                vw_elements.add(VideoWorker.DatabaseElement(1, employee.personId, template, distance_threshold))


        }
        return@withContext vw_elements
    }




}

