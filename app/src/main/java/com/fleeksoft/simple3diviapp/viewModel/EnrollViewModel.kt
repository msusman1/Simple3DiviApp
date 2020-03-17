package com.fleeksoft.tmaattendanceviewModel;

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleeksoft.tmaattendancelocal.PersonDao
import com.fleeksoft.tmaattendancelocal.PersonModel
import com.vdt.face_recognition.sdk.*
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*


class EnrollViewModel() : ViewModel() {


    private val distance_threshold = 8800f
    var allPersons = listOf<PersonModel>()
    private lateinit var personDao: PersonDao;
    private lateinit var recognizerWithProcessor: Recognizer
    private lateinit var recognizerWithMatcher: Recognizer

    fun initilize(
            personDao: PersonDao,
            recognizerWithProcessor: Recognizer,
            recognizerWithMatcher: Recognizer
    ) {
        this.personDao = personDao
        this.recognizerWithProcessor = recognizerWithProcessor
        this.recognizerWithMatcher = recognizerWithMatcher
    }

    private val _personRegistertionResult = MutableLiveData<Result<String>>()
    val personRegistertionResult: LiveData<Result<String>>
        get() = _personRegistertionResult

    fun registerWorkerFace(newPersonName: String, rawSample: RawSample) {
        lateinit var newPersonModel: PersonModel
        kotlin.runCatching {
            val template = recognizerWithProcessor.processing(rawSample)
            if (templatesIndex != null) {
                val searchResult = recognizerWithMatcher.search(template, templatesIndex, 10, Recognizer.SearchAccelerationType.SEARCH_ACCELERATION_1)
                val filtered = searchResult.filter {
                    it.matchResult.distance < distance_threshold
                }
                val matchedPerson = filtered.map {
                    allPersons.get(it.i.toInt())
                }.firstOrNull()
                if (matchedPerson != null) {   // face allready registered  just update its name
                    matchedPerson.personName=newPersonName
                    viewModelScope.launch { personDao.update(matchedPerson) }
                }else{
                    val outStream = ByteArrayOutputStream()
                    template.save(outStream)
                    newPersonModel= PersonModel(personName = newPersonName,personFaceTemplate = outStream.toByteArray())
                    viewModelScope.launch { personDao.insert(newPersonModel) }
                }
            } else {
                val outStream = ByteArrayOutputStream()
                template.save(outStream)
                newPersonModel= PersonModel(personName = newPersonName,personFaceTemplate = outStream.toByteArray())
                viewModelScope.launch { personDao.insert(newPersonModel) }
            }

        }.onSuccess {
            _personRegistertionResult.value = Result.success("Registered successfully")
            prepareTemplateIndex()
        }.onFailure {
            _personRegistertionResult.value = Result.failure(Throwable(it.message))
        }

    }


    var templatesIndex: TemplatesIndex? = null
    fun prepareTemplateIndex() {
        viewModelScope.launch {
            allPersons = personDao.getAll()
            if (allPersons.size > 0) {
                val allTemplates = Vector<Template>()
                allPersons.forEach {
                    allTemplates.addElement(recognizerWithMatcher.loadTemplate(ByteArrayInputStream(it.personFaceTemplate)))
                }
                templatesIndex = recognizerWithProcessor.createIndex(allTemplates, 1)

            }
        }

    }
}

